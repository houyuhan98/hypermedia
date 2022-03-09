
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 1920;
	int height = 1080;

	double scale = 1.0;
	boolean anti_aliasing = false;

	public ImageDisplay(String imgPath, double scale, int anti_aliasing) {
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, imgPath, imgOne);
		this.scale = scale;
		this.anti_aliasing = anti_aliasing == 1 ? true : false;
	}

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
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
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					img.setRGB(x,y,pix);
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

	private int get_pix_val(int x, int y) {
		int original_x = (int)(x / scale);
		int original_y = (int)(y / scale);
		if (original_x < 0 || original_x >= width)
			return 0;
		if (original_y < 0 || original_y >= height)
			return 0;
		if (anti_aliasing) {
			int pix = imgOne.getRGB(original_x, original_y);
			int r = (pix & 0xff0000) >> 16;
			int g = (pix & 0xff00) >> 8;
			int b = (pix & 0xff);
			int[] rs = {r, r, r, r};
			int[] gs = {g, g, g, g};
			int[] bs = {b, b, b, b};
			if (original_x + 1 < width) {
				pix = imgOne.getRGB(original_x+1, original_y);
				r = (pix & 0xff0000) >> 16;
				g = (pix & 0xff00) >> 8;
				b = (pix & 0xff);
				rs[1] = r;
				gs[1] = g;
				bs[1] = b;
			}
			if (original_y + 1 < height) {
				pix = imgOne.getRGB(original_x, original_y+1);
				r = (pix & 0xff0000) >> 16;
				g = (pix & 0xff00) >> 8;
				b = (pix & 0xff);
				rs[2] = r;
				gs[2] = g;
				bs[2] = b;
			}
			if (original_x + 1 < width && original_y + 1 < height) {
				pix = imgOne.getRGB(original_x+1, original_y+1);
				r = (pix & 0xff0000) >> 16;
				g = (pix & 0xff00) >> 8;
				b = (pix & 0xff);
				rs[3] = r;
				gs[3] = g;
				bs[3] = b;
			}
			r = (rs[0] + rs[1] + rs[2] + rs[3]) / 4;
			g = (gs[0] + gs[1] + gs[2] + gs[3]) / 4;
			b = (bs[0] + bs[1] + bs[2] + bs[3]) / 4;
			return 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
		} else {
			return imgOne.getRGB(original_x, original_y);
		}
	}

	public void showScaledImg() {
		int new_width = (int) (width * scale);
		int new_height = (int) (height * scale);
		BufferedImage img = new BufferedImage(new_width, new_height, BufferedImage.TYPE_INT_RGB);

		for (int x = 0; x < new_width; x++) {
			for (int y = 0; y < new_height; y++) {
				img.setRGB(x, y, get_pix_val(x, y));
			}
		}
		showIms(img);
	}

	private void copy_to(BufferedImage img) {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int pix = imgOne.getRGB(x, y);
				int r = (pix & 0xff0000) >> 17;
				int g = (pix & 0xff00) >> 9;
				int b = (pix & 0xff) >> 1;
				img.setRGB(x, y, 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff));
			}
		}
	}

	public void showInteraction() {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		// Use label to display the image
		frame = new JFrame();
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

		// interact
		int hx = -1;
		int hy = -1;
		final int r = 50;
		while (true) {
			double mx = MouseInfo.getPointerInfo().getLocation().x;
			double my = MouseInfo.getPointerInfo().getLocation().y;
			double sx = lbIm1.getLocationOnScreen().x;
			double sy = lbIm1.getLocationOnScreen().y;
			int cx = (int)(mx - sx);
			int cy = (int)(my - sy);
			if (cx == hx && cy == hy) {
				continue;
			} else {
				hx = cx;
				hy = cy;
			}
			copy_to(img);

			for (int x = cx - r; x <= cx + r; x++) {
				if (x < 0 || x >= width) {
					continue;
				}
				int dy = (int)Math.sqrt(r * r - (cx - x) * (cx - x));
				for (int y = cy - dy; y <= cy + dy; y++) {
					if (y < 0 || y >= height)
						continue;
					
					int xx = (int)(cx * scale + (x - cx));
					int yy = (int)(cy * scale + (y - cy));
					img.setRGB(x, y, get_pix_val(xx, yy));
				}
			}
			System.out.printf("%d %d\n", cx, cy);
			lbIm1.setIcon(new ImageIcon(img));
		}

	}

	public void showIms(BufferedImage img){
		// Use label to display the image
		frame = new JFrame();
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

	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.printf("Usage: main.exe <img-path> <mode> <scale> <anti-aliasing>");
		}
		ImageDisplay ren = new ImageDisplay(args[0], Double.parseDouble(args[2]), Integer.parseInt(args[3]));

		int mode = Integer.parseInt(args[1]);
		if (mode == 1) {
			ren.showScaledImg();
		} else {
			ren.showInteraction();
		}
	}

}
