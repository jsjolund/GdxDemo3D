package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

/**
 * Created by user on 7/31/15.
 */
public class ModelComponent extends Component {

	public static final String tag = "ModelComponent";
	public final Vector3 center = new Vector3();
	public final Vector3 dimensions = new Vector3();
	public final float radius;
	private final BoundingBox bounds = new BoundingBox();
	public ModelInstance modelInstance;
	public String id;

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
