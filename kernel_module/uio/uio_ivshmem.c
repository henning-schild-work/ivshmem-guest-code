/*
 * UIO IVShmem Driver
 *
 * (C) 2009 Cam Macdonell
 * (C) 2014 Henning Schild
 * (C) 2017 Andreas Rollbühler
 * based on Hilscher CIF card driver (C) 2007 Hans J. Koch <hjk@linutronix.de>
 *
 * Licensed under GPL version 2 only.
 *
 */

#include <linux/device.h>
#include <linux/module.h>
#include <linux/pci.h>
#include <linux/uio_driver.h>
#include <linux/io.h>
#include <linux/version.h>

#define IntrStatus 0x04
#define IntrMask 0x00

#define JAILHOUSE_CFG_SHMEM_PTR	0x40
#define JAILHOUSE_CFG_SHMEM_SZ	0x48

struct ivshmem_info {
	struct uio_info *uio;
	struct pci_dev *dev;
	char (*msix_names)[256];
	struct msix_entry *msix_entries;
	int nvectors;
};

static irqreturn_t ivshmem_handler(int irq, struct uio_info *dev_info)
{

	void __iomem *plx_intscr = dev_info->mem[0].internal_addr
					+ IntrStatus;
	u32 val;

	val = readl(plx_intscr);
	if (val == 0)
		return IRQ_NONE;

	return IRQ_HANDLED;
}

static irqreturn_t ivshmem_msix_handler(int irq, void *opaque)
{

	struct uio_info *dev_info = (struct uio_info *) opaque;

	/* we have to do this explicitly when using MSI-X */
	uio_event_notify(dev_info);
	return IRQ_HANDLED;
}

static void free_msix_vectors(struct ivshmem_info *ivs_info,
							const int max_vector)
{
	int i;
	unsigned int irq;

	for (i = 0; i < max_vector; i++) {
#if LINUX_VERSION_CODE < KERNEL_VERSION(4,8,0)
		irq = ivs_info->msix_entries[i].vector;
#else
		irq = pci_irq_vector(ivs_info->dev, ivs_info->msix_entries[i].entry);
#endif
		free_irq(irq, ivs_info->uio);
	}
}

static int request_msix_vectors(struct ivshmem_info *ivs_info, int nvectors)
{
	int i, err;
	unsigned int irq;
	const char *name = "ivshmem";

	ivs_info->nvectors = nvectors;

	ivs_info->msix_entries = kmalloc(nvectors *
					 sizeof(*ivs_info->msix_entries),
					 GFP_KERNEL);
	if (ivs_info->msix_entries == NULL)
		return -ENOSPC;

	ivs_info->msix_names = kmalloc(nvectors * sizeof(*ivs_info->msix_names),
				       GFP_KERNEL);
	if (ivs_info->msix_names == NULL) {
		kfree(ivs_info->msix_entries);
		return -ENOSPC;
	}

	for (i = 0; i < nvectors; ++i)
		ivs_info->msix_entries[i].entry = i;

#if LINUX_VERSION_CODE < KERNEL_VERSION(4,8,0)
	err = pci_enable_msix(ivs_info->dev, ivs_info->msix_entries,
					ivs_info->nvectors);
#else
	err = pci_alloc_irq_vectors(ivs_info->dev, 1, ivs_info->nvectors,
			PCI_IRQ_MSIX);
#endif
	if (err > 0) {
		ivs_info->nvectors = err;
#if LINUX_VERSION_CODE < KERNEL_VERSION(4,8,0)
		err = pci_enable_msix(ivs_info->dev, ivs_info->msix_entries,
					ivs_info->nvectors);
#endif
	}

	if (err < 0) {
		dev_info(&ivs_info->dev->dev,
			 "no MSI (%d). Back to INTx.\n", err);
		goto error;
	}

	for (i = 0; i < ivs_info->nvectors; i++) {

		snprintf(ivs_info->msix_names[i], sizeof(*ivs_info->msix_names),
			"%s-config", name);

#if LINUX_VERSION_CODE < KERNEL_VERSION(4,8,0)
		irq = ivs_info->msix_entries[i].vector;
#else
		irq = pci_irq_vector(ivs_info->dev, ivs_info->msix_entries[i].entry);
#endif
		err = request_irq(irq, ivshmem_msix_handler, 0,
				ivs_info->msix_names[i], ivs_info->uio);

		if (err) {
			free_msix_vectors(ivs_info, i - 1);
			goto error;
		}
	}

	return 0;
error:
	kfree(ivs_info->msix_entries);
	kfree(ivs_info->msix_names);
	ivs_info->nvectors = 0;
#if LINUX_VERSION_CODE >= KERNEL_VERSION(4,8,0)
	pci_free_irq_vectors(ivs_info->dev);
#endif
	return err;

}

