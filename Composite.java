
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;



class HSV {

    int h;
    int s;
    int v;

    static float f[] = new float[3];

    public HSV(int r, int g, int b) {

        Color.RGBtoHSB(r, g, b, f);

        this.h = (int) (f[0] * 360);

        this.s = (int) (f[1] * 100);

        this.v = (int) (f[2] * 100);

    }

    static HSV toHSV(int x) {

        int r = (x >> 16) & 0xff;

        int g = (x >> 8) & 0xff;

        int b = x & 0xff;

        return new HSV(r, g, b);
    }


    int getDistance(HSV other) {

        return (h - other.h) * (h - other.h) + (s - other.s) * (s - other.s) + (v - other.v) * (v - other.v);
    }

}


public class Composite {

    JFrame frame;
    JLabel lbIm1;
    BufferedImage imgOne;
    int width = 960;
    int height = 540;

    boolean[][] cluster(BufferedImage img) {


        boolean cluster[][] = new boolean[height][width];

        HSV rgbArr[][] = new HSV[height][width];


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                HSV rgb = HSV.toHSV(img.getRGB(x, y));

                rgbArr[y][x] = rgb;
            }
        }


        HSV arr[] = new HSV[6];

        int offset = 20;

        arr[0] = rgbArr[offset][offset];

        arr[1] = rgbArr[offset][width - offset];

        arr[2] = rgbArr[height - offset][offset];

        arr[3] = rgbArr[height - offset][width - offset];


        arr[4] = rgbArr[height / 2][offset];


        arr[5] = rgbArr[height / 2][width - offset];

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                HSV HSV = rgbArr[y][x];

                boolean good = false;

                for (int i = 0; i < arr.length; i++) {

                    if (HSV.getDistance(arr[i]) < 1200) {

                        good = true;
                    }

                }


                if (good && HSV.v > 20) {

                    cluster[y][x] = false;

                } else {
                    cluster[y][x] = true;

                }
            }

        }

        return cluster;
    }

    BufferedImage combine(BufferedImage input, BufferedImage bg, boolean[][] foreFlag) {

        BufferedImage resultImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                if (foreFlag[y][x]) {

                    resultImg.setRGB(x, y, input.getRGB(x, y));

                } else {


                    resultImg.setRGB(x, y, bg.getRGB(x, y));
                }

            }
        }

        return resultImg;
    }


    /**
     * Read Image HSV
     * Reads the image of given width and height at the given imgPath into the provided BufferedImage.
     */
    private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            int frameLength = width * height * 3;

            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = frameLength;
            byte[] bytes = new byte[(int) len];

            raf.read(bytes);

            int ind = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind + height * width];
                    byte b = bytes[ind + height * width * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //int pix = ((a << 24) + (h << 16) + (s << 8) + v);
                    img.setRGB(x, y, pix);
                    ind++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showIms(String[] args) {

        // Read a parameter from command line

        if (args.length != 2) {

            System.out.println("usage: java Composite <foregroundImage> <backgroundImage>");

            System.exit(1);
        }

        String forePath = args[0];
        String backPath = args[1];

        BufferedImage foreImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, forePath, foreImg);

        BufferedImage backImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, backPath, backImg);


        boolean flag[][] = cluster(foreImg);

        BufferedImage resultImg = combine(foreImg, backImg, flag);


        // Use label to display the image
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        lbIm1 = new JLabel(new ImageIcon(resultImg));

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

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        Composite ren = new Composite();
        ren.showIms(args);
    }

}
