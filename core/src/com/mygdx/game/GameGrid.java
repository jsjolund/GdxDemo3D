package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Created by user on 9/17/15.
 */
public class GameGrid {

	/**
	 * 3D box tile
	 */
	public class Tile {
		// World position of tile center.
		public Vector3 origin = new Vector3();
		// Half extents
		public Vector3 halfExt = new Vector3();
		// Bottom, left, front corner.
		public Vector3 boxPos = new Vector3();
		// Width, height, depth.
		public Vector3 boxDim = new Vector3();

		public Array<Ray> collisionRays = new Array<Ray>();

		public Tile(Vector3 origin, Vector3 halfExt) {
			this.origin.set(origin);
			this.halfExt.set(halfExt);
			boxPos.set(origin).sub(halfExt);
			boxDim.set(halfExt).scl(2);
		}
	}

	public Array<Tile> grid = new Array<Tile>();

	private static boolean isValid(int x) {
		return (x > 0 && x % 2 == 0);
	}

//	private static int getXTiles(float tileXSize, Array<Vector3> gridCorners) {
//		float xMin = 0;
//		float xMax = 0;
//		for (Vector3 v : gridCorners) {
//			if (v.x > xMax) {
//				xMax = v.x;
//			}
//			if (v.x < xMin) {
//				xMin = v.x;
//			}
//		}
//	}

//	public GameGrid(Vector3 origin, Vector3 tileHalfExt, Array<Vector3> gridCorners) {
	public GameGrid(Vector3 origin, Vector3 tileHalfExt, int xTiles, int zTiles, int yTiles) {

		if (!isValid(xTiles) || !isValid(zTiles) || yTiles < 1) {
			throw new GdxRuntimeException("Invalid grid parameters.");
		}

		int xStart = -xTiles / 2;
		int xEnd = xTiles / 2;
		int zStart = -zTiles / 2;
		int zEnd = zTiles / 2;
		for (int x = xStart; x < xEnd; x++) {
			for (int z = zStart; z < zEnd; z++) {
				Vector3 tileOrigin = new Vector3();
				tileOrigin.x = origin.x + tileHalfExt.x * 2 * x;
				tileOrigin.z = origin.z + tileHalfExt.z * 2 * z;
				tileOrigin.y = origin.y;
				Tile tile = new Tile(tileOrigin, tileHalfExt);
				grid.add(tile);
			}
		}
	}

	public Tile getTile(Vector3 worldPos) {
		Tile tile = null;
		int xi = (int) worldPos.x;
		int zi = (int) worldPos.z;
		for (Tile t : grid) {
			float xDst = t.origin.x - xi;
			float zDst = t.origin.z - zi;
			if (xDst + zDst < 0.01f) {
				tile = t;
				break;
			}
		}
		return tile;
	}

}
