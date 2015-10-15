package com.mygdx.game.components;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btTypedConstraint;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Array;

/**
 * Created by user on 7/31/15.
 */
public class PhysicsComponent implements DisposableComponent {

	private final static Vector3 localInertia = new Vector3();
	public final btRigidBody body;
	public final short belongsToFlag;
	public final short collidesWithFlag;
	public final btCollisionShape shape;
	public final btRigidBody.btRigidBodyConstructionInfo constructionInfo;
	public final PhysicsMotionState motionState;
	protected final float mass;
	public Array<btTypedConstraint> constraints;

	public PhysicsComponent(btCollisionShape shape,
							Matrix4 motionStateTransform,
							float mass,
							short belongsToFlag,
							short collidesWithFlag,
							boolean callback,
							boolean noDeactivate) {
		this.mass = mass;
		this.shape = shape;
		this.belongsToFlag = belongsToFlag;
		this.collidesWithFlag = collidesWithFlag;

		if (mass > 0f) {
			shape.calculateLocalInertia(mass, localInertia);
		} else {
			localInertia.set(0, 0, 0);
		}
		this.constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(
				mass, null, shape, localInertia);
		body = new btRigidBody(constructionInfo);
		body.setContactCallbackFlag(belongsToFlag);
		if (callback) {
			body.setCollisionFlags(body.getCollisionFlags()
					| btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		}
		if (noDeactivate) {
			body.setActivationState(Collision.DISABLE_DEACTIVATION);
		}
		if (motionStateTransform != null) {
			motionState = new PhysicsMotionState(motionStateTransform);
			body.setMotionState(motionState);
		} else {
			motionState = null;
		}
	}

	public void addConstraint(btTypedConstraint constraint) {
		if (constraints == null) {
			constraints = new Array<btTypedConstraint>();
		}
		constraints.add(constraint);
	}

	@Override
	public void dispose() {
		// Let the calling class be responsible for shape dispose since it can be reused
		// shape.dispose();
		constructionInfo.dispose();
		motionState.dispose();
		body.dispose();
		for (btTypedConstraint constraint : constraints) {
			constraint.dispose();
		}
		constraints.clear();
		if (motionState != null) {
			motionState.dispose();
		}
	}

	public class PhysicsMotionState extends btMotionState {
		public final Matrix4 transform;

		public PhysicsMotionState(Matrix4 transform) {
			this.transform = transform;
		}

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
