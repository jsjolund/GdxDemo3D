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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.*;
import com.mygdx.game.components.blender.BlenderComponent;
import com.mygdx.game.components.blender.BlenderComponentsLoader;
import com.mygdx.game.systems.*;
import com.mygdx.game.utilities.RagdollFactory;

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
	AssetManager assets;
	private ShapeRenderer shapeRenderer;

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

		if (GameSettings.DRAW_COLLISION_SHAPES || GameSettings.DRAW_CONSTRAINTS) {
			engine.getSystem(PhysicsSystem.class).debugDrawWorld(camera);
			btIDebugDraw debugDraw = engine.getSystem(PhysicsSystem.class).dynamicsWorld.getDebugDrawer();
			btIDebugDraw.DebugDrawModes modes = new btIDebugDraw.DebugDrawModes();
			int mode = 0;
			if (GameSettings.DRAW_COLLISION_SHAPES) {
				mode |= modes.DBG_DrawWireframe;
			}
			if (GameSettings.DRAW_CONSTRAINTS) {
				mode |= modes.DBG_DrawConstraints;
				mode |= modes.DBG_DrawConstraintLimits;
			}
			debugDraw.setDebugMode(mode);

		}

		stage.act(delta);
		stage.draw();
	}

	public GameScreen(int reqWidth, int reqHeight) {
		assets = new AssetManager();
		engine = new PooledEngine();
		Bullet.init();

		viewportBackgroundColor = Color.BLACK;
//		viewportBackgroundColor = new Color(0.27f,0.57f,0.82f,1);

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV, reqWidth, reqHeight);
		viewport = new FitViewport(reqWidth, reqHeight, camera);
		stage = new GameStage(viewport);
		shapeRenderer = new ShapeRenderer();

		camera.position.set(5, 10, 5);
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
		BlenderComponentsLoader levelBlender = new BlenderComponentsLoader(
				assets,
				"models/json/scene0_model.json",
				"models/json/scene0_empty.json",
				"models/json/scene0_light.json"
		);
		NavMesh navMesh = new NavMesh(levelBlender.navmesh.model.meshes.first());

		Gdx.app.debug(tag, "Loading environment system");
		EnvironmentSystem envSys = new EnvironmentSystem();
		engine.addEntityListener(envSys.systemFamily, envSys.lightListener);
		engine.addSystem(envSys);

		// TODO: dispose
		Gdx.app.debug(tag, "Loading render system");
		RenderSystem renderSys = new RenderSystem(viewport, camera,
				envSys.environment,
				levelBlender.sunDirection);
		renderSys.setNavmesh(navMesh);
		engine.addSystem(renderSys);

		// TODO: dispose
		Gdx.app.debug(tag, "Loading physics system");
		PhysicsSystem phySys = new PhysicsSystem();
		engine.addSystem(phySys);
		engine.addEntityListener(phySys.systemFamily, phySys.physicsComponentListener);
		engine.addEntityListener(Family.all(RagdollComponent.class).get(), phySys.ragdollComponentListener);
		engine.addEntityListener(Family.all(RagdollConstraintComponent.class).get(), phySys.ragdollConstraintListener);

		Gdx.app.debug(tag, "Adding entities");
		Vector3 gridUnit = new Vector3();
		for (Entity entity : levelBlender.entities) {
			engine.addEntity(entity);

			BlenderComponent cmp = entity.getComponent(BlenderComponent.class);
			if (cmp != null && cmp.name.equals("grid_unit") && gridUnit.isZero()) {
				gridUnit.set(Math.abs(cmp.scale.x), Math.abs(cmp.scale.y), Math.abs(cmp.scale.z));
				Gdx.app.debug(tag, "Using grid unit " + gridUnit);
			}
		}

		ImmutableArray<Entity> modelEntities = engine.getEntitiesFor(Family.all(ModelComponent.class).get());
		for (Entity entity : modelEntities) {
//			entity.add(intentCmp);
			ModelComponent modelCmp = entity.getComponent(ModelComponent.class);

			if (modelCmp.id.startsWith("door")) {
				PhysicsComponent phyCmp = entity.getComponent(PhysicsComponent.class);
				btHingeConstraint hinge = new btHingeConstraint(phyCmp.body, new Vector3(0, 0, -0.6f), Vector3.Y);
				hinge.enableAngularMotor(true, 0, 5);

				hinge.setDbgDrawSize(1);
				phySys.dynamicsWorld.addConstraint(hinge);
				phyCmp.addConstraint(hinge);
			}
		}

		Gdx.app.debug(tag, "Adding input controller");
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage);
		InputSystem inputSys = new InputSystem(intentCmp);
		engine.addSystem(inputSys);
		multiplexer.addProcessor(inputSys.inputProcessor);
		Gdx.input.setInputProcessor(multiplexer);


		Gdx.app.debug(tag, "Adding camera system");
		CameraSystem camSys = new CameraSystem();
		engine.addSystem(camSys);


		Gdx.app.debug(tag, "Adding selection system");
		SelectionSystem selSys = new SelectionSystem(phySys, viewport);
		engine.addSystem(selSys);

		Gdx.app.debug(tag, "Adding billboard system");
		Family billFamily = Family.all(BillboardComponent.class).get();
		BillboardSystem billSys = new BillboardSystem(billFamily, camera);
		engine.addSystem(billSys);

		spawnCharacter(new Vector3(5, 1, 0), intentCmp);
		spawnCharacter(new Vector3(5, 1, 5), intentCmp);

		Family pathFamily = Family.all(PathFindingComponent.class, PhysicsComponent.class).get();
		engine.addSystem(new PathFindingSystem(pathFamily));

		Family animFamily = Family.all(CharacterActionComponent.class).get();
		engine.addSystem(new AnimationSystem(animFamily));

		Family ragdollFamily = Family.all(
				RagdollComponent.class,
				CharacterActionComponent.class,
				ModelComponent.class,
				MotionStateComponent.class,
				PhysicsComponent.class,
				SelectableComponent.class,
				IntentBroadcastComponent.class).get();
		engine.addSystem(new RagdollSystem(ragdollFamily));

