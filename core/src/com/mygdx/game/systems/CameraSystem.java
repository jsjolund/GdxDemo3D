package com.mygdx.game.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.input.IntentBroadcast;
import com.mygdx.game.settings.GameSettings;

/**
 * Created by user on 8/24/15.
 */
public class CameraSystem extends EntitySystem {

	public static final String tag = "CameraSystem";

	private final Vector3 panDirection = new Vector3();
	private final Vector3 tmp = new Vector3();
	private final Plane worldGroundPlane = new Plane(Vector3.Y, 0);
	private final Vector3 worldDragCurrent = new Vector3();
	private final Vector3 worldDragLast = new Vector3();
	private final Vector3 worldGroundTarget = new Vector3();
	private final Vector2 cursorDelta = new Vector2();
	private final Ray ray = new Ray();
	private final Vector2 lastDragProcessed = new Vector2();
	private final Quaternion deltaRotation = new Quaternion();
	private final Vector2 keysMoveDirection = new Vector2();
	private final Vector2 dragCurrent = new Vector2();
	private final IntentBroadcast intent;
	private float currentZoom;
	private Camera ghostCamera;
	private Camera realCam;
	private Viewport viewport;

	public CameraSystem(Viewport viewport, Camera camera, IntentBroadcast intent) {
		this.intent = intent;
		this.viewport = viewport;
		this.realCam = camera;
		this.ghostCamera = new PerspectiveCamera();
		copyCamera(this.realCam, this.ghostCamera);
	}

	private static void copyCamera(Camera from, Camera to) {
		to.position.set(from.position);
		to.direction.set(from.direction);
		to.up.set(from.up);
		to.projection.set(from.projection);
		to.view.set(from.view);
		to.combined.set(from.combined);
		to.invProjectionView.set(from.invProjectionView);
		to.near = from.near;
		to.far = from.near;
		to.viewportHeight = from.viewportHeight;
		to.viewportWidth = from.viewportWidth;
	}

	private static void lerp(Camera target, Camera to, float alpha) {
		to.position.lerp(target.position, alpha);
		to.direction.lerp(target.direction, alpha);
		to.up.lerp(target.up, alpha);
		to.view.lerp(target.view, alpha);
	}

	private void processKeyboardPan(float deltaTime) {
		tmp.set(ghostCamera.direction).scl(keysMoveDirection.y);
		panDirection.set(tmp);
		tmp.set(ghostCamera.direction).crs(ghostCamera.up).scl(keysMoveDirection.x);
		panDirection.add(tmp);
		panDirection.y = 0;
		panDirection.nor();
		ghostCamera.position.add(panDirection.scl(deltaTime * GameSettings.CAMERA_MAX_PAN_VELOCITY));
	}

	private void processZoom() {
		ray.set(ghostCamera.position, ghostCamera.direction);
		Intersector.intersectRayPlane(ray, worldGroundPlane, worldGroundTarget);
		ghostCamera.position.set(ghostCamera.direction).nor().scl(intent.getZoom() * -2).add(worldGroundTarget);
		currentZoom = intent.getZoom();
	}

	private void processDragPan() {
		ray.set(viewport.getPickRay(dragCurrent.x, dragCurrent.y));
		Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragCurrent);
		ray.set(viewport.getPickRay(lastDragProcessed.x, lastDragProcessed.y));
		Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragLast);
		tmp.set(worldDragLast).sub(worldDragCurrent);
		tmp.y = 0;
		worldGroundTarget.add(tmp);
		ghostCamera.position.add(tmp);
	}

	private void processDragRotation() {
		cursorDelta.set(lastDragProcessed).sub(dragCurrent).scl(GameSettings.MOUSE_SENSITIVITY);
		tmp.set(ghostCamera.direction).crs(ghostCamera.up).nor();
		deltaRotation.setEulerAngles(cursorDelta.x, cursorDelta.y * tmp.x, cursorDelta.y * tmp.z);
		rotateAround(ghostCamera, worldGroundTarget, deltaRotation);
	}

	public void rotateAround(Camera ghostCamera, Vector3 point, Quaternion quat) {
		tmp.set(point).sub(ghostCamera.position);
		ghostCamera.translate(tmp);
		ghostCamera.rotate(quat);
		quat.transform(tmp);
		ghostCamera.translate(-tmp.x, -tmp.y, -tmp.z);
	}

	@Override
	public void update(float deltaTime) {
		intent.getKeysMoveDirection(keysMoveDirection);
		intent.getDragCurrent(dragCurrent);

		if (keysMoveDirection.len() > 0) {
			processKeyboardPan(deltaTime);
		}
		if (intent.getZoom() != currentZoom) {
			processZoom();
		}
		if (!intent.isDragging()) {
			lastDragProcessed.setZero();

		} else if (!lastDragProcessed.equals(dragCurrent)) {
			boolean newDrag = false;
			if (lastDragProcessed.isZero()) {
				// New dragging
				ray.set(ghostCamera.position, ghostCamera.direction);
				Intersector.intersectRayPlane(ray, worldGroundPlane, worldGroundTarget);
				newDrag = true;
			}
			if (intent.isPan() && !newDrag) {
				processDragPan();
			}
			if (intent.isRotate() && !newDrag) {
				processDragRotation();
			}
			lastDragProcessed.set(dragCurrent);
		}
		ghostCamera.update();
		lerp(ghostCamera, realCam, GameSettings.CAMERA_LERP_ALPHA);
		realCam.update();
	}


}
