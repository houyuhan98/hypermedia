
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class VideoPlayer extends JFrame {

    VideoPlayer(String sourcePath,String fileDirectory,int frameRate,int frameNum){
	    localDataDirectory=sourcePath;
        //initialize (include loading localDataFiles)
        this.currFrame=frameNum;
        this.frameRate=frameRate;
        this.fileDirectory=fileDirectory;
        this.setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        localData=new LinkedList<>();
        File localDataDir=new File(localDataDirectory);
        File[] allFiles= localDataDir.listFiles();
        if(localDataDir.exists()&&localDataDir.isDirectory()&&allFiles!=null){
            String videoFileName=ImagePathGenerator.getFileName(fileDirectory);
            for(File f:allFiles){
                //add linked files to localData:ArrayList<File>
                if(f.getName().contains(videoFileName+"_")&&ImagePathGenerator.getFileName(f.getName()).matches("\\w+_\\w+.txt")){
                    localData.add(f);
                }
            }
        }else {
            System.out.println("invalid authoring data directory");
        }
        setTitle(ImagePathGenerator.getFileName(fileDirectory));
        //new Thread for audioPlaying

        new Thread(()->{
            audio=new AudioPlayer(fileDirectory+"\\"+ImagePathGenerator.getFileName(fileDirectory)+".wav",frameNum);
        }).start();



        //basic settings for window
        setSize(718,800);

        //include play,restart and pause
        JPanel controllers=new JPanel();

        //Play ,Restart and Pause buttons
        controllers.setPreferredSize(new Dimension(1600,150));
        JButton play=new JButton("Play");
        play.addActionListener(l->{
            audio.setPlayed("PLAY");
            isPlayed=true;
        });

        JButton pause=new JButton("Pause");
        pause.addActionListener(l-> {

            this.audio.setPlayed("PAUSE");
            isPlayed = false;
        });

        JButton reStart=new JButton("Stop");
        reStart.addActionListener(l->{
            currFrame=1;
            audio.setPlayed("SET 0");
            isPlayed=false;
        });

        pause.setPreferredSize(new Dimension(100,100));
        play.setPreferredSize(new Dimension(100,100));
        reStart.setPreferredSize(new Dimension(100,100));
        controllers.add(pause);
        controllers.add(play);
        controllers.add(reStart);
        add(BorderLayout.SOUTH,controllers);
        setVisible(true);


        //preload resources
        frames=new LinkedList<>();

        //videoScreen
        PanelRGB p=new PanelRGB(0,0);
        p.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //find if the click point is linked with hyperLinked video
                //In addition, each hyperlinked will exist for 50 frames

                try {

                    String targetFileMsg=containHyperLink(new Point(e.getX(),e.getY()));

                    if(targetFileMsg==null){
                        return;
                    }

                    int targetFrame=Integer.parseInt(targetFileMsg.split(" ")[0]);
                    String targetFile=targetFileMsg.split(" ")[1];


                    if(targetFile!=null){
                        System.out.println("Link to "+targetFile+" "+targetFrame);
                        audio.setPlayed("PAUSE");
                        isPlayed=false;
                        new Thread(()->new VideoPlayer(sourcePath,targetFile,30,targetFrame)).start();
                    }
                }catch (IOException ei){
                    ei.printStackTrace();
                }

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        p.frame=new ImageRGB(fileDirectory+ImagePathGenerator.generate(fileDirectory,currFrame));
        add(p);

        while(currFrame<9001){
            if(!isPlayed&&currFrame!=1){
                System.out.print("");        //for certain reasons,do not delete this line
                continue;
            }

            //sync audio and video, use audioFrame as a benchmark
            long currAudioFrame=1;
            if(currFrame!=1){
                currAudioFrame=audio.audioClip.getFramePosition();
            }

            long expectedVideoFrame=   (long) (currAudioFrame/1470.2);
            if(expectedVideoFrame>currFrame&&currFrame!=1){
                currFrame=(int) expectedVideoFrame;
                if(!(currFrame==expectedVideoFrame)){
                    System.out.println("Data Truncation");
                }
            }


            frames.add(new ImageRGB(fileDirectory+ImagePathGenerator.generate(fileDirectory,currFrame)));
            p.frame=frames.getFirst();
            p.repaint();

            //control frameRate
            int sleepTime=(int)((float) 1000/frameRate);
            try {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            }catch (Exception e){
                e.printStackTrace();
            }
            //prevent heap overflow in LinkedList<>
            frames.removeFirst();
            ++currFrame;
        }
        System.out.println("resource loading completes...");

    }



    /**
     * @see Rect
     * @param point the click point
     * @throws IOException if the filePath passed in is invalid
     * @return linked video's fileName or, null if no relevant files have been found
     * */
    String containHyperLink(Point point)throws IOException
    {

        for(File f:localData){
            BufferedReader br=new BufferedReader(new FileReader(f));

            //read each line in localFile
            while(true){
                String currLine=br.readLine();
                if(currLine==null){
                    break;
                }
                String[] data=currLine.split(" ");
                int priFrameNum=Integer.parseInt(data[2]);
                int secFrameNum=Integer.parseInt(data[7]);
                Rect rect=Rect.valueOf(currLine);
                int x1=rect.a.x;
                int y1=rect.a.y;
                int x2=rect.b.x;
                int y2=rect.b.y;

                //there is time limit and range limit to trigger hyperlink
                if(currFrame>priFrameNum && currFrame-priFrameNum<=45
                        && point.x>=x1 && point.x<=x2
                        && point.y>=y1 && point.y<=y2){
                    return data[7]+" "+data[8];
                }
            }

            br.close();
        }
        return null;
    }

    int                  frameRate;
    int                  currFrame;
    String               fileDirectory;
    boolean              isPlayed;
    LinkedList<ImageRGB> frames;
    LinkedList<File>     localData;
    String               localDataDirectory;
    AudioPlayer          audio;

    public static void main(String[] args) {
	String sourcePath;
        System.out.println("Type in sourceFolder's path");
        while(true){
            String sourcePathIn=new Scanner(System.in).next();
            if(!sourcePathIn.equals("")){
                File detect=new File(sourcePathIn);
                if(detect.exists()&&detect.isDirectory()){
                    sourcePath=sourcePathIn;
                    System.out.println("Set SourcePath: to "+sourcePath);
                    System.out.println("This folder contains videos: \n");
                    //display files in this folder
                    for(File file:detect.listFiles()){
                        if(!file.getName().contains("txt")){
                            System.out.println("        "+file.getName());
                        }
                    }

                    break;
                }
            }
            System.out.println("Invalid sourcePath, type again");

        }

        System.out.println("\nCommand usage");
        System.out.println("-quit            #to close videoPlayer ");
        System.out.println("-open [FileName] #to open certain video file");

        String command=new Scanner(System.in).nextLine();

        while(!command.equals("quit")){
            if(command.contains("open")){
                String[] fileCommand=command.split(" ");
                if(fileCommand.length==2){
                    String fileName=fileCommand[1];
                    System.out.println("You've selected "+fileName);
                    new VideoPlayer(sourcePath,sourcePath+"\\"+fileName,30,1);
                }else{
                    command="NULL";
                    System.out.println("wrong syntax");
                }

            }
            command=new Scanner(System.in).nextLine();
        }
        System.out.println("See you~");

    }

}

