/*
 * UIO IVShmem Driver
 *
 * (C) 2009 Cam Macdonell
 * (C) 2017 Henning Schild
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

#define IntrStatus 0x04
#define IntrMask 0x00

struct ivshmem_info {
	struct uio_info *uio;
	struct pci_dev *dev;
};

static irqreturn_t ivshmem_handler(int irq, struct uio_info *dev_info)
{

	struct ivshmem_info *ivshmem_info;
	void __iomem *plx_intscr;
	u32 val;

	ivshmem_info = dev_info->priv;

	if (ivshmem_info->dev->msix_enabled) {
		/* we have to do this explicitly when using MSI-X */
		uio_event_notify(dev_info);
		return IRQ_HANDLED;
	}

	plx_intscr = dev_info->mem[0].internal_addr + IntrStatus;
	val = readl(plx_intscr);
	if (val == 0)
		return IRQ_NONE;

	return IRQ_HANDLED;
}

static int ivshmem_pci_probe(struct pci_dev *dev,
					const struct pci_device_id *id)
{
	struct uio_info *info;
	struct ivshmem_info *ivshmem_info;

	info = kzalloc(sizeof(struct uio_info), GFP_KERNEL);
	if (!info)
		return -ENOMEM;

	ivshmem_info = kzalloc(sizeof(struct ivshmem_info), GFP_KERNEL);
	if (!ivshmem_info) {
		kfree(info);
		return -ENOMEM;
	}
	info->priv = ivshmem_info;

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

	if (1 > pci_alloc_irq_vectors(dev, 1, 1,
				      PCI_IRQ_LEGACY | PCI_IRQ_MSIX))
		goto out_vector;

	info->mem[0].memtype = UIO_MEM_PHYS;
	info->mem[0].name = "registers";

	info->mem[1].addr = pci_resource_start(dev, 2);
	if (!info->mem[1].addr)
		goto out_unmap;

	info->mem[1].size = pci_resource_len(dev, 2);
	info->mem[1].memtype = UIO_MEM_PHYS;
	info->mem[1].name = "shmem";

	ivshmem_info->uio = info;
	ivshmem_info->dev = dev;

	if (pci_irq_vector(dev, 0)) {
		info->irq = pci_irq_vector(dev, 0);
		info->irq_flags = IRQF_SHARED;
		info->handler = ivshmem_handler;
	} else {
		dev_warn(&dev->dev, "No IRQ assigned to device: "
			 "no support for interrupts?\n");
	}
	pci_set_master(dev);

	info->name = "uio_ivshmem";
	info->version = "0.0.1";

	if (uio_register_device(&dev->dev, info))
		goto out_unmap;

	if (!dev->msix_enabled)
		writel(0xffffffff, info->mem[0].internal_addr + IntrMask);

	pci_set_drvdata(dev, ivshmem_info);

	return 0;
out_vector:
	pci_free_irq_vectors(dev);
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
	pci_free_irq_vectors(dev);
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
