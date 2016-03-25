/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw.DebugDrawModes;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.blender.objects.BlenderEmpty;
import com.mygdx.game.gdxkit.LoadableGdxScreen;
import com.mygdx.game.objects.*;
import com.mygdx.game.scene.GameObjectBlueprint;
import com.mygdx.game.scene.GameScene;
import com.mygdx.game.scene.GameSceneManager;
import com.mygdx.game.settings.DebugViewSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.CameraController;
import com.mygdx.game.utilities.GhostCamera;
import com.mygdx.game.utilities.ModelFactory;
import com.mygdx.game.utilities.Sounds;

/**
 * @author jsjolund
 */
public class GameScreen extends LoadableGdxScreen<GdxDemo3D> {

	private final static String TAG = "GameScreen";

	public static GameScreen screen;

	private final Viewport viewport;
	public GameStage stage;
	public final GameEngine engine;
	public final Sounds sounds;
	private final Color viewportBackgroundColor;
	private final GhostCamera camera;
	private final ShapeRenderer viewportBackgroundRenderer;
	private CameraController cameraController;
	private final GameRenderer renderer;
	private final GameSceneManager sceneManager;
	private final GameScene defaultScene;

	public GameScreen(GdxDemo3D game) {
		super(game);

		// FIXME Ugly hack to access the screen from anywhere
		GameScreen.screen = this;

		camera = new GhostCamera(GameSettings.CAMERA_FOV, GdxDemo3D.WIDTH, GdxDemo3D.HEIGHT);
		camera.near = GameSettings.CAMERA_NEAR;
		camera.far = GameSettings.CAMERA_FAR;
		camera.update();
		viewport = new FitViewport(GdxDemo3D.WIDTH, GdxDemo3D.HEIGHT, camera);

		Bullet.init();
		MipMapGenerator.setUseHardwareMipMap(true);

		ModelLoader.ModelParameters modelParameters = new ModelLoader.ModelParameters();
		modelParameters.textureParameter.genMipMaps = true;
		modelParameters.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		modelParameters.textureParameter.magFilter = Texture.TextureFilter.Linear;

		TextureLoader.TextureParameter textureParameter = new TextureLoader.TextureParameter();
		textureParameter.genMipMaps = true;
		textureParameter.minFilter = Texture.TextureFilter.MipMap;
		textureParameter.magFilter = Texture.TextureFilter.Linear;

		ParticleSystem particleSystem = ParticleSystem.get();
		BillboardParticleBatch pointSpriteBatch = new BillboardParticleBatch();
		pointSpriteBatch.setCamera(camera);
		particleSystem.add(pointSpriteBatch);
		ParticleEffectLoader.ParticleEffectLoadParameter peLoadParam =
				new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches());

		sounds = new Sounds();
		engine = new GameEngine();
		viewportBackgroundColor = Color.BLACK;

		renderer = new GameRenderer(viewport, camera, engine);

		viewportBackgroundRenderer = new ShapeRenderer();

		sceneManager = new GameSceneManager(modelParameters, textureParameter, peLoadParam, "particles/", "models/g3db/", ".g3db");

		// Create a default scene, which will be the game world.
		defaultScene = sceneManager.open("scene0");

		// Add placeholders consisting of blender objects to the scene, loaded from blender json.
		// These will later be used in various ways to spawn actual game objects.
		// The reason for this decoupling is that some models have their collision shapes defined as Blender empties,
		// coupled by matching string ids. These are later compared, and used for creating game object blueprints.
		defaultScene.assets.load("models/json/scene0.json");

