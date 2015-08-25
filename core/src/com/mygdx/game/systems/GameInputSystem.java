package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
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
	Vector2 moveDirection = new Vector2();
	float zoom;

	public final Family family;
	private ImmutableArray<Entity> entities;
	private final ComponentMapper<IntentComponent> inputCmps = ComponentMapper.getFor(IntentComponent
			.class);


	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(family);
	}

	public GameInputSystem() {
		family = Family.all(IntentComponent.class).get();
		zoom = GameSettings.CAMERA_MAX_ZOOM;
	}


	@Override
	public void update(float deltaTime) {

		moveDirection.setZero();
		float movementSpeedFactor = 1;
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

		for (Entity entity : entities) {
			IntentComponent intent = inputCmps.get(entity);
			intent.moveDirection.set(moveDirection);
			intent.zoom = zoom;
		}

		if (moveDirection.isZero() && touchMap.containsKey(0)) {
			TouchData data = touchMap.get(0);
			if (data.isDragging) {
				switch (data.button) {
					case Input.Buttons.LEFT:
						for (Entity entity : entities) {
							IntentComponent intent = inputCmps.get(entity);
							intent.dragStart.set(data.down);
							intent.dragCurrent.set(data.lastDrag);
							intent.isDragging = true;
						}
						data.isDragging = false;
						break;
					case Input.Buttons.RIGHT:
						break;
					default:
						break;
				}
			}
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
//		System.out.println(String.format("Down button %s, pointer %s, coords (%s,%s)", button, pointer, screenX,screenY));
		if (!touchMap.containsKey(pointer)) {
			touchMap.put(pointer, new TouchData());
		}
		TouchData data = touchMap.get(pointer);
		if (data.button == -1) {
			data.down.set(screenX, screenY);
			data.button = button;
		}
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
			down.setZero();
			lastDrag.setZero();
			isDragging = false;
			button = -1;
			dragHistoryCursor = 0;
		}
	}

	ArrayMap<Integer, TouchData> touchMap = new ArrayMap<Integer, TouchData>();

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
//		System.out.println(String.format("Up button %s, pointer %s, coords (%s,%s)", button, pointer, screenX, screenY));
		touchMap.get(pointer).reset();
		for (Entity entity : entities) {
			IntentComponent intent = inputCmps.get(entity);
			intent.dragStart.setZero();
			intent.dragCurrent.setZero();
			intent.isDragging = false;
	}
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
//		System.out.println(String.format("Drag (%s,%s), pointer %s", screenX, screenY, pointer));
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
