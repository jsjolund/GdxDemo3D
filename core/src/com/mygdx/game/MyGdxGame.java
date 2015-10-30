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

/**
 * @author jsjolund
 */
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
