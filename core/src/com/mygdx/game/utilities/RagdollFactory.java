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
import com.mygdx.game.components.blender.BlenderObject;
import com.mygdx.game.components.blender.BlenderScene;

import java.util.ArrayList;

/**
 * Created by user on 9/10/15.
 */
public class RagdollFactory {

	final static float PI = MathUtils.PI;
	final static float PI2 = 0.5f * PI;
	final static float PI4 = 0.25f * PI;


	public static RagdollComponent createRagdoll(ModelInstance model, float bodyMass, short belongsToFlag, short
			collidesWithFlag) {

		RagdollComponent cmp = new RagdollComponent();

		cmp.belongsToFlag = belongsToFlag;
		cmp.collidesWithFlag = collidesWithFlag;

		// Load shape half extent data from Blender
		ArrayMap<String, Vector3> halfExtMap = new ArrayMap<String, Vector3>();
		String path = "models/json/character_empty.json";
		ArrayList<BlenderObject.BEmpty> empties =
				new Json().fromJson(ArrayList.class, BlenderObject.BEmpty.class, Gdx.files.local(path));
		for (BlenderObject.BEmpty empty : empties) {
			BlenderScene.blenderToGdxCoordinates(empty);
			Vector3 halfExtents = new Vector3(empty.scale);
			halfExtents.x = Math.abs(halfExtents.x);
			halfExtents.y = Math.abs(halfExtents.y);
			halfExtents.z = Math.abs(halfExtents.z);
			halfExtMap.put(empty.name, halfExtents);
		}

		float massHead = bodyMass * 0.073f;
		float massNeck = bodyMass * 0.01f;
		float massAbdomen = bodyMass * 0.254f;
		float massChest = bodyMass * 0.254f;
		float massUpperArm = bodyMass * 0.027f;
		float massForearm = bodyMass * 0.016f;
		float massThigh = bodyMass * 0.0988f;
		float massShin = bodyMass * 0.0465f;

		Node armature = model.getNode("armature");

		btRigidBody head = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("head")),
				null, massHead, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(head, armature.getChild("head", true, true));

		btRigidBody neck = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("neck")),
				null, massNeck, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(neck, armature.getChild("neck", true, true));

		btRigidBody abdomen = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("abdomen")),
				null, massAbdomen, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(abdomen, armature.getChild("abdomen", true, true));
		cmp.addPart(abdomen, armature, new Matrix4().trn(0, halfExtMap.get("abdomen").y * 1.6f, 0));

		btRigidBody chest = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("chest")),
				null, massChest, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(chest, armature.getChild("chest", true, true));

		btRigidBody leftUpperArm = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_upper_arm")),
				null, massUpperArm, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(leftUpperArm, armature.getChild("left_upper_arm", true, true));

		btRigidBody leftForearm = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_forearm")),
				null, massForearm, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(leftForearm, armature.getChild("left_forearm", true, true));

		btRigidBody leftThigh = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_thigh")),
				null, massThigh, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(leftThigh, armature.getChild("left_thigh", true, true));

		btRigidBody leftShin = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_shin")),
				null, massShin, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(leftShin, armature.getChild("left_shin", true, true));

		btRigidBody rightUpperArm = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_upper_arm")),
				null, massUpperArm, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(rightUpperArm, armature.getChild("right_upper_arm", true, true));

		btRigidBody rightForearm = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_forearm")),
				null, massForearm, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(rightForearm, armature.getChild("right_forearm", true, true));

		btRigidBody rightThigh = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_thigh")),
				null, massThigh, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(rightThigh, armature.getChild("right_thigh", true, true));

		btRigidBody rightShin = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_shin")),
				null, massShin, cmp.belongsToFlag,
				cmp.collidesWithFlag, false, true).body;
		cmp.addFollowPart(rightShin, armature.getChild("right_shin", true, true));

		final Matrix4 localA = new Matrix4();
		final Matrix4 localB = new Matrix4();
		btHingeConstraint hingeC;
		btConeTwistConstraint coneC;
		btFixedConstraint fixedC;

		// Abdomen - Chest
		localA.setFromEulerAnglesRad(0, PI4, 0).trn(0, halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAnglesRad(0, PI4, 0).trn(0, -halfExtMap.get("chest").y, 0);
		cmp.constraints.add(
				hingeC = new btHingeConstraint(abdomen, chest, localA, localB));
		hingeC.setLimit(-PI4, PI2);
//		hingeC.setDbgDrawSize(0);

		// Chest - Neck
		localA.setFromEulerAnglesRad(0, 0, 0).trn(0, halfExtMap.get("chest").y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).trn(0, -halfExtMap.get("neck").y, 0);
		cmp.constraints.add(fixedC = new btFixedConstraint(chest, neck, localA, localB));
//		fixedC.setDbgDrawSize(0);

		// Neck - Head
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get("neck").y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get("head").y, 0);
		cmp.constraints.add(
				coneC = new btConeTwistConstraint(neck, head, localA, localB));
		coneC.setLimit(PI4, PI4, PI4);
