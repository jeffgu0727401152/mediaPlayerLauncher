/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.wsd.android;


// Wrapper for native library

public class NativeMask {
	private static String TAG = "NativePolygon";

	static {
		System.loadLibrary("PolygonMask");
	}

	public static native void createPolygonBuffer(
		int[] polygonBufferArray,
		int width,
		int height,
		int[] polygonCountArray,	//这个数组的size决定了有画几个多边形,数组的每个元素表示多边形的顶点数
		int[] xPointArray,			//所有多边形的顶点的x
		int[] yPointArray,			//所有多边形的顶点的y
		int insideColor,
		int outsideColor);
}
