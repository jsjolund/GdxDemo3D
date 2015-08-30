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
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
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

	// TODO: pan-begränsningar, bättre skuggor, pathfinding, selection,

	public GameScreen(int reqWidth, int reqHeight) {
		engine = new PooledEngine();
		Bullet.init();

//		viewportBackgroundColor = new Color(0.28f, 0.56f, 0.83f, 1);
		viewportBackgroundColor = new Color(0.49f, 0.49f, 0.49f, 1);
//		viewportBackgroundColor = Color.BLACK;

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV, reqWidth, reqHeight);
		viewport = new FitViewport(reqWidth, reqHeight, camera);
		stage = new GameStage(viewport);

		camera.position.set(10, 20, 10);
		camera.lookAt(0, 0, 0);
		camera.near = GameSettings.CAMERA_NEAR;
		camera.far = GameSettings.CAMERA_FAR;
		camera.update();


		IntentComponent intentCmp = new IntentComponent();
		Entity interactionEntity = engine.createEntity();
		interactionEntity.add(new CameraTargetingComponent(camera, viewport));
		interactionEntity.add(intentCmp);
		engine.addEntity(interactionEntity);

		shapeRenderer = new ShapeRenderer();

		Gdx.app.debug(tag, "Loading environment system");
		ModelEnvironmentSystem envSys = new ModelEnvironmentSystem();
		engine.addEntityListener(envSys.systemFamily, envSys.lightListener);
		engine.addSystem(envSys);

		Gdx.app.debug(tag, "Loading models system");
		ModelRenderSystem modelSys = new ModelRenderSystem(viewport, camera, envSys.environment);
		engine.addSystem(modelSys);

		Gdx.app.debug(tag, "Loading physics system");
		PhysicsSystem phySys = new PhysicsSystem();
		engine.addSystem(phySys);
		engine.addEntityListener(phySys.systemFamily, phySys.listener);

		Gdx.app.debug(tag, "Loading json");
		BlenderComponentsLoader loader = new BlenderComponentsLoader(
				"models/json/test_model.json",
				"models/json/test_empty.json",
				"models/json/test_light.json"
		);

		Gdx.app.debug(tag, "Adding entities");
		for (Entity entity : loader.entities) {
			engine.addEntity(entity);
		}

		boolean s = true;
		ImmutableArray<Entity> modelEntities = engine.getEntitiesFor(Family.all(ModelComponent.class).get());
		for (Entity entity : modelEntities) {

			entity.add(new SelectableComponent());
			entity.add(intentCmp);

			ModelComponent modelCmp = entity.getComponent(ModelComponent.class);
			if (modelCmp.id.endsWith("ball")) {
////				ModelComponent ballModel = entity.getComponent(ModelComponent.class);
				MotionStateComponent ballMotionState = entity.getComponent(MotionStateComponent.class);

//				Entity billboard = new Entity();
//				billboard.add(ballMotionState);
//
//				Pixmap billboardPixmap = new Pixmap(Gdx.files.local("badlogic.jpg"));
//				TextureComponent billboardTexture = new TextureComponent(billboardPixmap);
//				billboard.add(billboardTexture);
//
//				Material material = new Material();
//				material.set(new TextureAttribute(TextureAttribute.Diffuse, billboardTexture.textureRegion));
//				BlendingAttribute blendAttrib = new BlendingAttribute(0.5f);
//				material.set(blendAttrib);
//
//				ModelComponent billboardModel = new ModelComponent(ModelFactory.buildPlaneModel(2, 2, material, 0, 0,
//						1, 1), "plane");
//				billboard.add(billboardModel);
//
//				engine.addEntity(billboard);

			}
		}

		Gdx.app.debug(tag, "Adding input controller");

		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		GameInputSystem inputSys = new GameInputSystem(intentCmp);
		engine.addSystem(inputSys);
		multiplexer.addProcessor(inputSys);
		Gdx.input.setInputProcessor(multiplexer);

		Gdx.app.debug(tag, "Adding camera system");
		OverheadCameraSystem camSys = new OverheadCameraSystem();
		engine.addSystem(camSys);

		Gdx.app.debug(tag, "Adding selection system");
		ModelSelectionSystem selSys = new ModelSelectionSystem(phySys, viewport);
		engine.addSystem(selSys);

//		Gdx.app.debug(tag, "Adding movement system");
//		Family phyFamily = Family.all(MoveAimComponent.class, PhysicsComponent.class).get();
//		PhysicsMoveAimSystem phyMoveSys = new PhysicsMoveAimSystem(phyFamily);
//		engine.addEntityListener(phyFamily, phyMoveSys.listener);
//		engine.addSystem(phyMoveSys);

		Gdx.app.debug(tag, "Adding billboard system");
		Family billFamily = Family.all(
				TextureComponent.class,
				MotionStateComponent.class,
				ModelComponent.class).get();
		BillboardSystem billSys = new BillboardSystem(billFamily, camera);
		engine.addSystem(billSys);


//		Entity entity = new Entity();
//		engine.addEntity(entity);
//
//		AssetManager assets = new AssetManager();
//		assets.load("models/g3db/man.g3db", Model.class);
//		assets.finishLoading();
//		Model model = assets.get("models/g3db/man.g3db", Model.class);
//		entity.add(new ModelComponent(model, "man", new Vector3(0, 1, -3), new Vector3(0, 0, 0), new Vector3(1, 1,
//				1)));
//		ModelInstance instance = entity.getComponent(ModelComponent.class).modelInstance;
//
//
//		btCollisionShape shape = new btCapsuleShape(0.5f, 1f);
//		MotionStateComponent motionStateCmp = new MotionStateComponent(instance.transform);
//		PhysicsComponent phyCmp = new PhysicsComponent(
//				shape, motionStateCmp.motionState, 100,
//				PhysicsSystem.OBJECT_FLAG,
//				PhysicsSystem.ALL_FLAG,
//				true, false);
//		phyCmp.body.setAngularFactor(Vector3.Y);
//		phyCmp.body.setWorldTransform(instance.transform);
//
//		entity.add(motionStateCmp);
//		entity.add(phyCmp);
//		entity.add(intentCmp);
//		entity.add(new SelectableComponent());
//
//		phyCmp.body.getWorldTransform().translate(5, 0, 0);
//
//		controller = new AnimationController(instance);
//		controller.setAnimation("Armature|walk", -1);
	}

	AnimationController controller;

	@Override
	public void show() {

	}

	@Override
	public void render(float delta) {
		Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(viewportBackgroundColor);
		shapeRenderer.rect(0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
		shapeRenderer.end();

		engine.update(Gdx.graphics.getDeltaTime());

		if (GameSettings.DRAW_COLLISION_DEBUG) {
			engine.getSystem(PhysicsSystem.class).debugDrawWorld(camera);
		}

		stage.act(delta);
		stage.draw();

		if (controller != null) {
			controller.update(delta);
		}
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
