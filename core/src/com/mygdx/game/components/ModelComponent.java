package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

/**
 * Created by user on 7/31/15.
 */
public class ModelComponent implements Component {

	public static final String tag = "ModelComponent";
	public final Vector3 center = new Vector3();
	public final Vector3 dimensions = new Vector3();
	public final float radius;
	public final BoundingBox bounds = new BoundingBox();
	public final String id;
	public ModelInstance modelInstance;
	public boolean ignoreCulling = false;

	public ModelComponent(Model model, String id, Vector3 location, Vector3 rotation, Vector3 scale) {
		this.id = id;
		modelInstance = new ModelInstance(model);

		for (Node node : modelInstance.nodes) {
			node.scale.set(Math.abs(scale.x), Math.abs(scale.y), Math.abs(scale.z));
		}
		modelInstance.transform.rotate(Vector3.X, rotation.x);
		modelInstance.transform.rotate(Vector3.Z, rotation.z);
		modelInstance.transform.rotate(Vector3.Y, rotation.y);
		modelInstance.transform.setTranslation(location);

		modelInstance.calculateTransforms();

		try {
			modelInstance.calculateBoundingBox(bounds).mul(modelInstance.transform);
		} catch (Exception e) {
			Gdx.app.debug(tag, "Error when calculating bounding box.", e);
		}
		bounds.getCenter(center);
		bounds.getDimensions(dimensions);
		radius = dimensions.len() / 2f;
	}

	public ModelComponent(Model model, String id) {
		this.id = id;
		this.modelInstance = new ModelInstance(model);
		try {
			modelInstance.calculateBoundingBox(bounds);
		} catch (Exception e) {
			Gdx.app.debug(tag, "Error when calculating bounding box.", e);
		}
		bounds.getCenter(center);
		bounds.getDimensions(dimensions);
		radius = dimensions.len() / 2f;

	}

}
