package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.IntentComponent;
import com.mygdx.game.components.SelectableComponent;

/**
 * Created by user on 8/25/15.
 */
public class ModelSelectionSystem extends EntitySystem {

	public static final String tag = "ModelSelectionSystem";

	public Family systemFamily;
	private ImmutableArray<Entity> entities;
	private ComponentMapper<IntentComponent> intentCmps = ComponentMapper.getFor(IntentComponent.class);
	private ComponentMapper<SelectableComponent> selCmps = ComponentMapper.getFor(SelectableComponent.class);

	PhysicsSystem phySys;
	Viewport viewport;

	Vector2 lastClick = new Vector2();
	private Ray ray = new Ray();
	private final Vector3 tmp = new Vector3();

	public ModelSelectionSystem(PhysicsSystem phySys, Viewport viewport) {
		systemFamily = Family.all(IntentComponent.class, SelectableComponent.class).get();
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
		IntentComponent intent = intentCmps.get(entities.get(0));
		if (intent.click.equals(lastClick)) {
			return;
		}

		float screenX = intent.click.x;
		float screenY = intent.click.y;

		ray.set(viewport.getPickRay(screenX, screenY));
		float distance = 100;
		Gdx.app.debug(tag, String.format(
				"Mouse: %s, %s, Pick ray: origin: %s, direction: %s.",
				screenX, screenY, ray.origin, ray.direction));

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
			}
		}

		lastClick.set(intent.click);
	}

}
