package com.mygdx.game.objects;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by Johannes Sjolund on 10/18/15.
 */
public class Billboard extends GameModel {

	public final Vector3 offset = new Vector3();
	private final Vector3 worldPos = new Vector3();
	public boolean faceUp;
	public Matrix4 followTransform;
	private Quaternion quat = new Quaternion();
	private Camera camera;

	public Billboard(Model model, String id, Camera camera, boolean faceUp, Matrix4 followTransform, Vector3 offset) {
		super(model, id, followTransform.getTranslation(new Vector3()), new Vector3(), new Vector3(1, 1, 1));
		this.faceUp = faceUp;
		this.offset.set(offset);
		this.followTransform = followTransform;
		this.camera = camera;
	}

	public void setCamera(Camera camera) {
		this.camera = camera;
	}

	public void setFollowTransform(Matrix4 followTransform) {
		this.followTransform = followTransform;
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		followTransform.getTranslation(worldPos);
		modelInstance.transform.set(camera.view).inv();
		modelInstance.transform.setTranslation(Vector3.Zero);
		if (faceUp) {
			camera.view.getRotation(quat);
			modelInstance.transform.setFromEulerAngles(-quat.getYaw(), -90, 0);
		}
		modelInstance.transform.setTranslation(worldPos.add(offset));
		modelInstance.calculateTransforms();
	}
}
