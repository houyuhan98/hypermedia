import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

class Rect{
     Rect(){
        a=new Point();
        b=new Point();
    }

    Rect(Point a,Point b){
         this.a=a;
         this.b=b;
    }


    public static Rect valueOf(String recordFormat){
         Rect r=new Rect();
         String[] data=recordFormat.split(" ");
         //example:
         //link priFrame: 1 with Rect(303,182,179,139) to secFrame: 1
         String rectStr=data[4];
         rectStr=rectStr.replaceAll("\\D+"," ").replaceFirst(" ","");
         String[] positions=rectStr.split("\\s+");
         r.a.x=Integer.parseInt(positions[0]);
         r.a.y=Integer.parseInt(positions[1]);
         r.b.x=Integer.parseInt(positions[2]);
         r.b.y=Integer.parseInt(positions[3]);
         return r;
    }
    Point a;
    Point b;
}


public class VideoAuthoringTool extends JFrame
{
    VideoAuthoringTool(ArrayList<String> filePaths){
        menuItems=new ArrayList<>();
        records=new ArrayList<>();
        this.filePaths=filePaths;
        dr=new DrawArea(10,20);
        dr2=new DrawArea(750,20);
        setTitle("VideoAuthoringTool");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500,1000);

        setResizable(false);
        setLayout(null);

        //set menu
        setMenu();

        //first and secondary window
        setWindows();

        //scroll bars
        setSliders();

