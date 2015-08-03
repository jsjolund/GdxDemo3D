package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.components.MoveAimComponent;

/**
 * Created by user on 8/1/15.
 */
public class CameraMoveAimSystem extends EntitySystem {

	public Family systemFamily;
	private ImmutableArray<Entity> entities;
	private Camera camera;
	private ComponentMapper<MoveAimComponent> moveCmps = ComponentMapper.getFor(MoveAimComponent.class);

	public CameraMoveAimSystem(Camera camera) {
		systemFamily = Family.all(MoveAimComponent.class).get();

		this.camera = camera;
		camera.lookAt(Vector3.X);
		camera.update();
	}

	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}

	@Override
	public void update(float deltaTime) {
		if (entities.size() > 0) {
			Entity entity = entities.get(0);
			MoveAimComponent cmp = moveCmps.get(entity);
			camera.position.set(cmp.position);

			camera.position.add(cmp.cameraPosOffset);

			camera.direction.set(cmp.directionAim);
			camera.up.set(cmp.up);
			camera.update();
		}
	}

}
