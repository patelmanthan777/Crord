/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
#include "ffmpeg.h"
#include "libavutil/log.h"

#ifndef _Included_me_s1rius_ffmpeglib_FFmpegHelper
#define _Included_me_s1rius_ffmpeglib_FFmpegHelper
#ifdef __cplusplus
extern "C" {
#endif

#include <android/log.h>

#define LOGTAG "ffmpeghelper"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOGTAG, __VA_ARGS__)

void my_log_callback(void *ptr, int level, const char *fmt, va_list vargs)
{
    char buf[1024];
    char string[256];
    vsprintf(buf, fmt, vargs);
    LOGV("%s", buf);
}

/*
 * Class:     me_s1rius_ffmpeglib_FFmpegHelper
 * Method:    run
 * Signature: ([Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_me_s1rius_ffmpeglib_FFmpegHelper_run
  (JNIEnv *env, jclass obj, jobjectArray commands){

    av_log_set_level(AV_LOG_ERROR);
    av_log_set_callback(my_log_callback);

    int i = 0;
    int argc = 0;
    char **argv = NULL;
    jstring *strr = NULL;

    if (commands != NULL) {
        argc = (*env)->GetArrayLength(env, commands);
        argv = (char **) malloc(sizeof(char *) * argc);
        strr = (jstring *) malloc(sizeof(jstring) * argc);

        for(i=0;i<argc;i++)
        {
            strr[i] = (jstring)(*env)->GetObjectArrayElement(env, commands, i);
            argv[i] = (char *)(*env)->GetStringUTFChars(env, strr[i], 0);
        }
    }

    int ret = run(argc, argv);

    for(i=0;i<argc;i++)
    {
        (*env)->ReleaseStringUTFChars(env, strr[i], argv[i]);
    }
    free(argv);
    free(strr);

    return ret;
}

#ifdef __cplusplus
}
#endif
#endif
