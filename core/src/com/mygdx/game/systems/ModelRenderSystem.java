package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.DepthMapShader;
import com.mygdx.game.GameSettings;
import com.mygdx.game.MyShaderProvider;
import com.mygdx.game.components.ModelComponent;
import com.mygdx.game.components.SelectableComponent;


/**
 * Created by user on 7/31/15.
 */
public class ModelRenderSystem extends EntitySystem {

	public static final String tag = "ModelRenderSystem";

	public Family systemFamily;
	private Vector3 pos = new Vector3();
	private ModelBatch modelBatch;
	private ImmutableArray<Entity> entities;
	private Camera camera;
	private ComponentMapper<ModelComponent> models = ComponentMapper.getFor(ModelComponent.class);
	private ComponentMapper<SelectableComponent> selectables = ComponentMapper.getFor(SelectableComponent.class);
	private Environment environment;

	private OrthographicCamera depthMapCamera;
	private ModelBatch depthMapModelBatch;
	private SpriteBatch depthMapBatch = new SpriteBatch();
	private ShaderProgram depthMapShaderProgram;
	public Texture depthMap;

	private ShaderProgram sceneShaderProgram;
	private ModelBatch shadowModelBatch;

//	private ShaderProgram shaderProgramNormalMap;


	Viewport viewport;


	public ModelRenderSystem(Viewport viewport, Camera camera, Environment environment) {
		systemFamily = Family.all(ModelComponent.class).get();
		this.viewport = viewport;
		this.camera = camera;
		this.environment = environment;

		String vert = Gdx.files.internal("shaders/vertex.glsl").readString();
		String frag = Gdx.files.internal("shaders/fragment.glsl").readString();
		DefaultShader.Config config = new DefaultShader.Config(vert, frag);
		DefaultShaderProvider p = new MyShaderProvider(config);
		modelBatch = new ModelBatch(p);


//		final DefaultShader.Config aconfig = new DefaultShader.Config();
//		shaderProgramNormalMap = new MyShader();

//		modelBatch = new ModelBatch(new DefaultShaderProvider() {
//			@Override
//			protected Shader createShader(final Renderable renderable) {
////				Shader shader = new DefaultShader(renderable, config, shaderProgramNormalMap);
//				return new MyShader(renderable, aconfig);
////				return  new DefaultShader(renderable, config, shaderProgramNormalMap);
//			}
//		});
//
		sceneShaderProgram = new ShaderProgram(Gdx.files.internal("shaders/scene_v.glsl"),
				Gdx.files.internal("shaders/scene_f.glsl"));

		shadowModelBatch = new ModelBatch(new DefaultShaderProvider() {
			@Override
			protected Shader createShader(final Renderable renderable) {
				return new DepthMapShader(renderable, sceneShaderProgram, false);
			}
		});


		depthMapShaderProgram = new ShaderProgram(Gdx.files.internal("shaders/depthmap_v.glsl"),
				Gdx.files.internal("shaders/depthmap_f.glsl"));
		depthMapModelBatch = new ModelBatch(new DefaultShaderProvider() {
			@Override
			protected Shader createShader(final Renderable renderable) {
				return new DepthMapShader(renderable, depthMapShaderProgram, true);
			}
		});

//		depthMapCamera = new PerspectiveCamera(25f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		depthMapCamera = new OrthographicCamera(Gdx.graphics.getWidth()/12, Gdx.graphics.getHeight()/12);
//		depthMapCamera = new OrthographicCamera(viewport.getScreenWidth(), viewport.getScreenHeight());
//		depthMapCamera = new OrthographicCamera(DEPTHMAPIZE, DEPTHMAPIZE);
		depthMapCamera.zoom = 1f;
		depthMapCamera.near = 1f;
		depthMapCamera.far = 100;
		depthMapCamera.position.set(40, 50, -40);
		depthMapCamera.lookAt(0, -5, 0);
		depthMapCamera.update();

	}

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}

	private boolean isVisible(final Camera camera, final ModelComponent cmp) {
		cmp.modelInstance.transform.getTranslation(pos);
//		pos.add(cmp.center);
		return camera.frustum.sphereInFrustum(pos, cmp.radius);
	}


	public FrameBuffer frameBuffer;
	public static final int DEPTHMAPIZE = 1024;

	public void renderLight() {
		if (frameBuffer == null) {
			frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, DEPTHMAPIZE, DEPTHMAPIZE, true);
//			frameBuffer = new FrameBuffer(Pixmap.Format.Alpha, DEPTHMAPIZE, DEPTHMAPIZE, true);
		}
		frameBuffer.begin();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		depthMapShaderProgram.begin();
		depthMapShaderProgram.setUniformf("u_cameraFar", depthMapCamera.far);
		depthMapShaderProgram.setUniformf("u_lightPosition", depthMapCamera.position);
		depthMapShaderProgram.end();

		depthMapModelBatch.begin(depthMapCamera);
		for (int i = 0; i < entities.size(); ++i) {
			Entity entity = entities.get(i);
			ModelComponent cmp = models.get(entity);
			depthMapModelBatch.render(cmp.modelInstance);
		}
		depthMapModelBatch.end();
		frameBuffer.end();
		depthMap = frameBuffer.getColorBufferTexture();
	}

	@Override
	public void update(float deltaTime) {
		int vw = viewport.getScreenWidth();
		int vh = viewport.getScreenHeight();
		int vx = viewport.getScreenX();
		int vy = viewport.getScreenY();

		renderLight();

		viewport.update(vw, vh);
		viewport.setScreenX(vx);
		viewport.setScreenY(vy);
		viewport.apply();


		sceneShaderProgram.begin();
		final int textureNum = 1000;
		depthMap.bind(textureNum);
		sceneShaderProgram.setUniformi("u_depthMap", textureNum);
		sceneShaderProgram.setUniformMatrix("u_lightTrans", depthMapCamera.combined);
		sceneShaderProgram.setUniformf("u_cameraFar", depthMapCamera.far);
		sceneShaderProgram.setUniformf("u_lightPosition", depthMapCamera.position);
		sceneShaderProgram.end();

		camera.update();


//		modelBatch.begin(camera);
//		for (int i = 0; i < entities.size(); ++i) {
//
//			Entity entity = entities.get(i);
//			ModelComponent cmp = models.get(entity);
//
//			if (isVisible(camera, cmp)) {
//				modelBatch.render(cmp.modelInstance, environment);
//			}
//
//			SelectableComponent selCmp = selectables.get(entity);
//			if (selCmp != null && selCmp.isSelected) {
////				modelBatch.render(cmp.modelInstance);
//			}
//		}
//		modelBatch.flush();
//		modelBatch.end();


		shadowModelBatch.begin(camera);
		for (int i = 0; i < entities.size(); ++i) {
			Entity entity = entities.get(i);
			ModelComponent cmp = models.get(entity);
//			if (isVisible(camera, cmp)) {
			shadowModelBatch.render(cmp.modelInstance, environment);
//			}
		}
		shadowModelBatch.end();


		if (GameSettings.DISPLAY_SHADOWBUFFER) {
//			Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1f);
//			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			float size = Math.min(vw, vh) / 2;
			depthMapBatch.begin();
			depthMapBatch.draw(frameBuffer.getColorBufferTexture(), 0, 0, size, size, 0, 0, 1, 1);
			depthMapBatch.end();
		}
	}
}
