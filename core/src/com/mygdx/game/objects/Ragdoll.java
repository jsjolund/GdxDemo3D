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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConeTwistConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btFixedConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.mygdx.game.blender.BlenderObject;
import com.mygdx.game.blender.BlenderScene;
import com.mygdx.game.settings.GameSettings;

import java.util.Iterator;

/**
 * @author jsjolund
 */
public abstract class Ragdoll extends GameCharacter {

	public class RigidBodyNodeConnection {
		// Stores the offset from the center of a rigid body to the node which connects to it
		// Used when multiple nodes should follow one body under ragdoll controll
		public ArrayMap<Node, Vector3> bodyNodeOffsets = new ArrayMap<Node, Vector3>();
		// The node this bone should follow in animation mode
		public Node followNode = null;
	}

	private final static float PI = MathUtils.PI;
	private final static float PI2 = 0.5f * PI;
	private final static float PI4 = 0.25f * PI;

	public final ArrayMap<btRigidBody, RigidBodyNodeConnection> bodyPartMap = new ArrayMap<btRigidBody, RigidBodyNodeConnection>();
	public final Array<Node> nodes = new Array<Node>();
	public final Matrix4 capsuleTransform = new Matrix4();
	public final Matrix4 resetRotationTransform = new Matrix4();
	public final Matrix4 tmpMatrix = new Matrix4();
	public final Vector3 tmpVec = new Vector3();
	public final Vector3 nodeTranslation = new Vector3();
	public final Vector3 capsuleTranslation = new Vector3();
	public boolean ragdollControl = false;

