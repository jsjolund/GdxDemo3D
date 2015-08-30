#ifdef GL_ES
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() {}

varying vec4 v_position;
vec4 g_position = vec4(0.0, 0.0, 0.0, 1.0);
#define pullPosition() (g_position = v_position)

////////////////////////////////////////////////////////////////////////////////////
////////// COLOR ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec4 v_color;
vec4 g_color = vec4(1.0, 1.0, 1.0, 1.0);
#define pullColor()	(g_color = v_color)

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_normal;
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pullNormal() (g_normal = v_normal)

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_binormal;
vec3 g_binormal = vec3(0.0, 0.0, 1.0);
#define pullBinormal() (g_binormal = v_binormal)

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_tangent;
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#define pullTangent() (g_tangent = v_tangent)

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec2 v_texCoord0;
vec2 g_texCoord0 = vec2(0.0, 0.0);
#define pullTexCoord0() (g_texCoord0 = v_texCoord0)

// Uniforms which are always available
uniform mat4 u_projViewTrans;

uniform mat4 u_worldTrans;

uniform vec4 u_cameraPosition;

uniform mat3 u_normalMatrix;

// Other uniforms
#ifdef blendedFlag
uniform float u_opacity;
#else
const float u_opacity = 1.0;
#endif

#ifdef alphaTestFlag
uniform float u_alphaTest;
#else
const float u_alphaTest = 0.0;
#endif

#ifdef shininessFlag
uniform float u_shininess;
#else
const float u_shininess = 20.0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#if defined(specularFlag) || defined(fogFlag)
#define cameraPositionFlag
#endif

#ifdef shadowMapFlag
#define separateAmbientFlag
#endif

#if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
#define fetchColorDiffuseTD(texCoord, defaultValue) texture2D(u_diffuseTexture, texCoord) * u_diffuseColor
#elif defined(diffuseTextureFlag)
#define fetchColorDiffuseTD(texCoord, defaultValue) texture2D(u_diffuseTexture, texCoord)
#elif defined(diffuseColorFlag)
#define fetchColorDiffuseTD(texCoord, defaultValue) u_diffuseColor
#else
#define fetchColorDiffuseTD(texCoord, defaultValue) (defaultValue)
#endif

#define fetchColorDiffuseD(defaultValue) fetchColorDiffuseTD(g_texCoord0, defaultValue)
#define fetchColorDiffuse() fetchColorDiffuseD(vec4(1.0))

#if defined(diffuseTextureFlag) || defined(diffuseColorFlag)
#define applyColorDiffuse(baseColor) ((baseColor) * fetchColorDiffuseD(vec4(1.0))
#else
#define applyColorDiffuse(baseColor) (baseColor)
#endif

#if defined(specularTextureFlag) && defined(specularColorFlag)
#define fetchColorSpecularTD(texCoord, defaultValue) (texture2D(u_specularTexture, texCoord).rgb * u_specularColor.rgb)
#elif defined(specularTextureFlag)
#define fetchColorSpecularTD(texCoord, defaultValue) texture2D(u_specularTexture, texCoord).rgb
#elif defined(specularColorFlag)
#define fetchColorSpecularTD(texCoord, defaultValue) u_specularColor.rgb
#else
#define fetchColorSpecularTD(texCoord, defaultValue) (defaultValue)
#endif

