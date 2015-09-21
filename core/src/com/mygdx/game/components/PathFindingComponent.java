package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Created by user on 8/30/15.
 */
public class PathFindingComponent extends Component {
	public Array<Vector3> path = new Array<Vector3>();
	public Vector3 currentGoal = null;
	public Vector3 currentPosition = new Vector3();
	public boolean run = false;

	public PathFindingComponent(Vector3 initialPosition) {
		currentPosition.set(initialPosition);
	}
}
