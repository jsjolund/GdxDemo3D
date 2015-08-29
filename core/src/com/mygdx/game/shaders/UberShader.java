package com.mygdx.game.shaders;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.mygdx.game.systems.ModelRenderSystem;

/**
 * Created by user on 8/29/15.
 */
public class UberShader extends DefaultShader {

	public Renderable renderable;
	private ModelRenderSystem.ShadowData shadowData;

	public static class UberShaderSettings {
		public static float u_hue = 1f;
		public static float u_saturation = 0.7f;
		public static float u_value = 1.2f;
		public static float u_specOpacity = 0.3f;
		public static float u_lightIntensity = 2f;
		public static float u_shadowIntensity = 0.3f;
	}

	public UberShader(Renderable renderable, Config config, ModelRenderSystem.ShadowData shadowData) {
		super(renderable, config);
		this.renderable = renderable;
		this.shadowData = shadowData;
		String prefix = DefaultShader.createPrefix(renderable, config);
		program = new ShaderProgram(prefix + config.vertexShader, prefix + config.fragmentShader);
	}

	@Override
	public void begin(final Camera camera, final RenderContext context) {
		super.begin(camera, context);
//		context.setDepthTest(GL20.GL_LEQUAL);
		program.begin();
		program.setUniformi("u_depthMap", shadowData.u_depthMap);
		program.setUniformMatrix("u_lightTrans", shadowData.u_lightTrans);
		program.setUniformf("u_cameraFar", shadowData.u_cameraFar);
		program.setUniformf("u_lightPosition", shadowData.u_lightPosition);

		program.setUniformf("u_hue", UberShaderSettings.u_hue);
		program.setUniformf("u_saturation", UberShaderSettings.u_saturation);
		program.setUniformf("u_value", UberShaderSettings.u_value);
		program.setUniformf("u_specOpacity", UberShaderSettings.u_specOpacity);
		program.setUniformf("u_lightIntensity", UberShaderSettings.u_lightIntensity);
		program.setUniformf("u_shadowIntensity", UberShaderSettings.u_shadowIntensity);
	}

	@Override
	public void end() {
		program.end();
		super.end();
	}

	@Override
	public void render(final Renderable renderable, final Attributes combinedAttributes) {
		super.render(renderable, combinedAttributes);
	}

}
