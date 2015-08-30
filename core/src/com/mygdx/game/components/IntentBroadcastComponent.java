package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by user on 8/25/15.
 */
public class IntentBroadcastComponent extends Component {

	public Vector2 moveDirection = new Vector2();

	public boolean isDragging = false;
	public Vector2 dragStart = new Vector2();
	public Vector2 dragCurrent = new Vector2();

	public Vector2 click = new Vector2();

	public float zoom = 0;

	public boolean pan = false;
	public boolean rotate = false;

//	public void reset() {
//		moveDirection.setZero();
//		dragStart.setZero();
//		dragCurrent.setZero();
//		click.setZero();
//		isDragging = false;
//		pan = false;
//		rotate = false;
//		// zoom = 0;
//	}
}
