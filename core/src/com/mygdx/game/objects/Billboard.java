package com.mygdx.game.objects;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * A simple billboard-like class which rotates a model to face the camera on update.
 *
 * @author jsjolund
 */
public class Billboard extends GameModel {

	private final Vector3 offset = new Vector3();
	private final Vector3 worldPos = new Vector3();
	private boolean faceUp;
	private Matrix4 followTransform;
	private final Quaternion quat = new Quaternion();
	private Camera camera;

	/**
	 * @param model           The model which is to be instantiated
	 * @param id              Id of the model (doesn't really matter)
	 * @param camera          The camera which the billboard should face
	 * @param faceUp          If true, the model will always face the Y-axis, but otherwise rotate with the camera
	 * @param followTransform The billboard will follow the translation of this matrix
	 * @param offset          Constant offset from the follow-matrix translation
	 */
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
		camera.view.getRotation(quat).conjugate();
		if (faceUp) {
			modelInstance.transform.setFromEulerAngles(quat.getYaw(), -90, quat.getRoll());
		} else {
			modelInstance.transform.set(quat);
		}
		modelInstance.transform.setTranslation(worldPos.add(offset));
	}
}