//		coneC.setDbgDrawSize(0);

		// Abdomen - Left Thigh
		localA.setFromEulerAnglesRad(0, PI, 0).scl(-1, 1, 1).trn(halfExtMap.get("abdomen").x * 0.5f, -halfExtMap.get
				("abdomen").y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).scl(-1, 1, 1).trn(0, -halfExtMap.get("left_thigh").y, 0);
		cmp.constraints.add(
				coneC = new btConeTwistConstraint(abdomen, leftThigh, localA, localB));
//		coneC.setLimit(PI, PI2, PI4);
		coneC.setLimit(PI4, PI4, PI4);
		coneC.setDamping(10);
//		coneC.setDbgDrawSize(0);

		// Abdomen - Right Thigh
		localA.setFromEulerAnglesRad(0, PI, 0).trn(-halfExtMap.get("abdomen").x * 0.5f, -halfExtMap.get
				("abdomen").y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).trn(0, -halfExtMap.get("right_thigh").y, 0);
		cmp.constraints.add(
				coneC = new btConeTwistConstraint(abdomen, rightThigh, localA, localB));
//		coneC.setLimit(PI, PI2, PI4);
		coneC.setLimit(PI4, PI4, PI4);
		coneC.setDamping(10);
//		coneC.setDbgDrawSize(0);

		// Left Thigh - Left Shin
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get("left_thigh").y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get("left_shin").y, 0);
		cmp.constraints.add(
				hingeC = new btHingeConstraint(leftThigh, leftShin, localA, localB));
		hingeC.setLimit(0, PI4 * 3);
//		hingeC.setDbgDrawSize(0);

		// Right Thigh - Right Shin
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get("right_thigh").y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get("right_shin").y, 0);
		cmp.constraints.add(
				hingeC = new btHingeConstraint(rightThigh, rightShin, localA, localB));
		hingeC.setLimit(0, PI4 * 3);
//		hingeC.setDbgDrawSize(0);


		// Chest - Left Upper Arm
		localA.setFromEulerAnglesRad(0, PI, 0).scl(-1, 1, 1).trn(
				halfExtMap.get("abdomen").x + halfExtMap.get("left_upper_arm").x, halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAnglesRad(PI4, -0, 0).trn(
				0, -halfExtMap.get("left_upper_arm").y, 0);
		cmp.constraints.add(
				coneC = new btConeTwistConstraint(chest, leftUpperArm, localA, localB));
		coneC.setLimit(PI2, PI2, 0);
		coneC.setDamping(10);
//		coneC.setDbgDrawSize(0);

		// Chest - Right Upper Arm
		localA.setFromEulerAnglesRad(0, PI, 0).trn(
				-halfExtMap.get("abdomen").x - halfExtMap.get("right_upper_arm").x, halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAnglesRad(-PI4, -0, 0).trn(
				0, -halfExtMap.get("right_upper_arm").y, 0);
		cmp.constraints.add(
				coneC = new btConeTwistConstraint(chest, rightUpperArm, localA, localB));
		coneC.setLimit(PI2, PI2, 0);
		coneC.setDamping(10);
//		coneC.setDbgDrawSize(0);


		// Left Upper Arm - Left Forearm
		localA.setFromEulerAnglesRad(PI2, 0, 0).trn(0, halfExtMap.get("left_upper_arm").y, 0);
		localB.setFromEulerAnglesRad(PI2, 0, 0).trn(0, -halfExtMap.get("left_forearm").y, 0);
		cmp.constraints.add(
				hingeC = new btHingeConstraint(leftUpperArm, leftForearm, localA, localB));
		hingeC.setLimit(0, PI2);
//		hingeC.setDbgDrawSize(0);

		// Right Upper Arm - Right Forearm
		localA.setFromEulerAnglesRad(PI2, 0, 0).trn(0, halfExtMap.get("right_upper_arm").y, 0);
		localB.setFromEulerAnglesRad(PI2, 0, 0).trn(0, -halfExtMap.get("right_forearm").y, 0);
		cmp.constraints.add(
				hingeC = new btHingeConstraint(rightUpperArm, rightForearm,
						localA, localB));
		hingeC.setLimit(0, PI2);
//		hingeC.setDbgDrawSize(0);

		return cmp;
	}

}
