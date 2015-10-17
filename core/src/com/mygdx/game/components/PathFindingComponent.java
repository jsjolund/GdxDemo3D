package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.pathfinding.NavMeshGraphPath;

/**
 * Created by user on 8/30/15.
 */
public class PathFindingComponent implements Component {
	public Array<Vector3> path = new Array<Vector3>();
	public NavMeshGraphPath trianglePath = new NavMeshGraphPath();

	public Vector3 currentGoal = null;
	public Vector3 currentPosition = new Vector3();
	public Ray posGroundRay = new Ray(new Vector3(), new Vector3(0,-1,0));

	public float moveSpeed = 1;
	public boolean goalReached = true;

	public PathFindingComponent(Vector3 initialPosition) {
		currentPosition.set(initialPosition);
	}

	public void setPath(Array<Vector3> newPath) {
		goalReached = false;
		path.clear();
		path.addAll(newPath);
		currentGoal = path.pop();
	}

	public void clearPath() {
		goalReached = true;
		path.clear();
		trianglePath.clear();
		currentGoal = null;
	}
}
