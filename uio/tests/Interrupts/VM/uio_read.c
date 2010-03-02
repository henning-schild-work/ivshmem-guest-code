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

    int i, rv, fd;

    if (argc != 2) {
        printf("USAGE: uio_read <filename>\n");
        exit(-1);
    }

    fd=open(argv[1], O_RDWR);
    printf("[UIO] opening file %s\n", argv[1]);

    printf("[UIO] reading\n");
    rv = read(fd, &i, sizeof(i));

    printf("[UIO] rv is %d\n", rv);

    close(fd);

    printf("[UIO] Exiting...\n");
}
