package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConeTwistConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btFixedConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Json;
import com.mygdx.game.components.PhysicsComponent;
import com.mygdx.game.components.RagdollComponent;
import com.mygdx.game.components.RagdollConstraintComponent;
import com.mygdx.game.components.blender.BlenderComponentsLoader;
import com.mygdx.game.components.blender.BlenderEmptyComponent;

import java.util.ArrayList;

/**
 * Created by user on 9/10/15.
 */
public class RagdollFactory {

	public RagdollComponent ragCmp = new RagdollComponent();
	public RagdollConstraintComponent conCmp = new RagdollConstraintComponent();

	final static float PI = MathUtils.PI;
	final static float PI2 = 0.5f * PI;
	final static float PI4 = 0.25f * PI;


	public RagdollFactory(ModelInstance model, float bodyMass, short belongsToFlag, short collidesWithFlag) {
		// Ragdoll test
		// Animation playing -> ragdoll rigid bodies controlled by animation transforms
		// No animation -> ragdoll rigid bodies controlled by dynamics world -> nodes controlled by rigid bodies

		ragCmp.belongsToFlag = belongsToFlag;
		ragCmp.collidesWithFlag = collidesWithFlag;

		// Load shape half extent data from Blender
		ArrayMap<String, Vector3> halfExtMap = new ArrayMap<String, Vector3>();
		String path = "models/json/character_empty.json";
		ArrayList<BlenderEmptyComponent> empties =
				new Json().fromJson(ArrayList.class, BlenderEmptyComponent.class, Gdx.files.local(path));
		for (BlenderEmptyComponent empty : empties) {
			BlenderComponentsLoader.blenderToGdxCoordinates(empty);
			Vector3 halfExtents = new Vector3(empty.scale);
			halfExtents.x = Math.abs(halfExtents.x);
			halfExtents.y = Math.abs(halfExtents.y);
			halfExtents.z = Math.abs(halfExtents.z);
			halfExtMap.put(empty.name, halfExtents);
		}

		Node armature = model.getNode("armature");

		btRigidBody head = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("head")),
				null, bodyMass * 0.073f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(head, armature.getChild("head", true, true));