static int ivshmem_pci_probe(struct pci_dev *dev,
					const struct pci_device_id *id)
{
	struct uio_info *info;
	struct ivshmem_info *ivshmem_info;
	int nvectors = 4;

	info = kzalloc(sizeof(struct uio_info), GFP_KERNEL);
	if (!info)
		return -ENOMEM;

	ivshmem_info = kzalloc(sizeof(struct ivshmem_info), GFP_KERNEL);
	if (!ivshmem_info) {
		kfree(info);
		return -ENOMEM;
	}

	if (pci_enable_device(dev))
		goto out_free;

	if (pci_request_regions(dev, "ivshmem"))
		goto out_disable;

	info->mem[0].addr = pci_resource_start(dev, 0);
	if (!info->mem[0].addr)
		goto out_release;

	info->mem[0].size = (pci_resource_len(dev, 0) + PAGE_SIZE - 1)
		& PAGE_MASK;
	info->mem[0].internal_addr = pci_ioremap_bar(dev, 0);
	if (!info->mem[0].internal_addr)
		goto out_release;

	info->mem[0].memtype = UIO_MEM_PHYS;
	info->mem[0].name = "registers";

	info->mem[1].size = pci_resource_len(dev, 2);

	/* if we are dealing with a jailhouse provided ivshmem device the
	 * memory bar will not exist. Location and size will be in custom
	 * config space registers instead */
	if (info->mem[1].size == 0) {
		pci_read_config_dword(dev, JAILHOUSE_CFG_SHMEM_PTR,
			(u32*)&info->mem[1].addr);
		pci_read_config_dword(dev, JAILHOUSE_CFG_SHMEM_PTR + 4,
			((u32*)&info->mem[1].addr) + 1);
		pci_read_config_dword(dev, JAILHOUSE_CFG_SHMEM_SZ,
			(u32*)&info->mem[1].size);
		pci_read_config_dword(dev, JAILHOUSE_CFG_SHMEM_SZ + 4,
			((u32*)&info->mem[1].size) + 1);
		dev_info(&dev->dev, "using jailhouse mode\n");
	} else {
		info->mem[1].addr = pci_resource_start(dev, 2);
	}
	if (!info->mem[1].addr)
		goto out_unmap;

	info->mem[1].memtype = UIO_MEM_PHYS;
	info->mem[1].name = "shmem";

	ivshmem_info->uio = info;
	ivshmem_info->dev = dev;

	if (request_msix_vectors(ivshmem_info, nvectors) != 0) {
		dev_info(&ivshmem_info->dev->dev, "regular IRQs\n");
		info->irq = dev->irq;
		info->irq_flags = IRQF_SHARED;
		info->handler = ivshmem_handler;
		writel(0xffffffff, info->mem[0].internal_addr + IntrMask);
	} else {
		dev_info(&ivshmem_info->dev->dev, "MSI-X enabled\n");
		pci_set_master(dev);
		info->irq = -1;
	}

	info->name = "ivshmem";
	info->version = "0.0.1";

	if (uio_register_device(&dev->dev, info))
		goto out_unmap;

	pci_set_drvdata(dev, ivshmem_info);

	return 0;
out_unmap:
	iounmap(info->mem[0].internal_addr);
out_release:
	pci_release_regions(dev);
out_disable:
	pci_disable_device(dev);
out_free:
	kfree(ivshmem_info);
	kfree(info);
	return -ENODEV;
}

static void ivshmem_pci_remove(struct pci_dev *dev)
{
	struct ivshmem_info *ivshmem_info = pci_get_drvdata(dev);
	struct uio_info *info = ivshmem_info->uio;

	pci_set_drvdata(dev, NULL);
	uio_unregister_device(info);
	if (ivshmem_info->nvectors) {
		free_msix_vectors(ivshmem_info, ivshmem_info->nvectors);
		pci_disable_msix(dev);
		kfree(ivshmem_info->msix_entries);
		kfree(ivshmem_info->msix_names);
#if LINUX_VERSION_CODE >= KERNEL_VERSION(4,8,0)
		pci_free_irq_vectors(ivshmem_info->dev);
#endif
	}
	iounmap(info->mem[0].internal_addr);
	pci_release_regions(dev);
	pci_disable_device(dev);
	kfree(info);
	kfree(ivshmem_info);
}

static struct pci_device_id ivshmem_pci_ids[] = {
	{
		.vendor =	0x1af4,
		.device =	0x1110,
		.subvendor =	PCI_ANY_ID,
		.subdevice =	PCI_ANY_ID,
	},
	{ 0, }
};

static struct pci_driver ivshmem_pci_driver = {
	.name = "uio_ivshmem",
	.id_table = ivshmem_pci_ids,
	.probe = ivshmem_pci_probe,
	.remove = ivshmem_pci_remove,
};

module_pci_driver(ivshmem_pci_driver);
MODULE_DEVICE_TABLE(pci, ivshmem_pci_ids);
MODULE_LICENSE("GPL v2");
MODULE_AUTHOR("Cam Macdonell");
