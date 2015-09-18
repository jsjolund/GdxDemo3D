package com.mygdx.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Created by user on 9/17/15.
 */
public class GameGrid {

	public class Tile {
		public float x;
		public float z;
		public float xDim;
		public float zDim;

	}

	public Array<Rectangle> grid = new Array<Rectangle>();

	public GameGrid(Vector3 origin, Vector3 tileHalfExt, int xTiles, int zTiles) {

		Vector3 tileDim = new Vector3(tileHalfExt).scl(2);
		int xStart = -xTiles / 2;
		int xEnd = xTiles / 2;
		int zStart = -zTiles / 2;
		int zEnd = zTiles / 2;
		for (int x = xStart; x < xEnd; x++) {
			for (int z = zStart; z < zEnd; z++) {
				Rectangle tile = new Rectangle();
				tile.x = origin.x + tileDim.x * x;
				tile.y = origin.z + tileDim.z * z;
				tile.width = tileDim.x;
				tile.height = tileDim.z;
				grid.add(tile);
			}
		}

	}

}
