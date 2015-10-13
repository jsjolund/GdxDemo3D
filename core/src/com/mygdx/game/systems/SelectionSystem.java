package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.GameStage;
import com.mygdx.game.components.CharacterStateComponent;
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.components.SelectableComponent;
import com.mygdx.game.input.IntentBroadcast;
import com.mygdx.game.navmesh.NavMesh;
import com.mygdx.game.navmesh.Triangle;
import com.mygdx.game.settings.GameSettings;

/**
 * Created by user on 8/25/15.
 */
public class SelectionSystem extends EntitySystem {

	public static final String tag = "SelectionSystem";
	public final Family systemFamily;
	private final Vector3 surfaceHitPoint = new Vector3();
	private final PhysicsSystem phySys;
	private final ComponentMapper<SelectableComponent> selCmps = ComponentMapper.getFor(SelectableComponent.class);
	private final ComponentMapper<PathFindingComponent> pathCmps = ComponentMapper.getFor(PathFindingComponent.class);
	private final float rayDistance = 100;
	private final GameStage stage;
	private final Viewport viewport;
	private ImmutableArray<Entity> entities;
	private NavMesh navMesh;

	public InputAdapter inputAdapter;

	public class SelectionInputAdapter extends InputAdapter {

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			Ray ray = viewport.getPickRay(screenX, screenY);

			// Check if player clicked a selectable entity
			Entity hitEntity = phySys.rayTest(ray, surfaceHitPoint,
					PhysicsSystem.PC_FLAG,
					PhysicsSystem.ALL_FLAG,
					rayDistance);
			if (hitEntity != null && selCmps.has(hitEntity)) {
				Gdx.app.debug(tag, "Hit PC: " + hitEntity);
				SelectableComponent hitEntitySelCmp = selCmps.get(hitEntity);
				// If hit entity is selectable, deselect previous, select this one
				for (Entity entity : entities) {
					SelectableComponent selCmp = selCmps.get(entity);
					selCmp.isSelected = false;
				}
				hitEntitySelCmp.isSelected = true;

				CharacterStateComponent stateCmp = hitEntity.getComponent(CharacterStateComponent.class);
				// TODO: an observer pattern could be used
				stage.setSelectedCharacterState(stateCmp);

				return true;
			}

			// Check if player clicked navigation mesh
			if ((phySys.rayTest(
					ray, surfaceHitPoint,
					PhysicsSystem.NAVMESH_FLAG,
					PhysicsSystem.NAVMESH_FLAG,
					rayDistance)) != null) {

				// Check which navmesh triangle was hit
				Triangle hitTriangle = navMesh.rayTest(ray, rayDistance);

				if (hitTriangle != null) {
					Gdx.app.debug(tag, "Hit navmesh at: " + surfaceHitPoint);
					for (Entity entity : entities) {
						if (!selCmps.get(entity).isSelected) {
							continue;
						}
						PathFindingComponent pathCmp = pathCmps.get(entity);

						// Check which triangle the entity is currently standing on
						pathCmp.posGroundRay.origin.set(pathCmp.currentPosition);
						Triangle posTriangle = navMesh.rayTest(pathCmp.posGroundRay, rayDistance);

						Vector3 posPoint = new Vector3(pathCmp.currentPosition).sub(0, GameSettings
								.CHAR_CAPSULE_Y_HALFEXT, 0);

						pathCmp.clearPath();
						navMesh.calculatePath(posTriangle, hitTriangle, posPoint, surfaceHitPoint, pathCmp.trianglePath);
						pathCmp.trianglePath.setStartEnd(posPoint, surfaceHitPoint);
						pathCmp.setPath(pathCmp.trianglePath.getDirectPath());
					}
				}
			}

			return true;
		}
	}

	public SelectionSystem(GameStage stage, Viewport viewport, PhysicsSystem phySys) {
		systemFamily = Family.all(
				PathFindingComponent.class,
				SelectableComponent.class).get();
		this.phySys = phySys;
		this.stage = stage;
		this.viewport = viewport;
		this.inputAdapter = new SelectionInputAdapter();
	}

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}

	@Override
	public void update(float deltaTime) {

	}

	public void setNavMesh(NavMesh navMesh) {
		this.navMesh = navMesh;
	}
}
