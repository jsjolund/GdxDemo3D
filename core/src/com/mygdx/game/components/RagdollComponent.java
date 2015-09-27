package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btTypedConstraint;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

/**
 * Created by user on 9/6/15.
 */
public class RagdollComponent extends Component {

	public final Array<btTypedConstraint> constraints = new Array<btTypedConstraint>();
	public final ArrayMap<btRigidBody, NodeConnection> map = new ArrayMap<btRigidBody, NodeConnection>();
	public final Array<Node> nodes = new Array<Node>();

	public final Matrix4 baseBodyTransform = new Matrix4();
	public final Matrix4 resetRotationTransform = new Matrix4();
	public final Matrix4 tmp = new Matrix4();
	public final Vector3 nodeTrans = new Vector3();
	public final Vector3 baseTrans = new Vector3();

	public boolean ragdollControl = false;
	public short belongsToFlag;
	public short collidesWithFlag;

	public void addPart(btRigidBody body, Node node, Matrix4 nodeBodyOffset) {
		if (!map.containsKey(body)) {
			map.put(body, new NodeConnection());
		}
		NodeConnection conn = map.get(body);
		conn.bodyNodeOffsets.put(node, new Matrix4(nodeBodyOffset));

		if (!nodes.contains(node, true)) {
			nodes.add(node);
		}
	}

	public void addFollowPart(btRigidBody body, Node node) {
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

	public class NodeConnection {
		// Stores the offset from the center of a rigid body to the node which connects to it
		public ArrayMap<Node, Matrix4> bodyNodeOffsets = new ArrayMap<Node, Matrix4>();
		// The node this bone should follow in animation mode
		public Node followNode = null;
	}

}
