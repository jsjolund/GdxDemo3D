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
uniform vec4 u_diffuseColor;
void main() {
  gl_FragColor = u_diffuseColor;
}
