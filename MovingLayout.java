package capstone.project.qrnav;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Typeface;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;

/**
 * Created by lhakpalama on 3/31/16.
 */
public class MovingLayout extends ImageView{

    int[] myX1 =new int[100];
    int[] myY1 =new int[100];
    int[] myX2 =new int[100];
    int[] myY2 =new int[100];
    int[] aX1 =new int[100];
    int[] aY1 =new int[100];
    int[] aX2 =new int[100];
    int[] aY2 =new int[100];
    /*private ArrayList<Float> First = new ArrayList<>();
    private ArrayList<Float> Second = new ArrayList<>();*/
    public static Canvas mycanvas;
    Paint WhiteFill,redStroke,yellowStroke,blinkfill,redline,cline,Tpaint,GreenFill;
    Bitmap UserIcon;
    public static int DestFlag=0;
    int objectt,incre_decre,increment, boundary,setFlag=0;
    float x1dir,y1dir,x2dir,y2dir;


    public MovingLayout(Context context) {
        super(context);
        //MainActivity.fAttacher=new PhotoViewAttacher(this);
       // setBackground(R.drawable.MainActivity.floors.get(MainActivity.curMap));
        setImageBitmap(MainActivity.floors.get(MainActivity.curMap));       // setImageBitmap();
        //setBackgroundResource(R.drawable.floor_1c);
      //  fAttacher.setOnPhotoTapListener(new PhotoTapListener());
        objectt=4;
        incre_decre=(1);
        increment=1;
        findViewById(R.id.map);

        WhiteFill=new Paint();
        WhiteFill.setColor(Color.WHITE);
        WhiteFill.setStyle(Paint.Style.FILL);

        GreenFill=new Paint();
        GreenFill.setColor(Color.rgb(46,139,87));
        GreenFill.setStyle(Paint.Style.STROKE);
        GreenFill.setStrokeWidth(10);

        yellowStroke=new Paint();
        yellowStroke.setColor(Color.YELLOW);
        yellowStroke.setStyle(Paint.Style.STROKE);
        yellowStroke.setStrokeWidth(5);

        redStroke=new Paint();
        redStroke.setColor(Color.RED);
        redStroke.setStyle(Paint.Style.STROKE);
        redStroke.setStrokeWidth(10);

        Tpaint=new Paint();
        //Tpaint.setColor(Color.RED);
        //Paint paint = new Paint();
        Tpaint.setColor(Color.RED);
        Tpaint.setTextSize(30);
        Tpaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        redline=new Paint();
        redline.setStrokeWidth(15);
        redline.setColor(Color.RED);
        redline.setStyle(Paint.Style.FILL);
       // redline.setStrokeWidth(20);

        cline=new Paint();
        cline.setColor(Color.CYAN);
        cline.setStrokeWidth(15);
        cline.setStyle(Paint.Style.FILL);

        blinkfill=new Paint();
        blinkfill.setColor(Color.CYAN);
        blinkfill.setStyle(Paint.Style.FILL);

        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas = new Canvas(MainActivity.floors.get(MainActivity.curMap));
        super.onDraw(canvas);

        canvas = new Canvas(MainActivity.floors.get(MainActivity.curMap));
        //Canvas canvas1=new Canvas(MainActivity.floors.get(MainActivity.curMap));

        if (objectt==4){
            boundary=0;}
        if(objectt==25)
        {boundary=1;}

        if((increment>2) && (increment%2==0)){
            canvas.drawCircle(MainActivity.xCoords.get(MainActivity.lastScan), MainActivity.yCoords.get(MainActivity.lastScan), objectt-3, yellowStroke);
            canvas.drawCircle(MainActivity.xCoords.get(MainActivity.lastScan), MainActivity.yCoords.get(MainActivity.lastScan), 24, WhiteFill);
        }
        if(increment%4==0){
            canvas.drawCircle(MainActivity.xCoords.get(MainActivity.lastScan), MainActivity.yCoords.get(MainActivity.lastScan), 24, blinkfill);
        }
        canvas.drawCircle(MainActivity.xCoords.get(MainActivity.lastScan), MainActivity.yCoords.get(MainActivity.lastScan), objectt, redStroke);
        canvas.drawText("You Are Here",(MainActivity.xCoords.get(MainActivity.lastScan)-100), (MainActivity.yCoords.get(MainActivity.lastScan)-40),Tpaint);
       // mycanvas.drawCircle(MainActivity.xCoords.get(MainActivity.lastScan) - 135, MainActivity.yCoords.get(MainActivity.lastScan) - 35,objectt,MainActivity.paint);
        //MyLocation();

        //-----dESITNATION-------------
        if(DestFlag==1){
            if((increment>2) && (increment%2==0)){
                canvas.drawCircle(MainActivity.xCoords.get(MainActivity.tappedCode), MainActivity.yCoords.get(MainActivity.tappedCode), objectt-3, yellowStroke);
                canvas.drawCircle(MainActivity.xCoords.get(MainActivity.tappedCode), MainActivity.yCoords.get(MainActivity.tappedCode), 24, WhiteFill);
            }
            if(increment%4==0){
                canvas.drawCircle(MainActivity.xCoords.get(MainActivity.tappedCode), MainActivity.yCoords.get(MainActivity.tappedCode), 24, blinkfill);
            }
            canvas.drawCircle(MainActivity.xCoords.get(MainActivity.tappedCode), MainActivity.yCoords.get(MainActivity.tappedCode), objectt, GreenFill);

            int source = MainActivity.NodeIdx.get(MainActivity.lastScan);
            int destination = MainActivity.NodeIdx.get(MainActivity.tappedCode);
            int floor = MainActivity.FloorIdx.get(MainActivity.lastScan) - 1;
            MainActivity.floorGraph.get(floor).execute(MainActivity.nodeArray.get(floor).get(source));
            ArrayList<Integer> path = MainActivity.floorGraph.get(floor).getPathIndices(MainActivity.nodeArray.get(floor).get(destination));
            for(int index = 1; index < path.size(); index++){
                int prevIdx = path.get(index - 1);
                int curIdx = path.get(index);
                if(setFlag==0) {
                    x1dir = (float) MainActivity.nodePoints.get(floor).get(prevIdx).x;
                    y1dir = (float) MainActivity.nodePoints.get(floor).get(prevIdx).y;
                    x2dir = (float) MainActivity.nodePoints.get(floor).get(curIdx).x;
                    y2dir = (float) MainActivity.nodePoints.get(floor).get(curIdx).y;

                    myX1[index]=(int)x1dir;     myY1[index]=(int)y1dir;
                    myX2[index]=(int)x2dir;     myY2[index]=(int)y2dir;

                    aX1[index]=myX1[index];
                    aY1[index]=myY1[index];
                    aX2[index]=myX2[index];
                    aY2[index]=myY2[index];
                    if(index==(path.size()-1)) {
                        setFlag = 1;
                    }
                }
                for(int in = 1; in < path.size(); in++) {
                    if ((increment % 3) == 0) {
                        //canvas.drawLine(myX1[in], myY1[in], myX2[in], myY2[in],redline );

                        if((myX1[in]==myX2[in])&&(myY1[in]<myY2[in])){
                            myY1[in]=myY1[in]+1;
                            if(myY1[in]==myY2[in]){
                                myY1[in]=aY1[in];
                            }
                           if((increment%2)==0) {
                                canvas.drawCircle(myX1[in], myY1[in],5,yellowStroke);
                                canvas.drawCircle(myX1[in], myY1[in] - 10, 5, redStroke);
                                canvas.drawCircle(myX1[in], myY1[in]-20,5, blinkfill);
                            }

                        }
                        if((myX1[in]==myX2[in])&&(myY1[in]>myY2[in])){
                            myY1[in]=myY1[in]-1;
                            if(myY1[in]==myY2[in]){
                                myY1[in]=aY1[in];
                            }
                            if((increment%2)==0) {
                                canvas.drawCircle(myX1[in], myY1[in],5,yellowStroke);
                                canvas.drawCircle(myX1[in], myY1[in] - 10, 5, redStroke);
                                canvas.drawCircle(myX1[in], myY1[in]-20,5, blinkfill);
                            }
                        }
                        if((myY1[in]==myY2[in])&&(myX1[in])<myX2[in]){
                            myX1[in]=myX1[in]+1;
                            if(myX1[in]==myX2[in]){
                                myX1[in]=aX1[in];
                            }
                            if((increment%2)==0) {
                                canvas.drawCircle(myX1[in], myY1[in],5,yellowStroke);
                                canvas.drawCircle(myX1[in]-10, myY1[in], 5, redStroke);
                                canvas.drawCircle(myX1[in]-20, myY1[in],5, blinkfill);
                            }
                        }
                        if((myY1[in]==myY2[in])&&(myX1[in]>myX2[in])){
                            myX1[in]=myX1[in]-1;
                            if(myX1[in]==myX2[in]){
                                myX1[in]=aX1[in];
                            }
                            if((increment%2)==0) {
                                canvas.drawCircle(myX1[in], myY1[in],5,yellowStroke);
                                canvas.drawCircle(myX1[in]-10, myY1[in], 5, redStroke);
                                canvas.drawCircle(myX1[in]-20, myY1[in],5, blinkfill);
                            }
                        }
                        //======diagonal tranverse===left top to right bottom
                        if((myX1[in]<myX2[in])&&(myY1[in]<myY2[in])) {
                            myX1[in] = myX1[in] + 1;
                            myY1[in] = myY1[in] + 1;
                            if ((myX1[in] == myX2[in])||(myY1[in] == myY2[in])) {
                                myX1[in] = aX1[in];myY1[in] = aY1[in];
                            }

                        }
                         //======diagonal traverse==left bottom to right top
                            if((myX1[in]<myX2[in])&&(myY1[in]>myY2[in])) {
                                myX1[in] = myX1[in] + 1;
                                myY1[in] = myY1[in] - 1;
                                if ((myX1[in] == myX2[in])||(myY1[in] == myY2[in])) {
                                    myX1[in] = aX1[in];myY1[in] = aY1[in];
                                }

                            }
                        //=====diagonal traverse=== right top to left bottom
                                if((myX1[in]>myX2[in])&&(myY1[in]<myY2[in])) {
                                    myX1[in] = myX1[in] - 1;
                                    myY1[in] = myY1[in] + 1;
                                    if ((myX1[in] == myX2[in])||(myY1[in] == myY2[in])) {
                                        myX1[in] = aX1[in];myY1[in] = aY1[in];
                                    }

                                }
                     //====================diagonal traverse ==== right bottom to left top
                                    if((myX1[in]>myX2[in])&&(myY1[in]>myY2[in])) {
                                        myX1[in] = myX1[in] - 1;
                                        myY1[in] = myY1[in] - 1;
                                        if ((myX1[in] == myX2[in])||(myY1[in] == myY2[in])) {
                                            myX1[in] = aX1[in];myY1[in] = aY1[in];
                                        }

                                    }
                       //=================
                      /* if((increment%4)==0) {
                           // canvas.drawCircle(myX1[in], myY1[in],10,yellowStroke);
                           // canvas.drawCircle(myX1[in] - 10, myY1[in] - 10, 10, redStroke);
                           // canvas.drawCircle(myX1[in]-20, myY1[in]-20,10, blinkfill);
                            canvas.drawRect(myX1[in], myY1[in], myX1[in] + 4, myY1[in] + 4, redStroke);
                           // canvas.drawRect(myX1[in]-10, myY1[in]-10, myX1[in] -6, myY1[in] - 6, redStroke);
                        }*/

                        //canvas.drawCircle(myX1[in],myY1[in],1,redStroke);
                    }
                    canvas.drawLine(myX1[in], myY1[in], myX2[in], myY2[in], redStroke);
                    if((increment%4)==0){
                        canvas.drawLine(myX1[in], myY1[in], myX2[in], myY2[in], cline);
                    }
                    // if ((increment % 4) == 0) {
                  /*  if((increment%6)==0) {
                        if ((in % 2) == 0) {
                            canvas.drawLine(myX1[in], myY1[in], myX2[in], myY2[in], redStroke);
                        } else {
                            canvas.drawLine(myX1[in], myY1[in], myX2[in], myY2[in], cline);
                        }
                    }
                    if((increment%8)==0) {
                        if ((in % 2) == 0) {
                            canvas.drawLine(myX1[in], myY1[in], myX2[in], myY2[in], cline);
                        } else {
                            canvas.drawLine(myX1[in], myY1[in], myX2[in], myY2[in], redStroke);
                        }
                    }*/

                    //}
                }


                    fillArrow(canvas, (float) MainActivity.nodePoints.get(floor).get(prevIdx).x, (float) MainActivity.nodePoints.get(floor).get(prevIdx).y, (float) MainActivity.nodePoints.get(floor).get(curIdx).x, (float) MainActivity.nodePoints.get(floor).get(curIdx).y);
            }
        }
        //=======================
        if(increment%2==0) {
            if (boundary == 0) {

                objectt = objectt + incre_decre;
            } else {
                objectt = objectt - incre_decre;
            }
        }

        increment=increment+1;
       //MainActivity.fAttacher=new PhotoViewAttacher(this);
        //MainActivity.fAttacher.update();

        invalidate();


    }
    private void fillArrow(Canvas canvas, float x0, float y0, float x1, float y1) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);

        float deltaX = x1 - x0;
        float deltaY = y1 - y0;
        double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        float frac = (float) (1 / (distance / 20));

        float point_x_1 = x0 + (float) ((1 - frac) * deltaX + frac * deltaY);
        float point_y_1 = y0 + (float) ((1 - frac) * deltaY - frac * deltaX);

        float point_x_2 = x1;
        float point_y_2 = y1;

        float point_x_3 = x0 + (float) ((1 - frac) * deltaX - frac * deltaY);
        float point_y_3 = y0 + (float) ((1 - frac) * deltaY + frac * deltaX);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);

        path.moveTo(point_x_1, point_y_1);
        path.lineTo(point_x_2, point_y_2);
        path.lineTo(point_x_3, point_y_3);
        path.lineTo(point_x_1, point_y_1);
        path.lineTo(point_x_1, point_y_1);
        path.close();

        canvas.drawPath(path, paint);
    }
}