	public Ragdoll(Model model,
				   String id,
				   Vector3 location,
				   Vector3 rotation,
				   Vector3 scale,
				   btCollisionShape shape,
				   float mass,
				   short belongsToFlag,
				   short collidesWithFlag,
				   boolean callback,
				   boolean noDeactivate,
				   String ragdollJson,
				   String armatureNodeId,
				   SteerSettings steerSettings) {

		super(model, id, location, rotation, scale, shape, mass,
				belongsToFlag, collidesWithFlag, callback, noDeactivate, steerSettings);

		createRagdoll(ragdollJson, armatureNodeId);
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		if (ragdollControl) {
			updateArmatureToBodies();
		} else {
			updateBodiesToArmature();
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		for (btRigidBody body : bodyPartMap.keys) {
			body.dispose();
		}
		bodyPartMap.clear();
	}

	private void updateArmatureToBodies() {
		// Let dynamicsworld control ragdoll. Loop over all ragdoll part collision shapes
		// and their node connection data.
		for (Iterator<ObjectMap.Entry<btRigidBody, RigidBodyNodeConnection>> iterator1
			 = bodyPartMap.iterator(); iterator1.hasNext(); ) {
			ObjectMap.Entry<btRigidBody, RigidBodyNodeConnection> bodyEntry = iterator1.next();
			btRigidBody partBody = bodyEntry.key;
			RigidBodyNodeConnection connection = bodyEntry.value;
			capsuleTransform.getTranslation(capsuleTranslation);

			// Loop over each node connected to this collision shape
			for (Iterator<ObjectMap.Entry<Node, Vector3>> iterator2
				 = connection.bodyNodeOffsets.iterator(); iterator2.hasNext(); ) {
				ObjectMap.Entry<Node, Vector3> nodeEntry = iterator2.next();
				// A node which is to follow this collision shape
				Node node = nodeEntry.key;
				// The offset of this node from the untranslated collision shape origin
				Vector3 offset = nodeEntry.value;
				// Set the node to the transform of the collision shape it follows
				partBody.getWorldTransform(node.localTransform);
				// Calculate difference in translation between the node/ragdoll part and the
				// base capsule shape.
				node.localTransform.getTranslation(nodeTranslation);
				// Calculate the final node transform
				node.localTransform.setTranslation(nodeTranslation.sub(capsuleTranslation)).translate(tmpVec.set(offset).scl(-1));
			}
		}
		// Calculate the final transform of the model.
		modelInstance.calculateTransforms();
	}

	private void updateBodiesToArmature() {
		// Ragdoll parts should follow the model animation.
		// Loop over each part and set it to the global transform of the armature node it should follow.
		capsuleTransform.set(transform);
		for (Iterator<ObjectMap.Entry<btRigidBody, RigidBodyNodeConnection>> iterator
			 = bodyPartMap.iterator(); iterator.hasNext(); ) {
			ObjectMap.Entry<btRigidBody, RigidBodyNodeConnection> entry = iterator.next();
			RigidBodyNodeConnection data = entry.value;
			btRigidBody body = entry.key;
			Node followNode = data.followNode;
			Vector3 offset = data.bodyNodeOffsets.get(followNode);

			body.proceedToTransform(tmpMatrix.set(capsuleTransform)
					.mul(followNode.globalTransform).translate(offset));
		}
	}

	public void setRagdollControl(boolean setRagdollControl) {

		if (setRagdollControl) {

			updateBodiesToArmature();

			// Ragdoll follows animation currently, set it to use physics control.
			// Animations should be paused for this model.
			ragdollControl = true;

			// Get the current translation of the base collision shape (the capsule)
			capsuleTransform.getTranslation(capsuleTranslation);

			// Reset any rotation of the model caused by the motion state from the physics engine,
			// but keep the translation.
			modelInstance.transform = resetRotationTransform.idt().inv().setToTranslation(capsuleTranslation);

			// Set the velocities of the ragdoll collision shapes to be the same as the base shape.
			for (btRigidBody bodyPart : bodyPartMap.keys()) {
				bodyPart.setLinearVelocity(body.getLinearVelocity().scl(1, 0, 1));
				bodyPart.setAngularVelocity(body.getAngularVelocity());
				bodyPart.setGravity(GameSettings.GRAVITY);
			}

			// We don't want to use the translation, rotation, scale values of the model when calculating the
			// model transform, and we don't want the nodes inherit the transform of the parent node,
			// since the physics engine will be controlling the nodes.
			for (Node node : nodes) {
				node.isAnimated = true;
				node.inheritTransform = false;
			}

		} else {
			// Ragdoll physics control is enabled, disable it, reset nodes and ragdoll components to animation.
			ragdollControl = false;

			modelInstance.transform = motionState.transform;

			// Reset the nodes to default model animation state.
			for (Node node : nodes) {
				node.isAnimated = false;
				node.inheritTransform = true;
			}
			modelInstance.calculateTransforms();

			// Disable gravity to prevent problems with the physics engine adding too much velocity
			// to the ragdoll
			for (btRigidBody bodyPart : bodyPartMap.keys()) {
				bodyPart.setGravity(Vector3.Zero);
			}
		}
	}


	/**
	 * @param bodyPart       The rigid body which is to be synchronized with a node
	 * @param node           The node which is to be synchronized with a body
	 * @param nodeBodyOffset The offset from the node to rigid body origin
	 */
	private void addPart(btRigidBody bodyPart, Node node, Vector3 nodeBodyOffset) {
		if (!bodyPartMap.containsKey(bodyPart)) {
			bodyPartMap.put(bodyPart, new RigidBodyNodeConnection());
		}
		RigidBodyNodeConnection conn = bodyPartMap.get(bodyPart);
		conn.bodyNodeOffsets.put(node, nodeBodyOffset);

		if (!nodes.contains(node, true)) {
			nodes.add(node);
		}
	}

	/**
	 * @param bodyPart The rigid body which is to be synchronized with a node
	 * @param node     The node which is to be synchronized with a body
	 */
	private void addPart(btRigidBody bodyPart, Node node) {
		if (!bodyPartMap.containsKey(bodyPart)) {
			bodyPartMap.put(bodyPart, new RigidBodyNodeConnection());
		}
		RigidBodyNodeConnection conn = bodyPartMap.get(bodyPart);
		conn.followNode = node;
		// Set the follow offset to the middle of the armature bone
		Vector3 offsetTranslation = new Vector3();
		node.getChild(0).localTransform.getTranslation(offsetTranslation).scl(0.5f);
		conn.bodyNodeOffsets.put(node, offsetTranslation);

		if (!nodes.contains(node, true)) {
			nodes.add(node);
		}
	}


	/**
	 * @param ragdollJson    The json file containing rigid body dimension data
	 * @param armatureNodeId The name of the root skeleton/armature node
	 */
	private void createRagdoll(String ragdollJson, String armatureNodeId) {
		Node armature = modelInstance.getNode(armatureNodeId, true, true);

		// Load mass and shape half extent data from Blender json
		ArrayMap<String, Vector3> halfExtMap = new ArrayMap<String, Vector3>();
		ArrayMap<String, Float> massMap = new ArrayMap<String, Float>();

		@SuppressWarnings("unchecked")
		Array<BlenderObject.BEmpty> empties =
				new Json().fromJson(Array.class, BlenderObject.BEmpty.class, Gdx.files.local(ragdollJson));

		for (BlenderObject.BEmpty empty : empties) {
			BlenderScene.blenderToGdxCoordinates(empty);
			Vector3 halfExtents = new Vector3(empty.scale);
			halfExtents.x = Math.abs(halfExtents.x);
			halfExtents.y = Math.abs(halfExtents.y);
			halfExtents.z = Math.abs(halfExtents.z);
			halfExtMap.put(empty.name, halfExtents);

			float partMass = Float.parseFloat(empty.custom_properties.get("mass"));
			massMap.put(empty.name, super.mass * partMass);
		}

		ArrayMap<String, btCollisionShape> shapeMap = new ArrayMap<String, btCollisionShape>();
		ArrayMap<String, btRigidBody> bodyMap = new ArrayMap<String, btRigidBody>();

		// Create rigid bodies using the previously loaded mass and half extents.
		// Put them along with the shapes into maps.
		for (Iterator<ObjectMap.Entry<String, Vector3>> iterator = halfExtMap.iterator(); iterator.hasNext(); ) {
			ObjectMap.Entry<String, Vector3> entry = iterator.next();
			String partName = entry.key;
			Vector3 partHalfExt = entry.value;
			float partMass = massMap.get(partName);

			btCollisionShape partShape = new btBoxShape(partHalfExt);
			shapeMap.put(partName, partShape);

			InvisibleBody phyCmp = new InvisibleBody(
					partShape, partMass, new Matrix4(), this.belongsToFlag,
					this.collidesWithFlag, false, true);
			phyCmp.constructionInfo.dispose();

			bodyMap.put(partName, phyCmp.body);
			this.addPart(phyCmp.body, armature.getChild(partName, true, true));
		}
		// Abdomen is the at the top of the armature hierarchy
		this.addPart(bodyMap.get("abdomen"), armature, new Vector3(0, halfExtMap.get("abdomen").y * 1.6f, 0));

		final Matrix4 localA = new Matrix4();
		final Matrix4 localB = new Matrix4();
		btHingeConstraint hingeC;
		btConeTwistConstraint coneC;
		btFixedConstraint fixedC;
		String a, b;

		// TODO: This part could probably be automated somehow...

		// Set the ragdollConstraints
		a = "abdomen";
		b = "chest";
		localA.setFromEulerAnglesRad(0, PI4, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(0, PI4, 0).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(-PI4, PI2);

		a = "chest";
		b = "neck";
		localA.setFromEulerAnglesRad(0, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(fixedC = new btFixedConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));

		a = "neck";
		b = "head";
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI4, PI4, PI4);

		a = "abdomen";
		b = "left_thigh";
		localA.setFromEulerAnglesRad(0, PI, 0).scl(-1, 1, 1).trn(halfExtMap.get(a).x * 0.5f, -halfExtMap.get
				("abdomen").y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).scl(-1, 1, 1).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI4, PI4, PI4);
		coneC.setDamping(10);

		a = "abdomen";
		b = "right_thigh";
		localA.setFromEulerAnglesRad(0, PI, 0).trn(-halfExtMap.get(a).x * 0.5f, -halfExtMap.get
				("abdomen").y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI4, PI4, PI4);
		coneC.setDamping(10);

		a = "left_thigh";
		b = "left_shin";
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(0, PI4 * 3);

		a = "right_thigh";
		b = "right_shin";
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(0, PI4 * 3);

		// TODO: causes shoulder rotation
		a = "chest";
		b = "left_upper_arm";
		localA.setFromEulerAnglesRad(0, PI, 0).trn(halfExtMap.get(a).x + halfExtMap.get(b).x, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(PI4, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI2, PI2, 0);
		coneC.setDamping(10);

		// TODO: as above
		a = "chest";
		b = "right_upper_arm";
		localA.setFromEulerAnglesRad(0, PI, 0).trn(-halfExtMap.get(a).x - halfExtMap.get(b).x, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(-PI4, 0, 0).trn(0, -halfExtMap.get("right_upper_arm").y, 0);
		this.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI2, PI2, 0);
		coneC.setDamping(10);

		a = "left_upper_arm";
		b = "left_forearm";
		localA.setFromEulerAnglesRad(PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(0, PI2);

		a = "right_upper_arm";
		b = "right_forearm";
		localA.setFromEulerAnglesRad(PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		this.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(0, PI2);

	}


}
