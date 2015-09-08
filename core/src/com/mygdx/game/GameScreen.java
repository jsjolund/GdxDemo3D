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
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConeTwistConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.components.*;
import com.mygdx.game.components.blender.BlenderComponent;
import com.mygdx.game.components.blender.BlenderComponentsLoader;
import com.mygdx.game.components.blender.BlenderEmptyComponent;
import com.mygdx.game.systems.*;

import java.util.ArrayList;

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
				"models/json/scene0_model.json",
				"models/json/scene0_empty.json",
				"models/json/scene0_light.json"
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
		engine.addEntityListener(phySys.systemFamily, phySys.physicsComponentListener);
		engine.addEntityListener(Family.all(RagdollComponent.class).get(), phySys.ragdollComponentListener);
		engine.addEntityListener(Family.all(RagdollConstraintComponent.class).get(), phySys.ragdollConstraintListener);

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
//			entity.add(intentCmp);
			ModelComponent modelCmp = entity.getComponent(ModelComponent.class);

			if (modelCmp.id.startsWith("door")) {
				PhysicsComponent phyCmp = entity.getComponent(PhysicsComponent.class);
				btHingeConstraint hinge = new btHingeConstraint(phyCmp.body, new Vector3(0, 0, -0.6f), Vector3.Y);
				hinge.enableAngularMotor(true, 0, 5);

				hinge.setDbgDrawSize(5);
				phySys.dynamicsWorld.addConstraint(hinge);
				phyCmp.addConstraint(hinge);
			}

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


		Family pathFamily = Family.all(PathFindingComponent.class, PhysicsComponent.class).get();
		engine.addSystem(new PathFindingSystem(pathFamily));

		Family animFamily = Family.all(CharacterActionComponent.class).get();
		engine.addSystem(new AnimationSystem(animFamily));

		Family ragdollFamily = Family.all(CharacterActionComponent.class, RagdollComponent.class).get();
		engine.addSystem(new RagdollSystem(ragdollFamily));
	}


	private void spawnCharacter(Vector3 pos, IntentBroadcastComponent intentCmp) {
		Entity entity = new Entity();

		short belongsToFlag = PhysicsSystem.PC_FLAG;
		short collidesWithFlag = (short) (PhysicsSystem.OBJECT_FLAG | PhysicsSystem.GROUND_FLAG);

		// Get character model data
		UBJsonReader jsonReader = new UBJsonReader();
		ModelLoader modelLoader = new G3dModelLoader(jsonReader);
		ModelData modelData = modelLoader.
				loadModelData(Gdx.files.getFileHandle("models/g3db/character_male_base.g3db", Files.FileType.Internal));

		// Create normal model and outline model
		// TODO: manage, dispose
		Model model = new Model(modelData);
		Model outlineModel = new Model(modelData);
		ModelFactory.createOutlineModel(outlineModel, Color.WHITE, 0.002f);

		// Create model components containing model instances
		ModelComponent mdlCmp = new ModelComponent(model, "man", pos,
				new Vector3(0, 0, 0),
				new Vector3(1, 1, 1));
		entity.add(mdlCmp);
		ModelComponent outlineMdlCmp = new ModelComponent(outlineModel, "character_male_base_outline", pos,
				new Vector3(0, 0, 0),
				new Vector3(1, 1, 1));
		// Connect the normal modelinstance transform to outline transform, then connect them to motion state
		outlineMdlCmp.modelInstance.transform = mdlCmp.modelInstance.transform;
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
		entity.add(new SelectableComponent(outlineMdlCmp));
		entity.add(new PathFindingComponent());
		CharacterActionComponent actionCmp = new CharacterActionComponent(mdlCmp.modelInstance);
		actionCmp.addModel(outlineMdlCmp.modelInstance);
		entity.add(actionCmp);


		// Ragdoll test
		// Animation playing -> ragdoll rigid bodies controlled by animation transforms
		// No animation -> ragdoll rigid bodies controlled by dynamics world -> nodes controlled by rigid bodies

		RagdollComponent ragCmp = new RagdollComponent();
		ragCmp.belongsToFlag = belongsToFlag;
		ragCmp.collidesWithFlag = collidesWithFlag;

		// Load shape defs
		String path = "models/json/character_empty.json";
		ArrayList<BlenderEmptyComponent> empties = new Json().fromJson(ArrayList.class, BlenderEmptyComponent.class, Gdx
				.files.local(path));
		ArrayMap<String, Vector3> halfExtMap = new ArrayMap<String, Vector3>();
		for (BlenderEmptyComponent empty : empties) {
			BlenderComponentsLoader.blenderToGdxCoordinates(empty);
			Vector3 halfExtents = new Vector3(empty.scale);
			halfExtents.x = Math.abs(halfExtents.x);
			halfExtents.y = Math.abs(halfExtents.y);
			halfExtents.z = Math.abs(halfExtents.z);
			halfExtMap.put(empty.name, halfExtents);
		}

		Node armature = mdlCmp.modelInstance.getNode("armature");
		ragCmp.armatureNode = armature;

		ragCmp.headNode = armature.getChild("head", true, true);
		ragCmp.headBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("head")),
				null, bodyMass * 0.073f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.abdomenNode = armature.getChild("abdomen", true, true);
		ragCmp.abdomenBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("abdomen")),
				null, bodyMass * 0.254f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.chestNode = armature.getChild("chest", true, true);
		ragCmp.chestBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("chest")),
				null, bodyMass * 0.254f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.leftUpperArmNode = armature.getChild("left_upper_arm", true, true);
		ragCmp.leftUpperArmBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_upper_arm")),
				null, bodyMass * 0.027f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.leftForearmNode = armature.getChild("left_forearm", true, true);
		ragCmp.leftForearmBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_forearm")),
				null, bodyMass * 0.016f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.leftThighNode = armature.getChild("left_thigh", true, true);
		ragCmp.leftThighBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_thigh")),
				null, bodyMass * 0.0988f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.leftShinNode = armature.getChild("left_shin", true, true);
		ragCmp.leftShinBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("left_shin")),
				null, bodyMass * 0.0465f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.rightUpperArmNode = armature.getChild("right_upper_arm", true, true);
		ragCmp.rightUpperArmBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_upper_arm")),
				null, bodyMass * 0.027f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.rightForearmNode = armature.getChild("right_forearm", true, true);
		ragCmp.rightForearmBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_forearm")),
				null, bodyMass * 0.016f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.rightThighNode = armature.getChild("right_thigh", true, true);
		ragCmp.rightThighBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_thigh")),
				null, bodyMass * 0.0988f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.rightShinNode = armature.getChild("right_shin", true, true);
		ragCmp.rightShinBody = new PhysicsComponent(
				new btBoxShape(halfExtMap.get("right_shin")),
				null, bodyMass * 0.0465f, ragCmp.belongsToFlag,
				ragCmp.collidesWithFlag, false, true).body;

		ragCmp.populateNodeBodyMap();
		entity.add(ragCmp);

		RagdollConstraintComponent conCmp = new RagdollConstraintComponent();
		ragCmp.constraints = conCmp;

		final Matrix4 localA = new Matrix4();
		final Matrix4 localB = new Matrix4();
		btHingeConstraint hingeC = null;
		btConeTwistConstraint coneC = null;

		// Abdomen - Chest
		localA.setFromEulerAngles(0, PI2, 0).trn(0, halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAngles(0, PI2, 0).trn(0, -halfExtMap.get("chest").y, 0);
		conCmp.constraintArray.add(
				hingeC = new btHingeConstraint(ragCmp.abdomenBody, ragCmp.chestBody, localA, localB));
		hingeC.setLimit(-PI4, PI2);

		// Chest - Head
		localA.setFromEulerAngles(PI2, 0, 0).trn(0, halfExtMap.get("chest").y, 0);
		localB.setFromEulerAngles(PI2, 0, 0).trn(0, -halfExtMap.get("head").y, 0);
		conCmp.constraintArray.add(coneC = new btConeTwistConstraint(ragCmp.chestBody, ragCmp.headBody, localA, localB));
		coneC.setLimit(PI4, PI4, PI2);

		// Abdomen - Left Thigh
		localA.setFromEulerAngles(-PI4 * 5f, 0, 0).trn(halfExtMap.get("abdomen").x * 0.5f, -halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAngles(-PI4 * 5f, 0, 0).trn(0, halfExtMap.get("left_thigh").y, 0);
		conCmp.constraintArray.add(coneC = new btConeTwistConstraint(ragCmp.abdomenBody, ragCmp.leftThighBody, localA, localB));
		coneC.setLimit(PI4, PI4, 0);

		// Abdomen - Right Thigh
		localA.setFromEulerAngles(-PI4 * 5f, 0, 0).trn(-halfExtMap.get("abdomen").x * 0.5f, -halfExtMap.get
						("abdomen").y,
				0);
		localB.setFromEulerAngles(-PI4 * 5f, 0, 0).trn(0, halfExtMap.get("right_thigh").y, 0);
		conCmp.constraintArray.add(coneC = new btConeTwistConstraint(ragCmp.abdomenBody, ragCmp.rightThighBody, localA,
				localB));
		coneC.setLimit(PI4, PI4, 0);

		// Left Thigh - Left Shin
		localA.setFromEulerAngles(0, PI2, 0).trn(0, -halfExtMap.get("left_thigh").y, 0);
		localB.setFromEulerAngles(0, PI2, 0).trn(0, halfExtMap.get("left_shin").y, 0);
		conCmp.constraintArray.add(hingeC = new btHingeConstraint(ragCmp.leftThighBody, ragCmp.leftShinBody, localA, localB));
		hingeC.setLimit(0, PI2);

		// Right Thigh - Right Shin
		localA.setFromEulerAngles(0, PI2, 0).trn(0, -halfExtMap.get("right_thigh").y, 0);
		localB.setFromEulerAngles(0, PI2, 0).trn(0, halfExtMap.get("right_shin").y, 0);
		conCmp.constraintArray.add(hingeC = new btHingeConstraint(ragCmp.rightThighBody, ragCmp.rightShinBody, localA,
				localB));
		hingeC.setLimit(0, PI2);

		// Chest - Left Upper Arm
		localA.setFromEulerAngles(PI, 0, 0).trn(halfExtMap.get("abdomen").x, halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAngles(PI2, 0, 0).trn(0, -halfExtMap.get("left_upper_arm").y, 0);
		conCmp.constraintArray.add(coneC = new btConeTwistConstraint(ragCmp.chestBody, ragCmp.leftUpperArmBody, localA, localB));
		coneC.setLimit(PI2, PI2, 0);

		// Chest - Right Upper Arm
		localA.setFromEulerAngles(PI, 0, 0).trn(-halfExtMap.get("abdomen").x, halfExtMap.get("abdomen").y, 0);
		localB.setFromEulerAngles(PI2, 0, 0).trn(0, -halfExtMap.get("right_upper_arm").y, 0);
		conCmp.constraintArray.add(coneC = new btConeTwistConstraint(ragCmp.chestBody, ragCmp.rightUpperArmBody,
				localA, localB));
		coneC.setLimit(PI2, PI2, 0);


		// Left Upper Arm - Left Forearm
		localA.setFromEulerAngles(0, PI2, 0).trn(0, halfExtMap.get("left_upper_arm").y, 0);
		localB.setFromEulerAngles(0, PI2, 0).trn(0, -halfExtMap.get("left_forearm").y, 0);
		conCmp.constraintArray.add(hingeC = new btHingeConstraint(ragCmp.leftUpperArmBody, ragCmp.leftForearmBody, localA, localB));
		hingeC.setLimit(0, PI2);

		// Right Upper Arm - Right Forearm
		localA.setFromEulerAngles(0, PI2, 0).trn(0, halfExtMap.get("right_upper_arm").y, 0);
		localB.setFromEulerAngles(0, PI2, 0).trn(0, -halfExtMap.get("right_forearm").y, 0);
		conCmp.constraintArray.add(hingeC = new btHingeConstraint(ragCmp.rightUpperArmBody, ragCmp.rightForearmBody,
				localA, localB));
		hingeC.setLimit(0, PI2);

		engine.addEntity(entity);
		// Done
		Gdx.app.debug(tag, "Finished adding character");
	}

	final static float PI = MathUtils.PI;
	final static float PI2 = 0.5f * PI;
	final static float PI4 = 0.25f * PI;

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
