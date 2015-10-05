package com.mygdx.game.input;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.Ray;

/**
 * Created by user on 8/25/15.
 */
public class IntentBroadcast {

	 final Vector2 keysMoveDirection = new Vector2();
	 final Vector2 dragStart = new Vector2();
	 final Vector2 dragCurrent = new Vector2();
	 final Vector2 click = new Vector2();
	 boolean isDragging = false;
	 boolean pan = false;
	 boolean rotate = false;
	 boolean doubleClick = false;
	 boolean killSelected = false;
	 float zoom = 0;
	 Ray pickRay;

	public Vector2 getClick(Vector2 v) {
		return v.set(click);
	}

	public Vector2 getDragCurrent(Vector2 v) {
		return v.set(dragCurrent);
	}

	public Vector2 getDragStart(Vector2 v) {
		return v.set(dragStart);
	}

	public Vector2 getKeysMoveDirection(Vector2 v) {
		return v.set(keysMoveDirection);
	}

	public boolean isDoubleClick() {
		return doubleClick;
	}

	public boolean isDragging() {
		return isDragging;
	}

	public boolean isKillSelected() {
		return killSelected;
	}

	public boolean isPan() {
		return pan;
	}

	public boolean isRotate() {
		return rotate;
	}

	public Ray getPickRay() {
		return pickRay;
	}

	public float getZoom() {
		return zoom;
	}
}
