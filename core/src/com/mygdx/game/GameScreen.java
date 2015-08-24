package com.mygdx.game;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.*;
import com.mygdx.game.components.blender.BlenderComponentsLoader;
import com.mygdx.game.systems.*;

/**
 * Created by user on 8/1/15.
 */
public class GameScreen implements Screen {

	private final static String tag = "GameScreen";

	private final Viewport viewport;
	private final GameStage stage;
	PooledEngine engine;
	Color viewportBackgroundColor;
	Camera camera;
	private ShapeRenderer shapeRenderer;

	public GameScreen(int reqWidth, int reqHeight) {
		engine = new PooledEngine();
		Bullet.init();

		viewportBackgroundColor = new Color(0.28f, 0.56f, 0.83f, 1);

		camera = new PerspectiveCamera(60, reqWidth, reqHeight);
		camera.near = 1e-3f;
		camera.far = 3000f;
		camera.update();

		viewport = new FitViewport(reqWidth, reqHeight, camera);
		shapeRenderer = new ShapeRenderer();

		stage = new GameStage(viewport);


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
		BlenderComponentsLoader loader = new BlenderComponentsLoader(
				"models/test_model.json",
				"models/test_empty.json",
				"models/test_light.json"
		);

		Gdx.app.debug(tag, "Adding entities");
		for (Entity entity : loader.entities) {
			Gdx.app.debug(tag, "Adding" + entity.toString());
			engine.addEntity(entity);
			Gdx.app.debug(tag, "Finished adding" + entity.toString());
		}

		boolean s = true;
		ImmutableArray<Entity> modelEntities = engine.getEntitiesFor(Family.all(ModelComponent.class).get());
		for (Entity entity : modelEntities) {

			SelectableComponent cmp = new SelectableComponent();
			entity.add(cmp);
			if (s) {
				cmp.isSelected = true;
				s = false;
			}

			ModelComponent modelCmp = entity.getComponent(ModelComponent.class);
//			if (modelCmp.id.endsWith("human")) {
//				break;
//			}
			if (modelCmp.id.endsWith("ball")) {
//				ModelComponent ballModel = entity.getComponent(ModelComponent.class);
				MotionStateComponent ballMotionState = entity.getComponent(MotionStateComponent.class);

				Entity billboard = new Entity();
				billboard.add(ballMotionState);

				Pixmap billboardPixmap = new Pixmap(Gdx.files.local("badlogic.jpg"));
				BillboardTextureComponent billboardTexture = new BillboardTextureComponent(billboardPixmap);
				billboard.add(billboardTexture);

				Material material = new Material();
				material.set(new TextureAttribute(TextureAttribute.Diffuse, billboardTexture.textureRegion));
				BlendingAttribute blendAttrib = new BlendingAttribute(0.5f);
				material.set(blendAttrib);

				ModelComponent billboardModel = new ModelComponent(ModelFactory.buildPlaneModel(5, 5, material, 0, 0,
						1, 1), "plane");
				billboard.add(billboardModel);

				engine.addEntity(billboard);
			}
		}

		Gdx.app.debug(tag, "Adding input controller");
		UserInputSystem inputSys = new UserInputSystem(viewport, phySys);
		engine.addSystem(inputSys);
//		Gdx.input.setInputProcessor(inputSys);

		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		multiplexer.addProcessor(inputSys);
		Gdx.input.setInputProcessor(multiplexer);
//		Gdx.input.setInputProcessor(stage);

		Gdx.app.debug(tag, "Adding camera system");
		OverheadCameraSystem camSys = new OverheadCameraSystem(camera);
		engine.addSystem(camSys);
//
//		Gdx.app.debug(tag, "Adding movement system");
//		Family phyFamily = Family.all(MoveAimComponent.class, PhysicsComponent.class).get();
//		PhysicsMoveAimSystem phyMoveSys = new PhysicsMoveAimSystem(phyFamily);
//		engine.addEntityListener(phyFamily, phyMoveSys.listener);
//		engine.addSystem(phyMoveSys);

		Gdx.app.debug(tag, "Adding billboard system");
		Family billFamily = Family.all(
				BillboardTextureComponent.class,
				MotionStateComponent.class,
				ModelComponent.class).get();
		BillboardSystem billSys = new BillboardSystem(billFamily, camera);
		engine.addSystem(billSys);
	}

	@Override
	public void show() {

	}

	@Override
	public void render(float delta) {
		Gdx.graphics.getGL20().glClearColor(0, 0, 0, 0.5f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(viewportBackgroundColor);
		shapeRenderer.rect(0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
		shapeRenderer.end();

		engine.update(Gdx.graphics.getDeltaTime());
//		engine.getSystem(PhysicsSystem.class).debugDrawWorld(camera);

		stage.act(delta);
		stage.draw();

		stage.btn.drawDebug(shapeRenderer);
	}

	@Override
	public void resize(int width, int height) {
		stage.resize(width, height);
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void dispose() {
		// TODO
		stage.dispose();
	}

}
