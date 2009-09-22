#ifndef IVSHMEM_HDR
#define IVSHMEM_HDR
enum ivshmem_ioctl { SET_SEMA, DOWN_SEMA, SEMA_IRQ, WAIT_EVENT, WAIT_EVENT_IRQ };

int ivshmem_send(int fd, int ivshmem_cmd, int destination_vm);
int ivshmem_print_opts(void);

extern char banana[];
#endif
