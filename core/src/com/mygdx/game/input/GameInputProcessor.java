package com.mygdx.game.input;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.CameraController;
import com.mygdx.game.utilities.Observable;
import com.mygdx.game.utilities.Observer;

/**
 * Created by Johannes Sjolund on 10/17/15.
 */
public class GameInputProcessor implements InputProcessor, Observable {

	private final Ray dragCurrentRay = new Ray();
	private final Ray lastDragProcessedRay = new Ray();
	private final Ray touchDownRay = new Ray();
	private final Ray touchUpRay = new Ray();

	private final Vector2 lastDragProcessed = new Vector2();
	private final Vector2 cursorDelta = new Vector2();
	private final Vector2 dragCurrent = new Vector2();
	private final Vector2 keyPanDirection = new Vector2();

	private final IntIntMap keys = new IntIntMap();
	private final Viewport viewport;
	private final CameraController cameraController;
	private final SelectionController selectionController;
	private boolean isDragging = false;

	public GameInputProcessor(Viewport viewport,
							  CameraController cameraController,
							  SelectionController selectionController) {
		this.viewport = viewport;
		this.cameraController = cameraController;
		this.selectionController = selectionController;
	}

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
		if (keycode == Input.Keys.F5) {
			selectionController.killSelectedCharacter();
		}
		if (keycode == Input.Keys.NUM_0) {
			notifyObserversLayerSelected(0);
		}
		if (keycode == Input.Keys.NUM_1) {
			notifyObserversLayerSelected(1);
		}
		if (keycode == Input.Keys.NUM_2) {
			notifyObserversLayerSelected(2);
		}
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
		lastDragProcessed.set(screenX, screenY);

		if (button == Input.Buttons.LEFT) {
			touchDownRay.set(viewport.getPickRay(screenX, screenY));
			cameraController.processTouchDownLeft(touchDownRay);

		} else if (button == Input.Buttons.RIGHT) {
			cameraController.processTouchDownRight();
		}
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (!isDragging) {
			touchUpRay.set(viewport.getPickRay(screenX, screenY));
			selectionController.processTouch(touchUpRay);
		}
		isDragging = false;
		dragCurrent.setZero();
		lastDragProcessed.setZero();
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		isDragging = true;
		dragCurrent.set(screenX, screenY);

		if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
			dragCurrentRay.set(viewport.getPickRay(dragCurrent.x, dragCurrent.y));
			lastDragProcessedRay.set(viewport.getPickRay(lastDragProcessed.x, lastDragProcessed.y));
			cameraController.processDragPan(dragCurrentRay, lastDragProcessedRay);

		} else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
			cursorDelta.set(lastDragProcessed).sub(screenX, screenY).scl(GameSettings.MOUSE_SENSITIVITY);
			cameraController.processDragRotation(cursorDelta);
		}
		lastDragProcessed.set(screenX, screenY);

		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		cameraController.processZoom(amount);
		return true;
	}

	public void update(float deltaTime) {
		keyPanDirection.setZero();
		if (keys.containsKey(GameSettings.KEY_PAN_FORWARD)) {
			keyPanDirection.y++;
		}
		if (keys.containsKey(GameSettings.KEY_PAN_BACKWARD)) {
			keyPanDirection.y--;
		}
		if (keys.containsKey(GameSettings.KEY_PAN_LEFT)) {
			keyPanDirection.x--;
		}
		if (keys.containsKey(GameSettings.KEY_PAN_RIGHT)) {
			keyPanDirection.x++;
		}
		if (!keyPanDirection.isZero()) {
			keyPanDirection.nor();
			cameraController.processKeyboardPan(keyPanDirection, deltaTime);
		}
	}

	@Override
	public void addObserver(Observer observer) {

	}

	@Override
	public void removeObserver(Observer observer) {

	}

	@Override
	public void notifyObserversEntitySelected(Entity entity) {

	}

	@Override
	public void notifyObserversLayerSelected(int layer) {

	}
}