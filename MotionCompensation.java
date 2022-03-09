import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.swing.*;

public class MotionCompensation {

	JFrame frame;
	JLabel lbIm1;

	final int width = 640;
	final int height = 320;
	final int block_size = 16;

	int x_blocks = width / block_size;
	int y_blocks = height / block_size;

	final double thresh = 200;

	Image img1 = new Image(width, height);
	Image img2 = new Image(width, height);
	Image img_blur = new Image(width, height);
	Image img_diff = new Image(width, height);


	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, Image img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					img.data[x][y][0] = bytes[ind];
					img.data[x][y][1] = bytes[ind+height*width];
					img.data[x][y][2] = bytes[ind+height*width*2]; 
					ind++;
				}
			}
			raf.close();
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void showIms(BufferedImage img, String title){
		// Use label to display the image
		frame = new JFrame(title);
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(img));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	private int error(int cx1, int cy1, int cx2, int cy2) {
		int err = 0;
		for (int dx = 0; dx < block_size; dx++) {
			for (int dy = 0; dy < block_size; dy++) {
				int diff = img1.data[cx1+dx][cy1+dy][0] - img2.data[cx2+dx][cy2+dy][0];
				err += diff > 0 ? diff : -diff;
			}
		}
		return err;
	}

	private void copy(int x_from, int y_from, int x_to, int y_to) {
		for (int dx = 0; dx < block_size; dx++) {
			for (int dy = 0; dy < block_size; dy++) {
				img_blur.data[x_to+dx][y_to+dy][0] = img1.data[x_from+dx][y_from+dy][0];
			}
		}
	}

	private void find_and_fill(int cx, int cy, int k) {
		int x0 = cx * block_size;
		int y0 = cy * block_size;

		int x_best = 0;
		int y_best = 0;
		double err_best = 1e10;
		for (int x = x0 - k; x <= x0 + k; x++) {
			if (x < 0 || x+block_size >= width)	
				continue;

			for (int y = y0 - k; y <= y0 + k; y++) {
				if (y < 0 || y+block_size >= height)
					continue;

				double err = error(x, y, x0, y0);
				if (err < err_best) {
					x_best = x;
					y_best = y;
					err_best = err;
				}
			}
		}
		copy(x_best, y_best, x0, y0);
	}

	private void compute(int k) {
		for (int nx = 0; nx < x_blocks; nx++) {
			for (int ny = 0; ny < y_blocks; ny++) {
				find_and_fill(nx, ny, k);
			}
		}

		// find diff
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				byte d = (byte)Math.abs(img2.data[x][y][0] - img_blur.data[x][y][0]);
				if (d < 0)
					d = (byte)-d;
				img_diff.data[x][y][0] = d;
			}
		}
	}

	public void work(String imgPath1, String imgPath2, int k) {
		readImageRGB(width, height, imgPath1, img1);
		readImageRGB(width, height, imgPath2, img2);

		compute(k);

		showIms(img_blur.yuv2rgb().toImage(), "Reconstructed frame");
		showIms(img_diff.yuv2rgb().toImage(), "Error difference");
	}

	public static void main(String[] args) {
		MotionCompensation comp = new MotionCompensation();
		comp.work(args[0], args[1], Integer.parseInt(args[2]));
	}

}

class Image {
	public byte data[][][];
	public int w;
	public int h;

	static double trans[][] = {
		{.299,.587,.114},
		{.596,-.274,-.322},
		{.211,-.523,.312}
	};

	static double inv_trans[][] = {
		{1., 0.956171, 0.621433},
		{1., -0.272689, -0.646813},
		{1., -1.10374, 1.70062}
	};

	public Image(int w, int h) {
		data = new byte[w][h][3];
		this.w = w;
		this.h = h;
	}

	public Image rgb2yuv() {
		Image img = new Image(this.w, this.h);

		for (int x = 0; x < this.w; x++) {
			for (int y = 0; y < this.h; y++) {
				for (int i = 0; i < 3; i++) {
					double pix = 0.0;
					for (int j = 0; j < 3; j++) {
						pix += trans[i][j] * data[x][y][j];
					}
					img.data[x][y][i] = (byte)pix;
				}
			}
		}
		return img;
	}

	public Image yuv2rgb() {
		Image img = new Image(this.w, this.h);

		for (int x = 0; x < this.w; x++) {
			for (int y = 0; y < this.h; y++) {
				for (int i = 0; i < 3; i++) {
					img.data[x][y][i] = data[x][y][0]; // only use Y channel
				}
			}
		}
		return img;
	}

	public BufferedImage toImage() {
		BufferedImage img = new BufferedImage(this.w, this.h, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < this.h; y++) {
			for (int x = 0; x < this.w; x++) {
				byte r = data[x][y][0];
				byte g = data[x][y][1];
				byte b = data[x][y][2];
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				img.setRGB(x,y,pix);
			}
		}
		return img;
	}
}
