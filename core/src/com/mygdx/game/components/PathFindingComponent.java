package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

/**
 * Created by user on 8/30/15.
 */
public class PathFindingComponent implements Component {
	public Array<Vector3> path = new Array<Vector3>();
	public Vector3 currentGoal = null;
	public Vector3 currentPosition = new Vector3();
	public Ray posGroundRay = new Ray(new Vector3(), new Vector3(Vector3.Y).scl(-100));

	public float moveSpeed = 1;
	public boolean goalReached = true;

	public PathFindingComponent(Vector3 initialPosition) {
		currentPosition.set(initialPosition);
	}

	public void setPath(Array<Vector3> newPath) {
		if (newPath == null) {
			goalReached = true;
			path.clear();
			currentGoal = null;
		} else {
			goalReached = false;
			path.clear();
			path.addAll(newPath);
			currentGoal = path.pop();
		}
	}
}
