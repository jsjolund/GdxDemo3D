package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by user on 8/30/15.
 */
public class PathFindingComponent extends Component {
	public Vector3 goal = null;
	public Vector3 lastProcessedGoal = null;
	public boolean run = false;
}