//		assets.load("models/g3db/weapons_m4_carbine.g3db", Model.class);
//		assets.finishLoading();
//		Model model = assets.get("models/g3db/weapons_m4_carbine.g3db");

		BlenderComponentsLoader weaponsBlender = new BlenderComponentsLoader(
				assets,
				"models/json/weapons_model.json",
				"models/json/weapons_empty.json",
				null
		);
		for (Entity entity : weaponsBlender.entities) {
			engine.addEntity(entity);
		}
	}

	private Entity spawnCharacter(Vector3 pos, IntentBroadcastComponent intentCmp) {
		Entity entity = new Entity();

		short belongsToFlag = PhysicsSystem.PC_FLAG;
		short collidesWithFlag = (short) (PhysicsSystem.OBJECT_FLAG | PhysicsSystem.GROUND_FLAG);

		MipMapGenerator.setUseHardwareMipMap(true);
		ModelLoader.ModelParameters param = new ModelLoader.ModelParameters();
		param.textureParameter.genMipMaps = true;
		param.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		param.textureParameter.magFilter = Texture.TextureFilter.Linear;

		assets.load("models/g3db/character_male_base.g3db", Model.class, param);
		assets.finishLoading();
		Model model = assets.get("models/g3db/character_male_base.g3db");

		// Get character model data
//		UBJsonReader jsonReader = new UBJsonReader();
//		ModelLoader modelLoader = new G3dModelLoader(jsonReader);
//		ModelData modelData = modelLoader.
//				loadModelData(Gdx.files.getFileHandle("models/g3db/character_male_base.g3db", Files.FileType
//						.Internal), param);

		// Create normal model and outline model
		// TODO: manage, dispose
//		Model model = new Model(modelData);
//		Model outlineModel = new Model(modelData);
//		ModelFactory.createOutlineModel(outlineModel, Color.WHITE, 0.002f);

		// Create model components containing model instances
		ModelComponent mdlCmp = new ModelComponent(model, "man", pos,
				new Vector3(0, 0, 0),
				new Vector3(1, 1, 1));
		// TODO: Ragdoll problems with culling. Move capsule to ragdoll.
		mdlCmp.ignoreCulling = true;
		entity.add(mdlCmp);

//		ModelComponent outlineMdlCmp = new ModelComponent(outlineModel, "character_male_base_outline", new Vector3(0,
//				0, 0),
//				new Vector3(0, 0, 0),
//				new Vector3(1, 1, 1));
//		// Connect the normal modelinstance transform to outline transform, then connect them to motion state
//		outlineMdlCmp.modelInstance.transform = mdlCmp.modelInstance.transform;
		MotionStateComponent motionStateCmp = new MotionStateComponent(mdlCmp.modelInstance.transform);

		// Create base collision shape
		float bodyMass = 100;
		btCollisionShape shape = new btCapsuleShape(0.5f, 1f);
		PhysicsComponent phyCmp = new PhysicsComponent(
				shape, motionStateCmp.motionState, bodyMass,
				belongsToFlag,
				collidesWithFlag,
				true, true);
		phyCmp.body.setAngularFactor(Vector3.Y);
		phyCmp.body.setWorldTransform(mdlCmp.modelInstance.transform);
		entity.add(motionStateCmp);
		entity.add(phyCmp);
		entity.add(intentCmp);

		// Make model selectable, add pathfinding, animation components
//		entity.add(new SelectableComponent(outlineMdlCmp));
		entity.add(new PathFindingComponent());
		CharacterActionComponent actionCmp = new CharacterActionComponent(mdlCmp.modelInstance);
		for (Animation a : mdlCmp.modelInstance.animations) {
			Gdx.app.debug(tag, "Found animation: " + a.id);
		}
//		actionCmp.addModel(outlineMdlCmp.modelInstance);
		entity.add(actionCmp);

		// Ragdoll
		RagdollFactory ragdoll = new RagdollFactory(mdlCmp.modelInstance, bodyMass, belongsToFlag, collidesWithFlag);
		entity.add(ragdoll.ragCmp);
		entity.add(ragdoll.conCmp);
		engine.addEntity(entity);


		// Selection billboard TODO: Dispose
		String markerName = "marker.png";
		assets.load(markerName, Pixmap.class);
		assets.finishLoading();
		Pixmap billboardPixmap = assets.get(markerName, Pixmap.class);
		float offsetY = -mdlCmp.bounds.getHeight() + mdlCmp.bounds.getCenterY();
		BillboardComponent billboard = new BillboardComponent(billboardPixmap, 1, 1, true, new Vector3(0, offsetY, 0));
		entity.add(new SelectableComponent(billboard.modelInstance));
		entity.add(billboard);

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

	@Override
	public void dispose() {
		stage.dispose();
	}

}
