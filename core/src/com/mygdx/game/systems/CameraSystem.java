package com.mygdx.game.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.GameSettings;
import com.mygdx.game.components.IntentBroadcastComponent;

/**
 * Created by user on 8/24/15.
 */
public class CameraSystem extends EntitySystem {

	public static final String tag = "CameraSystem";
	private final Vector3 panDirection = new Vector3();
	private final Vector3 panResult = new Vector3();
	private final Vector3 tmp = new Vector3();
	private final Plane worldGroundPlane = new Plane(Vector3.Y, 0);
	private final Vector3 worldDragCurrent = new Vector3();
	private final Vector3 worldDragLast = new Vector3();
	private final Vector3 worldGroundTarget = new Vector3();
	private final Vector2 cursorDelta = new Vector2();
	private final Ray ray = new Ray();
	private final Vector2 lastDragProcessed = new Vector2();
	private float currentPanSpeed = 0;
	private float currentZoom = 5;
	private Vector3 velocity = new Vector3();
	Quaternion deltaRotation = new Quaternion();
	private IntentBroadcastComponent intent;
	private Camera cam;
	private Viewport viewport;

	public CameraSystem(Viewport viewport, Camera cam, IntentBroadcastComponent intent) {
		this.viewport = viewport;
		this.cam = cam;
		this.intent = intent;
	}

	@Override
	public void update(float deltaTime) {
		// Keyboard pan
		panDirection.setZero();
		if (intent.moveDirection.len() >= 0) {
			tmp.set(cam.direction).scl(intent.moveDirection.y);
			panDirection.add(tmp);
			tmp.set(cam.direction).crs(cam.up).scl(intent.moveDirection.x);
			panDirection.add(tmp);
			panDirection.y = 0;
			panDirection.nor();
		}

		if (panDirection.isZero()) {
			velocity.nor();
			currentPanSpeed -= GameSettings.CAMERA_PAN_DECELERATION * deltaTime;
			if (currentPanSpeed < 0) {
				currentPanSpeed = 0;
			}
		} else {
			velocity.set(panDirection);
			currentPanSpeed += GameSettings.CAMERA_PAN_ACCELERATION * deltaTime;
		}

		if (currentPanSpeed > GameSettings.CAMERA_MAX_PAN_VELOCITY) {
			currentPanSpeed = GameSettings.CAMERA_MAX_PAN_VELOCITY;
		}
		velocity.scl(currentPanSpeed);

		panResult.set(velocity).scl(deltaTime);
		cam.position.add(panResult);

		// Scroll wheel zoom
		if (intent.zoom != currentZoom) {
			ray.set(cam.position, cam.direction);
			Intersector.intersectRayPlane(ray, worldGroundPlane, worldGroundTarget);
			float zoomLength = worldGroundTarget.dst(cam.position) / currentZoom;
			float zoomSteps = currentZoom - intent.zoom;
			cam.position.add(tmp.set(cam.direction).nor().scl(zoomLength * zoomSteps));
			currentZoom = intent.zoom;
		}

		// Mouse dragging
		if (!intent.isDragging) {
			lastDragProcessed.setZero();
		} else {
			if (!lastDragProcessed.equals(intent.dragCurrent)) {
				if (lastDragProcessed.isZero()) {
					// This is run every time the user starts dragging
					ray.set(cam.position, cam.direction);
					Intersector.intersectRayPlane(ray, worldGroundPlane, worldGroundTarget);

				} else {
					// Mouse drag pan
					if (intent.pan) {
						ray.set(viewport.getPickRay(intent.dragCurrent.x, intent.dragCurrent.y));

						Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragCurrent);
						ray.set(viewport.getPickRay(lastDragProcessed.x, lastDragProcessed.y));
						Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragLast);

						panResult.set(worldDragLast).sub(worldDragCurrent);
						panResult.y = 0;
						cam.position.add(panResult);

					}

					// Mouse drag rotate
					if (intent.rotate) {
						cursorDelta.set(lastDragProcessed).sub(intent.dragCurrent).scl(GameSettings
								.MOUSE_SENSITIVITY).scl(10);
						tmp.set(cam.direction).crs(cam.up).nor();
						deltaRotation.setEulerAngles(cursorDelta.x, cursorDelta.y * tmp.x, cursorDelta.y * tmp.z);
						tmp.set(worldGroundTarget).sub(cam.position);
						cam.translate(tmp);
						cam.rotate(deltaRotation);
						deltaRotation.transform(tmp);
						cam.translate(-tmp.x, -tmp.y, -tmp.z);
						cam.update();
					}

				}
				lastDragProcessed.set(intent.dragCurrent);
			}
		}

		cam.update();
	}
}
