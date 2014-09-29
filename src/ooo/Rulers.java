package ooo;

import processing.core.PGraphics;

public class Rulers {
	BrainVis parent;
	int cC;
	int sR;

	float scale, x, y;
	float width;
	float height;
	float vertTimeStep;
	float tsMult;

	Rulers(BrainVis _parent, int _channelCount, int _sampleRate) {
		parent = _parent;
		cC = _channelCount;
		sR = _sampleRate;
		vertTimeStep = 100;
		scale = 1;
		x = 0;
		y = 0;
	}

	public void setPos(float _x, float _y) {
		x = _x;
		y = _y;
	}

	public void setScale(float _scale) {
		scale = _scale;
	}

	public void update(float _scale, float _x, float _y, float _w, float _h) {
		setPos(_x, _y);
		setScale(_scale);
		width = _w;
		height = _h;
		vertTimeStep = 1.f / (_w / cC * (1.f / 127.f));
		if (vertTimeStep * scale > 25.f)
			tsMult = 1.f;
		else if (vertTimeStep * scale > 9.f)
			tsMult = 5.f;
		else
			tsMult = 10.f;
		vertTimeStep *= tsMult;

	}

	public void draw(PGraphics t) {
		t.stroke(0 * Math.min(scale, 1));
		t.strokeWeight(1 * Math.min(scale, 1));
		t.textAlign(BrainVis.RIGHT);
		for (float i = 0; i <= width * scale; i += cC * scale) {
			t.line(i + x, y - 10 * scale, i + x, y - 1 * scale);
		}
		for (float i = 0; i <= height * scale; i += vertTimeStep * scale) {
			t.text(Math.round(i / scale / vertTimeStep * tsMult) + " s", x - 15
					* scale, i + y);
			t.line(x - 10 * scale, i + y, x - 1 * scale, i + y);
		}
		t.textAlign(BrainVis.LEFT);
	}
}
