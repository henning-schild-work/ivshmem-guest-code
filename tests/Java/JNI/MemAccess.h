// MemAccess.h:
#ifndef _Included_MemAccess
#define _Included_MemAccess
 
#include <jni.h>
 
#ifdef __cplusplus
extern "C" {
#endif
 
JNIEXPORT jobject JNICALL Java_MemAccess_getGeneratedMem(JNIEnv * env, jobject obj);
 
#ifdef __cplusplus
}
#endif
 
#endif /* _Included_MemAccess */
