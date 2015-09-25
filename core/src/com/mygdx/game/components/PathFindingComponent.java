package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

/**
 * Created by user on 8/30/15.
 */
public class PathFindingComponent extends Component {
	public Array<Vector3> path = new Array<Vector3>();
	public Vector3 currentGoal = null;
	public Vector3 currentPosition = new Vector3();
	public boolean run = false;

	public Ray posGroundRay = new Ray(new Vector3(),new Vector3(Vector3.Y).scl(-100));

	public PathFindingComponent(Vector3 initialPosition) {
		currentPosition.set(initialPosition);
	}
}
