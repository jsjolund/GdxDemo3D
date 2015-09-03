package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;

/**
 * Created by user on 7/31/15.
 */
public class PhysicsComponent extends Component implements Disposable {

	private static Vector3 localInertia = new Vector3();
	public final MotionStateComponent.PhysicsMotionState motionState;
	public final btRigidBody body;
	public final short belongsToFlag;
	public final short collidesWithFlag;
	private final btCollisionShape shape;
	private final btRigidBody.btRigidBodyConstructionInfo constructionInfo;

	public PhysicsComponent(btCollisionShape shape,
							MotionStateComponent.PhysicsMotionState motionState,
							float mass,
							short belongsToFlag,
							short collidesWithFlag,
							boolean callback,
							boolean noDeactivate) {
		this.shape = shape;
		this.belongsToFlag = belongsToFlag;
		this.collidesWithFlag = collidesWithFlag;
		this.motionState = motionState;

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
		if (motionState != null) {
			body.setMotionState(motionState);
		}
	}


	@Override
	public void dispose() {
		shape.dispose();
		constructionInfo.dispose();
		motionState.dispose();
		body.dispose();
	}
}
