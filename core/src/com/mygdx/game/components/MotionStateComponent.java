package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

/**
 * Created by user on 7/31/15.
 */
public class MotionStateComponent implements Component {

	public final Matrix4 transform;
	public final PhysicsMotionState motionState;

	public MotionStateComponent(Matrix4 transform) {
		this.transform = transform;
		motionState = new PhysicsMotionState();
	}

	public class PhysicsMotionState extends btMotionState {

		@Override
		public void getWorldTransform(Matrix4 worldTrans) {
			worldTrans.set(transform);
		}

		@Override
		public void setWorldTransform(Matrix4 worldTrans) {
			transform.set(worldTrans);
		}


	}


}
