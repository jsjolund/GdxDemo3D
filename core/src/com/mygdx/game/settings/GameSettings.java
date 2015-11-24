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

package com.mygdx.game.settings;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;

public class GameSettings {

	public static final float CAMERA_FOV = 60;
	public static final float CAMERA_FAR = 100f;
	public static final float CAMERA_NEAR = 0.1f;
	public static final float CAMERA_MAX_PAN_VELOCITY = 50;
	public static final float CAMERA_MIN_ZOOM = 1;
	public static final float CAMERA_ZOOM_STEP = 4;
	public static final float CAMERA_MAX_ZOOM = 40;
	public static final float CAMERA_LERP_ALPHA = 5f;

	public static final float CAMERA_PICK_RAY_DST = 100;

	public static final float SCENE_AMBIENT_LIGHT = 0.1f;

	public static final Vector3 GRAVITY = new Vector3(0, -9.8f, 0);

	public static final int SHADOW_MAP_WIDTH = 2048;
	public static final int SHADOW_MAP_HEIGHT = 2048;
	public static final float SHADOW_VIEWPORT_HEIGHT = 100;
	public static final float SHADOW_VIEWPORT_WIDTH = 100;
	public static final float SHADOW_NEAR = 1;
	public static final float SHADOW_FAR = 100;
	public static final float SHADOW_INTENSITY = 1f;

	public static float GAME_SPEED = 1;
	public static float GAME_SPEED_PAUSE = 0;
	public static float GAME_SPEED_PLAY = 1;
	public static float GAME_SPEED_SLOW = 0.05f;

	public static float MOUSE_SENSITIVITY = 0.1f;
	public static float MOUSE_DRAG_THRESHOLD = 10f;

	public static int KEY_PAUSE = Input.Keys.SPACE;
	public static int KEY_PAN_FORWARD = Input.Keys.W;
	public static int KEY_PAN_LEFT = Input.Keys.A;
	public static int KEY_PAN_BACKWARD = Input.Keys.S;
	public static int KEY_PAN_RIGHT = Input.Keys.D;

	public static float SOUND_VOLUME = 1f;
	public static float MUSIC_VOLUME = 1f;
}
