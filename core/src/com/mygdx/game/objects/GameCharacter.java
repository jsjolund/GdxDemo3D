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
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.mygdx.game.utilities.BulletSteeringUtils;

/**
 * @author jsjolund
 */
public abstract class GameCharacter extends SteerableBody {

	public final Vector3 selectionMarkerOffset;

	public GameCharacter(Model model, String name,
						 Vector3 location, Vector3 rotation, Vector3 scale,
						 btCollisionShape shape, float mass,
						 short belongsToFlag, short collidesWithFlag,
						 boolean callback, boolean noDeactivate,
						 SteerSettings steerSettings) {

		super(model, name, location, rotation, scale, shape, mass, belongsToFlag, collidesWithFlag, callback,
				noDeactivate, steerSettings);
		selectionMarkerOffset = new Vector3(0, -halfExtents.y * 0.95f, 0);
	}

	private final static Vector3 TMP_V1 = new Vector3();
	private final static Vector3 TMP_V2 = new Vector3();
	private final static Vector3 TMP_V3 = new Vector3();
	private final static Quaternion TMP_Q = new Quaternion();
	private final static Matrix4 TMP_M = new Matrix4();

	public float getBoneOrientation(String nodeId) {
		return BulletSteeringUtils.vectorToAngle(getBoneDirection(nodeId, TMP_V3));
	}

	/**
	 * Direction vector of an armature bone, in world coordinate system.
	 *
	 * @param nodeId Name of the bone
	 * @param out    Output vector
	 * @return Output vector for chaining
	 */
	public Vector3 getBoneDirection(String nodeId, Vector3 out) {
		Node node = modelInstance.getNode(nodeId);
		Node endPointNode = (node.hasChildren()) ? node.getChild(0) : node;
		node.globalTransform.getTranslation(TMP_V1);
		endPointNode.globalTransform.getTranslation(TMP_V2);
		TMP_V1.sub(TMP_V2).scl(-1);
		modelInstance.transform.getRotation(TMP_Q);
		TMP_Q.transform(TMP_V1);
		return out.set(TMP_V1).nor();
	}

	/**
	 * Midpoint of an armature bone, in world coordinate system.
	 *
	 * @param nodeId Name of the bone
	 * @param out    Output vector
	 * @return Output vector for chaining
	 */
	public Vector3 getBoneMidpointWorldPosition(String nodeId, Vector3 out) {
		Node node = modelInstance.getNode(nodeId);
		Node endPointNode = (node.hasChildren()) ? node.getChild(0) : node;
		// Use global transform to account for model scaling
		node.globalTransform.getTranslation(TMP_V1);
		TMP_V3.set(TMP_V1);
		endPointNode.globalTransform.getTranslation(TMP_V2);
		TMP_V3.sub(TMP_V1.sub(TMP_V2).scl(0.5f));
		modelInstance.transform.getRotation(TMP_Q, true).transform(TMP_V3);
		TMP_V3.add(getPosition());
		return out.set(TMP_V3);
	}
}
