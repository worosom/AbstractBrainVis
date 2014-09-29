package ooo;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JFileChooser;

import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;
import controlP5.Canvas;
import controlP5.ControlP5;

/**
 * @author Alexander Morosow
 * 
 */
@SuppressWarnings("serial")
public class BrainVis extends PApplet {
	public static int MAXIMUM_TEXTURE_SIZE = 8192;

	private static final int MAXWIDTH = 8191;
	static int CONTROL_WIDTH = 0;
	static int CHANNELS_WIDTH = 14;
	static int PixPerChannel = 2;
	static int SAMPLE_RATE = 128;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(new String[] { "ooo.BrainVis" });
	}

	File file;
	byte[] dataBytes;
	int[] dataInts = new int[300000];
	int[] pix;
	int skipBytes = 0;

	BinGraphics canvas;
	public int canvasWidth = 480;
	public int LcanvasWidth = 0;
	public float xPos, yPos;
	public float scale = 1.f;

	Canvas usage;
	ControlP5 cp5;

	Shader hilbert;
	boolean drawHilbert = false;
	boolean rulers = false;

	Shader difference;

	boolean[] keys = new boolean[526];

	int pixMode = 0;

	boolean play = false;

	// boolean displayUsage = true;

	// String[][] usage = { { "Use:", "" }, { "Move:", "W|A|S|D" },
	// { "Change Width:", "Q|E" }, { "Faster:", " |+Shift" },
	// { "Fine Width:", "Y|X" }, { "Zoom:", "+|-" },
	// { "+Position Reset:", "0" }, { "hide/show Usage:", "Return" } };

	public void setup() {
		size(1200, 720, P2D);
		frame.setResizable(true);
		hint(DISABLE_DEPTH_TEST);
		((PGraphicsOpenGL) g).textureSampling(3);
		background(0);
		noStroke();
		rectMode(CENTER);
		setSkipBytes(0);
		canvas = new BinGraphics(this);
		hilbert = new Shader(this, canvas, "./shaders/hilbert.fsh");
		file = new File("./gdf");
		fetchData(file);
	}

	public void draw() {
		background(255);
		checkKeys();
		if (canvasWidth != LcanvasWidth) {
			updateBitmaps();
			LcanvasWidth = canvasWidth;
		}

		pushMatrix();
		translate(width / 2, height / 2);
		float s = scale % 1.f;
		if (s / scale < .25f && scale <= 5) {
			s = (int) scale;
		} else
			s = scale;
		if (drawHilbert)
			hilbert.put(g, xPos, yPos, s);
		else
			canvas.draw(g, xPos, yPos, s);
		if (play) {
			skipBytes += 8 * CHANNELS_WIDTH * 5;
			updateBitmaps();
			// saveImage();
		}
		popMatrix();
		fill(0);
		textAlign(LEFT);
		text("Scale: " + truncateFloat(s) + "x", 5, 15);
		text("Normalized: " + canvas.normalize, 5, 30);
		text("Difference: " + canvas.drawDifference, 5, 45);
		text("Draw Mode: " + pixMode % 4, 5, 60);
		text("Horizontal Samplecount: " + canvasWidth / CHANNELS_WIDTH, 5, 75);
		text("Skipping Bytes: " + skipBytes, 5, 90);
		textAlign(RIGHT);
		frame.setTitle(file.getName());
	}

	public int[] bytesToPix(byte[] in) {
		if (in != null) {
			int len = (int) Math.ceil(in.length / (8. / PixPerChannel)
					- skipBytes);
			if (len <= 0)
				setSkipBytes(0);
			len = (int) Math.ceil(in.length / (8. / PixPerChannel) - skipBytes);
			if (dataInts.length != len)
				dataInts = new int[len];
			int i = skipBytes;
			int[][] c = new int[len][4];
			println("Byte Array length: " + in.length + "; " + dataInts.length
					+ " Color values");
			for (int v = 0; v < len; v++) {
				if (i + 3 < in.length) {
					c[v][0] = 255 << 24;
					switch (pixMode % 2) {
					case 0:
						c[v][1] = 0 << 16;
						c[v][2] = (in[i + 1]) << 8;
						c[v][3] = (in[i + 2]);
						break;
					case 1:
						c[v][1] = (128 + in[i + 0]) << 16;
						c[v][2] = (128 + in[i + 0]) << 8;
						c[v][3] = (128 + in[i + 0]);
						break;
					}
				}
				i += (8 / PixPerChannel);
			}
			for (int v = 0; v < len; v++) {
				dataInts[v] = c[v][0] | c[v][1] | c[v][2] | c[v][3];
			}
		}
		return dataInts;
	}

	public void updateBitmaps() {
		canvas.drawPix(bytesToPix(dataBytes));
		hilbert.draw(canvas.canvas);
	}

	public void setSkipBytes(int in) {
		skipBytes = in;
	}

	public float truncateFloat(float in) {
		return ((int) (in * 100) / 100.f);
	}

	public void fetchData(File dir) {
		JFileChooser fileDialog = new JFileChooser(dir);
		int openChoice = fileDialog.showOpenDialog(this);

		switch (openChoice) {
		case JFileChooser.APPROVE_OPTION:
			Path path = fileDialog.getSelectedFile().toPath();
			try {
				dataBytes = Files.readAllBytes(path);
				file = fileDialog.getSelectedFile();
				String ps = path.getFileName().toString().toLowerCase();
				String format = "";
				if (ps.contains("gdf")) {
					int id = dataBytes[0] | dataBytes[1] | dataBytes[2];
					if (id == 71) {
						CHANNELS_WIDTH = dataBytes[252] | dataBytes[252 + 1]
								| dataBytes[252 + 2] | dataBytes[252 + 3];
						println("Number of Channels: " + CHANNELS_WIDTH);
						setSkipBytes(256 * (CHANNELS_WIDTH + 1) + 4);
					} else {
						setSkipBytes(0);
						CHANNELS_WIDTH = 14;
					}
					PixPerChannel = 1;
					format = "GDF";
					canvasWidth = 2 * CHANNELS_WIDTH;
				} else {
					PixPerChannel = 2;
					format = "Unknown";
				}

				println(dataBytes.length + " Bytes loaded. Format: " + format);
				updateBitmaps();
			} catch (IOException e) {
				System.err.println("File err.");
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Keyboard Interface
	 */

	boolean checkKey(int k) {
		if (keys.length >= k) {
			return keys[k];
		}
		return false;
	}

	public void checkKeys() {
		double s = 10. * (1. / scale);
		if (keys[KeyEvent.VK_W])
			yPos += s;
		if (keys[KeyEvent.VK_S] && !keys[KeyEvent.VK_SHIFT])
			yPos -= s;
		if (keys[KeyEvent.VK_A])
			xPos += s;
		if (keys[KeyEvent.VK_D])
			xPos -= s;
		if (keys[KeyEvent.VK_S] && keys[KeyEvent.VK_SHIFT]) {
			saveImage();
		}
		if (keys[KeyEvent.VK_E] && !keys[KeyEvent.VK_SHIFT]
				&& canvasWidth < dataInts.length && canvasWidth + 1 < MAXWIDTH)
			canvasWidth++;

		if (keys[KeyEvent.VK_E] && keys[KeyEvent.VK_SHIFT]
				&& canvasWidth < dataInts.length - CHANNELS_WIDTH
				&& canvasWidth < MAXWIDTH)
			canvasWidth += CHANNELS_WIDTH;

		if (keys[KeyEvent.VK_Q] && !keys[KeyEvent.VK_SHIFT] && canvasWidth > 1)
			canvasWidth--;

		if (keys[KeyEvent.VK_Q] && keys[KeyEvent.VK_SHIFT]
				&& canvasWidth - CHANNELS_WIDTH > 0)
			canvasWidth -= CHANNELS_WIDTH;

		if (keys[93])
			scale *= 1.02;

		if (keys[47])
			scale *= .98;
		for (int i = KeyEvent.VK_1; i < KeyEvent.VK_8; i++) {
			if (keys[i]) {
				setCanvasWidth(i - 48);
				keys[i] = false;
			}
		}
	}

	public void keyPressed() {
		keys[keyCode] = true;
		if (key == 'y' && canvasWidth > 1)
			canvasWidth--;
		if (key == 'x' && canvasWidth < dataInts.length
				&& canvasWidth < MAXWIDTH)
			canvasWidth++;
		else if (key == '0') {
			resetPosition();
			scale = 1;
		} else if (key == 'r')
			rulers = !rulers;
		else if (key == 'h')
			drawHilbert = !drawHilbert;
		else if (key == 'u') {
			canvas.drawDifference = !canvas.drawDifference;
			updateBitmaps();
		} else if (key == 'n') {
			canvas.normalize = !canvas.normalize;
			updateBitmaps();
		}
	}

	public void keyReleased() {
		if (keys[KeyEvent.VK_UP]) {
			skipBytes++;
			updateBitmaps();
		} else if (keys[KeyEvent.VK_DOWN] && skipBytes > 0) {
			skipBytes--;
			updateBitmaps();
		} else if (keys[KeyEvent.VK_RIGHT]
				&& skipBytes + 128 < dataBytes.length) {
			skipBytes += 128;
			updateBitmaps();
		} else if (keys[KeyEvent.VK_LEFT] && skipBytes - 128 > 0) {
			skipBytes -= 128;
			updateBitmaps();
		}
		if (keys[KeyEvent.VK_M]) {
			pixMode++;
			updateBitmaps();
		} else if (keys[KeyEvent.VK_SHIFT] && keys[KeyEvent.VK_M]) {
			pixMode--;
			updateBitmaps();
		}
		if (keys[KeyEvent.VK_O])
			fetchData(file);
		if (keys[KeyEvent.VK_P]) {
			play = !play;
		}
		keys[keyCode] = false;
		// if (key == ENTER)
		// displayUsage = !displayUsage;
	}

	public void resetPosition() {
		if (drawHilbert) {
			xPos = 0;
			yPos = hilbert.out.height / 2 - height / 2;
		} else {
			xPos = 0;
			yPos = canvas.canvas.height / 2 - height / 2;
		}
	}

	public void setCanvasWidth(int mult) {
		canvasWidth = CHANNELS_WIDTH * (mult * mult);
		updateBitmaps();
		resetPosition();
	}

	public void saveImage() {
		long time = System.currentTimeMillis();
		if (drawHilbert) {
			try {
				hilbert.out.save(file.getCanonicalPath() + "Hilbert-"
						+ file.getName() + time + ".png");
			} catch (IOException e) {
			}
		} else {
			try {
				canvas.save(file.getCanonicalPath() + file.getName() + time
						+ ".png");
			} catch (IOException e) {
			}
		}
	}
}