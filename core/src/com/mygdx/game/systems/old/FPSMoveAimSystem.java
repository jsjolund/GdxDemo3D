package com.mygdx.game.systems.old;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;
import com.mygdx.game.GameSettings;

/**
 * Created by user on 8/1/15.
 */
public class FPSMoveAimSystem extends EntitySystem implements InputProcessor {

	private final IntIntMap keys = new IntIntMap();
	public Family systemFamily;

	private Vector3 directionAim = new Vector3(Vector3.X);
	private Vector3 directionAimOld = new Vector3(Vector3.X);
	private Vector3 directionMove = new Vector3();
	private Vector3 xzRotationAim = new Vector3();
	private Vector3 moveVector = new Vector3();
	private Vector3 up = new Vector3(Vector3.Y);

	private Vector2 screenCenter = new Vector2();

	private boolean captureMouse = false;

	private ImmutableArray<Entity> entities;
	private ComponentMapper<MoveAimComponent> moveComponents = ComponentMapper.getFor(MoveAimComponent.class);

	public FPSMoveAimSystem() {
		systemFamily = Family.all(MoveAimComponent.class).get();
	}

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
		Gdx.input.setCursorCatched(true);
		captureMouse = true;

	}

	@Override
	public boolean keyDown(int keycode) {
		keys.put(keycode, keycode);
		if (keycode == Input.Keys.ESCAPE) {
			if (captureMouse) {
				Gdx.input.setCursorCatched(false);
				captureMouse = false;
			} else {
				Gdx.input.setCursorCatched(true);
				captureMouse = true;
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
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	public void centerMouseCursor() {
		Gdx.input.setCursorPosition((int) screenCenter.x, (int) screenCenter.y);
	}

	private void calculateMoveDirection() {
		directionMove.setZero();

		if (keys.containsKey(GameSettings.FORWARD)) {
			directionMove.add(directionAim);
		}
		if (keys.containsKey(GameSettings.BACKWARD)) {
			directionMove.sub(directionAim);
		}
		if (keys.containsKey(GameSettings.STRAFE_LEFT)) {
			moveVector.setZero().sub(directionAim).crs(up);
			directionMove.add(moveVector);
		}
		if (keys.containsKey(GameSettings.STRAFE_RIGHT)) {
			moveVector.setZero().add(directionAim).crs(up);
			directionMove.add(moveVector);
		}
		directionMove.nor();
	}

	@Override
	public void update(float deltaTime) {
		calculateMoveDirection();

		for (int i = 0; i < entities.size(); ++i) {
			Entity entity = entities.get(i);

			MoveAimComponent moveCmp = moveComponents.get(entity);

			moveCmp.directionAim.set(directionAim);
			moveCmp.directionMove.set(directionMove);
			moveCmp.up.set(up);
		}
	}


	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		float mouseSens = GameSettings.MOUSE_SENSITIVITY;

		directionAimOld.set(directionAim);

		float mouseDx = screenX - screenCenter.x;
		float mouseDy = screenY - screenCenter.y;

		directionAim.rotate(
				xzRotationAim.set(directionAim).crs(Vector3.Y),
				-mouseSens * mouseDy);
		directionAim.rotate(Vector3.Y, -mouseSens * mouseDx);

		if ((Math.signum(directionAim.x) != Math.signum(directionAimOld.x))
				&& Math.signum(directionAim.z) != Math.signum(directionAimOld.z)) {
			directionAim.set(directionAimOld);
		}
		directionAim.nor();
		centerMouseCursor();
		return true;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}
