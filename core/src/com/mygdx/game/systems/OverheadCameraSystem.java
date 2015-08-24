package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.components.PhysicsComponent;
import com.mygdx.game.components.SelectableComponent;

/**
 * Created by user on 8/24/15.
 */
public class OverheadCameraSystem extends EntitySystem {

	public static final String tag = "OverheadCameraSystem";

	public Family family;
	private ImmutableArray<Entity> entities;
	private Camera camera;

	private ComponentMapper<SelectableComponent> selCmps = ComponentMapper.getFor(SelectableComponent.class);
	private ComponentMapper<PhysicsComponent> phyCmps = ComponentMapper.getFor(PhysicsComponent.class);

	private Vector3 pos = new Vector3();

	float cameraDst = 10;
	Vector3 cameraDir = new Vector3(0.5f, -1, 0.5f);
	Vector3 cameraOffset = new Vector3();

	public OverheadCameraSystem(Camera camera) {
		family = Family.all(SelectableComponent.class, PhysicsComponent.class).get();

		this.camera = camera;
		cameraOffset.set(cameraDir.cpy().scl(cameraDst, -cameraDst, cameraDst));
		camera.position.set(Vector3.Zero).add(cameraOffset);
		camera.lookAt(cameraDir);
		camera.update();
	}

	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(family);
	}

	@Override
	public void update(float deltaTime) {
		for (Entity entity : entities) {
			SelectableComponent selCmp = selCmps.get(entity);
			if (selCmp.isSelected && !selCmp.hasCameraFocus) {
				selCmp.hasCameraFocus = true;
				PhysicsComponent phyCmp = phyCmps.get(entity);
				phyCmp.body.getWorldTransform().getTranslation(pos);
				camera.position.set(pos.add(cameraOffset));
				camera.update();
				Gdx.app.debug(tag, "Moving camera to " + camera.position);
			}
		}
	}
}
