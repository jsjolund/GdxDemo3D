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
import com.mygdx.game.input.IntentBroadcast;
import com.mygdx.game.settings.GameSettings;

import java.util.Iterator;

/**
 * Created by Johannes Sjolund on 9/6/15.
 */
public class RagdollSystem extends IteratingSystem {


	private final ComponentMapper<CharacterActionComponent> actionCmps =
			ComponentMapper.getFor(CharacterActionComponent.class);

	private final ComponentMapper<ModelComponent> modelCmps =
			ComponentMapper.getFor(ModelComponent.class);

	private final ComponentMapper<MotionStateComponent> motionCmps =
			ComponentMapper.getFor(MotionStateComponent.class);

	private final ComponentMapper<RagdollComponent> ragdollCmps =
			ComponentMapper.getFor(RagdollComponent.class);

	private final ComponentMapper<PhysicsComponent> phyCmps =
			ComponentMapper.getFor(PhysicsComponent.class);

	private final ComponentMapper<SelectableComponent> selCmps =
			ComponentMapper.getFor(SelectableComponent.class);

	IntentBroadcast intentCmp;

	public RagdollSystem(Family family, IntentBroadcast intentCmp) {
		super(family);
		this.intentCmp = intentCmp;
	}

	private static void updateArmatureToBodies(ModelComponent modelCmp, RagdollComponent ragdollCmp) {
		// Let dynamicsworld control ragdoll. Loop over all ragdoll part collision shapes
		// and their node connection data.
		for (Iterator<ObjectMap.Entry<btRigidBody, RagdollComponent.NodeConnection>> iterator1
			 = ragdollCmp.map.iterator(); iterator1.hasNext(); ) {
			ObjectMap.Entry<btRigidBody, RagdollComponent.NodeConnection> bodyEntry = iterator1.next();
			btRigidBody partBody = bodyEntry.key;
			RagdollComponent.NodeConnection connection = bodyEntry.value;

			// Loop over each node connected to this collision shape
			for (Iterator<ObjectMap.Entry<Node, Matrix4>> iterator2
				 = connection.bodyNodeOffsets.iterator(); iterator2.hasNext(); ) {
				ObjectMap.Entry<Node, Matrix4> nodeEntry = iterator2.next();
				// A node which is to follow this collision shape
				Node node = nodeEntry.key;
				// The offset of this node from the untranslated collision shape origin
				Matrix4 offsetMatrix = nodeEntry.value;
				// Set the node to the transform of the collision shape it follows
				partBody.getWorldTransform(node.localTransform);
				// Calculate difference in translation between the node/ragdoll part and the
				// base capsule shape.
				node.localTransform.getTranslation(ragdollCmp.nodeTrans);
				ragdollCmp.baseBodyTransform.getTranslation(ragdollCmp.baseTrans);
				ragdollCmp.nodeTrans.sub(ragdollCmp.baseTrans);
				// Calculate the final node transform
				node.localTransform.setTranslation(ragdollCmp.nodeTrans)
						.mul(ragdollCmp.tmp.set(offsetMatrix).inv());
			}
		}
		// Calculate the final transform of the model.
		modelCmp.modelInstance.calculateTransforms();
	}

	private static void updateBodiesToArmature(PhysicsComponent phyCmp, RagdollComponent ragdollCmp) {
		// Ragdoll parts should follow the model animation.
		// Loop over each part and set it to the global transform of the armature node it should follow.
		phyCmp.body.getWorldTransform(ragdollCmp.baseBodyTransform);
		for (Iterator<ObjectMap.Entry<btRigidBody, RagdollComponent.NodeConnection>> iterator
			 = ragdollCmp.map.iterator(); iterator.hasNext(); ) {
			ObjectMap.Entry<btRigidBody, RagdollComponent.NodeConnection> entry = iterator.next();
			RagdollComponent.NodeConnection data = entry.value;
			btRigidBody body = entry.key;
			Node followNode = data.followNode;
			Matrix4 offsetMatrix = data.bodyNodeOffsets.get(followNode);

			body.proceedToTransform(ragdollCmp.tmp.set(ragdollCmp.baseBodyTransform)
					.mul(followNode.globalTransform).mul(offsetMatrix));
		}
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {

		ModelComponent modelCmp = modelCmps.get(entity);
		RagdollComponent ragdollCmp = ragdollCmps.get(entity);
		PhysicsComponent phyCmp = phyCmps.get(entity);
		CharacterActionComponent actionCmp = actionCmps.get(entity);
		SelectableComponent selCmp = selCmps.get(entity);

		MotionStateComponent motionCmp = motionCmps.get(entity);

		// Check if we should enable or disable physics control of the ragdoll
		if (selCmp.isSelected && intentCmp.isKillSelected()) {
			if (ragdollCmp.ragdollControl) {

				// Ragdoll physics control is enabled, disable it, reset nodes and ragdoll components to animation.
				ragdollCmp.ragdollControl = false;
				actionCmp.ragdollControl = false;

				modelCmp.modelInstance.transform = motionCmp.transform;

				// Reset the nodes to default model animation state.
				for (Node node : ragdollCmp.nodes) {
					node.isAnimated = false;
					node.inheritTransform = true;
				}
				modelCmp.modelInstance.calculateTransforms();

				// Disable gravity to prevent problems with the physics engine adding too much velocity
				// to the ragdoll
				for (btRigidBody body : ragdollCmp.map.keys()) {
					body.setGravity(Vector3.Zero);
				}

			} else {

				updateBodiesToArmature(phyCmp, ragdollCmp);

				// Ragdoll follows animation currently, set it to use physics control.
				// Disallow animations for this model.
				ragdollCmp.ragdollControl = true;
				actionCmp.ragdollControl = true;

				// Get the current translation of the base collision shape (the capsule)
				ragdollCmp.baseBodyTransform.getTranslation(ragdollCmp.baseTrans);
				// Reset any rotation of the model caused by the motion state from the physics engine,
				// but keep the translation.
				modelCmp.modelInstance.transform =
						ragdollCmp.resetRotationTransform.idt().inv().setToTranslation(ragdollCmp.baseTrans);

				// Set the velocities of the ragdoll collision shapes to be the same as the base shape.
				for (btRigidBody body : ragdollCmp.map.keys()) {
					body.setLinearVelocity(phyCmp.body.getLinearVelocity().scl(1, 0, 1));
					body.setAngularVelocity(phyCmp.body.getAngularVelocity());
					body.setGravity(GameSettings.GRAVITY);
				}

				// We don't want to use the translation, rotation, scale values of the model when calculating the
				// model transform, and we don't want the nodes inherit the transform of the parent node,
				// since the physics engine will be controlling the nodes.
				for (Node node : ragdollCmp.nodes) {
					node.isAnimated = true;
					node.inheritTransform = false;
				}
			}
		}
		if (ragdollCmp.ragdollControl) {
			updateArmatureToBodies(modelCmp, ragdollCmp);
		} else {
			updateBodiesToArmature(phyCmp, ragdollCmp);
		}
	}
}
