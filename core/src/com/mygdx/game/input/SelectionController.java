package com.mygdx.game.input;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.components.CharacterState;
import com.mygdx.game.components.CharacterStateComponent;
import com.mygdx.game.components.SelectableComponent;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.systems.PhysicsSystem;
import com.mygdx.game.utilities.Observable;
import com.mygdx.game.utilities.Observer;

/**
 * Created by user on 8/25/15.
 */
public class SelectionController implements Observable {

	public static final String tag = "SelectionController";

	private final ComponentMapper<SelectableComponent> selCmps = ComponentMapper.getFor(SelectableComponent.class);
	private final Vector3 surfaceHitPoint = new Vector3();
	private final Array<Observer> observers = new Array<Observer>();
	private final PhysicsSystem phySys;
	private Entity selectedEntity;


	public SelectionController(PhysicsSystem phySys) {
		this.phySys = phySys;
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

	@Override
	public void notifyObserversLayerSelected(int layer) {
	}


	public boolean processTouch(Ray pickRay) {
		// Check if player clicked a selectable entity
		Entity hitEntity = phySys.rayTest(pickRay, surfaceHitPoint,
				PhysicsSystem.PC_FLAG,
				PhysicsSystem.ALL_FLAG,
				GameSettings.CAMERA_PICK_RAY_DST);
		if (hitEntity != null && selCmps.has(hitEntity)) {
			notifyObserversEntitySelected(hitEntity);
			selectedEntity = hitEntity;
			return true;
		}
		return false;
	}

	public void killSelectedCharacter() {
		if (selectedEntity != null) {
			CharacterStateComponent cmp = selectedEntity.getComponent(CharacterStateComponent.class);
			if (cmp != null) {
				cmp.stateMachine.changeState(CharacterState.DEAD);
			}
		}
	}

}
