package com.mygdx.game;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.utils.UBJsonReader;
import com.mygdx.game.components.ModelFactory;
import com.mygdx.game.shaders.TestShader;


public class TestGame extends Game {

	public PerspectiveCamera cam;
	public CameraInputController camController;
	public Model model;
	public Model modelBig;
	public ModelInstance modelInstance;
	public ModelInstance modelInstanceBig;
	public ModelBatch batch;
	public ModelBatch batchOutline;
	public Environment environment;

	public AnimationController controller;
	public AnimationController controllerBig;

	@Override
	public void create() {

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(2f, 2f, 2f);
		cam.lookAt(0, 0, 0);
		cam.near = 1f;
		cam.far = 300f;
		cam.update();

		camController = new CameraInputController(cam);
		Gdx.input.setInputProcessor(camController);

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, .4f, .4f, .4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		UBJsonReader jsonReader = new UBJsonReader();
		ModelLoader modelLoader = new G3dModelLoader(jsonReader);

		ModelData modelData = modelLoader.loadModelData(Gdx.files.getFileHandle("models/g3db/man.g3db", Files.FileType.Internal));

		model = new Model(modelData);
		modelBig = new Model(modelData);
		ModelFactory.createOutlineModel(modelBig, Color.BLUE, 0.001f);

		float s = 0.9f;
		modelInstance = new ModelInstance(model);
		for (Node node : modelInstance.nodes) {
			node.scale.set(s, s, s);
		}
		modelInstance.calculateTransforms();
		modelInstanceBig = new ModelInstance(modelBig);
		for (Node node : modelInstanceBig.nodes) {
			node.scale.set(s, s, s);
		}
		modelInstanceBig.calculateTransforms();


		controller = new AnimationController(modelInstance);
		controller.setAnimation("Armature|run", -1);

		controllerBig = new AnimationController(modelInstanceBig);
		controllerBig.setAnimation("Armature|run", -1);

		final String vert = Gdx.files.internal("shaders/xoppa.vert").readString();
		final String frag = Gdx.files.internal("shaders/xoppa.frag").readString();
		batch = new ModelBatch(new DefaultShaderProvider(vert, frag) {
			@Override
			protected Shader createShader(final Renderable renderable) {
				return new TestShader(renderable, config);
			}
		});

		final String vert2 = Gdx.files.internal("shaders/singlecolor.vert").readString();
		final String frag2 = Gdx.files.internal("shaders/singlecolor.frag").readString();
		batchOutline = new ModelBatch(new DefaultShaderProvider(vert2, frag2) {
			@Override
			protected Shader createShader(final Renderable renderable) {
				return new TestShader(renderable, config);
			}
		});
	}

	@Override
	public void render() {
		camController.update();

		float c = 0.5f;
		Gdx.gl.glClearColor(c, c, c, 1f);
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		batchOutline.begin(cam);
		batchOutline.render(modelInstanceBig, environment);
		batchOutline.end();

//		Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

		batch.begin(cam);
//		batch.render(modelInstanceBig, environment);
		batch.render(modelInstance, environment);
		batch.end();

		controller.update(Gdx.graphics.getDeltaTime());
		controllerBig.update(Gdx.graphics.getDeltaTime());
	}

	@Override
	public void dispose() {
		model.dispose();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
