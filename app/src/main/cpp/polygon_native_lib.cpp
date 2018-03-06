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

#define LOG_TAG    "POLYGON_NATIVE"

#define LINE_WIDTH	6

extern "C" {
    JNIEXPORT void JNICALL Java_com_wsd_android_NativeMask_createPolygonBuffer(
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

JNIEXPORT void JNICALL Java_com_wsd_android_NativeMask_createPolygonBuffer(
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

    int nPolygonCount = env->GetArrayLength(polygonCountArray);

	int* nPolygonCountArray = env->GetIntArrayElements(polygonCountArray, 0);
	int* xPoint = env->GetIntArrayElements(xPointArray, 0);
	int* yPoint = env->GetIntArrayElements(yPointArray, 0);

	int alphaOutside = ((unsigned int)outsideColor) >> 24;
	int alphaInside = ((unsigned int)insideColor) >> 24;

	for (int i = 0; i < height; i++)
	{
		int prevInside = 0;

		for (int j = 0; j < width; j++)
		{
			int isInsidePolygon = -1;
		    int nPointIndex = 0;
			for (int m = 0; m < nPolygonCount; m++)
			{
				isInsidePolygon = IsPointInPolygonSides(
					j,
					i,
					xPoint+nPointIndex,
					yPoint+nPointIndex,
					nPolygonCountArray[m]);
				if( isInsidePolygon >= 0)
				{
					break;
				}

				nPointIndex += nPolygonCountArray[m];
			}

			if ( isInsidePolygon == 0 )
			{
				for (int k = 0; k < LINE_WIDTH && (j+k < width); k++)
				{
					int alpha = 0;
					if (prevInside)
					{
						alpha = alphaInside + (alphaOutside - alphaInside) * k / LINE_WIDTH;
					}
					else
					{
						alpha = alphaOutside + (alphaInside - alphaOutside) * k / LINE_WIDTH;
					}

					if (alpha > 255)
					{
						alpha = 255;
					}
					else if (alpha < 0)
					{
						alpha = 0;
					}
					polygonBuffer[i*width+j+k] = (insideColor & 0x00FFFFFF) | (alpha << 24);
				}

				prevInside = 1;

				j += LINE_WIDTH-1;
			}
			else if ( isInsidePolygon > 0 )
			{
				polygonBuffer[i*width+j] = insideColor;
			}
			else
			{
				polygonBuffer[i*width+j] = outsideColor;
				prevInside = 0;
			}
		}
	}

	env->ReleaseIntArrayElements(xPointArray, xPoint, 0);
	env->ReleaseIntArrayElements(yPointArray, yPoint, 0);
	env->ReleaseIntArrayElements(polygonCountArray, nPolygonCountArray, 0);

	env->ReleaseIntArrayElements(polygonBufferArray, polygonBuffer, 0);
}
