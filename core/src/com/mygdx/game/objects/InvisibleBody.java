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

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

/**
 * @author jsjolund
 */
public class InvisibleBody extends GameObject {

	private final static Vector3 localInertia = new Vector3();
	public final btRigidBody body;
	public final short belongsToFlag;
	public final short collidesWithFlag;
	public final btCollisionShape shape;
	public final btRigidBody.btRigidBodyConstructionInfo constructionInfo;
	protected final float mass;

	public InvisibleBody(String name,
						 btCollisionShape shape,
						 float mass,
						 Matrix4 transform,
						 short belongsToFlag,
						 short collidesWithFlag,
						 boolean callback,
						 boolean noDeactivate) {
		super(name);
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

	public InvisibleBody(String id,
						 btCollisionShape shape,
						 float mass,
						 Vector3 location,
						 Vector3 rotation,
						 short belongsToFlag,
						 short collidesWithFlag,
						 boolean callback,
						 boolean noDeactivate) {
		this(id, shape, mass,
				new Matrix4()
						.rotate(Vector3.X, rotation.x)
						.rotate(Vector3.Z, rotation.z)
						.rotate(Vector3.Y, rotation.y)
						.setTranslation(location),
				belongsToFlag, collidesWithFlag, callback, noDeactivate);
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
