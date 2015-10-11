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
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.Animation;
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
import com.mygdx.game.components.*;
import com.mygdx.game.components.blender.BlenderScene;
import com.mygdx.game.input.InputSystem;
import com.mygdx.game.input.IntentBroadcast;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.systems.*;
import com.mygdx.game.utilities.RagdollFactory;

/**
 * Created by user on 8/1/15.
 */
public class GameScreen implements Screen {

	private final static String tag = "GameScreen";

	private final Viewport viewport;
	private final GameStage stage;
	private final PooledEngine engine;
	private final Color viewportBackgroundColor;
	private final PerspectiveCamera camera;
	private final AssetManager assets;
	private final ShapeRenderer shapeRenderer;
	private final btIDebugDraw.DebugDrawModes modes;
	private final btIDebugDraw debugDraw;

	private final Array<BlenderScene> scenes = new Array<BlenderScene>();
	private final Array<Class<? extends DisposableComponent>> disposableClasses;

	public GameScreen(int reqWidth, int reqHeight) {
		disposableClasses = new Array<Class<? extends DisposableComponent>>();
		disposableClasses.add(BillboardComponent.class);
		disposableClasses.add(PhysicsComponent.class);
		disposableClasses.add(RagdollComponent.class);
		disposableClasses.add(MotionStateComponent.class);

		assets = new AssetManager();
		engine = new PooledEngine();
		Bullet.init();
		viewportBackgroundColor = Color.BLACK;

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV, reqWidth, reqHeight);
		camera.near = GameSettings.CAMERA_NEAR;
		camera.far = GameSettings.CAMERA_FAR;
		camera.update();
		viewport = new FitViewport(reqWidth, reqHeight, camera);
		stage = new GameStage(viewport);
		shapeRenderer = new ShapeRenderer();

		Gdx.app.debug(tag, "Loading json");
		BlenderScene blenderScene = new BlenderScene(
				"models/json/scene0_model.json",
				"models/json/scene0_empty.json",
				"models/json/scene0_light.json",
				"models/json/scene0_camera.json"
		);
		scenes.add(blenderScene);
		blenderScene.setToSceneCamera(camera);

		Gdx.app.debug(tag, "Loading render system");
		RenderSystem renderSys = new RenderSystem(viewport, camera);
		renderSys.setNavmesh(blenderScene.navMesh);
		renderSys.setEnvironmentLights(blenderScene.lights, blenderScene.shadowCameraDirection);
		engine.addSystem(renderSys);

		Gdx.app.debug(tag, "Loading physics system");
		PhysicsSystem phySys = new PhysicsSystem();
		engine.addSystem(phySys);
		engine.addEntityListener(phySys.systemFamily, phySys.physicsComponentListener);
		engine.addEntityListener(Family.all(RagdollComponent.class).get(), phySys.ragdollComponentListener);
		modes = new btIDebugDraw.DebugDrawModes();
		debugDraw = phySys.dynamicsWorld.getDebugDrawer();

		Gdx.app.debug(tag, "Adding entities");
		for (Entity entity : blenderScene.entities) {
			engine.addEntity(entity);
		}

		ImmutableArray<Entity> modelEntities = engine.getEntitiesFor(Family.all(ModelComponent.class).get());
		for (Entity entity : modelEntities) {
			ModelComponent modelCmp = entity.getComponent(ModelComponent.class);

			if (modelCmp.id.startsWith("door")) {
				PhysicsComponent phyCmp = entity.getComponent(PhysicsComponent.class);
				btHingeConstraint hinge = new btHingeConstraint(phyCmp.body, new Vector3(0, 0, -0.6f), Vector3.Y);
//				hinge.enableAngularMotor(true, 0, 5);

				hinge.setDbgDrawSize(1);
				phySys.dynamicsWorld.addConstraint(hinge);
				phyCmp.addConstraint(hinge);
			}
		}

		IntentBroadcast intent = new IntentBroadcast();
		Gdx.app.debug(tag, "Adding input controller");
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		InputSystem inputSys = new InputSystem(viewport, intent);
		engine.addSystem(inputSys);
		multiplexer.addProcessor(inputSys.inputProcessor);
		Gdx.input.setInputProcessor(multiplexer);


