package com.mygdx.game.components;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

/**
 * Created by user on 7/31/15.
 */
public class PhysicsMotionState extends btMotionState {

	public Matrix4 transform;

	@Override
	public void getWorldTransform(Matrix4 worldTrans) {
		worldTrans.set(transform);
	}

	@Override
	public void setWorldTransform(Matrix4 worldTrans) {
		transform.set(worldTrans);
	}

}
