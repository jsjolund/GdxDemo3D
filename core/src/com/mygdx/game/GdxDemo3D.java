/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.Logger;
import com.mygdx.game.gdxkit.GdxGame;

/**
 * @author jsjolund
 */
public class GdxDemo3D extends GdxGame {

	private static final String TAG = "GdxDemo3D";

	public static final int WIDTH = 1280;
	public static final int HEIGHT = 720;

	public void toggleFullscreen() {
		if (Gdx.graphics.isFullscreen()) {
			Gdx.app.debug(TAG, "Disabling fullscreen w=" + WIDTH + ", h=" + HEIGHT);
			Gdx.graphics.setWindowedMode(WIDTH, HEIGHT);
		} else {
			Gdx.app.debug(TAG, "Enabling fullscreen w=" + Gdx.graphics.getDisplayMode().width + ", h="
					+ Gdx.graphics.getDisplayMode().height);
			Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
		}
	}

	@Override
	public void create() {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		getAssetManager().getLogger().setLevel(Logger.DEBUG);

		Screen currentScreen = new LoadingGameScreen(this, new GameScreen(this));
		setScreen(currentScreen);
	}

}
