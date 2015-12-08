/*******************************************************************************
 * Copyright 2014 See AUTHORS file.
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

package com.mygdx.game.utilities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * @author davebaol
 */
public final class Constants {

	private Constants () {
	}

	//
	// Vector constants
	//
	public static final Vector3 V3_UP = Vector3.Y;
	public static final Vector3 V3_DOWN = new Vector3(V3_UP).scl(-1);

	//
	// Math constants
	//
	public static final float PI = MathUtils.PI;
	public static final float PI2 = 2f * MathUtils.PI;
	public static final float PI0_5 = 0.5f * PI;
	public static final float PI0_25 = 0.25f * PI;
	public static final float PI0_75 = 0.75f * PI;

	//
	// Message codes
	//
	public static final int MSG_DOG_LETS_PLAY = 1;
	public static final int MSG_DOG_LETS_STOP_PLAYING = 2;
	public static final int MSG_DOG_STICK_THROWN = 3;
	public static final int MSG_DOG_HUMAN_IS_DEAD = 4;
	public static final int MSG_DOG_HUMAN_IS_RESURRECTED = 5;

	public static final int MSG_GUI_CLEAR_DOG_BUTTON = 10;
	public static final int MSG_GUI_SET_DOG_BUTTON_TO_WHISTLE = 11;
	public static final int MSG_GUI_SET_DOG_BUTTON_TO_THROW = 12;
	public static final int MSG_GUI_UPDATE_DOG_BUTTON = 13;

}
