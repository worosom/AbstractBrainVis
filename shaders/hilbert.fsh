#ifdef GL_ES
precision highp float;
#endif

#define PROCESSING_COLOR_SHADER

const int MaxIter = 14;//try other values .

uniform sampler2D texture;
uniform vec2 resolution;

float scl=1.;
float scl2=1.;
void init(){
	scl=pow(0.5,float(MaxIter));
	scl2=scl*scl;
}

//Coposition of two "rotations"
vec2 fG(vec2 t0, vec2 t1){
	return vec2(dot(t0,t1), dot(t0, t1.yx));
}

//Action of rotation on "elementary" coordinates
vec2 fA(vec2 t, vec2 p){
	return fG(t,p-vec2(0.5))+vec2(0.5);
}

//Given "elementary" coordinates of position, returns the corresponding "rotation".
vec2 fCg(vec2 p){
	return vec2(p.y, (1.-2.*p.x)*(1.-p.y));
}

//Given "elementary" coordinates of position (c=2*p.x+p.y), returns the "elementary" linear coordinates
float fL(float c){
	return max(0.,0.5*((-3.*c+13.)*c-8.));
}

//Given a point inside unit square, return the linear coordinate
float C2L(vec2 p){
	vec2 t=vec2(1.,0.);//initial rotation is the identity
	float l=0.;//initial linear coordinate
	for(int i=0; i<MaxIter;i++){
		p*=2.; vec2 p0=floor(p); p-=p0;//extract leading bits from p. Those are the "elementary" (cartesian) coordinates.
		p0=fA(t,p0);//Rotate p0 by the current rotation
		t=fG(t,fCg(p0));//update the current rotation
		float c= p0.x*2.+p0.y;
		l=l*4.+fL(c);//update l
	}
	return l*scl2;//scale the result in order to keep between 0. and 1.
}

//Given the linear coordinate of a point (in [0,1]), return the coordinates in unit square
//it's the reverse of C2L
vec2 L2C(float l){
	vec2 t=vec2(1.,0.);
	vec2 p=vec2(0.,0.);
	for(int i=0; i<MaxIter;i++){
		l*=4.; float c=floor(l); l-=c;
		c=0.5* fL(c);
		vec2 p0=vec2(floor(c),2.*(c-floor(c)));
		t=fG(t,fCg(p0));
		p0=fA(t,p0);
		p=p*2.+p0;
	}
	return p*scl;
}

float dist2box(vec2 p, float a){
	p=abs(p)-vec2(a);
	return max(p.x,p.y);
}

float d2line(vec2 p, vec2 a, vec2 b){//distance to line (a,b)
	vec2 v=b-a;
	p-=a;
	p=p-v*clamp(dot(p,v)/(dot(v,v)),0.,1.);//Fortunately it still work well when a==b => division by 0
	return min(0.5*scl,length(p));
}

void main(void)
{
	vec2 uv = 0.5-(gl_FragCoord.xy-0.5*resolution.xy) / resolution.y;
	init();
	gl_FragColor = vec4(1.0);
	float ds=dist2box(uv-0.5,.5-0.5*scl);
	if(ds>0.5*scl) return;
	//scramble the texture
	float l=C2L(uv);
	l=mod(l*2.,1.);
	vec2 ps=L2C(l)+vec2(.5*scl);
	gl_FragColor = texture2D(texture,ps,-1.);
}