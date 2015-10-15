package com.mygdx.game.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.utilities.ModelFactory;

/**
 * Created by user on 9/14/15.
 */
public class BillboardComponent implements DisposableComponent {

	public final TextureRegion textureRegion;
	public final ModelInstance modelInstance;
	public final Vector3 offset = new Vector3();
	private final Model model;
	public boolean faceUp;
	public Matrix4 followTransform;

	public BillboardComponent(Pixmap pixmap, float width, float height, boolean faceUp, Vector3 offset, Matrix4
			followTransform) {
		textureRegion = new TextureRegion(new Texture(pixmap), pixmap.getWidth(), pixmap.getHeight());
		Material material = new Material();
		material.set(new TextureAttribute(TextureAttribute.Diffuse, textureRegion));
		material.set(new ColorAttribute(ColorAttribute.AmbientLight, Color.WHITE));
		material.set(new BlendingAttribute());
//		material.set(new IntAttribute(IntAttribute.CullFace, Gdx.gl.GL_NONE));

		model = ModelFactory.buildPlaneModel(width, height, material, 0, 0, 1, 1);
		modelInstance = new ModelComponent(model, "plane").modelInstance;

		this.faceUp = faceUp;
		this.offset.set(offset);
		this.followTransform = followTransform;
	}

	@Override
	public void dispose() {
		model.dispose();
	}

}
