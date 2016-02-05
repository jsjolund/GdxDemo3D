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

package com.mygdx.game.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Bits;

/**
 * @author jsjolund
 */
public class GameModel extends GameObject {

	private static final String TAG = "GameModel";

	/**
	 * Center of bounding box for model instance
	 */
	public final Vector3 center = new Vector3();
	/**
	 * Dimensions of bounding box for model instance
	 */
	public final Vector3 dimensions = new Vector3();
	/**
	 * Bounding box radius of model instance
	 */
	public final float boundingBoxRadius;
	/**
	 * Model bounding box
	 */
	public final BoundingBox boundingBox = new BoundingBox();
	/**
	 * Model instance half extents, half of dimensions
	 */
	public final Vector3 halfExtents = new Vector3();

	/**
	 * Global transform for model instance
	 */
	public final Matrix4 modelTransform;
	/**
	 * Which rendering layers (house levels) this model instance is visible on
	 */
	public final Bits visibleOnLayers = new Bits();
	/**
	 * The model instance for this object
	 */
	public final ModelInstance modelInstance;

	/**
	 * Holds a an instance of the model.
	 *
	 * @param name       Name of model
	 * @param model    Model to instantiate
	 * @param location World position at which to place the model instance
	 * @param rotation The rotation of the model instance in degrees
	 * @param scale    Scale of the model instance
	 */
	public GameModel(Model model,
					 String name,
					 Vector3 location,
					 Vector3 rotation,
					 Vector3 scale) {
		super(name);
		modelInstance = new ModelInstance(model);

		applyTransform(location, rotation, scale, modelInstance);

		try {
			modelInstance.calculateBoundingBox(boundingBox);
		} catch (Exception e) {
			Gdx.app.debug(TAG, "Error when calculating bounding box.", e);
		}
		boundingBox.getCenter(center);
		boundingBox.getDimensions(dimensions);
		boundingBoxRadius = dimensions.len() / 2f;
		modelTransform = modelInstance.transform;
		halfExtents.set(dimensions).scl(0.5f);
	}

	public static void applyTransform(Vector3 location, Vector3 rotation, Vector3 scale,
									  ModelInstance modelInstance) {
		for (Node node : modelInstance.nodes) {
			node.scale.set(Math.abs(scale.x), Math.abs(scale.y), Math.abs(scale.z));
		}
		modelInstance.transform.rotate(Vector3.X, rotation.x);
		modelInstance.transform.rotate(Vector3.Z, rotation.z);
		modelInstance.transform.rotate(Vector3.Y, rotation.y);
		modelInstance.transform.setTranslation(location);

		modelInstance.calculateTransforms();
	}


	@Override
	public void update(float deltaTime) {

	}

	@Override
	public void dispose() {

	}
}
