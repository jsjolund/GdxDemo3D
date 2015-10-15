package com.mygdx.game.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.mygdx.game.blender.BlenderObject;
import com.mygdx.game.blender.BlenderScene;

import java.util.Iterator;

/**
 * Created by user on 9/6/15.
 */
public class RagdollComponent extends PhysicsComponent implements DisposableComponent {

	private final static float PI = MathUtils.PI;
	private final static float PI2 = 0.5f * PI;
	private final static float PI4 = 0.25f * PI;

	public final Array<btTypedConstraint> constraints = new Array<btTypedConstraint>();
	public final ArrayMap<btRigidBody, NodeConnection> map = new ArrayMap<btRigidBody, NodeConnection>();
	public final Array<Node> nodes = new Array<Node>();
	public final Matrix4 baseBodyTransform = new Matrix4();
	public final Matrix4 resetRotationTransform = new Matrix4();
	public final Matrix4 tmp = new Matrix4();
	public final Vector3 nodeTrans = new Vector3();
	public final Vector3 baseTrans = new Vector3();
	public boolean ragdollControl = false;

	public RagdollComponent(btCollisionShape shape,
							Matrix4 motionStateTransform,
							float mass,
							short belongsToFlag,
							short collidesWithFlag,
							boolean callback,
							boolean noDeactivate,
							String ragdollJson,
							Node armature) {
		super(shape, motionStateTransform, mass, belongsToFlag, collidesWithFlag, callback, noDeactivate);
		createRagdoll(ragdollJson, armature);
	}

	private void addPart(btRigidBody body, Node node, Matrix4 nodeBodyOffset) {
		if (!map.containsKey(body)) {
			map.put(body, new NodeConnection());
		}
		NodeConnection conn = map.get(body);
		conn.bodyNodeOffsets.put(node, new Matrix4(nodeBodyOffset));

		if (!nodes.contains(node, true)) {
			nodes.add(node);
		}
	}

	private void addFollowPart(btRigidBody body, Node node) {
		if (!map.containsKey(body)) {
			map.put(body, new NodeConnection());
		}
		NodeConnection conn = map.get(body);
		Matrix4 nodeBodyOffset = new Matrix4();

		conn.followNode = node;
		// Set the follow offset to the middle of the armature bone
		Vector3 offsetTranslation = new Vector3();
		node.getChild(0).localTransform.getTranslation(offsetTranslation).scl(0.5f);
		nodeBodyOffset.translate(offsetTranslation);
		conn.bodyNodeOffsets.put(node, nodeBodyOffset);

		if (!nodes.contains(node, true)) {
			nodes.add(node);
		}
	}

	@Override
	public void dispose() {
		for (btTypedConstraint constraint : constraints) {
			constraint.dispose();
		}
		constraints.clear();
		for (btRigidBody body : map.keys) {
			body.dispose();
		}
		map.clear();
	}

	private void createRagdoll(String ragdollJson, Node armature) {
		// Load mass and shape half extent data from Blender json
		ArrayMap<String, Vector3> halfExtMap = new ArrayMap<String, Vector3>();
		ArrayMap<String, Float> massMap = new ArrayMap<String, Float>();

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

			PhysicsComponent phyCmp = new PhysicsComponent(
					partShape, null, partMass, this.belongsToFlag,
					this.collidesWithFlag, false, true);
			phyCmp.constructionInfo.dispose();

			bodyMap.put(partName, phyCmp.body);
			this.addFollowPart(phyCmp.body, armature.getChild(partName, true, true));
		}
		// Abdomen is the at the top of the armature hierarchy
		this.addPart(bodyMap.get("abdomen"), armature, new Matrix4().trn(0, halfExtMap.get("abdomen").y * 1.6f, 0));

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

	public class NodeConnection {
		// Stores the offset from the center of a rigid body to the node which connects to it
		public ArrayMap<Node, Matrix4> bodyNodeOffsets = new ArrayMap<Node, Matrix4>();
		// The node this bone should follow in animation mode
		public Node followNode = null;
	}


}
