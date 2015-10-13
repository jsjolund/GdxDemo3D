package com.mygdx.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.GhostCamera;

/**
 * Created by Johannes Sjolund on 10/12/15.
 */
public class CameraController {

	public final InputProcessor inputAdapter;
	private final Viewport viewport;
	private final GhostCamera camera;
	private final IntIntMap keys = new IntIntMap();
	private final Vector2 keyPanDirection = new Vector2();

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
	private BoundingBox worldBoundingBox;

	public CameraController(Viewport viewport, GhostCamera camera) {
		this.viewport = viewport;
		this.camera = camera;
		inputAdapter = new CameraInputAdapter();
	}

	public void update(float deltaTime) {
		keyPanDirection.setZero();
		if (keys.containsKey(GameSettings.KEY_PAN_FORWARD)) {
			keyPanDirection.y += 1;
		}
		if (keys.containsKey(GameSettings.KEY_PAN_BACKWARD)) {
			keyPanDirection.y -= 1;
		}
		if (keys.containsKey(GameSettings.KEY_PAN_LEFT)) {
			keyPanDirection.x -= 1;
		}
		if (keys.containsKey(GameSettings.KEY_PAN_RIGHT)) {
			keyPanDirection.x += 1;
		}
		keyPanDirection.nor();
		processKeyboardPan(keyPanDirection, deltaTime);
		camera.update(deltaTime, GameSettings.CAMERA_LERP_ALPHA);
	}

	public void setWorldBoundingBox(BoundingBox worldBoundingBox) {
		this.worldBoundingBox = worldBoundingBox;
	}

	private void processDragPan(Vector2 dragCurrent, Vector2 lastDragProcessed) {
		// TODO:
		// Can probably be optimized, but simply storing worldDragLast.set(worldDragCurrent)
		// caused jitter for some reason.
		ray.set(viewport.getPickRay(dragCurrent.x, dragCurrent.y));
		Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragCurrent);
		ray.set(viewport.getPickRay(lastDragProcessed.x, lastDragProcessed.y));
		Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragLast);
		tmp1.set(worldDragLast).sub(worldDragCurrent);
		tmp1.y = 0;

		ray.origin.set(camera.position).add(tmp1);
		ray.direction.set(camera.direction);
		if (Intersector.intersectRayBoundsFast(ray, worldBoundingBox)) {
			camera.position.add(tmp1);
			worldGroundTarget.add(tmp1);
		}

	}

	private void processDragRotation(Vector2 cursorDelta) {
		tmp1.set(camera.direction).crs(camera.up).nor();
		deltaRotation.setEulerAngles(cursorDelta.x, cursorDelta.y * tmp1.x, cursorDelta.y * tmp1.z);
		camera.rotateAround(worldGroundTarget, deltaRotation);
	}

	private void processZoom(float zoom) {
		camera.position.set(camera.direction).nor().scl(zoom).add(worldGroundTarget);
	}

	private void processKeyboardPan(Vector2 keysMoveDirection, float deltaTime) {
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

	public class CameraInputAdapter extends InputAdapter {

		Vector2 lastDragProcessed = new Vector2();
		Vector2 cursorDelta = new Vector2();
		Vector2 dragCurrent = new Vector2();
		boolean isDragging = false;
		float zoom = GameSettings.CAMERA_MAX_ZOOM;

		@Override
		public boolean keyDown(int keycode) {
			keys.put(keycode, keycode);
			if (keycode == GameSettings.KEY_PAUSE) {
				if (GameSettings.GAME_SPEED == 1) {
					GameSettings.GAME_SPEED = 0;
				} else if (GameSettings.GAME_SPEED == 0) {
					GameSettings.GAME_SPEED = 0.05f;
				} else if (GameSettings.GAME_SPEED == 0.05f) {
					GameSettings.GAME_SPEED = 1;
				}
			}
			return true;
		}

		@Override
		public boolean keyUp(int keycode) {
			keys.remove(keycode, 0);
			return true;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			lastDragProcessed.set(screenX, screenY);
			if (button == Input.Buttons.LEFT) {
				ray.set(viewport.getPickRay(screenX, screenY));
				Intersector.intersectRayPlane(ray, worldGroundPlane, worldDragCurrent);
				worldDragLast.set(worldDragCurrent);

			} else if (button == Input.Buttons.RIGHT) {
				ray.set(camera.position, camera.direction);
				Intersector.intersectRayPlane(ray, worldGroundPlane, worldGroundTarget);
			}
			return true;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			dragCurrent.setZero();
			lastDragProcessed.setZero();

			if (isDragging) {
				isDragging = false;
				return true;
			}
			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			isDragging = true;
			dragCurrent.set(screenX, screenY);
			if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
				processDragPan(dragCurrent, lastDragProcessed);

			} else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
				cursorDelta.set(lastDragProcessed).sub(screenX, screenY).scl(GameSettings.MOUSE_SENSITIVITY);
				processDragRotation(cursorDelta);
			}
			lastDragProcessed.set(screenX, screenY);

			return true;
		}

		@Override
		public boolean scrolled(int amount) {
			zoom += GameSettings.CAMERA_ZOOM_STEP * amount;
			zoom = MathUtils.clamp(zoom, GameSettings.CAMERA_MIN_ZOOM, GameSettings.CAMERA_MAX_ZOOM);
			processZoom(-zoom);
			return true;
		}

	}
}
