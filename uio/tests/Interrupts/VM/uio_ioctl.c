#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <fcntl.h>
#include <string.h>
#include <openssl/sha.h>
#include <unistd.h>
#include <sys/stat.h>
#include <errno.h>
#include "ivshmem.h"

int main(int argc, char ** argv){

    long length;
    void * memptr;
    unsigned short * short_array;
    int i,fd,j, k;
    struct test * myptr;
    int other;
    short msg, cmd, dest;

    if (argc != 5) {
        printf("USAGE: uio_ioctl <filename> <size in bytes> <cmd> <dest>\n");
        exit(-1);
    }

    fd=open(argv[1], O_RDWR);
    printf("[UIO] opening file %s\n", argv[1]);
    length=atol(argv[2]);
    cmd = (unsigned short) atoi(argv[3]);
    dest = (unsigned short) atoi(argv[4]);

    printf("[UIO] length is %d\n", length);
    printf("[UIO] size of short %d\n", sizeof(unsigned short));

    if ((memptr = mmap(NULL, length, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0)) == -1){
        printf("mmap failed (0x%x)\n", memptr);
        close (fd);
        exit (-1);
    }

    short_array = (unsigned short *)memptr;

    msg = ((dest & 0xff) << 8) + (cmd & 0xff);

    printf("[UIO] writing %u\n", msg);
    short_array[16] = msg;

//    printf("md is *%20s*\n", md);

    munmap(memptr, length);
    close(fd);

    printf("[UIO] Exiting...\n");
}
