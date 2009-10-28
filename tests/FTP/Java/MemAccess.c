#include <unistd.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <pthread.h>

#include <jni.h>

#include "MemAccess.h"
#include "ivshmem.h"

#define CHUNK_SZ (16*1024*1024)

static int fd;
char *mem;

JNIEXPORT jint JNICALL Java_MemAccess_openDevice (JNIEnv *env, jobject obj, jstring devname) {
    const char *fname = (*env)->GetStringUTFChars(env, devname, NULL);
    fd = open(fname, O_RDWR);
    (*env)->ReleaseStringUTFChars(env, devname, fname);

    if((mem = (char *)mmap(NULL, 16 * CHUNK_SZ, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0)) == MAP_FAILED) {
        close(fd);
        return(-1);
    }

    return(0);
}

JNIEXPORT jint JNICALL Java_MemAccess_closeDevice (JNIEnv *env, jobject obj) {
    munmap(mem, 16*CHUNK_SZ);
    close(fd);
    return(0);
}

JNIEXPORT jint JNICALL Java_MemAccess_writeBytes (JNIEnv *env, jobject obj, jbyteArray bytes, jint offset, jint cnt) {
    (*env)->SetByteArrayRegion(env, bytes, offset, cnt, (jbyte *)mem);
    return(0);
}

JNIEXPORT jint JNICALL Java_MemAccess_readBytes (JNIEnv *env, jobject obj, jbyteArray bytes, jint offset, jint cnt) {
    (*env)->GetByteArrayRegion(env, bytes, offset, cnt, (jbyte *)mem);
    return(0);
}

JNIEXPORT jint JNICALL Java_MemAccess_writeInt (JNIEnv *env, jobject obj, jint towrite, jint offset) {
    *((jint *)(mem + offset)) = towrite;
    return(0);
}

JNIEXPORT jint JNICALL Java_MemAccess_readInt (JNIEnv *env, jobject obj, jint offset) {
    return(*((jint *)(mem + offset)));
}

JNIEXPORT jint JNICALL Java_MemAccess_initLock (JNIEnv *env, jobject obj, jint offset) {
    pthread_spinlock_t *sl = (pthread_spinlock_t *)(mem + offset);
    pthread_spin_init(sl, 1);

    return(0);
}

JNIEXPORT jint JNICALL Java_MemAccess_spinLock (JNIEnv *env, jobject obj, jint offset) {
    pthread_spinlock_t *sl = (pthread_spinlock_t *)(mem + offset);
    while(pthread_spin_lock(sl) != 0);

    return(0);
}

JNIEXPORT jint JNICALL Java_MemAccess_spinUnlock (JNIEnv *env, jobject obj, jint offset) {
    pthread_spinlock_t *sl = (pthread_spinlock_t *)(mem + offset);
    pthread_spin_unlock(sl);

    return(0);
}

JNIEXPORT jint JNICALL Java_MemAccess_waitEvent (JNIEnv *env, jobject obj, jint machine) {
    ivshmem_send(fd, WAIT_EVENT, machine);
    return(0);
}

JNIEXPORT jint JNICALL Java_MemAccess_waitEventIrq (JNIEnv *env, jobject obj, jint machine) {
    ivshmem_send(fd, WAIT_EVENT_IRQ, machine);
    return(0);
}
