// Based on http://gist.github.com/xoppa/9766698
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

#define nop() {}

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef positionFlag
attribute vec3 a_position;
#endif //positionFlag

varying vec4 v_position;
vec4 g_position = vec4(0.0, 0.0, 0.0, 1.0);
#define pushPositionValue(value) (v_position = (value))
#if defined(positionFlag)
#define passPositionValue(value) pushPositionValue(value)
#else
#define passPositionValue(value) nop()
#endif
#define passPosition() passPositionValue(g_position)
#define pushPosition() pushPositionValue(g_position)

////////////////////////////////////////////////////////////////////////////////////
////////// COLOR ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef colorFlag
attribute vec4 a_color;
#endif //colorFlag

varying vec4 v_color;
vec4 g_color = vec4(1.0, 1.0, 1.0, 1.0);
#define pushColorValue(value) (v_color = (value))
#if defined(colorFlag)
#define passColorValue(value) pushColorValue(value)
#else
#define passColorValue(value) nop()
#endif
#define passColor() passColorValue(g_color)
#define pushColor()	pushColorValue(g_color)

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef normalFlag
attribute vec3 a_normal;
#endif //normalFlag

varying vec3 v_normal;
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pushNormalValue(value) (v_normal = (value))
#if defined(normalFlag)
#define passNormalValue(value) pushNormalValue(value)
#else
#define passNormalValue(value) nop()
#endif
#define passNormal() (passNormalValue(g_normal))
#define pushNormal() (pushNormalValue(g_normal))

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef binormalFlag
attribute vec3 a_binormal;
#endif //binormalFlag

varying vec3 v_binormal;
vec3 g_binormal = vec3(0.0, 1.0, 0.0);
#define pushBinormalValue(value) (v_binormal = (value))
#if defined(binormalFlag)
#define passBinormalValue(value) pushBinormalValue(value)
#else
#define passBinormalValue(value) nop()
#endif // binormalFlag
#define passBinormal() passBinormalValue(g_binormal)
#define pushBinormal() pushBinormalValue(g_binormal)

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef tangentFlag
attribute vec3 a_tangent;
#endif //tangentFlag

varying vec3 v_tangent;
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#define pushTangentValue(value) (v_tangent = (value))
#if defined(tangentFlag)
#define passTangentValue(value) pushTangentValue(value)
#else
#define passTangentValue(value) nop()
#endif // tangentFlag
#define passTangent() passTangentValue(g_tangent)
#define pushTangent() pushTangentValue(g_tangent)

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef texCoord0Flag
#ifndef texCoordsFlag
#define texCoordsFlag
#endif
attribute vec2 a_texCoord0;
#endif

varying vec2 v_texCoord0;
vec2 g_texCoord0 = vec2(0.0, 0.0);
#define pushTexCoord0Value(value) (v_texCoord0 = value)
#if defined(texCoord0Flag)
#define passTexCoord0Value(value) pushTexCoord0Value(value)
#else
#define passTexCoord0Value(value) nop()
#endif // texCoord0Flag
#define passTexCoord0() passTexCoord0Value(g_texCoord0)
#define pushTexCoord0() pushTexCoord0Value(g_texCoord0)

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

////////////////////////////////////////////////////////////////////////////////////
////////// SKINNING
///////////////////////////////////////////////////////////////////////////////////
#ifdef boneWeight0Flag
#ifndef boneWeightsFlag
#define boneWeightsFlag
#endif
attribute vec2 a_boneWeight0;
#endif //boneWeight0Flag

#ifdef boneWeight1Flag
#ifndef boneWeightsFlag
#define boneWeightsFlag
#endif
attribute vec2 a_boneWeight1;
#endif //boneWeight1Flag

#ifdef boneWeight2Flag
#ifndef boneWeightsFlag
#define boneWeightsFlag
#endif
attribute vec2 a_boneWeight2;
#endif //boneWeight2Flag

#ifdef boneWeight3Flag
#ifndef boneWeightsFlag
#define boneWeightsFlag
#endif
attribute vec2 a_boneWeight3;
#endif //boneWeight3Flag

#ifdef boneWeight4Flag
#ifndef boneWeightsFlag
#define boneWeightsFlag
#endif
attribute vec2 a_boneWeight4;
#endif //boneWeight4Flag

#ifdef boneWeight5Flag
#ifndef boneWeightsFlag
#define boneWeightsFlag
#endif
attribute vec2 a_boneWeight5;
#endif //boneWeight5Flag

#ifdef boneWeight6Flag
#ifndef boneWeightsFlag
#define boneWeightsFlag
#endif
attribute vec2 a_boneWeight6;
#endif //boneWeight6Flag

