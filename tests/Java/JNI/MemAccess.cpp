// MemAccess.cpp:
#include "MemAccess.h"
#include <stdlib.h>
#include <pthread.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <fcntl.h>
#include <string.h>
#include <openssl/sha.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <errno.h>

int global_fd;

JNIEXPORT jobject JNICALL Java_MemAccess_getGeneratedMem(JNIEnv * env, jobject obj) {
    int sz = 16 * 1024 * 1024;
    int i, nbr;

    nbr = sz / sizeof(long);
    long * buffer = new long[nbr]; // c++ buffer

    global_fd=open("/dev/ivshmem", O_RDWR);
    printf("fd is %d\n", global_fd);

    if ((buffer = (long *)mmap(NULL, sz, PROT_READ|PROT_WRITE, MAP_SHARED, global_fd, 0)) == (long *)(caddr_t)-1){ 
        printf("mmap failed (0x%x)\n", buffer);
        close (global_fd);
        exit (-1);
    }


    for (i = 0; i < 16; i++)
        printf("%ld\n", buffer[i]);
    printf("%ld\n", buffer[nbr-1]);
    printf("**************\n");

    // return a reference of a (Java) object that holds a pointer to the c++ buffer
    return env->NewDirectByteBuffer(buffer, nbr * sizeof(long));
}

JNIEXPORT void JNICALL Java_MemAccess_closeGeneratedMem(JNIEnv * env, jobject obj) {

    printf("global_fd is %d\n", global_fd);
    close(global_fd);

}
