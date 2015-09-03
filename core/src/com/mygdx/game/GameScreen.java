package com.mygdx.game;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.*;
import com.mygdx.game.components.blender.BlenderComponent;
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
	AssetManager assets;

	public GameScreen(int reqWidth, int reqHeight) {
		assets = new AssetManager();
		engine = new PooledEngine();
		Bullet.init();

		viewportBackgroundColor = Color.DARK_GRAY;

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV, reqWidth, reqHeight);
		viewport = new FitViewport(reqWidth, reqHeight, camera);
		stage = new GameStage(viewport);
		shapeRenderer = new ShapeRenderer();

		camera.position.set(10, 20, 10);
		camera.lookAt(0, 0, 0);
		camera.near = GameSettings.CAMERA_NEAR;
		camera.far = GameSettings.CAMERA_FAR;
		camera.update();

		IntentBroadcastComponent intentCmp = new IntentBroadcastComponent();
		Entity interactionEntity = engine.createEntity();
		interactionEntity.add(new CameraTargetingComponent(camera, viewport));
		interactionEntity.add(intentCmp);
		engine.addEntity(interactionEntity);

		// TODO: dispose
		Gdx.app.debug(tag, "Loading json");
		BlenderComponentsLoader blender = new BlenderComponentsLoader(
				assets,
				"models/json/test_model.json",
				"models/json/test_empty.json",
				"models/json/test_light.json"
		);

		Gdx.app.debug(tag, "Loading environment system");
		ModelEnvironmentSystem envSys = new ModelEnvironmentSystem();
		engine.addEntityListener(envSys.systemFamily, envSys.lightListener);
		engine.addSystem(envSys);

		// TODO: dispose
		Gdx.app.debug(tag, "Loading models system");
		ModelRenderSystem modelSys = new ModelRenderSystem(viewport, camera,
				envSys.environment,
				blender.sunDirection);
		engine.addSystem(modelSys);

		// TODO: dispose
		Gdx.app.debug(tag, "Loading physics system");
		PhysicsSystem phySys = new PhysicsSystem();
		engine.addSystem(phySys);
		engine.addEntityListener(phySys.systemFamily, phySys.listener);


		Gdx.app.debug(tag, "Adding entities");
		Vector3 gridUnit = new Vector3();
		for (Entity entity : blender.entities) {
			engine.addEntity(entity);

			BlenderComponent cmp = entity.getComponent(BlenderComponent.class);
			if (cmp != null && cmp.name.equals("grid_unit") && gridUnit.isZero()) {
				gridUnit.set(Math.abs(cmp.scale.x), Math.abs(cmp.scale.y), Math.abs(cmp.scale.z));
				Gdx.app.debug(tag, "Using grid unit " + gridUnit);
			}
		}


		ImmutableArray<Entity> modelEntities = engine.getEntitiesFor(Family.all(ModelComponent.class).get());
		for (Entity entity : modelEntities) {

//			entity.add(new SelectableComponent());
			entity.add(intentCmp);


			ModelComponent modelCmp = entity.getComponent(ModelComponent.class);
			if (modelCmp.id.endsWith("ball")) {
////				ModelComponent ballModel = entity.getComponent(ModelComponent.class);
//				MotionStateComponent ballMotionState = entity.getComponent(MotionStateComponent.class);

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
		multiplexer.addProcessor(inputSys.inputProcessor);
		Gdx.input.setInputProcessor(multiplexer);


		Gdx.app.debug(tag, "Adding camera system");
		OverheadCameraSystem camSys = new OverheadCameraSystem();
		engine.addSystem(camSys);


		Gdx.app.debug(tag, "Adding selection system");
		ModelSelectionSystem selSys = new ModelSelectionSystem(phySys, viewport);
		engine.addSystem(selSys);


//		Gdx.app.debug(tag, "Adding billboard system");
//		Family billFamily = Family.all(
//				TextureComponent.class,
//				MotionStateComponent.class,
//				ModelComponent.class).get();
//		BillboardSystem billSys = new BillboardSystem(billFamily, camera);
//		engine.addSystem(billSys);

		spawnCharacter(new Vector3(5, 1, 0), intentCmp);
		spawnCharacter(new Vector3(5, 1, 5), intentCmp);


		Family pathFamily = Family.all(
				PathFindingComponent.class,
				PhysicsComponent.class).get();
		PathFindingSystem pathSys = new PathFindingSystem(pathFamily);
		engine.addSystem(pathSys);

		Family animFamily = Family.all(CharacterActionComponent.class).get();
		AnimationSystem animSys = new AnimationSystem(animFamily);
		engine.addSystem(animSys);
	}

	private void spawnCharacter(Vector3 pos, IntentBroadcastComponent intentCmp) {
		Entity entity = new Entity();
		engine.addEntity(entity);

		UBJsonReader jsonReader = new UBJsonReader();
		ModelLoader modelLoader = new G3dModelLoader(jsonReader);
		ModelData modelData = modelLoader.
				loadModelData(Gdx.files.getFileHandle("models/g3db/man.g3db", Files.FileType.Internal));

		// TODO: manage, dispose
		Model model = new Model(modelData);
		Model outlineModel = new Model(modelData);

		ModelFactory.createOutlineModel(outlineModel, Color.WHITE, 0.002f);

		ModelComponent mdlCmp = new ModelComponent(model, "man", pos,
				new Vector3(0, 0, 0),
				new Vector3(1, 1, 1));
		entity.add(mdlCmp);
		ModelComponent outlineMdlCmp = new ModelComponent(outlineModel, "man_outline", pos,
				new Vector3(0, 0, 0),
				new Vector3(1, 1, 1));
		outlineMdlCmp.modelInstance.transform = mdlCmp.modelInstance.transform;


		btCollisionShape shape = new btCapsuleShape(0.5f, 1f);
		MotionStateComponent motionStateCmp = new MotionStateComponent(mdlCmp.modelInstance.transform);
		PhysicsComponent phyCmp = new PhysicsComponent(
				shape, motionStateCmp.motionState, 100,
				PhysicsSystem.OBJECT_FLAG,
				PhysicsSystem.ALL_FLAG,
				true, true);
		phyCmp.body.setAngularFactor(Vector3.Y);
		phyCmp.body.setWorldTransform(mdlCmp.modelInstance.transform);
		entity.add(motionStateCmp);
		entity.add(phyCmp);
		entity.add(intentCmp);
		entity.add(new SelectableComponent(outlineMdlCmp));
		entity.add(new PathFindingComponent());

		CharacterActionComponent actionCmp = new CharacterActionComponent(mdlCmp.modelInstance);
		actionCmp.addModel(outlineMdlCmp.modelInstance);
		entity.add(actionCmp);
	}

	@Override
	public void show() {
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearStencil(0);
		Gdx.gl.glClearColor(0, 0, 0, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

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
		stage.dispose();
	}

}
