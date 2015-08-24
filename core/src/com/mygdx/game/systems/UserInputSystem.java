package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.SelectableComponent;

/**
 * Created by user on 8/24/15.
 */
public class UserInputSystem extends EntitySystem implements InputProcessor {

	public static final String tag = "UserInputSystem";

	private final IntIntMap keys = new IntIntMap();
	private final Viewport viewport;
	public final Family family;
	private ImmutableArray<Entity> entities;
	private final ComponentMapper<SelectableComponent> selCmps = ComponentMapper.getFor(SelectableComponent.class);
	private Ray ray = new Ray();
	private final Vector3 tmp = new Vector3();

	private final PhysicsSystem phySys;

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(family);
	}

	public UserInputSystem(Viewport viewport, PhysicsSystem phySys) {
		this.phySys = phySys;
		this.viewport = viewport;
		family = Family.all(SelectableComponent.class).get();
	}

	@Override
	public void update(float deltaTime) {

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
		ray = viewport.getCamera().getPickRay(screenX, screenY);
		float distance = 1000;
		Gdx.app.debug(tag, String.format("Pick ray: origin: %s, direction: %s.", ray.origin, ray.direction));

		Entity hitEntity = phySys.rayTest(ray, tmp,
				(short) (PhysicsSystem.GROUND_FLAG
						| PhysicsSystem.OBJECT_FLAG),
				distance);

		if (hitEntity != null) {
			Gdx.app.debug(tag, "Hit entity: " + hitEntity);

			// Deselect previous
			for (Entity selectable : entities) {
				SelectableComponent selCmp = selectable.getComponent(SelectableComponent.class);
				if (selCmp != null) {
					selCmp.isSelected = false;
				}
			}

			SelectableComponent selCmp = hitEntity.getComponent(SelectableComponent.class);
			if (selCmp != null) {
				selCmp.isSelected = true;
				selCmp.hasCameraFocus = false;
			}
		}
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}
