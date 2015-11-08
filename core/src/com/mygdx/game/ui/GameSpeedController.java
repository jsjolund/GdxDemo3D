/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.ui;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mygdx.game.settings.GameSettings;

/**
 * @author jsjolund
 */
public class GameSpeedController extends Table {

	private final ImageButton imageButton;
	private final ImageButton.ImageButtonStyle btnPauseStyle;
	private final ImageButton.ImageButtonStyle btnPlayStyle;
	private final ImageButton.ImageButtonStyle btnSlowStyle;

	public GameSpeedController(TextureAtlas buttonAtlas) {
		btnPauseStyle = new ImageButton.ImageButtonStyle();
		btnPauseStyle.up = new TextureRegionDrawable(buttonAtlas.findRegion("pause-up"));
		btnPauseStyle.down = new TextureRegionDrawable(buttonAtlas.findRegion("pause-down"));

		btnPlayStyle = new ImageButton.ImageButtonStyle();
		btnPlayStyle.up = new TextureRegionDrawable(buttonAtlas.findRegion("play-up"));
		btnPlayStyle.down = new TextureRegionDrawable(buttonAtlas.findRegion("play-down"));

		btnSlowStyle = new ImageButton.ImageButtonStyle();
		btnSlowStyle.up = new TextureRegionDrawable(buttonAtlas.findRegion("slow-up"));
		btnSlowStyle.down = new TextureRegionDrawable(buttonAtlas.findRegion("slow-down"));

		imageButton = new ImageButton(btnPauseStyle);

		add(imageButton);

		imageButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setGameSpeed();
				event.cancel();
			}
		});
	}

	public void setGameSpeed() {
		if (GameSettings.GAME_SPEED == GameSettings.GAME_SPEED_PLAY) {
			GameSettings.GAME_SPEED = GameSettings.GAME_SPEED_PAUSE;
			imageButton.setStyle(btnSlowStyle);

		} else if (GameSettings.GAME_SPEED == GameSettings.GAME_SPEED_PAUSE) {
			GameSettings.GAME_SPEED = GameSettings.GAME_SPEED_SLOW;
			imageButton.setStyle(btnPlayStyle);

		} else if (GameSettings.GAME_SPEED == GameSettings.GAME_SPEED_SLOW) {
			GameSettings.GAME_SPEED = GameSettings.GAME_SPEED_PLAY;
			imageButton.setStyle(btnPauseStyle);
		}
	}
}