		Gdx.app.debug(tag, "Adding camera system");
		CameraSystem camSys = new CameraSystem(viewport, camera, intent);
		engine.addSystem(camSys);


		Gdx.app.debug(tag, "Adding selection system");
		SelectionSystem selSys = new SelectionSystem(stage, phySys, intent);
		selSys.setNavMesh(blenderScene.navMesh);
		engine.addSystem(selSys);

		Gdx.app.debug(tag, "Adding billboard system");
		Family billFamily = Family.all(BillboardComponent.class).get();
		BillboardSystem billSys = new BillboardSystem(billFamily, camera);
		engine.addSystem(billSys);

		engine.addEntity(spawnCharacter(new Vector3(5, 1, 0), intent));
		engine.addEntity(spawnCharacter(new Vector3(0, 1, 5), intent));
		engine.addEntity(spawnCharacter(new Vector3(10, 1, 5), intent));
		engine.addEntity(spawnCharacter(new Vector3(1, 1, 20), intent));

		Family pathFamily = Family.all(
				PathFindingComponent.class,
				PhysicsComponent.class
		).get();
		engine.addSystem(new PathFollowSystem(pathFamily));

		Family ragdollFamily = Family.all(
				RagdollComponent.class,
				ModelComponent.class,
				MotionStateComponent.class,
				PhysicsComponent.class).get();
		engine.addSystem(new RagdollSystem(ragdollFamily));

		engine.addSystem(new CharacterStateSystem(Family.all(CharacterStateComponent.class).get()));
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
			engine.getSystem(PhysicsSystem.class).debugDrawWorld(camera);
		}
		stage.act(delta);
		stage.draw();
	}

	private Entity spawnCharacter(Vector3 pos, IntentBroadcast intentCmp) {
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
		ModelComponent mdlCmp = new ModelComponent(model, "male", pos,
				new Vector3(0, 0, 0),
				new Vector3(1, 1, 1));
		// TODO: Ragdoll problems with frustum culling. Move capsule to ragdoll.
		mdlCmp.ignoreCulling = true;
		entity.add(mdlCmp);
		for (Animation a : mdlCmp.modelInstance.animations) {
			Gdx.app.debug(tag, "Found animation: " + a.id);
		}

		// Motion state
		MotionStateComponent motionCmp = new MotionStateComponent(mdlCmp.modelInstance.transform);
		entity.add(motionCmp);

		// Create basic capsule collision shape
		// TODO: Dispose
		btCollisionShape shape = new btCapsuleShape(0.5f, 1f);
		float bodyMass = 100;
		PhysicsComponent phyCmp = new PhysicsComponent(
				shape, motionCmp.motionState, bodyMass,
				belongsToFlag,
				collidesWithFlag,
				true, true);
		phyCmp.body.setAngularFactor(Vector3.Y);
		phyCmp.body.setWorldTransform(mdlCmp.modelInstance.transform);
		entity.add(phyCmp);

		// Ragdoll collision shapes
		String partDefJson = "models/json/character_empty.json";
		RagdollComponent ragCmp = RagdollFactory.createRagdoll(
				partDefJson, mdlCmp.modelInstance, bodyMass, belongsToFlag, collidesWithFlag);
		entity.add(ragCmp);

		// Pathfinding
		PathFindingComponent pathCmp = new PathFindingComponent(pos);
		entity.add(pathCmp);

		// Selection billboard
		assets.load("images/marker.png", Pixmap.class);
		assets.finishLoading();
		Pixmap billboardPixmap = assets.get("images/marker.png", Pixmap.class);
		float offsetY = -mdlCmp.bounds.getHeight() + mdlCmp.bounds.getCenterY();
		BillboardComponent billboard = new BillboardComponent(billboardPixmap, 1, 1, true, new Vector3(0, offsetY, 0));
		entity.add(billboard);

		// Selection component
		SelectableComponent selCmp = new SelectableComponent(billboard.modelInstance);
		entity.add(selCmp);

		// State machine component
		CharacterStateComponent charCmp = new CharacterStateComponent(
				intentCmp, mdlCmp, pathCmp, selCmp, ragCmp, phyCmp, motionCmp);
		entity.add(charCmp);

		Gdx.app.debug(tag, "Finished adding character");
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
