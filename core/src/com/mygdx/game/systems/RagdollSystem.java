package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.ObjectMap;
import com.mygdx.game.components.*;

import java.util.Iterator;

/**
 * Created by user on 9/6/15.
 */
public class RagdollSystem extends IteratingSystem {

	Matrix4 bodyTransform = new Matrix4();
	Matrix4 nodeTransform = new Matrix4();

	Vector3 childNodePosition = new Vector3();
	Vector3 instancePosition = new Vector3();
	Vector3 bodyPosition = new Vector3();

	Quaternion modelRotation = new Quaternion();
	Quaternion bodyRotation = new Quaternion();

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


		if (ragdollCmp.physicsControl) {
			// Let dynamicsworld control ragdoll. Update the node transforms from each ragdoll part.

			for (Iterator<ObjectMap.Entry<Node, btRigidBody>> iterator = ragdollCmp.nodeBodyMap.iterator();
				 iterator.hasNext(); ) {
				ObjectMap.Entry<Node, btRigidBody> entry = iterator.next();

				Node node = entry.key;
				btRigidBody body = entry.value;

				node.inheritTransform = false;
				node.isAnimated = true;

				body.getWorldTransform(bodyTransform);

				modelCmp.modelInstance.transform.getRotation(modelRotation, true);
				bodyTransform.getRotation(bodyRotation, true);

				node.localTransform.setFromEulerAngles(
						bodyRotation.getYaw() - modelRotation.getYaw(),
						bodyRotation.getPitch() - modelRotation.getPitch(),
						bodyRotation.getRoll() - modelRotation.getRoll());

//				node.localTransform.setFromEulerAngles(
//						modelRotation.getYaw() - bodyRotation.getYaw(),
//						modelRotation.getPitch() - bodyRotation.getPitch(),
//						modelRotation.getRoll() - bodyRotation.getRoll());

				bodyTransform.getTranslation(bodyPosition);
				modelCmp.modelInstance.transform.getTranslation(instancePosition);

//				childNodePosition.setZero();
//				if (node.hasChildren()) {
//					node.getChild(0).calculateLocalTransform();
//					node.getChild(0).localTransform.getTranslation(childNodePosition);
//					childNodePosition.scl(0.5f);
//				}

				node.localTransform.setTranslation(bodyPosition.sub(instancePosition));
			}
			modelCmp.modelInstance.calculateTransforms();
//			Xoppa: node.globalTransform.set(transform).mul(tmp.set(instance.transform).inv());

		} else {
			// Let model nodes and their animations control the ragdoll.
			ragdollCmp.armatureNode.calculateTransforms(true);
			phyCmp.body.getWorldTransform(bodyTransform);

			for (Iterator<ObjectMap.Entry<Node, btRigidBody>> iterator = ragdollCmp.nodeBodyMap.iterator();
				 iterator.hasNext(); ) {
				ObjectMap.Entry<Node, btRigidBody> entry = iterator.next();

				Node node = entry.key;
				btRigidBody body = entry.value;

				node.inheritTransform = true;
				node.isAnimated = false;

				childNodePosition.setZero();
				if (node.hasChildren()) {
					node.getChild(0).localTransform.getTranslation(childNodePosition);
					childNodePosition.scl(0.5f);
				}
				body.proceedToTransform(nodeTransform.set(bodyTransform).mul(node.globalTransform).translate(childNodePosition));
			}
		}

		if (selCmp != null && intentCmps != null && selCmp.isSelected && intentCmp.killSelected) {

			if (ragdollCmp.physicsControl) {
				// Ragdoll physics control is enabled, disable it, reset nodes and ragdoll components to animation.
				ragdollCmp.physicsControl = false;
				actionCmp.nextAction = CharacterActionComponent.Action.IDLE;
				modelCmp.modelInstance.calculateTransforms();
				entity.remove(RagdollConstraintComponent.class);

			} else {
				// Ragdoll follows animation, set it to use physics control.
				ragdollCmp.physicsControl = true;
				actionCmp.nextAction = CharacterActionComponent.Action.NULL;
				for (btRigidBody body : ragdollCmp.nodeBodyMap.values()) {
					body.setLinearVelocity(phyCmp.body.getLinearVelocity());
					body.setAngularVelocity(phyCmp.body.getLinearVelocity());
				}
				entity.add(ragdollCmp.constraints);
			}
		}

	}
}
