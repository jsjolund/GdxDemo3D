package com.mygdx.game.utilities;


import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

public class VertexColorTextureBlend extends TextureAttribute {

	public final static String RedAlias = "redVertexColorBlendTexture";
	public final static long Red = register(RedAlias);
	public final static String GreenAlias = "greenVertexColorBlendTexture";
	public final static long Green = register(GreenAlias);
	public final static String BlueAlias = "blueVertexColorBlendTexture";
	public final static long Blue = register(BlueAlias);

	static {
		Mask = Mask | Red | Green | Blue;
	}

	public VertexColorTextureBlend(long type, Texture texture) {
		super(type, texture);
		textureDescription.uWrap = Texture.TextureWrap.Repeat;
		textureDescription.vWrap = Texture.TextureWrap.Repeat;
	}

}
