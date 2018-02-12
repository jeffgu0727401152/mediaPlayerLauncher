/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// OpenGL ES 2.0 code

#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <dlfcn.h>

#define LOG_TAG    "ATC_NATIVE"

extern "C" {
    JNIEXPORT jbyteArray JNICALL Java_com_whiteskycn_wsd_android_NativeCertification_getPkcs12(
		JNIEnv * env, jobject obj);
};

JNIEXPORT jbyteArray JNICALL Java_com_whiteskycn_wsd_android_NativeCertification_getPkcs12(
	JNIEnv * env,
	jobject obj)
{
    void *libHandle = NULL;
    int (*getFunc)(const uint8_t**, uint32_t*) = NULL;
    int (*initFunc)() = NULL;

    const uint8_t* certData = NULL;
    uint32_t certSize = 0;

    int result = 0;

    libHandle = dlopen("/system/lib/libwsdatc.so", RTLD_LAZY);
    if(libHandle)
    {
        initFunc = (int (*)())dlsym(libHandle, "atc_base_env_init");
        getFunc = (int (*)(const uint8_t**, uint32_t*))dlsym(libHandle, "atc_get_ssl_pkcs12_cert");

        if(getFunc && initFunc) {
            initFunc();
            result = getFunc(&certData, &certSize);
        } else {
            __android_log_print(
                    ANDROID_LOG_ERROR,
                    LOG_TAG,
                    "missing some function in system library!");
        }

        dlclose(libHandle);
        libHandle = NULL;
    } else {
        __android_log_print(
                ANDROID_LOG_ERROR,
                LOG_TAG,
                "open system library failed!");
    }

    if (result==0 && certSize>0) {
        jbyteArray array = env->NewByteArray(certSize);
        env->SetByteArrayRegion(array,0,certSize,(const jbyte *)certData);
        return array;
    } else {
        jbyteArray array = env->NewByteArray(0);
        return array;
    }

	//env->ReleaseIntArrayElements(polygonCountArray, nPolygonCountArray, 0);

}
