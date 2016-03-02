/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.scene;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.*;
import com.mygdx.game.GameEngine;
import com.mygdx.game.blender.objects.BlenderEmpty;
import com.mygdx.game.blender.objects.BlenderModel;
import com.mygdx.game.blender.objects.BlenderObject;
import com.mygdx.game.objects.GameModel;

import java.nio.FloatBuffer;

/**
 * @author jsjolund
 */
public class GameObjectBlueprint implements Disposable {

	private static final Vector3 tmpScale = new Vector3();
	public static String blenderCollisionShapeField = "collision_shape";
	public static String blenderMassField = "mass";
	public static String blenderPfxField = "pfx";
	public String type;
	public String name;
	public Vector3 position;
	public Vector3 rotation;
	public Vector3 scale;
	public boolean[] layers;
	public ArrayMap<String, String> custom_properties;
	public Model model;
	public String shapeType;
	public btCollisionShape shape;
	public float mass;
	public short belongsToFlag;
	public short collidesWithFlag;
	public boolean callback;
	public boolean noDeactivate;
	public Array<BlenderEmpty> ragdollEmpties;
	public String armatureNodeId;
	public Bits visibleOnLayers;
	public String pfx;

	public GameObjectBlueprint() {
	}

	public GameObjectBlueprint(BlenderModel blenderModel, Model model,
							   btCollisionShape shape, float mass, String shapeType) {
		this.shape = shape;
		this.mass = mass;
		this.shapeType = shapeType;
		this.model = model;
		setFromObject(blenderModel);
		setCollisionFlags(this.mass);
	}

	public GameObjectBlueprint(BlenderEmpty blenderEmpty) {
		setFromObject(blenderEmpty);
		if (blenderEmpty.custom_properties.containsKey(blenderPfxField)) {
			pfx = blenderEmpty.custom_properties.get(blenderPfxField);
			return;
		}
		if (blenderEmpty.custom_properties.containsKey(blenderCollisionShapeField)
				&& blenderEmpty.custom_properties.containsKey(blenderMassField)) {
			setRigidBody(blenderEmpty, null, blenderEmpty.scale);
			return;
		}
		throw new GdxRuntimeException("Cannot load collision shape data from '" + blenderEmpty.name + "'");
	}
	

	public GameObjectBlueprint(BlenderModel blenderModel, Model model) {
		this.model = model;
		setFromObject(blenderModel);
		this.mass = 0;
		ModelInstance modelInstance = new ModelInstance(model);
		GameModel.applyTransform(position, rotation, blenderModel.scale, modelInstance);

		this.shape = Bullet.obtainStaticNodeShape(modelInstance.nodes);
		this.shapeType = "static_node_shape_" + blenderModel.name;
		setCollisionFlags(this.mass);
	}

	public GameObjectBlueprint(BlenderModel blenderModel, Model model, BlenderEmpty blenderEmpty) {
		this.model = model;
		setFromObject(blenderModel);
		if (blenderEmpty.custom_properties.containsKey(blenderPfxField)) {
			pfx = blenderEmpty.custom_properties.get(blenderPfxField);
			return;
		}
		if (blenderEmpty.custom_properties.containsKey(blenderCollisionShapeField)
				&& blenderEmpty.custom_properties.containsKey(blenderMassField)) {
			setRigidBody(blenderEmpty, model, blenderModel.scale);
			return;
		}
		throw new GdxRuntimeException("Cannot load collision shape data for " + blenderModel.name + " from '" + blenderEmpty.name + "'");
	}
	
