package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.ObjectMap;
import com.mygdx.game.components.*;

import java.util.Iterator;

/**
 * Created by user on 9/6/15.
 */
public class RagdollSystem extends IteratingSystem {

	Matrix4 tmp = new Matrix4();

	private ComponentMapper<CharacterActionComponent> actionCmps =
			ComponentMapper.getFor(CharacterActionComponent.class);

	private ComponentMapper<ModelComponent> modelCmps =
			ComponentMapper.getFor(ModelComponent.class);

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

		ModelComponent modelCmp = modelCmps.get(entity);
		RagdollComponent ragdollCmp = ragdollCmps.get(entity);
		PhysicsComponent phyCmp = phyCmps.get(entity);
		CharacterActionComponent actionCmp = actionCmps.get(entity);
		SelectableComponent selCmp = selCmps.get(entity);
		IntentBroadcastComponent intentCmp = intentCmps.get(entity);
		MotionStateComponent motionCmp = motionCmps.get(entity);


		// Check if we should enable or disable physics control of the ragdoll
		if (selCmp != null && intentCmps != null && selCmp.isSelected && intentCmp.killSelected) {
			if (ragdollCmp.ragdollControl) {
				// Ragdoll physics control is enabled, disable it, reset nodes and ragdoll components to animation.
				ragdollCmp.ragdollControl = false;
				actionCmp.ragdollControl = false;

				modelCmp.modelInstance.transform = motionCmp.transform;

				for (Node node : ragdollCmp.nodes) {
					node.isAnimated = false;
					node.inheritTransform = true;
				}

				modelCmp.modelInstance.calculateTransforms();

			} else {
				// Ragdoll follows animation, set it to use physics control.
				ragdollCmp.ragdollControl = true;
				actionCmp.ragdollControl = true;

				actionCmp.nextAction = CharacterActionComponent.Action.NULL;

				ragdollCmp.baseBodyTransform.getTranslation(ragdollCmp.baseTrans);
				modelCmp.modelInstance.transform = new Matrix4().inv().setToTranslation(ragdollCmp.baseTrans);

				for (btRigidBody body : ragdollCmp.map.keys()) {
					body.setLinearVelocity(phyCmp.body.getLinearVelocity().scl(1, 0, 1));
					body.setAngularVelocity(phyCmp.body.getLinearVelocity());
				}

				for (Node node : ragdollCmp.nodes) {
					node.isAnimated = true;
					node.inheritTransform = false;
				}
			}
		}

		if (ragdollCmp.ragdollControl) {
			// Let dynamicsworld control ragdoll.
			for (Iterator<ObjectMap.Entry<btRigidBody, RagdollComponent.NodeConnection>> iterator1
				 = ragdollCmp.map.iterator(); iterator1.hasNext(); ) {
				ObjectMap.Entry<btRigidBody, RagdollComponent.NodeConnection> bodyEntry = iterator1.next();
				btRigidBody body = bodyEntry.key;
				RagdollComponent.NodeConnection data = bodyEntry.value;

				for (Iterator<ObjectMap.Entry<Node, Matrix4>> iterator2
					 = data.bodyNodeOffsets.iterator(); iterator2.hasNext(); ) {
					ObjectMap.Entry<Node, Matrix4> nodeEntry = iterator2.next();
					Node node = nodeEntry.key;
					Matrix4 offsetMatrix = nodeEntry.value;

					body.getWorldTransform(node.localTransform);
					node.localTransform.getTranslation(ragdollCmp.partTrans);
					ragdollCmp.partTrans.sub(ragdollCmp.baseTrans);

					node.localTransform.setTranslation(ragdollCmp.partTrans).mul(tmp.set(offsetMatrix).inv());
				}
			}
			modelCmp.modelInstance.calculateTransforms();

		} else {
			// Let the ragdoll follow the animation bones
			phyCmp.body.getWorldTransform(ragdollCmp.baseBodyTransform);
			for (Iterator<ObjectMap.Entry<btRigidBody, RagdollComponent.NodeConnection>> iterator
				 = ragdollCmp.map.iterator(); iterator.hasNext(); ) {
				ObjectMap.Entry<btRigidBody, RagdollComponent.NodeConnection> entry = iterator.next();
				RagdollComponent.NodeConnection data = entry.value;
				btRigidBody body = entry.key;
				Node followNode = data.followNode;
				Matrix4 offsetMatrix = data.bodyNodeOffsets.get(followNode);

				body.proceedToTransform(tmp.set(ragdollCmp.baseBodyTransform).mul(followNode.globalTransform).mul(offsetMatrix));
			}
		}


	}
}
