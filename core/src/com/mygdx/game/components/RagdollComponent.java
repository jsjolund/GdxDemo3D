package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.ArrayMap;

/**
 * Created by user on 9/6/15.
 */
public class RagdollComponent extends Component {

	public RagdollConstraintComponent constraintComponent;

	public boolean physicsControl = false;
	public short belongsToFlag;
	public short collidesWithFlag;

	public Node armatureNode;

	public ArrayMap<Node, btRigidBody> nodeBodyMap;
	public ArrayMap<String, Vector3> halfExtMap;

	public Node headNode;
	public btRigidBody headBody;

	public Node neckNode;
	public btRigidBody neckBody;

	public Node chestNode;
	public btRigidBody chestBody;

	public Node abdomenNode;
	public btRigidBody abdomenBody;

	public Node leftUpperArmNode;
	public btRigidBody leftUpperArmBody;
	public Node leftForearmNode;
	public btRigidBody leftForearmBody;
	public Node leftThighNode;
	public btRigidBody leftThighBody;
	public Node leftShinNode;
	public btRigidBody leftShinBody;

	public Node rightUpperArmNode;
	public btRigidBody rightUpperArmBody;
	public Node rightForearmNode;
	public btRigidBody rightForearmBody;
	public Node rightThighNode;
	public btRigidBody rightThighBody;
	public Node rightShinNode;
	public btRigidBody rightShinBody;


	public void populateNodeBodyMap() {
		if (nodeBodyMap == null) {
			nodeBodyMap = new ArrayMap<Node, btRigidBody>();
		}
		nodeBodyMap.clear();
		nodeBodyMap.put(abdomenNode, abdomenBody);
		nodeBodyMap.put(neckNode, neckBody);
		nodeBodyMap.put(chestNode, chestBody);
		nodeBodyMap.put(leftUpperArmNode, leftUpperArmBody);
		nodeBodyMap.put(leftForearmNode, leftForearmBody);
		nodeBodyMap.put(rightUpperArmNode, rightUpperArmBody);
		nodeBodyMap.put(rightForearmNode, rightForearmBody);
		nodeBodyMap.put(headNode, headBody);
		nodeBodyMap.put(leftThighNode, leftThighBody);
		nodeBodyMap.put(leftShinNode, leftShinBody);
		nodeBodyMap.put(rightThighNode, rightThighBody);
		nodeBodyMap.put(rightShinNode, rightShinBody);
	}
}
