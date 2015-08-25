package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by user on 8/25/15.
 */
public class IntentComponent extends Component {

	public Vector2 moveDirection = new Vector2();

	public boolean isDragging = false;
	public Vector2 dragStart = new Vector2();
	public Vector2 dragCurrent = new Vector2();

	public float zoom = 0;

}
