#include "PolygonBuffer.h"

int IsPointInPolygonSides(
	int x,
	int y,
	int* ptPolygonSidesX,
	int* ptPolygonSidesY,
	int nCount)
{
	if (nCount < 3)
	{
		return -1;
	}

	int i, j, k;
	int isInside = 0;
	for (i = 0, j = nCount - 1; i < nCount; j = i++)
	{
		if (ptPolygonSidesY[i] == ptPolygonSidesY[j])
		{
			int minX = ptPolygonSidesX[i] < ptPolygonSidesX[j] ? ptPolygonSidesX[i] : ptPolygonSidesX[j];
			int maxX = ptPolygonSidesX[i] > ptPolygonSidesX[j] ? ptPolygonSidesX[i] : ptPolygonSidesX[j];
			// point在水平线段p1p2上,直接return true
			if ((y == ptPolygonSidesY[i]) && (x >= minX && x <= maxX))
			{
				return 0;
			}
		}
		else if ((ptPolygonSidesY[i] > y) != (ptPolygonSidesY[j] > y))
		{
			k = (ptPolygonSidesX[j] - ptPolygonSidesX[i]) * (y - ptPolygonSidesY[i]) /
				(ptPolygonSidesY[j] - ptPolygonSidesY[i]) + ptPolygonSidesX[i];
			if (x == k)
			{
				return 0;
			}

			if (x < k)
			{
				isInside = !isInside;
			}
		}
	}

	return isInside ? 1 : -1;
}
