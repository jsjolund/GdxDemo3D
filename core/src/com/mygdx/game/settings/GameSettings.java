package com.mygdx.game.settings;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by user on 8/1/15.
 */
public class GameSettings {

	public static final float CAMERA_FOV = 60;
	public static final float CAMERA_FAR = 100f;
	public static final float CAMERA_NEAR = 0.1f;
	public static final float CAMERA_MAX_PAN_VELOCITY = 50;
	public static final float CAMERA_MIN_ZOOM = 1;
	public static final float CAMERA_ZOOM_STEP = 4;
	public static final float CAMERA_MAX_ZOOM = 40;
	public static final float CAMERA_LERP_ALPHA = 0.1f;

	public static final float CAMERA_PICK_RAY_DST = 100;

	public static final float SCENE_AMBIENT_LIGHT = 0.1f;

	public static final Vector3 GRAVITY = new Vector3(0, -9.82f, 0);

	public static final int SHADOW_MAP_WIDTH = 1024;
	public static final int SHADOW_MAP_HEIGHT = 1024;
	public static final float SHADOW_VIEWPORT_HEIGHT = 60;
	public static final float SHADOW_VIEWPORT_WIDTH = 60;
	public static final float SHADOW_NEAR = 1;
	public static final float SHADOW_FAR = 100;
	public static final float SHADOW_INTENSITY = 1f;

	public static final float CHAR_CAPSULE_XZ_HALFEXT = 0.4f;
	public static final float CHAR_CAPSULE_Y_HALFEXT = 1.1f;
	public static final float CHAR_MASS = 100;

	public static float GAME_SPEED = 1;

	public static float MOUSE_SENSITIVITY = 0.1f;

	public static int KEY_PAUSE = Input.Keys.SPACE;
	public static int KEY_PAN_FORWARD = Input.Keys.W;
	public static int KEY_PAN_LEFT = Input.Keys.A;
	public static int KEY_PAN_BACKWARD = Input.Keys.S;
	public static int KEY_PAN_RIGHT = Input.Keys.D;

	public static float SOUND_VOLUME = 1f;
	public static float MUSIC_VOLUME = 1f;
}
