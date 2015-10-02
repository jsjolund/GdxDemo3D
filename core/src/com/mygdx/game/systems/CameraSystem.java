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
import com.mygdx.game.components.IntentBroadcastComponent;

/**
 * Created by user on 8/24/15.
 */
public class CameraSystem extends IteratingSystem {

	public static final String tag = "CameraSystem";
	private final Vector3 panDirection = new Vector3();
	private final Vector3 panResult = new Vector3();
	private final Vector3 tmp = new Vector3();
	private float currentPanSpeed = 0;

	private final Plane worldGroundPlane = new Plane(Vector3.Y, 0);
	private final Vector3 worldDragCurrent = new Vector3();
	private final Vector3 worldDragLast = new Vector3();
	private final Vector3 worldGroundTarget = new Vector3();
	private float worldGroundTargetDst;

	private final Vector2 cursorDelta = new Vector2();
	private final Vector3 perpendicular = new Vector3();
	private final Ray ray = new Ray();
	private float currentZoom = 10;
	private final Vector2 lastDragProcessed = new Vector2();
	private final Vector3 straightDown = new Vector3(Vector3.Y).scl(1,-1,1);
	private final Vector3 straightUp = new Vector3(Vector3.Y);

	private final  ComponentMapper<CameraTargetingComponent> camCmps =
			ComponentMapper.getFor(CameraTargetingComponent.class);
	private final  ComponentMapper<IntentBroadcastComponent> inputCmps =
			ComponentMapper.getFor(IntentBroadcastComponent.class);

	public CameraSystem() {
		super(Family.all(CameraTargetingComponent.class, IntentBroadcastComponent.class).get());
	}

	@Override
	public void processEntity(Entity entity, float deltaTime) {
		IntentBroadcastComponent intent = inputCmps.get(entity);

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

						cursorDelta.set(lastDragProcessed).sub(intent.dragCurrent).scl(GameSettings
								.MOUSE_SENSITIVITY).scl(worldGroundTargetDst);
						perpendicular.set(cam.direction).crs(cam.up).nor();

						panResult.setZero().add(cam.up).nor().scl(-cursorDelta.y);
						cam.position.add(panResult);
						panResult.setZero().add(perpendicular).nor().scl(cursorDelta.x * poleDst / 2);
						cam.position.add(panResult);
						panResult.setZero().add(cam.direction).nor().scl(cam.position.dst
								(worldGroundTarget) - worldGroundTargetDst);
						cam.position.add(panResult);

						cam.up.set(Vector3.Y);
						cam.lookAt(worldGroundTarget);

						// Try to prevent camera from flipping up axis when looking close to straight up/down.
						// Probably not a good solution...
						boolean poleIsCrossed = (Math.signum(cam.direction.x) != Math.signum(oldDirection.x))
								&& Math.signum(cam.direction.z) != Math.signum(oldDirection.z);
						if (poleIsCrossed || cam.direction.epsilonEquals(straightDown,0.001f)  || cam.direction
								.epsilonEquals(straightUp,0.001f) ) {
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
