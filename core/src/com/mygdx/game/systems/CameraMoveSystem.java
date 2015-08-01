package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Camera;
import com.mygdx.game.components.MoveAimComponent;

/**
 * Created by user on 8/1/15.
 */
public class CameraMoveSystem extends EntitySystem {

	public Family systemFamily;
	private ImmutableArray<Entity> entities;
	private Camera camera;
	private ComponentMapper<MoveAimComponent> moveCmps = ComponentMapper.getFor(MoveAimComponent.class);

	public CameraMoveSystem(Camera camera) {
		systemFamily = Family.all(MoveAimComponent.class).get();
		this.camera = camera;
	}

	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}

	@Override
	public void update(float deltaTime) {
		camera.update();
//		for (int i = 0; i < entities.size(); ++i) {
//			Entity entity = entities.get(i);
//			MoveAimComponent cmp = moveCmps.get(entity);
//		}
		Entity entity = entities.get(0);
		MoveAimComponent cmp = moveCmps.get(entity);
		camera.position.set(cmp.position);
		camera.position.add(cmp.directionAim.cpy().scl(-10));
		camera.position.add(cmp.up.cpy().scl(2));
		camera.direction.set(cmp.directionAim);
		camera.up.set(cmp.up);
		camera.update();

	}


}