		btRigidBody neck = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("neck")),
				null, 1f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(neck, armature.getChild("neck", true, true));

		btRigidBody abdomen = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("abdomen")),
				null, bodyMass * 0.254f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(abdomen, armature.getChild("abdomen", true, true));
		ragCmp.addPart(abdomen, armature, new Matrix4().trn(0, halfExtMap.get("abdomen").y * 1.6f, 0));

		btRigidBody chest = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("chest")),
				null, bodyMass * 0.254f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(chest, armature.getChild("chest", true, true));

		btRigidBody leftUpperArm = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_upper_arm")),
				null, bodyMass * 0.027f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(leftUpperArm, armature.getChild("left_upper_arm", true, true));

		btRigidBody leftForearm = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_forearm")),
				null, bodyMass * 0.016f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(leftForearm, armature.getChild("left_forearm", true, true));

		btRigidBody leftThigh = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_thigh")),
				null, bodyMass * 0.0988f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(leftThigh, armature.getChild("left_thigh", true, true));

		btRigidBody leftShin = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_shin")),
				null, bodyMass * 0.0465f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(leftShin, armature.getChild("left_shin", true, true));

		btRigidBody rightUpperArm = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_upper_arm")),
				null, bodyMass * 0.027f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(rightUpperArm, armature.getChild("right_upper_arm", true, true));

		btRigidBody rightForearm = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_forearm")),
				null, bodyMass * 0.016f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(rightForearm, armature.getChild("right_forearm", true, true));

		btRigidBody rightThigh = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_thigh")),
				null, bodyMass * 0.0988f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(rightThigh, armature.getChild("right_thigh", true, true));

		btRigidBody rightShin = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_shin")),
				null, bodyMass * 0.0465f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;
		ragCmp.addFollowPart(rightShin, armature.getChild("right_shin", true, true));


		ragCmp.constraintComponent = conCmp;

		final Matrix4 localA = new Matrix4();
		final Matrix4 localB = new Matrix4();
		btHingeConstraint hingeC;
		btConeTwistConstraint coneC;

		// Abdomen - Chest
		localA.setFromEulerAnglesRad(0, PI4, 0).trn(0, halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAnglesRad(0, PI4, 0).trn(0, -halfExtMap.get("chest").y, 0);
		conCmp.typedConstraints.add(
				hingeC = new btHingeConstraint(abdomen, chest, localA, localB));
		hingeC.setLimit(-PI4, PI2);

		// Chest - Neck
		localA.setFromEulerAnglesRad(0, 0, 0).trn(0, halfExtMap.get("chest").y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).trn(0, -halfExtMap.get("neck").y, 0);
		conCmp.typedConstraints.add(new btFixedConstraint(chest, neck, localA, localB));

		// Neck - Head
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get("neck").y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get("head").y, 0);
		conCmp.typedConstraints.add(
				coneC = new btConeTwistConstraint(neck, head, localA, localB));
		coneC.setLimit(-PI4, -PI4, PI2);

		// Abdomen - Left Thigh
		localA.setFromEulerAnglesRad(PI4 * 0f, 0, 0).trn(halfExtMap.get("abdomen").x * 0.5f, -halfExtMap.get
				("abdomen").y, 0);
		localB.setFromEulerAnglesRad(PI4 * 0f, PI, 0).trn(0, -halfExtMap.get("left_thigh").y, 0);
		conCmp.typedConstraints.add(
				coneC = new btConeTwistConstraint(abdomen, leftThigh, localA, localB));
		coneC.setLimit(PI4, PI4, 0);

		// Abdomen - Right Thigh
		localA.setFromEulerAnglesRad(PI4 * 0f, 0, 0).trn(-halfExtMap.get("abdomen").x * 0.5f, -halfExtMap.get
				("abdomen").y, 0);
		localB.setFromEulerAnglesRad(PI4 * 0f, PI, 0).trn(0, -halfExtMap.get("right_thigh").y, 0);
		conCmp.typedConstraints.add(
				coneC = new btConeTwistConstraint(abdomen, rightThigh, localA, localB));
		coneC.setLimit(PI4, PI4, 0);

		// Left Thigh - Left Shin
		localA.setFromEulerAnglesRad(0, PI2, 0).trn(0, halfExtMap.get("left_thigh").y, 0);
		localB.setFromEulerAnglesRad(0, PI2, 0).trn(0, -halfExtMap.get("left_shin").y, 0);
		conCmp.typedConstraints.add(
				hingeC = new btHingeConstraint(leftThigh, leftShin, localA, localB));
		hingeC.setLimit(0, PI2);

		// Right Thigh - Right Shin
		localA.setFromEulerAnglesRad(0, PI2, 0).trn(0, halfExtMap.get("right_thigh").y, 0);
		localB.setFromEulerAnglesRad(0, PI2, 0).trn(0, -halfExtMap.get("right_shin").y, 0);
		conCmp.typedConstraints.add(
				hingeC = new btHingeConstraint(rightThigh, rightShin, localA,
						localB));
		hingeC.setLimit(0, PI2);

		// Chest - Left Upper Arm
		localA.setFromEulerAnglesRad(0, PI, 0).trn(
				halfExtMap.get("abdomen").x + halfExtMap.get("left_upper_arm").x, halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(
				0, -halfExtMap.get("left_upper_arm").y, 0);
		conCmp.typedConstraints.add(
				coneC = new btConeTwistConstraint(chest, leftUpperArm, localA, localB));
		coneC.setLimit(PI2, PI2, 0);

		// Chest - Right Upper Arm
		localA.setFromEulerAnglesRad(0, PI, 0).trn(
				-halfExtMap.get("abdomen").x - halfExtMap.get("right_upper_arm").x, halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAnglesRad(PI2, 0, 0).trn(
				0, -halfExtMap.get("right_upper_arm").y, 0);
		conCmp.typedConstraints.add(
				coneC = new btConeTwistConstraint(chest, rightUpperArm, localA, localB));
		coneC.setLimit(PI2, PI2, 0);

		// Left Upper Arm - Left Forearm
		localA.setFromEulerAnglesRad(0, PI2, 0).trn(0, halfExtMap.get("left_upper_arm").y, 0);
		localB.setFromEulerAnglesRad(0, PI2, 0).trn(0, -halfExtMap.get("left_forearm").y, 0);
		conCmp.typedConstraints.add(
				hingeC = new btHingeConstraint(leftUpperArm, leftForearm, localA, localB));
		hingeC.setLimit(0, PI2);

		// Right Upper Arm - Right Forearm
		localA.setFromEulerAnglesRad(0, PI2, 0).trn(0, halfExtMap.get("right_upper_arm").y, 0);
		localB.setFromEulerAnglesRad(0, PI2, 0).trn(0, -halfExtMap.get("right_forearm").y, 0);
		conCmp.typedConstraints.add(
				hingeC = new btHingeConstraint(rightUpperArm, rightForearm,
						localA, localB));
		hingeC.setLimit(0, PI2);
	}
}
