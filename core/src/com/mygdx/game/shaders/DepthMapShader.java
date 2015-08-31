package com.mygdx.game.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class DepthMapShader extends DefaultShader {

	public static final String tag = "DepthMapShader";

	public Renderable renderable;

	public DepthMapShader(Renderable renderable, Config config) {
		super(renderable, config);
		this.renderable = renderable;

		String prefix = DefaultShader.createPrefix(renderable, config);
		program = new ShaderProgram(prefix + config.vertexShader, prefix + config.fragmentShader);
		System.out.println(prefix);
		if (!program.isCompiled()) {
			Gdx.app.debug(tag, program.getLog());
		}
	}

	@Override
	public void begin(final Camera camera, final RenderContext context) {
		super.begin(camera, context);
		context.setDepthTest(GL20.GL_LEQUAL);
//		context.setCullFace(GL20.GL_FRONT);
		program.begin();
		program.setUniformf("u_cameraFar", camera.far);
		program.setUniformf("u_lightPosition", camera.position);
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

	//	public Renderable renderable;
//	public boolean frontFaceCulling;
//
//	@Override
//	public void end() {
//		super.end();
//	}
//
//	public DepthMapShader(final Renderable renderable, final ShaderProgram shaderProgram, boolean frontFaceCulling) {
//		this.renderable = renderable;
//		this.program = shaderProgram;
//		this.frontFaceCulling = frontFaceCulling;
//		register(Inputs.worldTrans, Setters.worldTrans);
//		register(Inputs.projViewTrans, Setters.projViewTrans);
//		register(Inputs.normalMatrix, Setters.normalMatrix);
//		register(Inputs.diffuseTexture, Setters.diffuseTexture);
//		register(Inputs.diffuseColor, Setters.diffuseColor);
//
//	}
//

//
//	@Override
//	public void render(final Renderable renderable) {
//		if (!renderable.material.has(BlendingAttribute.Type)) {
//			context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
//		} else {
//			context.setBlending(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
//		}
//		super.render(renderable);
//	}
//
//	@Override
//	public void init() {
//		final ShaderProgram program = this.program;
//		this.program = null;
//		init(program, renderable);
//		renderable = null;
//	}
//
//	@Override
//	public int compareTo(final Shader other) {
//		return 0;
//	}
//
//	@Override
//	public boolean canRender(final Renderable instance) {
//		return true;
//	}
//
//	@Override
//	public void render(final Renderable renderable, final Attributes combinedAttributes) {
//		super.render(renderable, combinedAttributes);
//	}

}
