package com.mygdx.game;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.blender.BlenderScene;
import com.mygdx.game.components.*;
import com.mygdx.game.input.GameInputProcessor;
import com.mygdx.game.input.SelectionController;
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.pathfinding.PathFollowSystem;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.systems.*;
import com.mygdx.game.utilities.CameraController;
import com.mygdx.game.utilities.GhostCamera;

/**
 * Created by user on 8/1/15.
 */
public class GameScreen implements Screen {

	private final static String tag = "GameScreen";

	private final Viewport viewport;
	private final GameStage stage;
	private final PooledEngine engine;
	private final Color viewportBackgroundColor;
	private final GhostCamera camera;
	private final AssetManager assets;
	private final ShapeRenderer shapeRenderer;
	private final btIDebugDraw.DebugDrawModes modes;
	private final btIDebugDraw debugDraw;
	private final GameInputProcessor gameInputProcessor;
	private final PhysicsSystem phySys;
	private final Array<BlenderScene> scenes = new Array<BlenderScene>();
	private final Array<Class<? extends DisposableComponent>> disposableClasses;
	private final CameraController cameraController;

	public GameScreen(int reqWidth, int reqHeight) {
		disposableClasses = new Array<Class<? extends DisposableComponent>>();
		disposableClasses.add(BillboardComponent.class);
		disposableClasses.add(PhysicsComponent.class);
		disposableClasses.add(RagdollComponent.class);

		assets = new AssetManager();
		engine = new PooledEngine();
		Bullet.init();
		viewportBackgroundColor = Color.BLACK;

		camera = new GhostCamera(GameSettings.CAMERA_FOV, reqWidth, reqHeight);
		camera.near = GameSettings.CAMERA_NEAR;
		camera.far = GameSettings.CAMERA_FAR;
		camera.update();
		viewport = new FitViewport(reqWidth, reqHeight, camera);
		stage = new GameStage(viewport);
		shapeRenderer = new ShapeRenderer();

		BlenderScene blenderScene = new BlenderScene(
				"models/json/scene0_model.json",
				"models/json/scene0_empty.json",
				"models/json/scene0_light.json",
				"models/json/scene0_camera.json"
		);
		scenes.add(blenderScene);
		blenderScene.setToSceneCamera(camera);

		RenderSystem renderSys = new RenderSystem(viewport, camera);
		renderSys.setNavmesh(blenderScene.navMesh);
		renderSys.setEnvironmentLights(blenderScene.lights, blenderScene.shadowCameraDirection);
		engine.addSystem(renderSys);

		engine.addSystem(phySys = new PhysicsSystem());
		engine.addEntityListener(Family.all(PhysicsComponent.class).get(), phySys.physicsComponentListener);
		engine.addEntityListener(Family.all(RagdollComponent.class).get(), phySys.ragdollComponentListener);
		modes = new btIDebugDraw.DebugDrawModes();
		debugDraw = phySys.dynamicsWorld.getDebugDrawer();

		for (Entity entity : blenderScene.entities) {
			engine.addEntity(entity);
		}

		// TODO: Add constraint support in blender scene somehow
		ImmutableArray<Entity> modelEntities = engine.getEntitiesFor(Family.all(ModelComponent.class).get());
		for (Entity entity : modelEntities) {
			if (entity.getComponent(ModelComponent.class).id.startsWith("door")) {
				PhysicsComponent phyCmp = entity.getComponent(PhysicsComponent.class);
				btHingeConstraint hinge = new btHingeConstraint(phyCmp.body, new Vector3(0, 0, -0.6f), Vector3.Y);
				phySys.dynamicsWorld.addConstraint(hinge);
				phyCmp.addConstraint(hinge);
			}
		}


		PathFollowSystem pathSys = new PathFollowSystem();
		pathSys.setNavMesh(blenderScene.navMesh);
		engine.addSystem(pathSys);

		engine.addSystem(new BillboardSystem(camera));
		engine.addSystem(new RagdollSystem());
		engine.addSystem(new CharacterStateSystem());


		SelectionController selSys = new SelectionController(phySys);
		selSys.addObserver(stage);
		selSys.addObserver(renderSys);
		selSys.addObserver(pathSys);

		cameraController = new CameraController(camera);
		cameraController.setWorldBoundingBox(blenderScene.worldBounds);

		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		gameInputProcessor = new GameInputProcessor(viewport, cameraController, selSys, pathSys);
		multiplexer.addProcessor(gameInputProcessor);
		Gdx.input.setInputProcessor(multiplexer);

		gameInputProcessor.addObserver(renderSys);
		gameInputProcessor.addObserver(pathSys);


		engine.addEntity(spawnCharacter(new Vector3(5, 1, 0)));
		engine.addEntity(spawnCharacter(new Vector3(0, 1, 5)));
		engine.addEntity(spawnCharacter(new Vector3(10, 1, 5)));
		engine.addEntity(spawnCharacter(new Vector3(-12, 4, 10)));
	}

