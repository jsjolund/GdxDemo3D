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
	Vector3 thisNodePosition = new Vector3();

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

		// Check if we should enable or disable physics control of the ragdoll
		if (selCmp != null && intentCmps != null && selCmp.isSelected && intentCmp.killSelected) {
			if (ragdollCmp.physicsControl) {
				// Ragdoll physics control is enabled, disable it, reset nodes and ragdoll components to animation.
				ragdollCmp.physicsControl = false;
				actionCmp.nextAction = CharacterActionComponent.Action.IDLE;

				modelCmp.modelInstance.calculateTransforms();
//				entity.remove(RagdollConstraintComponent.class);

			} else {
				// Ragdoll follows animation, set it to use physics control.
				ragdollCmp.physicsControl = true;
				actionCmp.nextAction = CharacterActionComponent.Action.NULL;
				for (btRigidBody body : ragdollCmp.nodeBodyMap.values()) {
					body.setLinearVelocity(phyCmp.body.getLinearVelocity().scl(1, 0, 1));
					body.setAngularVelocity(phyCmp.body.getLinearVelocity());
				}
//				entity.add(ragdollCmp.constraintComponent);
			}
		}

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
//				node.localTransform.getRotation(modelRotation, true);

				bodyTransform.getRotation(bodyRotation, true);

				node.localTransform.setFromEulerAngles(
						bodyRotation.getYaw() - modelRotation.getYaw(),
						bodyRotation.getPitch() - modelRotation.getPitch(),
						bodyRotation.getRoll() - modelRotation.getRoll()
				);

				bodyTransform.getTranslation(bodyPosition);

				modelCmp.modelInstance.transform.getTranslation(instancePosition);
//				node.localTransform.getTranslation(instancePosition);

				childNodePosition.setZero();
				if (node.hasChildren()) {
					node.getChild(0).calculateLocalTransform();
					node.getChild(0).globalTransform.getTranslation(childNodePosition);

					node.globalTransform.getTranslation(thisNodePosition);
					childNodePosition.sub(thisNodePosition).scl(-0.5f);
				}

				node.localTransform.setTranslation(bodyPosition.sub(instancePosition).add(childNodePosition));
			}

//			for (Iterator<ObjectMap.Entry<Node, btRigidBody>> iterator = ragdollCmp.nodeBodyMap.iterator();
//				 iterator.hasNext(); ) {
//				ObjectMap.Entry<Node, btRigidBody> entry = iterator.next();
//				Node skeletonNode = entry.key;
//				btRigidBody ragdollPartBody = entry.value;
//
//				skeletonNode.inheritTransform = false;
//				skeletonNode.isAnimated = false;
//
//				boolean flip = false;
//				if (skeletonNode.id.equals("left_thigh") || skeletonNode.id.equals("right_thigh") ||
//						skeletonNode.id.equals("left_shin") || skeletonNode.id.equals("right_shin")) {
//					flip = true;
//				}
//				Matrix4 ragdollPartBodyTransform = new Matrix4();
//				ragdollPartBody.getWorldTransform(ragdollPartBodyTransform);
//
//				Matrix4 baseCollisionShapeTransform = new Matrix4();
//				phyCmp.body.getWorldTransform(baseCollisionShapeTransform);
//
//				Quaternion ragdollPartBodyRotation = new Quaternion();
//				ragdollPartBodyTransform.getRotation(ragdollPartBodyRotation, true);
//
//				Quaternion baseCollisionShapeRotation = new Quaternion();
//				baseCollisionShapeTransform.getRotation(baseCollisionShapeRotation, true);
//
////				skeletonNode.localTransform.setFromEulerAngles(
////						ragdollPartBodyRotation.getYaw() - baseCollisionShapeRotation.getYaw(),
////						ragdollPartBodyRotation.getPitch() - baseCollisionShapeRotation.getPitch(),
////						ragdollPartBodyRotation.getRoll() - baseCollisionShapeRotation.getRoll()
////				);
//
//				Vector3 ragdollPartBodyPosition = new Vector3();
//				ragdollPartBodyTransform.getTranslation(ragdollPartBodyPosition);
//				Vector3 baseCollisionShapePosition = new Vector3();
//				baseCollisionShapeTransform.getTranslation(baseCollisionShapePosition);
//
//				childNodePosition.setZero();
//				if (skeletonNode.hasChildren()) {
//					skeletonNode.getChild(0).calculateLocalTransform();
//					skeletonNode.getChild(0).globalTransform.getTranslation(childNodePosition);
//
//					skeletonNode.globalTransform.getTranslation(thisNodePosition);
//					childNodePosition.sub(thisNodePosition).scl(-0.5f);
//				}
//
//
////				skeletonNode.localTransform.setTranslation(ragdollPartBodyPosition.sub(baseCollisionShapePosition)
////						.add(childNodePosition));
//
//				skeletonNode.rotation.set(ragdollPartBodyRotation);
//				if (flip) {
//					skeletonNode.localTransform.inv();
//				}
//
//				skeletonNode.translation.set(ragdollPartBodyPosition.sub(baseCollisionShapePosition)
//						.add(childNodePosition));
//			}
//
			modelCmp.modelInstance.calculateTransforms();


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


	}
}
