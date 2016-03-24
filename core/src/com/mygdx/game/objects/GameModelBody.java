/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


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
 * @author jsjolund
 */
public class GameModelBody extends GameModel {

	/**
	 * Synchronizes the model world transform with the rigid body world transform
	 */
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

	private final static Vector3 localInertia = new Vector3();
	/**
	 * Bullet rigid body
	 */
	public final btRigidBody body;
	/**
	 * Flag for which collision layers this body belongs to
	 */
	public final short belongsToFlag;
	/**
	 * Flag for which collision layers this body collides with
	 */
	public final short collidesWithFlag;
	/**
	 * Collision shape which was used to create the rigid body.
	 */
	public final btCollisionShape shape;
	/**
	 * Provides information to create a rigid body
	 */
	public final btRigidBody.btRigidBodyConstructionInfo constructionInfo;
	/**
	 * {@see PhysicsMotionState}
	 */
	public final PhysicsMotionState motionState;
	/**
	 * Mass of the rigid body. From Bullet docs, works best if in range (0:1]
	 */
	public final float mass;
	/**
	 * Physics constraints belonging to this rigid body
	 */
	public Array<btTypedConstraint> constraints = new Array<btTypedConstraint>();

	/**
	 * Creates a model with rigid body
	 *
	 * @param model            Model to instantiate
	 * @param name               Name of model
	 * @param location         World position at which to place the model instance
	 * @param rotation         The rotation of the model instance in degrees
	 * @param scale            Scale of the model instance
	 * @param shape            Collision shape with which to construct a rigid body
	 * @param mass             Mass of the body
	 * @param belongsToFlag    Flag for which collision layers this body belongs to
	 * @param collidesWithFlag Flag for which collision layers this body collides with
	 * @param callback         If this body should trigger collision contact callbacks.
	 * @param noDeactivate     If this body should never 'sleep'
	 */
	public GameModelBody(Model model,
						 String name,
						 Vector3 location,
						 Vector3 rotation,
						 Vector3 scale,
						 btCollisionShape shape,
						 float mass,
						 short belongsToFlag,
						 short collidesWithFlag,
						 boolean callback,
						 boolean noDeactivate) {

		super(model, name, location, rotation, scale);

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

	public void dispose() {
		super.dispose();
		// Let the calling class be responsible for shape dispose since it can be reused
		// shape.dispose();
		constructionInfo.dispose();
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
