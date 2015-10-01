package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

public class MyGdxGame extends Game {

	public static final String tag = "MyGdxGame";
	public static int reqWidth = 1280;
	public static int reqHeight = 720;

	public void toggleFullscreen() {
		if (Gdx.graphics.isFullscreen()) {
			Gdx.app.debug(tag, String.format("Disabling fullscreen w=%s, h=%s", reqWidth, reqHeight));
			Gdx.graphics.setDisplayMode(reqWidth, reqHeight, false);
		} else {
			Gdx.app.debug(tag, String.format("Enabling fullscreen w=%s, h=%s",
					Gdx.graphics.getDesktopDisplayMode().width, Gdx.graphics.getDesktopDisplayMode().height));
			Gdx.graphics.setDisplayMode(Gdx.graphics.getDesktopDisplayMode().width,
					Gdx.graphics.getDesktopDisplayMode().height, true);
		}
	}

	@Override
	public void create() {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		Screen currentScreen = new GameScreen(reqWidth, reqHeight);
		setScreen(currentScreen);
	}

}
