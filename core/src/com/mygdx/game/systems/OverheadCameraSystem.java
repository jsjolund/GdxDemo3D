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


	private ComponentMapper<CameraTargetingComponent> camCmps =
			ComponentMapper.getFor(CameraTargetingComponent.class);
	private ComponentMapper<IntentComponent> inputCmps =
			ComponentMapper.getFor(IntentComponent.class);


	public OverheadCameraSystem() {
		super(Family.all(CameraTargetingComponent.class, IntentComponent.class).get());
	}

	Vector3 panDirection = new Vector3();
	Vector3 panResult = new Vector3();
	Vector3 tmp = new Vector3();
	float currentPanSpeed = 0;


	Vector3 worldDragCurrent = new Vector3();
	Vector3 worldDragLast = new Vector3();
	Plane worldGroundPlane = new Plane(Vector3.Y, 0);
	Ray ray = new Ray();

	float currentZoom = 10;
	Vector3 cameraGroundTarget = new Vector3();

	Vector2 lastDragProcessed = new Vector2();


	@Override
	public void processEntity(Entity entity, float deltaTime) {
		IntentComponent intent = inputCmps.get(entity);

		CameraTargetingComponent camCmp = camCmps.get(entity);
		Camera cam = camCmp.camera;
		Viewport viewport = camCmp.viewport;
		Vector3 velocity = camCmp.velocity;

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

		if (intent.zoom != currentZoom) {

			ray.set(cam.position, cam.direction);
			Intersector.intersectRayPlane(ray, worldGroundPlane, cameraGroundTarget);
			float zoomLength = cameraGroundTarget.dst(cam.position) / currentZoom;
			float zoomSteps = currentZoom - intent.zoom;
			cam.position.add(tmp.set(cam.direction).nor().scl(zoomLength * zoomSteps));
			currentZoom = intent.zoom;
		}

		if (!intent.isDragging) {
			lastDragProcessed.setZero();
		} else {
			if (!lastDragProcessed.equals(intent.dragCurrent)) {
				if (!lastDragProcessed.isZero()) {
					ray.set(viewport.getPickRay(intent.dragCurrent.x, intent.dragCurrent.y));

					Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragCurrent);
					ray.set(viewport.getPickRay(lastDragProcessed.x, lastDragProcessed.y));
					Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragLast);

					panResult.set(worldDragLast).sub(worldDragCurrent);
					panResult.y = 0;

				}
				lastDragProcessed.set(intent.dragCurrent);
			}
		}


		cam.position.add(panResult);
		cam.update();
	}
}