#ifdef boneWeight7Flag
#ifndef boneWeightsFlag
#define boneWeightsFlag
#endif
attribute vec2 a_boneWeight7;
#endif //boneWeight7Flag

// Declare the bones that are available
#if defined(numBones)
#if (numBones > 0)
uniform mat4 u_bones[numBones];
#endif //numBones
#endif

// If there are bones and there are bone weights, than we can apply skinning
#if defined(numBones) && defined(boneWeightsFlag)
#if (numBones > 0)
#define skinningFlag
#endif
#endif

#ifdef skinningFlag
mat4 skinningTransform = mat4(0.0);
vec3 applySkinning(const in vec3 x) { return (skinningTransform * vec4(x, 0.0)).xyz; }
vec4 applySkinning(const in vec4 x) { return (skinningTransform * x); }
#else
#define applySkinning(x) (x)
#endif //skinningFlag

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
uniform mat4 u_shadowMapProjViewTrans;
varying vec3 v_shadowMapUv;
#define separateAmbientFlag
#endif //shadowMapFlag

#if defined(normalFlag) && defined(binormalFlag) && defined(tangentFlag)
void calculateTangentVectors() {}
#elif defined(normalFlag) && defined(binormalFlag)
void calculateTangentVectors() {
	g_tangent = normalize(cross(g_normal, g_binormal));
}
#elif defined(normalFlag) && defined(tangentFlag)
void calculateTangentVectors() {
	g_binormal = normalize(cross(g_normal, g_tangent));
}
#elif defined(binormalFlag) && defined(tangentFlag)
void calculateTangentVectors() {
	g_normal = normalize(cross(g_binormal, g_tangent));
}
#elif defined(normalFlag) || defined(binormalFlag) || defined(tangentFlag)
vec3 biggestAngle(const in vec3 base, const in vec3 v1, const in vec3 v2) {
	vec3 c1 = cross(base, v1);
	vec3 c2 = cross(base, v2);
	return (dot(c2, c2) > dot(c1, c1)) ? c2 : c1;
}
#if defined(normalFlag)
void calculateTangentVectors() {
	g_binormal = normalize(cross(g_normal, biggestAngle(g_normal, vec3(1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0))));
	g_tangent = normalize(cross(g_normal, g_binormal));
}
#elif defined(binormalFlag)
void calculateTangentVectors() {
	g_tangent = normalize(cross(g_binormal, biggestAngle(g_binormal, vec3(0.0, 0.0, 1.0), vec3(0.0, 1.0, 0.0))));
	g_normal = normalize(cross(g_binormal, g_tangent));
}
#elif defined(tangentFlag)
void calculateTangentVectors() {
	g_binormal = normalize(cross(g_tangent, biggestAngle(g_binormal, vec3(0.0, 0.0, 1.0), vec3(0.0, 1.0, 0.0))));
	g_normal = normalize(cross(g_tangent, g_binormal));
}
#endif
#endif

//////////////////////////////////////////////////////
////// AMBIENT LIGHT
//////////////////////////////////////////////////////
#ifdef ambientLightFlag
#ifndef ambientFlag
#define ambientFlag
#endif
uniform vec3 u_ambientLight;
#define getAmbientLight() (u_ambientLight)
#else
#define getAmbientLight() (vec3(0.0))
#endif


//////////////////////////////////////////////////////
////// AMBIENT CUBEMAP
//////////////////////////////////////////////////////
#ifdef ambientCubemapFlag
#ifndef ambientFlag
#define ambientFlag
#endif
uniform vec3 u_ambientCubemap[6];
vec3 getAmbientCubeLight(const in vec3 normal) {
	vec3 squaredNormal = normal * normal;
	vec3 isPositive  = step(0.0, normal);
	return squaredNormal.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], isPositive.x) +
			squaredNormal.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], isPositive.y) +
			squaredNormal.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], isPositive.z);
}
#else
#define getAmbientCubeLight(normal) (vec3(0.0))
#endif

#if defined(ambientLightFlag) && defined(ambientCubemapFlag)
#define getAmbient(normal) (getAmbientLight() + getAmbientCubeLight(normal))
#elif defined(ambientLightFlag)
#define getAmbient(normal) getAmbientLight()
#elif defined(ambientCubemapFlag)
#define getAmbient(normal) getAmbientCubeLight(normal)
#else
#define getAmbient(normal) (vec3(0.0))
#endif

