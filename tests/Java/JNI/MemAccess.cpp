// MemAccess.cpp:
#include "MemAccess.h"

JNIEXPORT jobject JNICALL Java_MemAccess_getGeneratedMem(JNIEnv * env, jobject obj) {
    int nbr = 16;
    int * buffer = new int[nbr]; // c++ buffer
    int i;

    for (i = 0; i < nbr; i++)
        buffer[i] = i * 2;

    // return a reference of a (Java) object that holds a pointer to the c++ buffer
    return env->NewDirectByteBuffer(buffer, nbr * sizeof(int));
}

