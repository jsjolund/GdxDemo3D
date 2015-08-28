package com.mygdx.game;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class MyShader extends DefaultShader {
	public MyShader(Renderable renderable, ShaderProgram program) {
		super(renderable, new DefaultShader.Config(), program);
	}

}