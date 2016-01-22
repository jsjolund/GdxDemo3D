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
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.mygdx.game.utilities.Sounds;

/**
 * @author jsjolund
 */
public class GdxDemo3D extends Game {

	public static final String tag = "GdxDemo3D";
	public static int reqWidth = 1280;
	public static int reqHeight = 720;

	public void toggleFullscreen() {
		if (Gdx.graphics.isFullscreen()) {
			Gdx.app.debug(tag, "Disabling fullscreen w=" + reqWidth + ", h=" + reqHeight);
			Gdx.graphics.setWindowedMode(reqWidth, reqHeight);
		} else {
			Gdx.app.debug(tag, "Enabling fullscreen w=" + Gdx.graphics.getDisplayMode().width + ", h="
					+ Gdx.graphics.getDisplayMode().height);
			Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
		}
	}

	@Override
	public void create() {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		Screen currentScreen = new GameScreen(reqWidth, reqHeight);
		setScreen(currentScreen);
		
		Sounds.load();
	}

	@Override
	public void dispose () {
		Sounds.dispose();
		super.dispose();
	}

}
