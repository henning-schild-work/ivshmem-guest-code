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
#include <semaphore.h>
#include "ivshmem.h"

#define CHUNK_SZ  (16*1024*1024)
#define NEXT(i)   ((i + 1) % 15)
#define OFFSET(i) ((i + 1) * CHUNK_SZ)

int main(int argc, char ** argv){

    int ivfd, ffd;
    int sender;
    void * memptr;
    char * copyfrom;
    int idx, recvd, total;
    int dbg;

    sem_t *full, *empty;

    if (argc != 4){
        printf("USAGE: ftp_recv <ivshmem_device> <file> <sender>\n");
        exit(-1);
    }

    sender = atoi(argv[3]);

    if((ffd = open(argv[2], O_WRONLY|O_CREAT, 0644)) == -1) {
        printf("could not open file\n");
        exit(-1);
    }

    ivfd = open(argv[1], O_RDWR);

    if ((memptr = mmap(NULL, 16*CHUNK_SZ, PROT_READ|PROT_WRITE, MAP_SHARED, ivfd, 0)) == MAP_FAILED){
        printf("mmap failed (0x%p)\n", memptr);
        close(ivfd);
        close(ffd);
        exit (-1);
    }

    copyfrom = (char *)memptr;

    /* Get the filesize */
    printf("[RECV] waiting for size from %d\n", sender);
    ivshmem_send(ivfd, WAIT_EVENT, sender);
    memcpy((void*)&total, (void*)copyfrom, sizeof(int));
    /* We got the size! */
    printf("[RECV] got size %d, notifying\n", total);
    ivshmem_send(ivfd, WAIT_EVENT_IRQ, sender);

    full = (sem_t *)copyfrom;
    empty = (sem_t *)(copyfrom + sizeof(sem_t));

    for(idx = recvd = 0; recvd < total; idx = NEXT(idx)) {
        printf("[RECV] waiting for block notification\n");
        sem_getvalue(full, &dbg);
        printf("[RECV] full is %d\n", dbg);
        sem_wait(full);
        printf("[RECV] recieving bytes in block %d\n", idx);
        write(ffd, copyfrom + OFFSET(idx), CHUNK_SZ);
        recvd += CHUNK_SZ;
        printf("[RECV] block received, notifying sender. recvd size now %d\n", recvd);
        sem_post(empty);
    }

    ftruncate(ffd, total);

    munmap(memptr, 16*CHUNK_SZ);
    close(ffd);
    close(ivfd);
}
