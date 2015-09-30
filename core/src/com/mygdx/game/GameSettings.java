package com.mygdx.game;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by user on 8/1/15.
 */
public class GameSettings {

	public static final float CAMERA_FOV = 60;
	public static final float CAMERA_FAR = 1E3f;
	public static final float CAMERA_NEAR = 1E-3f;
	public static final float CAMERA_MAX_PAN_VELOCITY = 30;
	public static final float CAMERA_PAN_ACCELERATION = 60;
	public static final float CAMERA_PAN_DECELERATION = 200;
	public static final float CAMERA_MIN_ZOOM = 1;
	public static final float CAMERA_ZOOM_STEP = 1;
	public static final float CAMERA_MAX_ZOOM = 40;

	public static final float SCENE_AMBIENT_LIGHT = 0.1f;

	public static final float PLAYER_WALK_SPEED = 3.5f;
	public static final float PLAYER_RUN_SPEED = 8f;

	public static final Vector3 GRAVITY = new Vector3(0, -9.82f, 0);

	public static boolean DRAW_COLLISION_SHAPES = false;
	public static boolean DRAW_ARMATURE = false;
	public static boolean DRAW_CONSTRAINTS = false;
	public static boolean DRAW_NAVMESH = false;

	public static float MOUSE_SENSITIVITY = 0.01f;

	public static int KEY_PAUSE = Input.Keys.SPACE;
	public static float GAME_SPEED = 1;

	public static int KEY_DRAW_ARMATURE = Input.Keys.F1;
	public static int KEY_DRAW_COLLISION_SHAPES = Input.Keys.F2;
	public static int KEY_DRAW_CONSTRAINTS = Input.Keys.F3;
	public static int KEY_KILL_SELECTED = Input.Keys.F4;
	public static int KEY_DRAW_NAVMESH = Input.Keys.F5;


	public static int KEY_PAN_FORWARD = Input.Keys.W;
	public static int KEY_PAN_LEFT = Input.Keys.A;
	public static int KEY_PAN_BACKWARD = Input.Keys.S;
	public static int KEY_PAN_RIGHT = Input.Keys.D;

	public static float SOUND_VOLUME = 1f;
	public static float MUSIC_VOLUME = 1f;
}
