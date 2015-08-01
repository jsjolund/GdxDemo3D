package com.mygdx.game;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.mygdx.game.components.MoveAimComponent;
import com.mygdx.game.systems.*;

/**
 * Created by user on 8/1/15.
 */
public class GameScreen extends AbstractScreen {

	PooledEngine engine;


	public GameScreen(int reqWidth, int reqHeight) {
		super(reqWidth, reqHeight);
		engine = new PooledEngine();
		Bullet.init();

		camera = new PerspectiveCamera(35, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(10, 10, 10);
		camera.lookAt(0, 0, 0);
		camera.near = 1f;
		camera.far = 300f;
		camera.update();

		Gdx.app.debug(tag, "Loading environment system");
		ModelEnvironmentSystem envSys = new ModelEnvironmentSystem();
		engine.addEntityListener(envSys.systemFamily, envSys.lightListener);
		engine.addSystem(envSys);

		Gdx.app.debug(tag, "Loading models system");
		ModelRenderSystem modelSys = new ModelRenderSystem(camera, envSys.environment);
		engine.addSystem(modelSys);

		Gdx.app.debug(tag, "Loading physics system");
		PhysicsSystem phySys = new PhysicsSystem();
		engine.addSystem(phySys);
		engine.addEntityListener(phySys.systemFamily, phySys.listener);

		Gdx.app.debug(tag, "Loading json");
		BlenderLoader loader = new BlenderLoader("models/test.json");

		Gdx.app.debug(tag, "Adding entities");
		for (Entity entity : loader.entities) {
			Gdx.app.debug(tag, "Adding" + entity.toString());
			engine.addEntity(entity);
			Gdx.app.debug(tag, "Finished adding" + entity.toString());
		}
		engine.getEntity(4).add(new MoveAimComponent());
		engine.getEntity(5).add(new MoveAimComponent());
		engine.getEntity(6).add(new MoveAimComponent());

		Gdx.app.debug(tag, "Adding input controller");
		FPSMoveAimSystem moveSys = new FPSMoveAimSystem();
		engine.addSystem(moveSys);
		Gdx.input.setInputProcessor(moveSys);

		Gdx.app.debug(tag, "Adding camera system");
		CameraMoveSystem camSys = new CameraMoveSystem(camera);
		engine.addSystem(camSys);

		Gdx.app.debug(tag, "Adding movement system");
		PhysicsMoveSystem phyMoveSys = new PhysicsMoveSystem();
		engine.addEntityListener(phyMoveSys.systemFamily, phyMoveSys.listener);
		engine.addSystem(phyMoveSys);

	}

	@Override
	public void render(float delta) {
		super.render(delta);
		engine.update(Gdx.graphics.getDeltaTime());
		engine.getSystem(PhysicsSystem.class).debugDrawWorld(camera);
	}
}