        setVisible(true);
    }
    static String              resourcesPath;
    public static void main(String[] args) {
        ArrayList<String> filePaths=new ArrayList<>();
        //FilePathIn
        System.out.println("Type in sourcePath");
        while(true){
            String sourcePathIn=new Scanner(System.in).next();
            if(!sourcePathIn.equals("")){
                File detect=new File(sourcePathIn);
                if(detect.exists()&&detect.isDirectory()){
                    VideoAuthoringTool.resourcesPath=sourcePathIn;
                    System.out.println("Set SourcePath: to "+VideoAuthoringTool.resourcesPath);
                    break;
                }
            }
            System.out.println("Invalid sourcePath");

        }

        //start VideoAuthoringTool
        File directory=new File(resourcesPath);
        File[] files=directory.listFiles();
        if(files!=null){
            System.out.println("num of SourceFiles in this folder: "+files.length);
            for(File f:files){
                if(!f.getName().contains("txt")){
                    System.out.println(f.getAbsolutePath());
                    filePaths.add(f.getAbsolutePath());
                }
            }
        }
        new VideoAuthoringTool(filePaths);
    }


    public void setMenu(){
        //MenuBar
        MenuBar mb=new MenuBar();
        mb.setFont(new Font("Arial",Font.PLAIN,21));

        //import files options
        Menu file=new Menu("File");
        Menu imPort=new Menu("Import Files");   //import files

        Menu pri=new Menu("Primary File");
        for(String filePath:filePaths){
            pri.add(new MenuItem(filePath));
        }

        Menu sec=new Menu("Secondary File");
        for(String filePath:filePaths){
            sec.add(new MenuItem(filePath));
        }
        //add new primary files
        pri.addActionListener(l->{
            dr.currRect=null;
            records.clear();
            menuItems.clear();
            select.removeAll();
            select.add(new MenuItem("Rename Tags"));
            priFramePath=l.getActionCommand();
            priFrameNum=1;
            priSlider.setValue(priFrameNum);
            dr.data=new ImageRGB(priFramePath+ImagePathGenerator.generate(priFramePath,priFrameNum)).data;
            dr.repaint();
        });
        //add new secondary files
        sec.addActionListener(l->{
            secFramePath=l.getActionCommand();
            secFrameNum=1;
            secSlider.setValue(secFrameNum);
            dr2.data=new ImageRGB(secFramePath+ImagePathGenerator.generate(secFramePath,secFrameNum)).data;
            dr2.repaint();
        });


        MenuItem save=new MenuItem("Save File"); //save files

        save.addActionListener(l->{

            //do nothing when no files passed in
            if(priFramePath==null||secFramePath==null){
                System.out.println("You haven't imported the files properly");
                return;
            }

            String priName=ImagePathGenerator.getFileName(priFramePath);
            String secName=ImagePathGenerator.getFileName(secFramePath);

            String savedFilName=priName+"_"+secName+".txt";
            File savedFile=new File(resourcesPath+"\\"+savedFilName);

            if(!savedFile.exists()){
                try {
                    if(savedFile.createNewFile()){
                        System.out.println("generated new file");
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            System.out.println("Save files successfully");
            try {
                BufferedWriter out=new BufferedWriter(new FileWriter(savedFile));
                //write all record:String into localFile from records:Arraylist<String>
                for(String record:records){
                    out.write(record+"\n");
                }
                out.close();
            }catch (IOException e){
                System.out.println("Error writing data");
            }
        });
        imPort.add(pri);
        imPort.add(sec);
        file.add(imPort);
        file.add(save);
        mb.add(file);


        select=new Menu("Select");
        select.add(new MenuItem("Rename Tags"));
        select.addActionListener(l->{
            if(l.getActionCommand().equals("Rename Tags")){
                //pop up rename window
                new Thread(()->{
                    new RenameFrame(menuItems);
                }).start();

            }else{
                //set pri and sec Frame
                int index=Integer.parseInt(l.getActionCommand().split(" ")[0]);
                String command=records.get(index);
                String[] commands=command.split(" ");
                priFrameNum=Integer.parseInt(commands[2]);
                secFrameNum=Integer.parseInt(commands[7]);
                priSlider.setValue(priFrameNum);
                secSlider.setValue(secFrameNum);
                dr.currRect=Rect.valueOf(command);
                dr.repaint();

                //set Select MenuItem
                for(MenuItem m:menuItems){
                    String label=m.getLabel();
                    m.setLabel(label.replaceFirst("[*]",""));
                }
                menuItems.get(index).setLabel(menuItems.get(index).getLabel()+"*");

            }
        });


        mb.add(select);

        //connect frames
        Menu connect=new Menu("Connect");
        MenuItem connectOption=new MenuItem("Connect");
        connect.add(connectOption);

        connectOption.addActionListener(l->{
            //do nothing when no files passed in
            if(priFramePath==null||secFramePath==null){
                System.out.println("You haven't imported the files properly");
                return;
            }
            if(dr.currRect!=null){
                MenuItem newItem=new MenuItem(records.size()+"");

                menuItems.add(newItem);
                select.add(newItem);
                records.add(VideoAuthoringTool.setRecord(priFrameNum,dr.currRect,secFrameNum,secFramePath));
                dr.currRect=null;
                System.out.println("create a new record");
            }
        });
        mb.add(connect);

        //set MenuBar
        setMenuBar(mb);
    }

    public void setWindows(){
        try {
            Rect currRect=new Rect();
            this.add(dr);
            dr.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {
                    currRect.a.x=e.getX();
                    currRect.a.y=e.getY();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    currRect.b.x=e.getX();
                    currRect.b.y=e.getY();
                    if(priFramePath==null||secFramePath==null) {
                        return ;
                    }
                    dr.currRect=currRect;
                    dr.repaint();
                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }
            });

            this.add(dr2);
        }catch (Exception e){
            System.out.println("Error Reading File");
        }
    }

    public void setSliders(){
        priSlider=new JSlider(1,9000);
        priSlider.setValue(priFrameNum);
        JLabel priLabel=new JLabel("1/9000");
        priLabel.setFont(new Font("Arial",Font.PLAIN,18));

        secSlider=new JSlider(1,9000);
        secSlider.setValue(secFrameNum);
        JLabel secLabel=new JLabel("1/9000");
        secLabel.setFont(new Font("Arial",Font.PLAIN,18));

        //each box contains a combination of one slider and one label
        Box priBox=new Box(BoxLayout.X_AXIS);
        Box secBox=new Box(BoxLayout.X_AXIS);

        priBox.add(priLabel);
        priBox.add(priSlider);

        secBox.add(secLabel);
        secBox.add(secSlider);

        //add boxes to the frame
        priBox.setBounds(0,700,650,40);
        add(priBox);
        secBox.setBounds(800,700,650,40);
        add(secBox);

        //add Listener to these panels
        priSlider.addChangeListener(e-> {
            int frameNum=((JSlider) e.getSource()).getValue();
            priLabel.setText(frameNum + "/9000");
            priFrameNum=frameNum;
            dr.currRect=null;
            if(priFramePath!=null&&!priFramePath.equals("")){
                dr.data=new ImageRGB(priFramePath+ImagePathGenerator.generate(priFramePath,priFrameNum)).data;
                dr.repaint();
            }

        });

        secSlider.addChangeListener(e-> {
            int frameNum=((JSlider) e.getSource()).getValue();
            secLabel.setText(((JSlider) e.getSource()).getValue() + "/9000");
            secFrameNum=frameNum;
            if(secFramePath!=null&&!secFramePath.equals("")){
                dr2.data=new ImageRGB(secFramePath+ImagePathGenerator.generate(secFramePath,secFrameNum)).data;
                dr2.repaint();
            }
        });

    }

    /**
     * format: link priFrame:12 with Rect(x,y,width,height) to secFrame:25
     * @see Rect
     * @param priFrameNum,rect,secFriNum
     * @return record:String that can be saved in local file
     * */
    public static String setRecord(int priFrameNum,Rect rect,int secFrameNum,String secFramePath){
        String record=VideoAuthoringTool.linkFormat;

        //set priFrameNum
        record=record.replaceFirst("#",String.valueOf(priFrameNum));

        //set rectangle
        record=record.replaceFirst("#",String.valueOf(rect.a.x));
        record=record.replaceFirst("#",String.valueOf(rect.a.y));
        record=record.replaceFirst("#",String.valueOf(rect.b.x));
        record=record.replaceFirst("#",String.valueOf(rect.b.y));

        //set secFrameNum
        record=record.replaceFirst("#",String.valueOf(secFrameNum));

        //setTargetFile's Path
        record+=secFramePath;
        return record;
    }

    int priFrameNum=1;
    int secFrameNum=1;

    String priFramePath;
    String secFramePath;

    //add fileDirectories to it
    ArrayList<String> filePaths;

    DrawArea dr;
    DrawArea dr2;
    JSlider  priSlider;
    JSlider  secSlider;
    Menu     select;
    //to save connection records
    //exp: link priFrame:12 with Rect(x1,y1,x2,y2) to secFrame:25
    ArrayList<String>   records;
    ArrayList<MenuItem> menuItems;

    public final static String linkFormat="link priFrame: # with Rect(#,#,#,#) to secFrame: # ";
}


