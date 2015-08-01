package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

public class MyGdxGame extends Game {

	public static final String tag = "MyGdxGame";
	public static int reqWidth = 1280;
	public static int reqHeight = 720;


	@Override
	public void create() {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		setScreen(new GameScreen(reqWidth, reqHeight));
	}

}
