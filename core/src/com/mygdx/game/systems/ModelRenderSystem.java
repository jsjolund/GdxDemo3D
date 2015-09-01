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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.GameSettings;
import com.mygdx.game.components.ModelComponent;
import com.mygdx.game.components.SelectableComponent;
import com.mygdx.game.shaders.DepthMapShader;
import com.mygdx.game.shaders.UberShader;


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
	public Texture depthMap;
	private ShadowData shadowData;

	Viewport viewport;

	private Environment selectedEnvironment;

	public class ShadowData {
		public final int u_depthMap = 10000;
		public Matrix4 u_lightTrans;
		public float u_cameraFar;
		public Vector3 u_lightPosition;
		public Vector3 u_lightDirection;
	}

	public ModelRenderSystem(Viewport viewport, Camera camera, Environment environment, Vector3 sunDirection) {
		systemFamily = Family.all(ModelComponent.class).get();
		this.viewport = viewport;
		this.camera = camera;
		this.environment = environment;

		selectedEnvironment = new Environment();
		float c = 1;
		selectedEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, c, c, c, 1));

		float t = 50;
		depthMapCamera = new OrthographicCamera(
				Gdx.graphics.getWidth() / 10,
				Gdx.graphics.getHeight() / 10);
		depthMapCamera.zoom = 1f;
		depthMapCamera.near = 1f;
		depthMapCamera.far = t * 1.5f;
//		depthMapCamera.position.set(40, 50, -40);
		depthMapCamera.position.set(sunDirection).nor().scl(-1).scl(t);
		depthMapCamera.lookAt(0, 0, 0);
		depthMapCamera.update();

		shadowData = new ShadowData();
		shadowData.u_lightTrans = depthMapCamera.combined;
		shadowData.u_cameraFar = depthMapCamera.far;
		shadowData.u_lightPosition = depthMapCamera.position;
		shadowData.u_lightDirection = depthMapCamera.direction;
		shadowData.u_lightDirection.nor();

		ShaderProgram.pedantic = false;

		final String vertDepthMap = Gdx.files.internal("shaders/depthmap.vert").readString();
		final String fragDepthMap = Gdx.files.internal("shaders/depthmap.frag").readString();
		depthMapModelBatch = new ModelBatch(new DefaultShaderProvider(vertDepthMap, fragDepthMap) {
			@Override
			protected Shader createShader(final Renderable renderable) {
				return new DepthMapShader(renderable, config);
			}
		});

		final String vertUber = Gdx.files.internal("shaders/uber.vert").readString();
		final String fragUber = Gdx.files.internal("shaders/uber.frag").readString();
		modelBatch = new ModelBatch(new DefaultShaderProvider(vertUber, fragUber) {
			@Override
			protected Shader createShader(final Renderable renderable) {
				return new UberShader(renderable, config, shadowData);
			}
		});
//		modelBatch = new ModelBatch();

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

	public void renderShadowMap() {
		if (frameBuffer == null) {
			frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, DEPTHMAPIZE, DEPTHMAPIZE, true);
		}
		frameBuffer.begin();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

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

		renderShadowMap();

		viewport.update(vw, vh);
		viewport.setScreenX(vx);
		viewport.setScreenY(vy);
		viewport.apply();

		camera.update();

		depthMap.bind(shadowData.u_depthMap);

		modelBatch.begin(camera);
		for (int i = 0; i < entities.size(); ++i) {

			Entity entity = entities.get(i);
			ModelComponent cmp = models.get(entity);

			if (isVisible(camera, cmp)) {
				SelectableComponent selCmp = selectables.get(entity);
				if (selCmp != null && selCmp.isSelected) {
					modelBatch.render(cmp.modelInstance, selectedEnvironment);
				} else {
					modelBatch.render(cmp.modelInstance, environment);
				}
			}


		}
		modelBatch.end();


		if (GameSettings.DISPLAY_SHADOWBUFFER) {
			Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			float size = Math.min(vw, vh) / 2;
			depthMapBatch.begin();
			depthMapBatch.draw(frameBuffer.getColorBufferTexture(), 0, 0, size, size, 0, 0, 1, 1);
			depthMapBatch.end();
		}
	}
}