class DrawArea extends Panel{
    DrawArea(int x,int y){
        setBounds(x,y,718,650);
        this.x=x;
        this.y=y;
    }
    public void paint(Graphics g){
        if(data!=null){
            g.drawImage(data.getScaledInstance(718,650,BufferedImage.SCALE_SMOOTH),0,0,null);
        }

        //System.out.println(currSelectFrame.rectangles.size());
        if(currRect!=null){
            int x1=currRect.a.x;
            int y1=currRect.a.y;
            int x2=currRect.b.x;
            int y2=currRect.b.y;
            g.setColor(new Color(243, 11, 11));
            ((Graphics2D)g).setStroke(new BasicStroke(3.0f));
            g.drawRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1-x2), Math.abs(y1-y2));
        }

    }

    BufferedImage data;
    int x;
    int y;
    Rect currRect;
}


class RenameFrame extends JFrame{
    RenameFrame(ArrayList<MenuItem> items){
        setSize(420,150);
        setLayout(null);

        JTextField text=new JTextField("type in [index] [newName] to rename tag");
        text.setBounds(0,0,300,100);
        JButton confirm=new JButton("confirm");
        confirm.setBounds(300,0,100,100);
        confirm.addActionListener(l->{
            int index=0;
            if(text.getText().matches("\\d+ [a-zA-Z_]*")){
                index=Integer.parseInt(text.getText().split(" ")[0]);
            }
            if(items.size()>index){
                items.get(index).setLabel(index+" "+text.getText().split(" ")[1]);
                text.setText("Revise successfully");
            }
        });
        add(confirm);
        add(text);
        setVisible(true);
    }
}



