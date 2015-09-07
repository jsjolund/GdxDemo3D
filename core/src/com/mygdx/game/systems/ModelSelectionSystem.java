package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.IntentBroadcastComponent;
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.components.SelectableComponent;

/**
 * Created by user on 8/25/15.
 */
public class ModelSelectionSystem extends EntitySystem {

	public static final String tag = "ModelSelectionSystem";
	private final Vector3 surfacePoint = new Vector3();
	public Family systemFamily;
	PhysicsSystem phySys;
	Viewport viewport;
	Vector2 lastClick = new Vector2();
	private ImmutableArray<Entity> entities;
	private ComponentMapper<IntentBroadcastComponent> intentCmps = ComponentMapper.getFor(IntentBroadcastComponent.class);
	private ComponentMapper<SelectableComponent> selCmps = ComponentMapper.getFor(SelectableComponent.class);
	private ComponentMapper<PathFindingComponent> pathCmps = ComponentMapper.getFor(PathFindingComponent.class);
	private Ray ray = new Ray();

	public ModelSelectionSystem(PhysicsSystem phySys, Viewport viewport) {
		systemFamily = Family.all(IntentBroadcastComponent.class, SelectableComponent.class).get();
		this.phySys = phySys;
		this.viewport = viewport;
	}

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}


	@Override
	public void update(float deltaTime) {
		if (entities.size() == 0) {
			return;
		}
		IntentBroadcastComponent intent = intentCmps.get(entities.get(0));
		if (intent.click.equals(lastClick) && !intent.doubleClick) {
			return;
		}
		lastClick.set(intent.click);
		float screenX = intent.click.x;
		float screenY = intent.click.y;

		ray.set(viewport.getPickRay(screenX, screenY));
		float distance = 100;
		Gdx.app.debug(tag, String.format(
				"Mouse: %s, %s, Pick ray: origin: %s, direction: %s.",
				screenX, screenY, ray.origin, ray.direction));

		Entity hitEntity = phySys.rayTest(ray, surfacePoint,
				PhysicsSystem.ALL_FLAG,
				distance);

		if (hitEntity == null) {
			return;
		}

		Gdx.app.debug(tag, "Hit entity: " + hitEntity);

		SelectableComponent hitEntitySelCmp = hitEntity.getComponent(SelectableComponent.class);
		if (hitEntitySelCmp != null) {
			// If hit entity is selectable, deselect previous, select this one
			for (Entity entity : entities) {
				SelectableComponent selCmp = entity.getComponent(SelectableComponent.class);
				if (selCmp != null) {
					selCmp.isSelected = false;
				}
			}
			hitEntitySelCmp.isSelected = true;

		} else {
			// Hit entity not selectable, set point to path goal for all selected.
			for (Entity entity : entities) {
				PathFindingComponent pathCmp = entity.getComponent(PathFindingComponent.class);
				SelectableComponent selCmp = entity.getComponent(SelectableComponent.class);
				if (pathCmp != null && selCmp != null && selCmp.isSelected) {
					if (pathCmp.goal == null) {
						pathCmp.goal = new Vector3();
					}
					pathCmp.goal.set(surfacePoint);
					if (intent.doubleClick) {
						pathCmp.run = true;
					} else {
						pathCmp.run = false;
					}
					Gdx.app.debug(tag, String.format("Path target for %s set to %s", entity, surfacePoint));
				}
			}
		}


	}

}