	@Override
	public void dispose() {
		stage.dispose();
		assets.dispose();
		shapeRenderer.dispose();
		debugDraw.dispose();
		engine.getSystem(RenderSystem.class).dispose();
		engine.getSystem(PhysicsSystem.class).dispose();
		for (BlenderScene scene : scenes) {
			scene.dispose();
		}
		scenes.clear();
		ImmutableArray<Entity> entities;
		for (Class<? extends DisposableComponent> disposableClass : disposableClasses) {
			entities = engine.getEntitiesFor(Family.all(disposableClass).get());
			for (Entity entity : entities) {
				entity.getComponent(disposableClass).dispose();
			}
		}
	}

	@Override
	public void render(float delta) {
		delta *= GameSettings.GAME_SPEED;
		Gdx.gl.glClearColor(0, 0, 0, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(viewportBackgroundColor);
		shapeRenderer.rect(0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
		shapeRenderer.end();

		engine.update(delta);

		if (DebugViewSettings.drawCollShapes || DebugViewSettings.drawConstraints) {
			int mode = 0;
			if (DebugViewSettings.drawConstraints) {
				mode |= modes.DBG_DrawConstraints;
				mode |= modes.DBG_DrawConstraintLimits;
			} else if (DebugViewSettings.drawCollShapes) {
				mode |= modes.DBG_DrawWireframe;
			}
			debugDraw.setDebugMode(mode);
			phySys.debugDrawWorld(camera);
		}
		stage.act(delta);
		stage.draw();
		gameInputProcessor.update(delta);
		camera.update(delta, GameSettings.CAMERA_LERP_ALPHA);
	}

	private Entity spawnCharacter(Vector3 initialPosition) {
		Entity entity = new Entity();

		short belongsToFlag = PhysicsSystem.PC_FLAG;
		short collidesWithFlag = (short) (PhysicsSystem.OBJECT_FLAG | PhysicsSystem.GROUND_FLAG);

		// Model
		MipMapGenerator.setUseHardwareMipMap(true);
		ModelLoader.ModelParameters param = new ModelLoader.ModelParameters();
		param.textureParameter.genMipMaps = true;
		param.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		param.textureParameter.magFilter = Texture.TextureFilter.Linear;
		assets.load("models/g3db/character_male_base.g3db", Model.class, param);
		assets.finishLoading();
		Model model = assets.get("models/g3db/character_male_base.g3db");
		ModelComponent mdlCmp = new ModelComponent(model, "male", initialPosition,
				new Vector3(0, 0, 0),
				new Vector3(1, 1, 1));
		// TODO: Ragdoll problems with frustum culling. Move capsule to ragdoll.
		mdlCmp.ignoreCulling = true;
		entity.add(mdlCmp);
//		for (com.badlogic.gdx.graphics.g3d.model.Animation animation : mdlCmp.modelInstance.animations) {
//			System.out.println(animation.id);
//		}

		// Create basic capsule collision shape
		// TODO: Dispose
		btCollisionShape shape = new btCapsuleShape(GameSettings.CHAR_CAPSULE_XZ_HALFEXT,
				GameSettings.CHAR_CAPSULE_Y_HALFEXT);

		// Ragdoll collision shapes
		String partDefJson = "models/json/character_empty.json";
		RagdollComponent ragCmp = new RagdollComponent(shape, mdlCmp.modelInstance.transform, GameSettings.CHAR_MASS,
				belongsToFlag,
				collidesWithFlag,
				true, true, partDefJson, mdlCmp.modelInstance.getNode("armature"));
		ragCmp.body.setAngularFactor(Vector3.Y);
		ragCmp.body.setWorldTransform(mdlCmp.modelInstance.transform);
		entity.add(ragCmp);

		// Pathfinding
		PathFindingComponent pathCmp = new PathFindingComponent(initialPosition);
		entity.add(pathCmp);

		// Selection billboard
		assets.load("images/marker.png", Pixmap.class);
		assets.finishLoading();
		Pixmap billboardPixmap = assets.get("images/marker.png", Pixmap.class);
		BillboardComponent billboard = new BillboardComponent(billboardPixmap, 1, 1, true,
				new Vector3(0, -GameSettings.CHAR_CAPSULE_Y_HALFEXT * 0.8f, 0), mdlCmp.modelInstance.transform);
		entity.add(billboard);

		// Selection component
		SelectableComponent selCmp = new SelectableComponent(billboard.modelInstance);
		entity.add(selCmp);

		// State machine component
		CharacterStateComponent charCmp = new CharacterStateComponent(
				mdlCmp, pathCmp, selCmp, ragCmp);
		entity.add(charCmp);

		return entity;
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub
	}

	@Override
	public void resize(int width, int height) {
		stage.resize(width, height);
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
	}


}
