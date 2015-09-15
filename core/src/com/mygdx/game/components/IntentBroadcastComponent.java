package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

/**
 * Created by user on 8/25/15.
 */
public class IntentBroadcastComponent extends Component {

	public final Vector2 moveDirection = new Vector2();

	public boolean isDragging = false;
	public final Vector2 dragStart = new Vector2();
	public final Vector2 dragCurrent = new Vector2();

	public final Vector2 click = new Vector2();

	public float zoom = 0;

	public boolean pan = false;
	public boolean rotate = false;

	public boolean doubleClick = false;

	public boolean killSelected = false;

}
