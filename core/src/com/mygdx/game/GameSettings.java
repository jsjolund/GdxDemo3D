package com.mygdx.game;

import com.badlogic.gdx.Input;

/**
 * Created by user on 8/1/15.
 */
public class GameSettings {

	public static final float CAMERA_FOV = 60;
	public static final float CAMERA_FAR = 100;
	public static final float CAMERA_NEAR = 1E-3f;
	public static final float CAMERA_MAX_PAN_VELOCITY = 30;
	public static final float CAMERA_PAN_ACCELERATION = 60;
	public static final float CAMERA_PAN_DECELERATION = 200;
	public static final float CAMERA_MIN_ZOOM = 1;
	public static final float CAMERA_ZOOM_STEP = 1;
	public static final float CAMERA_MAX_ZOOM = 20;

	public static final float MOUSE_SENSITIVITY = 0.1f;

	public static boolean DRAW_COLLISION_DEBUG = false;
	public static int KEY_DRAW_COLLISION_DEBUG = Input.Keys.F12;

	public static int PAN_FORWARD = Input.Keys.W;
	public static int PAN_LEFT = Input.Keys.A;
	public static int PAN_BACKWARD = Input.Keys.S;
	public static int PAN_RIGHT = Input.Keys.D;

	public static float SOUND_VOLUME = 1f;
	public static float MUSIC_VOLUME = 1f;
}