	private void setFromObject(BlenderObject object) {
		this.name = object.name;
		this.position = object.position;
		this.rotation = object.rotation;
		this.scale = object.scale;
		this.visibleOnLayers = new Bits();
		for (int i = 0; i < object.layers.length; i++) {
			if (object.layers[i]) {
				this.visibleOnLayers.set(i);
			}
		}

	}
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("GameObjectBlueprint{");
		sb.append("armatureNodeId='").append(armatureNodeId).append('\'');
		sb.append(", name='").append(name).append('\'');
		sb.append(", model=").append(model);
		sb.append(", rotation=").append(rotation);
		sb.append(", scale=").append(scale);
		sb.append(", position=").append(position);
		sb.append(", shapeType='").append(shapeType).append('\'');
		sb.append(", mass=").append(mass);
		sb.append(", belongsToFlag=").append(belongsToFlag);
		sb.append(", collidesWithFlag=").append(collidesWithFlag);
		sb.append(", callback=").append(callback);
		sb.append(", noDeactivate=").append(noDeactivate);
		sb.append(", shape=").append(shape);
		sb.append(", visibleOnLayers=").append(visibleOnLayers);
		sb.append('}');
		return sb.toString();
	}

	private void setRigidBody(BlenderEmpty blenderEmpty, Model model, Vector3 scale) {
		this.shapeType = blenderEmpty.custom_properties.get(blenderCollisionShapeField);
		this.mass = Float.parseFloat(blenderEmpty.custom_properties.get(blenderMassField));

		tmpScale.set(blenderEmpty.scale.x * scale.x,
				blenderEmpty.scale.y * scale.y,
				blenderEmpty.scale.z * scale.z);

		if (shapeType.equals("capsule")) {
			float radius = Math.max(tmpScale.x, tmpScale.z);
			shape = new btCapsuleShape(radius, scale.y);

		} else if (shapeType.equals("sphere")) {
			float radius = Math.max(Math.max(tmpScale.x, tmpScale.y), tmpScale.z);
			shape = new btSphereShape(radius);

		} else if (shapeType.equals("box")) {
			shape = new btBoxShape(tmpScale);

		} else if (shapeType.equals("convex_hull")) {
			// We need a model instance with the correct scale
			ModelInstance modelInstance = new ModelInstance(model);
			GameModel.applyTransform(position, rotation, scale, modelInstance);
			// Copy the vertices to a work buffer, where we apply the model global transform to them
			Matrix4 transform = new Matrix4(modelInstance.nodes.get(0).globalTransform);
			Mesh mesh = modelInstance.model.meshes.get(0);
			FloatBuffer workBuffer = BufferUtils.newFloatBuffer(mesh.getVerticesBuffer().capacity());
			BufferUtils.copy(mesh.getVerticesBuffer(), workBuffer, mesh.getNumVertices() * mesh.getVertexSize() / 4);
			BufferUtils.transform(workBuffer, 3, mesh.getVertexSize(), mesh.getNumVertices(), transform);

			// First create a shape using all the vertices, then use the built in tool to reduce
			// the number of vertices to a manageable amount.
			btConvexShape convexShape = new btConvexHullShape(workBuffer, mesh.getNumVertices(), mesh.getVertexSize());
			btShapeHull hull = new btShapeHull(convexShape);
			hull.buildHull(convexShape.getMargin());
			shape = new btConvexHullShape(hull);
			convexShape.dispose();
			hull.dispose();

		} else if (shapeType.equals("none")) {
			shape = null;

		} else {
			throw new GdxRuntimeException("Cannot load collision shape data for " + blenderEmpty.name + " from '" + blenderEmpty.name + "'");
		}
		setCollisionFlags(this.mass);
	}

	private void setCollisionFlags(float mass) {
		if (mass > 0) {
			belongsToFlag = GameEngine.OBJECT_FLAG;
			collidesWithFlag = (short) (GameEngine.GROUND_FLAG
					| GameEngine.OBJECT_FLAG | GameEngine.PC_FLAG);
		} else {
			belongsToFlag = GameEngine.GROUND_FLAG;
			collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.PC_FLAG);
		}
	}
	
	@Override
	public void dispose() {
		if (shape != null) {
			shape.dispose();
		}
	}


}
