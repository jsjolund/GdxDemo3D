package com.mygdx.game.input;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.CharacterState;
import com.mygdx.game.components.CharacterStateComponent;
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.components.SelectableComponent;
import com.mygdx.game.navmesh.NavMesh;
import com.mygdx.game.navmesh.Triangle;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.systems.PhysicsSystem;
import com.mygdx.game.utilities.Observable;
import com.mygdx.game.utilities.Observer;

/**
 * Created by user on 8/25/15.
 */
public class SelectionSystem extends EntitySystem implements Observable {

	public static final String tag = "SelectionSystem";
	private final Vector3 surfaceHitPoint = new Vector3();
	private final PhysicsSystem phySys;
	private final ComponentMapper<SelectableComponent> selCmps = ComponentMapper.getFor(SelectableComponent.class);
	private final ComponentMapper<PathFindingComponent> pathCmps = ComponentMapper.getFor(PathFindingComponent.class);
	private final float rayDistance = 100;
	private final Viewport viewport;
	private final Array<Observer> observers = new Array<Observer>();
	public InputAdapter inputAdapter;
	private NavMesh navMesh;
	private Entity selectedEntity;

	public SelectionSystem(Viewport viewport, PhysicsSystem phySys) {
		this.phySys = phySys;
		this.viewport = viewport;
		this.inputAdapter = new SelectionInputAdapter();
	}

	@Override
	public void addObserver(Observer observer) {
		observers.add(observer);
	}

	@Override
	public void removeObserver(Observer observer) {
		observers.removeValue(observer, true);
	}

	@Override
	public void notifyObserversEntitySelected(Entity entity) {
		for (Observer observer : observers) {
			observer.notifyEntitySelected(entity);
		}
	}

	public void setNavMesh(NavMesh navMesh) {
		this.navMesh = navMesh;
	}

	private void checkNavMeshClick(Ray ray) {
		// Check if player clicked navigation mesh
		if ((phySys.rayTest(ray, surfaceHitPoint, PhysicsSystem.NAVMESH_FLAG,
				PhysicsSystem.NAVMESH_FLAG, rayDistance)) != null) {

			// Check which navmesh triangle was hit
			Triangle hitTriangle = navMesh.rayTest(ray, rayDistance);
			if (hitTriangle != null) {
				PathFindingComponent pathCmp = pathCmps.get(selectedEntity);
				if (pathCmp != null) {
					// Check which triangle the entity is currently standing on
					pathCmp.posGroundRay.origin.set(pathCmp.currentPosition);
					Triangle posTriangle = navMesh.rayTest(pathCmp.posGroundRay, rayDistance);

					if (posTriangle == null) {
						/*
						 Player was likely on the edge of the navmesh, and triangle raycast
						 was unable to find which triangle the player stands on.
						 TODO:
						 The path data structure should keep track of which triangle the
						 player is currently on so ray test on current position is not
						 necessary...
						 */
						Gdx.app.debug(tag, "Checking neighbouring positions");
						while (true) {
							float offs = 0.2f;
							pathCmp.posGroundRay.origin.set(pathCmp.currentPosition).sub(-offs, 0, 0);
							if ((posTriangle = navMesh.rayTest(pathCmp.posGroundRay, rayDistance)) != null) {
								break;
							}
							pathCmp.posGroundRay.origin.set(pathCmp.currentPosition).sub(offs, 0, 0);
							if ((posTriangle = navMesh.rayTest(pathCmp.posGroundRay, rayDistance)) != null) {
								break;
							}
							pathCmp.posGroundRay.origin.set(pathCmp.currentPosition).sub(0, 0, -offs);
							if ((posTriangle = navMesh.rayTest(pathCmp.posGroundRay, rayDistance)) != null) {
								break;
							}
							pathCmp.posGroundRay.origin.set(pathCmp.currentPosition).sub(0, 0, offs);
							posTriangle = navMesh.rayTest(pathCmp.posGroundRay, rayDistance);
							break;
						}

					}
					Vector3 posPoint = new Vector3(pathCmp.currentPosition).sub(0, GameSettings
							.CHAR_CAPSULE_Y_HALFEXT, 0);
					pathCmp.clearPath();
					boolean pathFound = navMesh.calculatePath(posTriangle, hitTriangle, pathCmp.trianglePath);
					pathCmp.trianglePath.setStartEnd(posPoint, surfaceHitPoint);
					pathCmp.setPath(pathCmp.trianglePath.getSmoothPath());
					if (!pathFound) {
						Gdx.app.debug(tag, "Path not found");
					}

				}
			}
		}
	}

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
				notifyObserversEntitySelected(hitEntity);
				selectedEntity = hitEntity;
			} else if (selectedEntity != null) {
				checkNavMeshClick(ray);
			}
			return true;
		}

		@Override
		public boolean keyDown(int keycode) {
			if (keycode == Input.Keys.F5) {
				CharacterStateComponent cmp = selectedEntity.getComponent(CharacterStateComponent.class);
				if (cmp != null) {
					cmp.stateMachine.changeState(CharacterState.DEAD);
				}
			}
			return false;
		}
	}
}
