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