//////////////////////////////////////////////////////
////// POINTS LIGHTS
//////////////////////////////////////////////////////
#ifdef lightingFlag
#if defined(numPointLights) && (numPointLights > 0)
#define pointLightsFlag
#endif // numPointLights
#endif //lightingFlag

#ifdef pointLightsFlag
struct PointLight
{
vec3 color;
vec3 position;
float intensity;
};
uniform PointLight u_pointLights[numPointLights];
#endif

//////////////////////////////////////////////////////
////// DIRECTIONAL LIGHTS
//////////////////////////////////////////////////////
#ifdef lightingFlag
#if defined(numDirectionalLights) && (numDirectionalLights > 0)
#define directionalLightsFlag
#endif // numDirectionalLights
#endif //lightingFlag

#ifdef directionalLightsFlag
struct DirectionalLight
{
vec3 color;
vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
#endif

varying vec3 v_lightDir;
varying vec3 v_lightCol;
varying vec3 v_viewDir;

#ifdef environmentCubemapFlag
varying vec3 v_reflect;
#endif

varying vec3 v_ambientLight;

void main() {
	// Non-constant global initializers do not work on Android - setting globals from attributes outside main()
	#if defined(positionFlag)
		g_position = vec4(a_position, 1.0);
	#endif
	#if defined(colorFlag)
		g_color = a_color;
	#endif
	#if defined(normalFlag)
		g_normal = a_normal;
	#endif
	#if defined(binormalFlag)
		g_binormal = a_binormal;
	#endif
	#if defined(tangentFlag)
		g_tangent = a_tangent;
	#endif
	#if defined(texCoord0Flag)
		g_texCoord0 = a_texCoord0;
	#endif
	#ifdef skinningFlag
	skinningTransform +=
	#ifdef boneWeight0Flag
		+ (a_boneWeight0.y) * u_bones[int(a_boneWeight0.x)]
	#endif //boneWeight0Flag
	#ifdef boneWeight1Flag
		+ (a_boneWeight1.y) * u_bones[int(a_boneWeight1.x)]
	#endif //boneWeight1Flag
	#ifdef boneWeight2Flag
		+ (a_boneWeight2.y) * u_bones[int(a_boneWeight2.x)]
	#endif //boneWeight2Flag
	#ifdef boneWeight3Flag
		+ (a_boneWeight3.y) * u_bones[int(a_boneWeight3.x)]
	#endif //boneWeight3Flag
	#ifdef boneWeight4Flag
		+ (a_boneWeight4.y) * u_bones[int(a_boneWeight4.x)]
	#endif //boneWeight4Flag
	#ifdef boneWeight5Flag
		+ (a_boneWeight5.y) * u_bones[int(a_boneWeight5.x)]
	#endif //boneWeight5Flag
	#ifdef boneWeight6Flag
		+ (a_boneWeight6.y) * u_bones[int(a_boneWeight6.x)]
	#endif //boneWeight6Flag
	#ifdef boneWeight7Flag
		+ (a_boneWeight7.y) * u_bones[int(a_boneWeight7.x)]
	#endif //boneWeight7Flag
		;
	#endif //skinningFlag
	
	calculateTangentVectors();
	
	g_position = applySkinning(g_position);
	g_normal = normalize(u_normalMatrix * applySkinning(g_normal));
	g_binormal = normalize(u_normalMatrix * applySkinning(g_binormal));
	g_tangent = normalize(u_normalMatrix * applySkinning(g_tangent));
	
	g_position = u_worldTrans * g_position;
	gl_Position = u_projViewTrans * g_position;

	#ifdef shadowMapFlag
	vec4 spos = u_shadowMapProjViewTrans * g_position;
	v_shadowMapUv.xy = (spos.xy / spos.w) * 0.5 + 0.5;
	v_shadowMapUv.z = min(spos.z * 0.5 + 0.5, 0.998);
	#endif //shadowMapFlag
	
	mat3 worldToTangent;
	worldToTangent[0] = g_tangent;
	worldToTangent[1] = g_binormal;
	worldToTangent[2] = g_normal;
	
	v_ambientLight = getAmbient(g_normal);
	
	v_lightDir = normalize(-u_dirLights[0].direction) * worldToTangent;
	v_lightCol = u_dirLights[0].color;
	vec3 viewDir = normalize(u_cameraPosition.xyz - g_position.xyz);
	v_viewDir = viewDir * worldToTangent;
	#ifdef environmentCubemapFlag
	v_reflect = reflect(-viewDir, g_normal);
	#endif
	
	pushColorValue(g_color);//pushColor(); does not work on Android
	pushTexCoord0Value(g_texCoord0);//pushTexCoord0(); does not work on Android
}