		// Since blender placeholder objects from different .blend scenes might have string name collisions,
		// load each into a new scene  then distribute shared object blueprints among game scenes (only one scene so far).
		sceneManager.open("human_scene").assets.load("models/json/human.json");
		sceneManager.open("dog_scene").assets.load("models/json/dog.json");
	}

	@Override
	public void loadingFinished() {

		// Create some game object blueprints which will be shared between game scenes.
		sceneManager.open("utils_scene").assets.manageDisposableFromPath("marker", "images/marker.png", Texture.class);
		Texture texture = sceneManager.open("utils_scene").assets.getAsset("marker", Texture.class);
		Model billboardModel = ModelFactory.buildBillboardModel(texture, 1, 1);
		sceneManager.open("utils_scene").assets.manageDisposable("markerModel", billboardModel, Model.class);
		GameObjectBlueprint markerBlueprint = new GameObjectBlueprint();
		markerBlueprint.name = "marker";
		markerBlueprint.model = billboardModel;
		sceneManager.addSharedBlueprint("marker", markerBlueprint);

		GameObjectBlueprint humanBlueprint = new GameObjectBlueprint();
		humanBlueprint.position = new Vector3();
		humanBlueprint.rotation = new Vector3();
		humanBlueprint.scale = new Vector3(1, 1, 1);
		humanBlueprint.model = sceneManager.open("human_scene").assets.getAsset("human", Model.class);
		humanBlueprint.name = "human";
		humanBlueprint.shape = new btCapsuleShape(0.4f, 1.1f);
		humanBlueprint.shapeType = "capsule"; // TODO: Create enums
		humanBlueprint.mass = 1;
		humanBlueprint.noDeactivate = true;
		humanBlueprint.ragdollEmpties = new Array<BlenderEmpty>();
		sceneManager.open("human_scene").assets.getAllPlaceholders(BlenderEmpty.class, humanBlueprint.ragdollEmpties);
		humanBlueprint.armatureNodeId = "armature";
		humanBlueprint.belongsToFlag = GameEngine.PC_FLAG;
		humanBlueprint.collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.GROUND_FLAG);
		sceneManager.addSharedBlueprint("human", humanBlueprint);

		GameObjectBlueprint dogBlueprint = new GameObjectBlueprint();
		dogBlueprint.position = new Vector3();
		dogBlueprint.rotation = new Vector3();
		dogBlueprint.scale = new Vector3(0.3f, 0.3f, 0.3f);
		dogBlueprint.model = sceneManager.open("dog_scene").assets.getAsset("dog", Model.class);
		dogBlueprint.name = "dog";
		dogBlueprint.shape = new btCapsuleShape(0.4f, 0.5f);
		dogBlueprint.shapeType = "capsule";
		dogBlueprint.mass = 1;
		dogBlueprint.noDeactivate = true;
		dogBlueprint.armatureNodeId = "armature";
		dogBlueprint.belongsToFlag = GameEngine.PC_FLAG;
		dogBlueprint.collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.GROUND_FLAG);
		sceneManager.addSharedBlueprint("dog", dogBlueprint);

		GameObjectBlueprint stickBlueprint = new GameObjectBlueprint();
		stickBlueprint.position = new Vector3();
		stickBlueprint.rotation = new Vector3();
		stickBlueprint.scale = new Vector3(0.3f, 0.3f, 0.3f);
		stickBlueprint.model = sceneManager.open("dog_scene").assets.getAsset("stick", Model.class);
		stickBlueprint.name = "stick";
		Array<BlenderEmpty> emptiesWithId = new Array<BlenderEmpty>();
		sceneManager.open("dog_scene").assets.getPlaceholders("stick", BlenderEmpty.class, emptiesWithId);
		stickBlueprint.shape = new btBoxShape(new Vector3(emptiesWithId.first().scale).scl(0.3f));
		stickBlueprint.shapeType = emptiesWithId.first().custom_properties.get("collision_shape");
		stickBlueprint.mass = Float.parseFloat(emptiesWithId.first().custom_properties.get("mass"));
		stickBlueprint.noDeactivate = true;
		// Generate collision callback in GameEngine when stick collides with something.
		stickBlueprint.callback = true;
		stickBlueprint.belongsToFlag = GameEngine.OBJECT_FLAG;
		// Don't collide with human and dog, causes problems when throwing.
		stickBlueprint.collidesWithFlag = (short) (GameEngine.GROUND_FLAG | GameEngine.OBJECT_FLAG); 
		stickBlueprint.visibleOnLayers = new Bits();
		stickBlueprint.visibleOnLayers.set(0);
		sceneManager.addSharedBlueprint("stick", stickBlueprint);

		// Spawn game objects for the house, ground, trees and stuff,
		// which were defined in the blender scene json.
		defaultScene.spawnGameObjectsFromPlaceholders();

		defaultScene.setToSceneCamera(camera);

		renderer.setEnvironmentLights(defaultScene.lights, defaultScene.shadowCameraDirection);
		Billboard markerBillboard = defaultScene.spawnSelectionBillboard("marker", camera);
		renderer.setSelectionMarker(markerBillboard);

		cameraController = new CameraController(camera);
		cameraController.setWorldBoundingBox(defaultScene.worldBounds);

		stage = new GameStage(engine, viewport, cameraController);
		stage.addObserver(renderer);
		stage.addObserver(engine);

		// Create humans by supplying the name of the shared blueprint "human", along with position
		HumanCharacter[] humans = new HumanCharacter[]{
				defaultScene.spawnHuman("human", new Vector3(20, 1, 0), 0),
				defaultScene.spawnHuman("human", new Vector3(24, 1, -5)),
				defaultScene.spawnHuman("human", new Vector3(20, 1, 5)),
		};

		// Create dogs by supplying the name of the shared blueprint "dog", along with position
		DogCharacter[] dogs = new DogCharacter[]{
				defaultScene.spawnDog("dog", "Buddy", new Vector3(7, 0.5f, -10)),
				defaultScene.spawnDog("dog", "Snoopy", new Vector3(12, 0.5f, 10)),
				defaultScene.spawnDog("dog", "Rocky", new Vector3(15, 0.5f, 4))
		};

		Stick[] sticks = new Stick[]{
				defaultScene.spawnStick("stick", new Vector3()),
				defaultScene.spawnStick("stick", new Vector3()),
				defaultScene.spawnStick("stick", new Vector3()),
		};

		// Assign each dog to a human
		for (int i = 0, n = Math.min(humans.length, dogs.length); i < n; i++) {
			humans[i].assignDog(dogs[i]);
		}
		stage.btreeController.reset();
		
		// Grabs all the game objects from the scene
		engine.setScene(defaultScene);

		// Assign each stick to a human
		for (int i = 0, n = Math.min(humans.length, sticks.length); i < n; i++) {
			humans[i].assignStick(sticks[i]);
		}
		
		// Set which scene layers should be visible on start. House scene only has 4 or 5
		Bits defaultSceneLayers = new Bits();
		for (int i = 0; i <= 5; i++) {
			defaultSceneLayers.set(i);
		}
		stage.setVisibleLayers(defaultSceneLayers);

		Array<GameObject> trees = new Array<GameObject>();
		defaultScene.getGameModelById("tree_0", trees);
		for (GameObject obj : trees) {
			GameModel tree = (GameModel) obj;
			Gdx.app.debug(TAG, "Found tree at " + tree.modelTransform.getTranslation(new Vector3()));
		}
	}

	@Override
	public void dispose() {
		stage.dispose();
		viewportBackgroundRenderer.dispose();
		renderer.dispose();

		engine.dispose();
		sounds.dispose();

		screen = null;
	}

	@Override
	public void render(float delta) {
		camera.update(delta * GameSettings.CAMERA_LERP_ALPHA);
		stage.act(delta);

		delta *= GameSettings.GAME_SPEED;
		Gdx.gl.glClearColor(0, 0, 0, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		viewportBackgroundRenderer.begin(ShapeRenderer.ShapeType.Filled);
		viewportBackgroundRenderer.setColor(viewportBackgroundColor);
		viewportBackgroundRenderer.rect(0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
		viewportBackgroundRenderer.end();

		engine.update(delta);
		renderer.update(delta);

		if (DebugViewSettings.drawCollShapes || DebugViewSettings.drawConstraints) {
			int mode = 0;
			if (DebugViewSettings.drawConstraints) {
				mode |= DebugDrawModes.DBG_DrawConstraints;
				mode |= DebugDrawModes.DBG_DrawConstraintLimits;
			}
			if (DebugViewSettings.drawCollShapes) {
				mode |= DebugDrawModes.DBG_DrawWireframe;
			}
			engine.setDebugMode(mode);
			engine.debugDrawWorld(camera);
		}
		stage.draw();

	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		stage.resize(width, height);
	}

}