#define fetchColorSpecularD(defaultValue) fetchColorSpecularTD(g_texCoord0, defaultValue)
#define fetchColorSpecular() fetchColorSpecularD(vec3(0.0))

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define applyColorSpecular(intensity) ((intensity) * fetchColorSpecularD(vec3(0.0))
#define addColorSpecular(baseColor, intensity)	((baseColor) + applyColorSpecular(intensity))
#else
#define applyColorSpecular(intensity) (vec3(0.0))
#define addColorSpecular(baseColor, intensity)	(baseColor)
#endif

varying vec3 v_lightDir;
varying vec3 v_lightCol;
varying vec3 v_viewDir;
varying vec3 v_ambientLight;
//
#ifdef environmentCubemapFlag
varying vec3 v_reflect;
#endif
//
#ifdef environmentCubemapFlag
uniform samplerCube u_environmentCubemap;
#endif

#ifdef reflectionColorFlag
uniform vec4 u_reflectionColor;
#endif

#define saturate(x) clamp( x, 0.0, 1.0 )

// Mine
uniform vec3 u_lightPosition;
uniform float u_cameraFar;
uniform sampler2D u_depthMap;

uniform float u_hue;
uniform float u_saturation;
uniform float u_value;
uniform float u_specOpacity;
uniform float u_lightIntensity;
uniform float u_shadowIntensity;
uniform vec3 u_lightDirection;

varying vec4 v_positionLightTrans;
varying vec3 normal;
varying float v_intensity;


vec3 rgb2hsv(vec3 c) {
	vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
	vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
	vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

	float d = q.x - min(q.w, q.y);
	float e = 1.0e-10;
	return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
	vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
	vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
	return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
	g_color = v_color; //pullColor();
	g_texCoord0 = v_texCoord0; //pullTexCoord0();

#if defined(diffuseTextureFlag) || defined(diffuseColorFlag)
	vec4 diffuse = g_color * fetchColorDiffuseD(vec4(1.0));
#else
	vec4 diffuse = g_color;
#endif

	vec3 specular = fetchColorSpecularD(vec3(0.0));

#ifdef normalTextureFlag
	vec4 N = vec4(normalize(texture2D(u_normalTexture, g_texCoord0).xyz * 2.0 - 1.0), 1.0);
#ifdef environmentCubemapFlag
	vec3 reflectDir = normalize(v_reflect + (vec3(0.0, 0.0, 1.0) - N.xyz));
#endif
#else
	vec4 N = vec4(0.0, 0.0, 1.0, 1.0);
#ifdef environmentCubemapFlag
	vec3 reflectDir = normalize(v_reflect);
#endif
#endif

	vec3 L = normalize(v_lightDir);
	vec3 V = normalize(v_viewDir);
	vec3 H = normalize(L + V);
	float NL = dot(N.xyz, L);
	float NH = max(0.0, dot(N.xyz, H));

	float specOpacity = u_specOpacity; //(1.0 - diffuse.w);
	float spec = min(1.0, pow(NH, 10.0) * specOpacity);
	float selfShadow = saturate(4.0 * NL);

#ifdef environmentCubemapFlag
	vec3 environment = textureCube(u_environmentCubemap, reflectDir).rgb;
	specular *= environment;
#ifdef reflectionColorFlag
	diffuse.rgb = saturate(vec3(1.0) - u_reflectionColor.rgb) * diffuse.rgb + environment * u_reflectionColor.rgb;
#endif
#endif


//	inputColor.w = 1;
//	inputColor.xyz = vec3(0.6);

	vec3 depth = (v_positionLightTrans.xyz / v_positionLightTrans.w) * 0.5 + 0.5;
	depth.y += 0.005;

	float shadow = 0;
	// Make sure the point is in the field of view of the light and also that it is not behind it
	if (v_positionLightTrans.z >= 0.0
	        && (depth.x >= 0.0) && (depth.x <= 1.0)
            && (depth.y >= 0.0) && (depth.y <= 1.0)) {

		float lenToLight = length(v_position.xyz - u_lightPosition) / u_cameraFar;
		float lenDepthMap = texture2D(u_depthMap, depth.xy).a;
		float d = dot(u_lightDirection, normal);

		if (normal.y > 0.5) {
			if (lenDepthMap > lenToLight -0) {
				// Horizontal surface exposed to light
//                inputColor.rgb = vec3(1);
                shadow = 0;
			} else {
				// Horizontal surface which recieves shadow
				shadow = 1;
//				inputColor.rgb = vec3(0,1,0)*(1-d*2);
			}
		} else {
            if ((d > 0) ){
            	// Vertical surface facing away from the light
				shadow = d*2;
//				inputColor.rgb = vec3(1,1,1)*(1-d*2);

            } else {
            	lenDepthMap = texture2D(u_depthMap, depth.xy).a;
            	if (lenDepthMap < lenToLight - 0.005) {
            		// Vertical surface which recieves shadow
            		shadow = 1;
//            		inputColor.rgb = vec3(0,0,1);

            	} else {
            		// Vertical surface exposed to light
            		shadow = 0;
//            		inputColor.rgb = vec3(1);
            	}
            }
		}
	} else {
	    // Outside field of view of light
	    shadow = 0;
	}

	vec4 inputColor = vec4(0);
	inputColor.w = diffuse.w;

	inputColor.rgb = saturate((v_lightCol * diffuse.rgb) * (NL-shadow));
	inputColor.rgb += (selfShadow * spec) * specular;
	inputColor.rgb += v_ambientLight * diffuse.rgb;
	inputColor.rgb*=(1-shadow*u_shadowIntensity);

    // Hue, saturation, value setting
	vec3 hsv = rgb2hsv(inputColor.rgb);
	hsv.x*=u_hue;
	hsv.y*=u_saturation;
	hsv.z*=u_value;
	inputColor.rgb = hsv2rgb(hsv);

	gl_FragColor = inputColor;


}

