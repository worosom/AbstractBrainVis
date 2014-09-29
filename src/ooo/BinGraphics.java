package ooo;

import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

public class BinGraphics {
	boolean drawDifference = false;
	boolean normalize = true;

	BrainVis parent;
	PGraphics canvas;

	PShape quad;

	Rulers rulers;

	PShader diff;

	BinGraphics(BrainVis _parent) {
		parent = _parent;
		rulers = new Rulers(parent, BrainVis.CHANNELS_WIDTH,
				BrainVis.SAMPLE_RATE);
		setup();
		diff = parent.loadShader("./shaders/difference.fsh");
	}

	public void setup() {
		quad = parent.createShape();
		quad.beginShape(BrainVis.QUADS);
		quad.textureMode(BrainVis.NORMAL);
		quad.vertex(0, 0, 0, 0);
		quad.vertex(0, 0, 1, 0);
		quad.vertex(0, 0, 1, 1);
		quad.vertex(0, 0, 0, 1);
		quad.endShape();
	}

	public PGraphics drawPix(int[] pix) {
		int size = (int) (pix.length / parent.canvasWidth);
		BrainVis.println("Basic Canvas size: " + parent.canvasWidth + "x"
				+ size);
		canvas = parent
				.createGraphics(parent.canvasWidth,
						BrainVis.min(size, BrainVis.MAXIMUM_TEXTURE_SIZE),
						BrainVis.P2D);
		((PGraphicsOpenGL) canvas).textureSampling(3);
		canvas.beginDraw();
		canvas.loadPixels();
		for (int x = 0; x < canvas.pixels.length; x++) {
			canvas.pixels[x] = pix[x];
		}
		canvas.updatePixels();

		if (normalize)
			canvas = normalize(canvas);
		if (drawDifference) {
			PImage vertAvg = canvas.get();
			vertAvg.resize(vertAvg.width, 1);
			diff.set("textureb", vertAvg);
			diff.set("resolution", (float) canvas.width, (float) canvas.height);
			canvas.filter(diff);
		}
		canvas.endDraw();
		updateShape();
		return canvas;
	}

	public void save(String filename) {
		canvas.save(filename);
	}

	public void updateShape() {
		int w = (int) (canvas.width);
		int h = (int) (canvas.height);
		quad.setVertex(0, -w / 2, -h / 2);
		quad.setVertex(1, w / 2, -h / 2);
		quad.setVertex(2, w / 2, h / 2);
		quad.setVertex(3, -w / 2, h / 2);
		quad.setTexture(canvas);
	}

	public void draw(PGraphics t, float x, float y, float scale) {
		float w, h;
		w = quad.getWidth() * scale;
		h = quad.getHeight() * scale;
		x *= scale;
		y *= scale;
		t.shape(quad, x, y, w, h);
		if (parent.rulers) {
			rulers.update(scale, x - w / 2, y - h / 2, canvas.width,
					canvas.height);
			rulers.draw(t);
		}
	}

	public static PGraphics normalize(PGraphics in) {
		in.beginDraw();
		in.loadPixels();
		int[] low = { 255, 255, 255 };
		int[] high = { 0, 0, 0 };
		for (int i = 0; i < in.pixels.length; i++) {
			int[] c = { (in.pixels[i] >> 16) & 0xFF,
					(in.pixels[i] >> 8) & 0xFF, (in.pixels[i]) & 0xFF };
			for (int j = 0; j < 3; j++) {
				if (low[j] > c[j])
					low[j] = c[j];
				else if (high[j] < c[j])
					high[j] = c[j];
			}
		}
		for (int j = 0; j < 3; j++) {
			if (low[j] == high[j]) {
				high[j] = 255;
			}
		}
		for (int i = 0; i < in.pixels.length; i++) {
			int r = (in.pixels[i] >> 16) & 0xFF;
			int g = (in.pixels[i] >> 8) & 0xFF;
			int b = in.pixels[i] & 0xFF;

			int a = 255 << 24;
			r = (int) (BrainVis.map(r, low[0], high[0], 0, 255) + .5) << 16;
			g = (int) (BrainVis.map(g, low[1], high[1], 0, 255) + .5) << 8;
			b = (int) (BrainVis.map(b, low[2], high[2], 0, 255) + .5);
			in.pixels[i] = a | r | g | b;
		}
		in.updatePixels();
		in.endDraw();
		return in;
	}
}