package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.ObjectMap;
import com.mygdx.game.components.*;

import java.util.Iterator;

/**
 * Created by user on 9/6/15.
 */
public class RagdollSystem extends IteratingSystem {

	Matrix4 baseTransform = new Matrix4();
	Matrix4 nodeTransform = new Matrix4();
	Vector3 childPosition = new Vector3();
	private ComponentMapper<CharacterActionComponent> actionCmps =
			ComponentMapper.getFor(CharacterActionComponent.class);
	private ComponentMapper<MotionStateComponent> motionCmps =
			ComponentMapper.getFor(MotionStateComponent.class);
	private ComponentMapper<RagdollComponent> ragdollCmps =
			ComponentMapper.getFor(RagdollComponent.class);
	private ComponentMapper<PhysicsComponent> phyCmps =
			ComponentMapper.getFor(PhysicsComponent.class);
	private ComponentMapper<SelectableComponent> selCmps =
			ComponentMapper.getFor(SelectableComponent.class);
	private ComponentMapper<IntentBroadcastComponent> intentCmps =
			ComponentMapper.getFor(IntentBroadcastComponent.class);
	public RagdollSystem(Family family) {
		super(family);
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		RagdollComponent ragdollCmp = ragdollCmps.get(entity);
		PhysicsComponent phyCmp = phyCmps.get(entity);

		SelectableComponent selCmp = selCmps.get(entity);
		IntentBroadcastComponent intentCmp = intentCmps.get(entity);

		if (selCmp != null && intentCmps != null && selCmp.isSelected && intentCmp.killSelected) {
			ragdollCmp.physicsControl = !ragdollCmp.physicsControl;
			for (btRigidBody body : ragdollCmp.nodeBodyMap.values()) {
				body.setLinearVelocity(phyCmp.body.getLinearVelocity());
				body.setAngularVelocity(phyCmp.body.getLinearVelocity());
			}
		}

		if (ragdollCmp.physicsControl) {
			// Let dynamicsworld control ragdoll. Update the node transforms from each ragdoll part.
			// TODO

		} else {
			// Let model nodes and their animations control the ragdoll.
//			baseTransform.set(motionCmp.transform);
			phyCmp.body.getWorldTransform(baseTransform);
			ragdollCmp.armatureNode.calculateTransforms(true);

			for (Iterator<ObjectMap.Entry<Node, btRigidBody>> iterator = ragdollCmp.nodeBodyMap.iterator();
				 iterator.hasNext(); ) {
				ObjectMap.Entry<Node, btRigidBody> entry = iterator.next();
				btRigidBody body = entry.value;
				Node node = entry.key;
				childPosition.setZero();
				if (node.hasChildren()) {
					node.getChild(0).localTransform.getTranslation(childPosition);
					childPosition.scl(0.5f);
				}
				body.proceedToTransform(nodeTransform.set(baseTransform).mul(node.globalTransform).translate(childPosition));
			}
		}

	}
}
