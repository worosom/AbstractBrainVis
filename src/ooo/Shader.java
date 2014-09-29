package ooo;

import processing.core.PGraphics;
import processing.core.PImage;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

public class Shader {
	BrainVis parent;
	BinGraphics src;
	PShader shader;
	String shaderPath;

	PGraphics out;

	public Shader(BrainVis _parent, BinGraphics _src, String _shaderPath) {
		parent = _parent;
		src = _src;
		shaderPath = _shaderPath;
	}

	private void createCanvas(PGraphics in) {
		int size = (int) Math.round(Math.sqrt(in.pixels.length));
		out = parent.createGraphics(size, size, BrainVis.P2D);
		((PGraphicsOpenGL) out).textureSampling(3);
		shader = out.loadShader(shaderPath);
	}

	public PImage draw(PGraphics in) {
		createCanvas(in);
		shader.set("resolution", (float) out.width, (float) out.height);
		shader.set("texture", in);
		out.beginDraw();
		out.shader(shader);
		out.rect(0, 0, out.width, out.height);
		out.endDraw();
		return out.get();
	}

	public void set(String name, int value) {
		shader.set(name, (float) value);
	}

	public void put(PGraphics g, float x, float y, float s) {
		float w, h;
		w = out.width * s;
		h = out.height * s;
		x *= s;
		y *= s;
		g.image(out, -w / 2 + x, -h / 2 + y, w, h);
	}
}
