package com.mygdx.game.objects;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

/**
 * Created by Johannes Sjolund on 10/19/15.
 */
public class InvisibleBody extends GameObject {

	private final static Vector3 localInertia = new Vector3();
	public final btRigidBody body;
	public final short belongsToFlag;
	public final short collidesWithFlag;
	public final btCollisionShape shape;
	public final btRigidBody.btRigidBodyConstructionInfo constructionInfo;
	protected final float mass;

	public InvisibleBody(btCollisionShape shape,
						 float mass,
						 Matrix4 transform,
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
		body.setWorldTransform(transform);
		body.setContactCallbackFlag(belongsToFlag);
		if (callback) {
			body.setCollisionFlags(body.getCollisionFlags()
					| btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		}
		if (noDeactivate) {
			body.setActivationState(Collision.DISABLE_DEACTIVATION);
		}

	}

	@Override
	public void update(float deltaTime) {

	}

	@Override
	public void dispose() {
		// Let the calling class be responsible for shape dispose since it can be reused
		// shape.dispose();
		constructionInfo.dispose();
		body.dispose();
	}
}
