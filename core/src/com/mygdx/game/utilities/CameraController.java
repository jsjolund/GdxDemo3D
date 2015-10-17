package com.mygdx.game.utilities;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.mygdx.game.settings.GameSettings;

/**
 * Created by Johannes Sjolund on 10/12/15.
 */
public class CameraController {

	private final GhostCamera camera;

	// World data
	private final Plane worldGroundPlane = new Plane(Vector3.Y, 0);
	private final Vector3 worldDragCurrent = new Vector3();
	private final Vector3 worldDragLast = new Vector3();
	private final Vector3 worldGroundTarget = new Vector3();
	// Temporary
	private final Ray ray = new Ray();
	private final Vector3 tmp1 = new Vector3();
	private final Vector3 tmp2 = new Vector3();
	private final Quaternion deltaRotation = new Quaternion();
	private float zoom = GameSettings.CAMERA_MAX_ZOOM;
	private BoundingBox worldBoundingBox;

	public CameraController(GhostCamera camera) {
		this.camera = camera;
	}

	public void setWorldBoundingBox(BoundingBox worldBoundingBox) {
		this.worldBoundingBox = worldBoundingBox;
	}

	public void processDragPan(Ray dragCurrentRay, Ray lastDragProcessedRay) {
		// TODO:
		// Can probably be optimized, but simply storing worldDragLast.set(worldDragCurrent)
		// caused jitter for some reason.
		Intersector.intersectRayPlane(dragCurrentRay, worldGroundPlane, worldDragCurrent);
		Intersector.intersectRayPlane(lastDragProcessedRay, worldGroundPlane, worldDragLast);
		tmp1.set(worldDragLast).sub(worldDragCurrent);
		tmp1.y = 0;

		ray.origin.set(camera.position).add(tmp1);
		ray.direction.set(camera.direction);
		if (Intersector.intersectRayBoundsFast(ray, worldBoundingBox)) {
			camera.position.add(tmp1);
			worldGroundTarget.add(tmp1);
		}
	}

	public void processDragRotation(Vector2 cursorDelta) {
		tmp1.set(camera.direction).crs(camera.up).nor();
		deltaRotation.setEulerAngles(cursorDelta.x, cursorDelta.y * tmp1.x, cursorDelta.y * tmp1.z);
		camera.rotateAround(worldGroundTarget, deltaRotation);
	}

	public void processZoom(float amount) {
		zoom += GameSettings.CAMERA_ZOOM_STEP * amount;
		zoom = MathUtils.clamp(zoom, GameSettings.CAMERA_MIN_ZOOM, GameSettings.CAMERA_MAX_ZOOM);
		camera.position.set(camera.direction).nor().scl(-zoom).add(worldGroundTarget);
	}

	public void processKeyboardPan(Vector2 keysMoveDirection, float deltaTime) {
		tmp1.set(camera.direction).crs(camera.up).scl(keysMoveDirection.x);
		tmp1.add(tmp2.set(camera.direction).scl(keysMoveDirection.y));
		tmp1.y = 0;
		tmp1.nor().scl(deltaTime * GameSettings.CAMERA_MAX_PAN_VELOCITY);

		ray.origin.set(camera.position).add(tmp1);
		ray.direction.set(camera.direction);
		if (Intersector.intersectRayBoundsFast(ray, worldBoundingBox)) {
			camera.position.add(tmp1);
			worldGroundTarget.add(tmp1);
		}
	}

	public void processTouchDownLeft(Ray ray) {
		Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragCurrent);
		worldDragLast.set(worldDragCurrent);
	}

	public void processTouchDownRight() {
		ray.set(camera.position, camera.direction);
		Intersector.intersectRayPlane(ray, worldGroundPlane, worldGroundTarget);
	}


}
