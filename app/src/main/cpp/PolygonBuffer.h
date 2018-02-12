#pragma once

// -1 : 不在多边形内
// 0: 在边界
// 1: 在多边形内
int IsPointInPolygonSides(
	int x,
	int y,
	int* ptPolygonSidesX,
	int* ptPolygonSidesY,
	int nCount);
