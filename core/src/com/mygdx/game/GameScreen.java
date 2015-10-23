package com.mygdx.game;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.blender.BlenderScene;
import com.mygdx.game.objects.Billboard;
import com.mygdx.game.objects.GameCharacter;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.CameraController;
import com.mygdx.game.utilities.GhostCamera;
import com.mygdx.game.utilities.ModelFactory;

/**
 * Created by user on 8/1/15.
 */
public class GameScreen implements Screen {

	private final static String tag = "GameScreen";

	private final Viewport viewport;
	private final GameStage stage;
	private final GameEngine engine;
	private final Color viewportBackgroundColor;
	private final GhostCamera camera;
	private final AssetManager assets;
	private final ShapeRenderer shapeRenderer;
	private final btIDebugDraw.DebugDrawModes modes;
	private final btIDebugDraw debugDraw;
	private final Array<BlenderScene> scenes = new Array<BlenderScene>();
	private final CameraController cameraController;
	private final GameRenderer renderSys;

	public GameScreen(int reqWidth, int reqHeight) {
		Bullet.init();
		MipMapGenerator.setUseHardwareMipMap(true);

		assets = new AssetManager();
		engine = new GameEngine();
		viewportBackgroundColor = Color.BLACK;

		camera = new GhostCamera(GameSettings.CAMERA_FOV, reqWidth, reqHeight);
		camera.near = GameSettings.CAMERA_NEAR;
		camera.far = GameSettings.CAMERA_FAR;
		camera.update();
		viewport = new FitViewport(reqWidth, reqHeight, camera);

		shapeRenderer = new ShapeRenderer();

		BlenderScene blenderScene = new BlenderScene(
				"models/json/scene0_model.json",
				"models/json/scene0_empty.json",
				"models/json/scene0_light.json",
				"models/json/scene0_camera.json"
		);
		scenes.add(blenderScene);

		engine.navmesh = blenderScene.navMesh;
		engine.navmeshEntity = blenderScene.navmeshEntity;
		engine.addEntity(blenderScene.navmeshEntity);

		blenderScene.setToSceneCamera(camera);

		renderSys = new GameRenderer(viewport, camera, engine);
		renderSys.setEnvironmentLights(blenderScene.lights, blenderScene.shadowCameraDirection);

		modes = new btIDebugDraw.DebugDrawModes();
		debugDraw = engine.dynamicsWorld.getDebugDrawer();

		for (Entity entity : blenderScene.entities) {
			engine.addEntity(entity);
		}
		for (Entity entity : blenderScene.ghosts) {
			engine.addEntity(entity);
		}

		cameraController = new CameraController(camera);
		cameraController.setWorldBoundingBox(blenderScene.worldBounds);
		stage = new GameStage(engine, viewport, cameraController);
		stage.addObserver(renderSys);

		engine.addEntity(spawnCharacter(new Vector3(5, 1, 0)));
		engine.addEntity(spawnCharacter(new Vector3(0, 1, 5)));
		engine.addEntity(spawnCharacter(new Vector3(10, 1, 5)));
		engine.addEntity(spawnCharacter(new Vector3(-12, 4, 10)));

		// Selection billboard
		TextureLoader.TextureParameter param = new TextureLoader.TextureParameter();
		param.genMipMaps = true;
		param.minFilter = Texture.TextureFilter.MipMap;
		param.magFilter = Texture.TextureFilter.Linear;
		assets.load("images/marker.png", Texture.class, param);
		assets.finishLoading();
		Texture billboardPixmap = assets.get("images/marker.png", Texture.class);
		// TODO: dispose or use an asset manager model
		Model billboardModel = ModelFactory.buildBillboardModel(billboardPixmap, 1, 1);
		Billboard markerBillboard = new Billboard(billboardModel, "marker", camera, true, new Matrix4(),
				new Vector3(0, -GameSettings.CHAR_CAPSULE_Y_HALFEXT * 0.8f, 0));
		renderSys.setSelectionMarker(markerBillboard);
		engine.addEntity(markerBillboard);
	}

	@Override
	public void dispose() {
		stage.dispose();
		assets.dispose();
		shapeRenderer.dispose();
		debugDraw.dispose();
		renderSys.dispose();
		for (BlenderScene scene : scenes) {
			scene.dispose();
		}
		scenes.clear();
		engine.dispose();
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
		renderSys.update(delta);

		if (DebugViewSettings.drawCollShapes || DebugViewSettings.drawConstraints) {
			int mode = 0;
			if (DebugViewSettings.drawConstraints) {
				mode |= modes.DBG_DrawConstraints;
				mode |= modes.DBG_DrawConstraintLimits;
			} else if (DebugViewSettings.drawCollShapes) {
				mode |= modes.DBG_DrawWireframe;
			}
			debugDraw.setDebugMode(mode);
			engine.debugDrawWorld(camera);
		}
		stage.act(delta);
		stage.draw();
		camera.update(delta, GameSettings.CAMERA_LERP_ALPHA);
	}

	private Entity spawnCharacter(Vector3 initialPosition) {

		short belongsToFlag = GameEngine.PC_FLAG;
		short collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.GROUND_FLAG);

		// Model
		ModelLoader.ModelParameters param = new ModelLoader.ModelParameters();
		param.textureParameter.genMipMaps = true;
		param.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		param.textureParameter.magFilter = Texture.TextureFilter.Linear;
		assets.load("models/g3db/character_male_base.g3db", Model.class, param);
		assets.finishLoading();
		Model model = assets.get("models/g3db/character_male_base.g3db");

		btCollisionShape shape = new btCapsuleShape(
				GameSettings.CHAR_CAPSULE_XZ_HALFEXT,
				GameSettings.CHAR_CAPSULE_Y_HALFEXT);
		float mass = 100;
		boolean callback = false;
		boolean noDeactivate = true;

		String ragdollJson = "models/json/character_empty.json";
		String armatureNodeId = "armature";

		GameCharacter character = new GameCharacter(model, "character", initialPosition, new Vector3(0, 0, 0),
				new Vector3(1, 1, 1), shape, mass, belongsToFlag, collidesWithFlag, callback, noDeactivate, ragdollJson, armatureNodeId);
		character.ignoreCulling = true;
		character.body.setAngularFactor(Vector3.Y);
		character.body.setRestitution(0);

		for (int i = 0; i < 10; i++) {
			character.layers.set(i);
		}
		character.pathData.currentTriangle = engine.navmesh.rayTest(character.pathData.posGroundRay, 100, character.layers);
		character.layers.clear();
		character.layers.set(character.pathData.currentTriangle.meshPartIndex);
		return character;
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



