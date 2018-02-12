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

#include "PolygonBuffer.h"

#define LOG_TAG    "MASK_NATIVE"

extern "C" {
    JNIEXPORT void JNICALL Java_com_whiteskycn_wsd_android_NativeMask_createPolygonBuffer(
		JNIEnv * env,
		jobject obj,
		jintArray polygonBufferArray,
		int width,
		int height,
		jintArray polygonCountArray,
		jintArray xPointArray,
		jintArray yPointArray,
		int insideColor,
		int outsideColor);
};

JNIEXPORT void JNICALL Java_com_whiteskycn_wsd_android_NativeMask_createPolygonBuffer(
	JNIEnv * env,
	jobject obj,
	jintArray polygonBufferArray,
	int width,
	int height,
	jintArray polygonCountArray,
	jintArray xPointArray,
	jintArray yPointArray,
	int insideColor,
	int outsideColor)
{
	int* polygonBuffer = env->GetIntArrayElements(polygonBufferArray, 0);

#if 0
	for (int i = 0; i < height/2; i++)
	{
		for (int j = 0; j < width; j++)
		{
			polygonBuffer[i*width+j] = 0xFFFFFFFF;
		}
	}

	for (int i = height/2; i < height; i++)
	{
		for (int j = 0; j < width; j++)
		{
			polygonBuffer[i*width+j] = 0x00000000;
		}
	}
#else
    int nPolygonCount = env->GetArrayLength(polygonCountArray);

	int* nPolygonCountArray = env->GetIntArrayElements(polygonCountArray, 0);
	int* xPoint = env->GetIntArrayElements(xPointArray, 0);
	int* yPoint = env->GetIntArrayElements(yPointArray, 0);

	for (int i = 0; i < height; i++)
	{
		for (int j = 0; j < width; j++)
		{
			int isInsidePolygon = 0;
		    int nPointIndex = 0;
			for (int m = 0; m < nPolygonCount; m++)
			{
				if( IsPointInPolygonSides(
					j,
					i,
					xPoint+nPointIndex,
					yPoint+nPointIndex,
					nPolygonCountArray[m]) >= 0)
				{
					__android_log_print(
						ANDROID_LOG_INFO,
						LOG_TAG,
						"point[%d,%d] inside polygon[%d]\n",
						j,
						i,
						m);

					isInsidePolygon = 1;
					break;
				}

				nPointIndex += nPolygonCountArray[m];
			}

			if( isInsidePolygon == 1 )
			{
				polygonBuffer[i*width + j] = insideColor;
			}
			else
			{
				polygonBuffer[i*width + j] = outsideColor;
			}
		}
	}

	env->ReleaseIntArrayElements(xPointArray, xPoint, 0);
	env->ReleaseIntArrayElements(yPointArray, yPoint, 0);
	env->ReleaseIntArrayElements(polygonCountArray, nPolygonCountArray, 0);
#endif

	env->ReleaseIntArrayElements(polygonBufferArray, polygonBuffer, 0);
}
