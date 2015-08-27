package com.mygdx.game.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.IntIntMap;
import com.mygdx.game.GameSettings;
import com.mygdx.game.components.IntentComponent;

/**
 * Created by user on 8/24/15.
 */
public class GameInputSystem extends EntitySystem implements InputProcessor {

	public static final String tag = "GameInputSystem";

	public final IntIntMap keys = new IntIntMap();
	public final Family family;
	IntentComponent intent;
	Vector2 moveDirection = new Vector2();
	float zoom;
	ArrayMap<Integer, TouchData> touchMap = new ArrayMap<Integer, TouchData>();
//	private ImmutableArray<Entity> entities;

	public GameInputSystem(IntentComponent intent) {
		family = Family.all(IntentComponent.class).get();
		zoom = GameSettings.CAMERA_MAX_ZOOM;
		this.intent = intent;
	}

	@Override
	public void addedToEngine(Engine engine) {
//		entities = engine.getEntitiesFor(family);
	}

	@Override
	public void update(float deltaTime) {

		moveDirection.setZero();
		if (keys.containsKey(GameSettings.PAN_FORWARD)) {
			moveDirection.y += 1;
		}
		if (keys.containsKey(GameSettings.PAN_BACKWARD)) {
			moveDirection.y -= 1;
		}
		if (keys.containsKey(GameSettings.PAN_LEFT)) {
			moveDirection.x -= 1;
		}
		if (keys.containsKey(GameSettings.PAN_RIGHT)) {
			moveDirection.x += 1;
		}
		moveDirection.nor();

		if (zoom > GameSettings.CAMERA_MAX_ZOOM) {
			zoom = GameSettings.CAMERA_MAX_ZOOM;
		} else if (zoom < GameSettings.CAMERA_MIN_ZOOM) {
			zoom = GameSettings.CAMERA_MIN_ZOOM;
		}

		intent.moveDirection.set(moveDirection);
		intent.zoom = zoom;

		if (moveDirection.isZero() && touchMap.containsKey(0)) {
			TouchData data = touchMap.get(0);
			if (data.isDragging) {
				intent.dragStart.set(data.down);
				intent.dragCurrent.set(data.lastDrag);
				intent.isDragging = true;
				intent.pan = false;
				intent.rotate = false;
				switch (data.button) {
					case Input.Buttons.LEFT:
						intent.pan = true;
						break;
					case Input.Buttons.RIGHT:
						intent.rotate = true;
						break;
					default:
						break;
				}
			}
			data.isDragging = false;
		}


	}

	@Override
	public boolean keyDown(int keycode) {
		keys.put(keycode, keycode);
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		keys.remove(keycode, 0);
		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (!touchMap.containsKey(pointer)) {
			touchMap.put(pointer, new TouchData());
		}
		TouchData data = touchMap.get(pointer);
		if (data.button == -1) {
			data.down.set(screenX, screenY);
			data.button = button;
			data.isDragging = false;
		}
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (!intent.isDragging) {
			intent.click.set(screenX, screenY);
		}
		intent.dragStart.setZero();
		intent.dragCurrent.setZero();
		intent.isDragging = false;
		intent.pan = false;
		intent.rotate = false;
		touchMap.get(pointer).reset();

		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		TouchData data = touchMap.get(pointer);
		data.lastDrag.set(screenX, screenY);
		data.isDragging = true;
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		zoom += GameSettings.CAMERA_ZOOM_STEP * amount;
		return true;
	}

	private class TouchData {
		int dragHistoryCursor = 0;
		Vector2 down = new Vector2();
		Vector2 lastDrag = new Vector2();
		int button;
		boolean isDragging = false;

		public TouchData() {
			reset();
		}

		public void reset() {
			dragHistoryCursor = 0;
			down.setZero();
			lastDrag.setZero();
			button = -1;
			isDragging = false;
		}
	}

//	@Override
//	public boolean mouseMoved(int screenX, int screenY) {
//		// Perform camera mouse look
//		float mouseSens = GameSettings.MOUSE_SENSITIVITY;
//
//		directionOld.set(player.direction);
//
//		float mouseDx = screenX - player.screenCenter.x;
//		float mouseDy = screenY - player.screenCenter.y;
//
//		player.direction.rotate(
//				xzMouseRotation.set(player.direction).crs(Vector3.Y),
//				-mouseSens * mouseDy);
//		player.direction.rotate(Vector3.Y, -mouseSens * mouseDx);
//
//		if ((Math.signum(player.direction.x) != Math.signum(directionOld.x))
//				&& Math.signum(player.direction.z) != Math.signum(directionOld.z)) {
//			player.direction.set(directionOld);
//		}
//
//		player.direction.nor();
//		centerMouseCursor();
//		return true;
//	}

}
