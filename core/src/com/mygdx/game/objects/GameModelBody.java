package com.mygdx.game.objects;

import com.badlogic.gdx.graphics.g3d.Model;
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
 * Created by Johannes Sjolund on 10/18/15.
 */
public class GameModelBody extends GameModel {

	protected class PhysicsMotionState extends btMotionState {
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

	private final static Vector3 localInertia = new Vector3();
	public final btRigidBody body;
	public final short belongsToFlag;
	public final short collidesWithFlag;
	public final btCollisionShape shape;
	public final btRigidBody.btRigidBodyConstructionInfo constructionInfo;
	public final PhysicsMotionState motionState;
	protected final float mass;
	public Array<btTypedConstraint> constraints = new Array<btTypedConstraint>();
	//	public PathFindingData pathData;
	private Matrix4 matrix = new Matrix4();

	public GameModelBody(Model model,
						 String id,
						 Vector3 location,
						 Vector3 rotation,
						 Vector3 scale,
						 btCollisionShape shape,
						 float mass,
						 short belongsToFlag,
						 short collidesWithFlag,
						 boolean callback,
						 boolean noDeactivate) {

		super(model, id, location, rotation, scale);

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


		if (mass > 0f) {
			motionState = new PhysicsMotionState(modelInstance.transform);
			body.setMotionState(motionState);
		} else {
			motionState = null;
		}

		body.setContactCallbackFlag(belongsToFlag);
		if (callback) {
			body.setCollisionFlags(body.getCollisionFlags()
					| btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		}
		if (noDeactivate) {
			body.setActivationState(Collision.DISABLE_DEACTIVATION);
		}
		body.setWorldTransform(modelInstance.transform);
	}

//	@Override
//	public void update(float deltaTime) {
//		super.update(deltaTime);
//		if (pathData == null || pathData.goalReached) {
//			return;
//		}
//		body.getWorldTransform().getTranslation(pathData.currentPosition);
//		pathData.currentGroundPosition.set(pathData.currentPosition);
//		pathData.currentGroundPosition.y -= bounds.getHeight() / 2;
//		pathData.posGroundRay.origin.set(pathData.currentPosition);
//		if (pathData.goalReached) {
//			return;
//		}
//		if (pathData.currentGoal == null && pathData.path.size == 0) {
//			pathData.goalReached = true;
//			return;
//		}
//		if (pathData.currentGoal != null) {
//			float yVelocity = body.getLinearVelocity().y;
//			float xzDst = Vector2.dst2(pathData.currentGoal.point.x, pathData.currentGoal.point.z,
//					pathData.currentPosition.x, pathData.currentPosition.z);
//
//			if (xzDst < 0.01f) {
//				pathData.currentGoal = null;
//				if (pathData.path.size > 0) {
//					pathData.currentGoal = pathData.path.pop();
//					pathData.currentTriangle = pathData.nextTriangle;
//					pathData.nextTriangle = pathData.currentGoal.toNode;
//					layers.clear();
//					layers.set(pathData.currentTriangle.meshPartIndex);
//
//				} else {
//					body.setLinearVelocity(pathData.newVelocity.set(0, yVelocity, 0));
//					body.setAngularVelocity(Vector3.Zero);
//				}
//			} else {
//				pathData.goalDirection.set(pathData.currentPosition).sub(pathData.currentGoal.point).scl(-1, 0, 1).nor();
//				matrix.setToLookAt(pathData.goalDirection, Vector3.Y).setTranslation(pathData.currentPosition);
//				body.setWorldTransform(matrix);
//
//				pathData.newVelocity.set(pathData.goalDirection.scl(1, 0, -1)).scl(pathData.moveSpeed);
//				pathData.goalReached = false;
//
//				pathData.newVelocity.y = yVelocity;
//				body.setLinearVelocity(pathData.newVelocity);
//			}
//		}
//	}

	public void dispose() {
		super.dispose();
		// Let the calling class be responsible for shape dispose since it can be reused
		// shape.dispose();
		constructionInfo.dispose();
		motionState.dispose();
		body.dispose();
		if (motionState != null) {
			motionState.dispose();
		}
		for (btTypedConstraint constraint : constraints) {
			constraint.dispose();
		}
		constraints.clear();
	}


}