/**
 * Used for generating the str sequence of .rgb file from 0001 to 9000
 * */
class ImagePathGenerator{
    ImagePathGenerator(){

    }

    /**
     * convert String like "C:\desktop\myFile" to "myFile" so as to get a simple name
     * @param path the whole name of the file
     * @return simpleName
     * */
    public static String getFileName(String path){
        return path.replaceFirst("[a-zA-Z:]\\\\","").replaceFirst("(\\w+\\\\)*","");
    }

    public static String generate(String fileDirectory,int index){
        //extract fileName from the filePath passed in
        String fileName=getFileName(fileDirectory);

        //some fixed format
        String format=".rgb";
        StringBuilder newIndex= new StringBuilder("\\"+fileName);

        //append '0' to fit the file format
        if(String.valueOf(index).length()<4){
            for(int i=0;i<4-String.valueOf(index).length();++i){
                newIndex.append("0");
            }
        }
        //piece together
        newIndex.append(index);
        newIndex.append(format);

        return newIndex.toString();
    }
}

class AudioPlayer {
    AudioPlayer(String wavPath,long videoFrame){
        frame=1+(long)((videoFrame-1)*1470.236);
        wavFile=new File(wavPath);
        if(wavFile.exists()&& wavFile.isFile()){
            try {
                //reading file
                input=AudioSystem.getAudioInputStream(wavFile);
                format=input.getFormat();
                data=new byte[(int)input.getFrameLength()*4];
                System.out.println(input.getFrameLength());
                System.out.println("read "+input.read(data)+" bytes from "+wavPath);
                //open audio file
                audioClip=AudioSystem.getClip();
                audioClip.open(format,data,0,data.length);
                audioClip.setFramePosition((int) frame);
                //release resource
                input.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            System.out.println("Error reading audio file from "+wavPath);
        }
    }


    void setPlayed(String command){

        switch (command) {
            case "PLAY":
                audioClip.start();
                break;
            case "PAUSE":
                audioClip.stop();
                break;
            default:
                audioClip.stop();
                audioClip.setFramePosition(Integer.parseInt(command.split(" ")[1]));
                break;
        }

    }

    File             wavFile;
    Clip             audioClip;
    AudioFormat      format;
    AudioInputStream input;
    byte[]           data;
    long             frame;
}