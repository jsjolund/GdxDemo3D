package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
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
import com.mygdx.game.components.PhysicsComponent;
import com.mygdx.game.components.RagdollComponent;
import com.mygdx.game.components.blender.BlenderObject;
import com.mygdx.game.components.blender.BlenderScene;

import java.util.Iterator;

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

		// Load mass and shape half extent data from Blender json
		ArrayMap<String, Vector3> halfExtMap = new ArrayMap<String, Vector3>();
		ArrayMap<String, Float> massMap = new ArrayMap<String, Float>();

		String path = "models/json/character_empty.json";
		Array<BlenderObject.BEmpty> empties =
				new Json().fromJson(Array.class, BlenderObject.BEmpty.class, Gdx.files.local(path));

		for (BlenderObject.BEmpty empty : empties) {
			BlenderScene.blenderToGdxCoordinates(empty);
			Vector3 halfExtents = new Vector3(empty.scale);
			halfExtents.x = Math.abs(halfExtents.x);
			halfExtents.y = Math.abs(halfExtents.y);
			halfExtents.z = Math.abs(halfExtents.z);
			halfExtMap.put(empty.name, halfExtents);

			float mass = Float.parseFloat(empty.custom_properties.get("mass"));
			massMap.put(empty.name, bodyMass * mass);
		}

		Node armature = model.getNode("armature");

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

			PhysicsComponent phyCmp = new PhysicsComponent(
					partShape, null, partMass, cmp.belongsToFlag,
					cmp.collidesWithFlag, false, true);
			phyCmp.constructionInfo.dispose();

			bodyMap.put(partName, phyCmp.body);
			cmp.addFollowPart(phyCmp.body, armature.getChild(partName, true, true));
		}
		// Abdomen is the at the top of the armature hierarchy
		cmp.addPart(bodyMap.get("abdomen"), armature, new Matrix4().trn(0, halfExtMap.get("abdomen").y * 1.6f, 0));

		final Matrix4 localA = new Matrix4();
		final Matrix4 localB = new Matrix4();
		btHingeConstraint hingeC;
		btConeTwistConstraint coneC;
		btFixedConstraint fixedC;
		String a, b;

		// TODO: This part could probably be automated somehow...

		// Set the constraints
		a = "abdomen";
		b = "chest";
		localA.setFromEulerAnglesRad(0, PI4, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(0, PI4, 0).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(-PI4, PI2);

		a = "chest";
		b = "neck";
		localA.setFromEulerAnglesRad(0, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(fixedC = new btFixedConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));

		a = "neck";
		b = "head";
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI4, PI4, PI4);

		a = "abdomen";
		b = "left_thigh";
		localA.setFromEulerAnglesRad(0, PI, 0).scl(-1, 1, 1).trn(halfExtMap.get(a).x * 0.5f, -halfExtMap.get
				("abdomen").y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).scl(-1, 1, 1).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI4, PI4, PI4);
		coneC.setDamping(10);

		a = "abdomen";
		b = "right_thigh";
		localA.setFromEulerAnglesRad(0, PI, 0).trn(-halfExtMap.get(a).x * 0.5f, -halfExtMap.get
				("abdomen").y, 0);
		localB.setFromEulerAnglesRad(0, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI4, PI4, PI4);
		coneC.setDamping(10);

		a = "left_thigh";
		b = "left_shin";
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(0, PI4 * 3);

		a = "right_thigh";
		b = "right_shin";
		localA.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(-PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(0, PI4 * 3);

		// TODO: causes shoulder rotation
		a = "chest";
		b = "left_upper_arm";
		localA.setFromEulerAnglesRad(0, PI, 0).scl(-1, 1, 1).trn(halfExtMap.get(a).x + halfExtMap.get(b).x, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(PI4, -0, 0).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI2, PI2, 0);
		coneC.setDamping(10);

		// TODO: as above
		a = "chest";
		b = "right_upper_arm";
		localA.setFromEulerAnglesRad(0, PI, 0).trn(-halfExtMap.get(a).x - halfExtMap.get(b).x, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(-PI4, -0, 0).trn(0, -halfExtMap.get("right_upper_arm").y, 0);
		cmp.constraints.add(coneC = new btConeTwistConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		coneC.setLimit(PI2, PI2, 0);
		coneC.setDamping(10);

		a = "left_upper_arm";
		b = "left_forearm";
		localA.setFromEulerAnglesRad(PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(0, PI2);

		a = "right_upper_arm";
		b = "right_forearm";
		localA.setFromEulerAnglesRad(PI2, 0, 0).trn(0, halfExtMap.get(a).y, 0);
		localB.setFromEulerAnglesRad(PI2, 0, 0).trn(0, -halfExtMap.get(b).y, 0);
		cmp.constraints.add(hingeC = new btHingeConstraint(bodyMap.get(a), bodyMap.get(b), localA, localB));
		hingeC.setLimit(0, PI2);

		return cmp;
	}

}
