
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;


public class ImageRGB {
    ImageRGB(String imagePath){
        data=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        try {
            FileInputStream fin=new FileInputStream(imagePath);
            //parse .rgb file
            fin.read(rBuf,0,rBuf.length);
            fin.read(gBuf,0,gBuf.length);
            fin.read(bBuf,0,bBuf.length);
            fin.close();
        }catch (IOException e){
            System.out.println("ERROR READING FILE: "+imagePath);
        }

        //save it in a image
        int curr=0;
        for(int i=0;i<HEIGHT;++i){
            for(int j=0;j<WIDTH;++j){
                int unitRGB = rBuf[curr] << 16;
                unitRGB+= gBuf[curr] << 8;
                unitRGB+= bBuf[curr] ;
                data.setRGB(j,i,unitRGB);
                ++curr;
            }
        }
    }

    static final int HEIGHT=288;
    static final int WIDTH=352;
    static final int screenHeight=600;
    static final int screenWidth=700;
    byte[] rBuf=new byte[HEIGHT*WIDTH];
    byte[] gBuf=new byte[HEIGHT*WIDTH];
    byte[] bBuf=new byte[HEIGHT*WIDTH];
    BufferedImage data;


}

class PanelRGB extends JPanel {
    PanelRGB(int x,int y){

        this.x=x;
        this.y=y;
    }
    public void paint(Graphics g){

        if(frame.data!=null){
            g.drawImage(frame.data.getScaledInstance(ImageRGB.screenWidth, ImageRGB.screenHeight, BufferedImage.SCALE_SMOOTH), x, y, null);
        }
    }

    ImageRGB frame;
    int x;
    int y;
}
