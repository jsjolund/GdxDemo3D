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

uniform sampler2D u_diffuseTexture;
uniform sampler2D u_diffuseColor;
uniform vec2 resolution;

varying vec2 v_texCoords0;
varying float v_intensity;
varying vec4 v_positionLightTrans;
varying vec4 v_position;
varying vec3 normal;
//varying vec4 v_color;




uniform vec3 u_lightPosition;
uniform float u_cameraFar;
uniform sampler2D u_depthMap;




void main()
{
    vec4 finalColor  = texture2D(u_diffuseTexture, v_texCoords0);
	finalColor.rgb   = finalColor.rgb*v_intensity;
	vec3 depth = (v_positionLightTrans.xyz / v_positionLightTrans.w)*0.5+0.5;

	// Make sure the point is in the field of view of the light
	// and also that it is not behind it
	if (v_positionLightTrans.z>=0.0 &&
			(depth.x >= 0.0) && (depth.x <= 1.0) &&
			(depth.y >= 0.0) && (depth.y <= 1.0) ) {
		float lenToLight=length(v_position.xyz-u_lightPosition)/u_cameraFar;
		float lenDepthMap= texture2D(u_depthMap, depth.xy).a;
		// If can not be viewed by light > shadows
        if (normal.y > 0.5) {
            // Horizontal
            if(lenDepthMap<lenToLight-0){
                finalColor.rgb*=0.4;
            }else{
                finalColor.rgb*=0.4+0.6*(1.0-lenToLight);
            }
        } else {
            // Vertical
            if(lenDepthMap<lenToLight-0.003){
                finalColor.rgb*=0.4;
            }else{
                finalColor.rgb*=0.4+0.6*(1.0-lenToLight);
            }
        }
	}else{
		finalColor.rgb*=0.4;
	}

	gl_FragColor = finalColor;
}
