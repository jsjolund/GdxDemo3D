package com.mygdx.game.systems.old;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by user on 8/1/15.
 */
public class MoveAimComponent extends Component {

	public Vector3 position = new Vector3();

	public Vector3 directionAim = new Vector3(Vector3.X);
	public Vector3 directionMove = new Vector3(Vector3.Zero);
	public Vector3 up = new Vector3(Vector3.Y);

	public float speed = 120f;

	public Vector3 cameraPosOffset = new Vector3(0, 1, 0);
}
