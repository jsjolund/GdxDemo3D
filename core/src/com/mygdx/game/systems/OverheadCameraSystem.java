package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.GameSettings;
import com.mygdx.game.components.CameraTargetingComponent;
import com.mygdx.game.components.IntentComponent;

/**
 * Created by user on 8/24/15.
 */
public class OverheadCameraSystem extends IteratingSystem {

	public static final String tag = "OverheadCameraSystem";
	Vector3 panDirection = new Vector3();
	Vector3 panResult = new Vector3();
	Vector3 tmp = new Vector3();
	float currentPanSpeed = 0;

	Plane worldGroundPlane = new Plane(Vector3.Y, 0);
	Vector3 worldDragCurrent = new Vector3();
	Vector3 worldDragLast = new Vector3();
	Vector3 worldGroundTarget = new Vector3();
	float worldGroundTargetDst;

	Vector2 cursorDelta = new Vector2();
	Vector3 perpendicular = new Vector3();
	Ray ray = new Ray();
	float currentZoom = 10;
	Vector2 lastDragProcessed = new Vector2();
	private ComponentMapper<CameraTargetingComponent> camCmps =
			ComponentMapper.getFor(CameraTargetingComponent.class);
	private ComponentMapper<IntentComponent> inputCmps =
			ComponentMapper.getFor(IntentComponent.class);

	public OverheadCameraSystem() {
		super(Family.all(CameraTargetingComponent.class, IntentComponent.class).get());
	}


	private boolean poleIsCrossed(Vector3 direction1, Vector3 direction2) {
		return (Math.signum(direction1.x) != Math.signum(direction2.x))
				&& Math.signum(direction1.z) != Math.signum(direction2.z);
	}


	@Override
	public void processEntity(Entity entity, float deltaTime) {
		IntentComponent intent = inputCmps.get(entity);

		CameraTargetingComponent camCmp = camCmps.get(entity);
		Camera cam = camCmp.camera;
		Viewport viewport = camCmp.viewport;
		Vector3 velocity = camCmp.velocity;

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
					worldGroundTargetDst = cam.position.dst(worldGroundTarget);

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

					// Mouse drag rotation
					if (intent.rotate) {
						Vector3 oldPosition = new Vector3();
						Vector3 oldDirection = new Vector3();
						Vector3 oldUp = new Vector3();

						float southPoleDst = tmp.set(cam.direction).nor().dst(Vector3.Y);
						float northPoleDst = tmp.set(cam.direction).scl(-1).nor().dst(Vector3.Y);
						float poleDst = Math.min(northPoleDst, southPoleDst);

						oldPosition.set(cam.position);
						oldDirection.set(cam.direction);
						oldUp.set(cam.up);

						cursorDelta.set(lastDragProcessed).sub(intent.dragCurrent).scl(GameSettings.MOUSE_SENSITIVITY);
						perpendicular.set(cam.direction).crs(cam.up).nor();

						panResult.setZero().add(cam.up).nor().scl(-cursorDelta.y);
						cam.position.add(panResult);
						panResult.setZero().add(perpendicular).nor().scl(cursorDelta.x * poleDst/2);
						cam.position.add(panResult);
						panResult.setZero().add(cam.direction).nor().scl(cam.position.dst
								(worldGroundTarget) - worldGroundTargetDst);
						cam.position.add(panResult);

						cam.up.set(Vector3.Y);
						cam.lookAt(worldGroundTarget);

						boolean poleIsCrossed = (Math.signum(cam.direction.x) != Math.signum(oldDirection.x))
								&& Math.signum(cam.direction.z) != Math.signum(oldDirection.z);

						if (poleIsCrossed) {
							cam.direction.set(oldDirection);
							cam.position.set(oldPosition);
							cam.up.set(oldUp);
							cam.normalizeUp();
						}

					}
				}
				lastDragProcessed.set(intent.dragCurrent);
			}
		}

		cam.update();
	}
}
