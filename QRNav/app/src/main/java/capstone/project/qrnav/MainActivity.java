package capstone.project.qrnav;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    // Initialize global variables
    private static final String TAG = "QRNavigation";
    public static final int ZXING_REQUEST_CODE = 0x0000c0de;
    public static final int SPEECH_REQUEST_CODE = 123;
    public static final float mapScaleMeters = 0.05335f;
    public static final float mapScaleFeet = 0.175f;

    private ArrayList<Path> route = new ArrayList<>();
    private ArrayList<Integer> distances = new ArrayList<>();
    private ArrayList<PointF> routePoints = new ArrayList<>();
    private Toolbar mainToolbar;
    private ImageButton mapButtonNext, mapButtonPrev, speechButton, scanButton, directionNext, directionPrev;
    private TextView locationTxt, destTxt, directionText;
    private TextToSpeech tts;
    public AlertDialog.Builder destinationsMenu, startPointMenu;
    private Toast toastError, toastSuccess;
    private ImageView floorMap;
    private PhotoViewAttacher fAttacher;
    private int[] imgArray;
    private ArrayList<Bitmap> floors = new ArrayList<>();
    private ArrayList<String> qrCodes = new ArrayList<>();
    private ArrayList<Integer> FloorIdx = new ArrayList<>();
    private ArrayList<Integer> xCoords = new ArrayList<>();
    private ArrayList<Integer> yCoords = new ArrayList<>();
    private ArrayList<String> Locations = new ArrayList<>();
    private ArrayList<Integer> NodeIdx = new ArrayList<>();
    private ArrayList<DijkstraAlgorithm> floorGraph = new ArrayList<>();
    private ArrayList<ArrayList<Vertex>> nodeArray = new ArrayList<>();
    private ArrayList<ArrayList<Point>> nodePoints = new ArrayList<>();
    private ArrayList<String> listDirections = new ArrayList<>();
    private int lastScan = -1;
    private int curMap = 0;
    private int curDirection = 0;
    private Vibrator vibrate;
    private boolean isVibrationOn = true;
    private int tappedCode = -1;
    private int tappedCodePrev = -2;
    private boolean samePoint = false;
    private boolean isMatch = false;
    private boolean isMetric = false;
    private boolean isSpeechOn = true;
    private boolean curPath = false;
    private boolean sayDest = false;
    private int x_tap,y_tap;
    private Matrix prevZoom;

    /*
    onCreate - This function is called at the program start. Some of our global variables and our UI elements are initialized here.
    R.id.<item> is used to specify that we want to edit (from our resource files)
    We also use this function to load our floorplans as bitmaps, as well as add the qr code values to an arraylist.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI Elements
        mainToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);
        getSupportActionBar().setTitle(R.string.default_floor);
        vibrate = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        scanButton = (ImageButton)findViewById(R.id.scan_button);
        mapButtonNext = (ImageButton)findViewById(R.id.map_button_next);
        mapButtonPrev = (ImageButton)findViewById(R.id.map_button_prev);
        speechButton = (ImageButton)findViewById(R.id.speech_button);
        directionPrev = (ImageButton)findViewById(R.id.direction_button_prev);
        directionNext = (ImageButton)findViewById(R.id.direction_button_next);
        //floorTxt = (TextView)findViewById(R.id.current_floor);
        locationTxt = (TextView)findViewById(R.id.source_text);
        destTxt = (TextView)findViewById(R.id.destination_text);
        directionText = (TextView)findViewById(R.id.direction_text);
        floorMap = (ImageView)findViewById(R.id.map);
        fAttacher = new PhotoViewAttacher(floorMap);


        // Declare error and success Toasts we use to display whether or not the scan was successful.
        toastError = Toast.makeText(getApplicationContext(), "QR code was unsuccessfully scanned", Toast.LENGTH_SHORT);
        toastSuccess = Toast.makeText(getApplicationContext(), "QR code was successfully scanned!", Toast.LENGTH_SHORT);

        // Get an array of IDs that correspond to each floorplan bitmap in the drawable folder (the name of the drawable is the filename w/o the extension)
        imgArray = new int[] {R.drawable.floor_1c, R.drawable.floor_2b, R.drawable.floor_3b, R.drawable.floor_4c,
                R.drawable.floor_5a, R.drawable.floor_6a, R.drawable.floor_7c, R.drawable.floor_8c};

        // Load all of the checkpoint (QR code) data from the checkpoints.txt file in the assets folder.
        loadCheckpoints();

        // Set our custom ImageViewTouch element to display the current map element and mark the maps.
        loadNodeCoords();

        markMaps();

        loadGraphs();

        // Set a listener to both the scan and map buttons. When a button is clicked, it will call the onClick function with the view corresponding to that button.
        scanButton.setOnClickListener(this);
        mapButtonNext.setOnClickListener(this);
        mapButtonPrev.setOnClickListener(this);
        speechButton.setOnClickListener(this);
        directionNext.setOnClickListener(this);
        directionPrev.setOnClickListener(this);
        fAttacher.setOnPhotoTapListener(new PhotoTapListener());

        tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result = tts.setLanguage(Locale.US);
                    if(result==TextToSpeech.LANG_MISSING_DATA || result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e(TAG, "Error: Language is not supported");
                    }
                }
                else
                    Log.e(TAG, "TextToSpeech Initialization failed!");
            }
        });

        CharSequence selectLocations[] = Locations.toArray(new CharSequence[Locations.size()]);
        destinationsMenu = new AlertDialog.Builder(this);
        destinationsMenu.setTitle("Choose a Destination");
        destinationsMenu.setItems(selectLocations, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String location = Locations.get(which);
                destTxt.setText("Destination: " + location);
                if (isVibrationOn)
                    vibrate.vibrate(100);
                tappedCode = which;
                Toast toast = Toast.makeText(MainActivity.this, "You have selected the " + location, Toast.LENGTH_SHORT);
                if(lastScan < 0)
                    speakDirections(2);
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
                markMaps();
            }
        });
        startPointMenu = new AlertDialog.Builder(this);
        startPointMenu.setTitle("Choose a Starting Point");
        startPointMenu.setItems(selectLocations, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String location = Locations.get(which);
                locationTxt.setText("Last scanned location: " + location);
                if(isVibrationOn)
                    vibrate.vibrate(250);
                lastScan = which;
                Toast toast = Toast.makeText(MainActivity.this, "Start location set to " + location, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL,0,0);
                toast.show();
                markMaps();
            }
        });
    }

    private void resetAll(){
        lastScan = -1;
        curMap = 0;
        tappedCode = -1;
        tappedCodePrev = -2;
        listDirections.clear();
        distances.clear();
        route.clear();
        isVibrationOn = true;
        samePoint = false;
        isMatch = false;
        isMetric = false;
        isSpeechOn = true;
        invalidateOptionsMenu();
        //floorTxt.setText("NAC Building Floor 1");
        getSupportActionBar().setTitle(R.string.default_floor);
        locationTxt.setText(R.string.default_source);
        destTxt.setText(R.string.default_destination);
        directionText.setText(R.string.default_direction);
        markMaps();
    }

    private void showNodes(int floorIdx, Canvas canvas, Paint paint){
        int prevColor = paint.getColor();
        paint.setColor(Color.rgb(255,69,0));
        paint.setTextSize(20f);
        for(int i = 0; i < nodePoints.get(floorIdx).size(); i++){
            canvas.drawCircle(nodePoints.get(floorIdx).get(i).x,nodePoints.get(floorIdx).get(i).y,5,paint);
            canvas.drawText(String.valueOf(i),nodePoints.get(floorIdx).get(i).x,nodePoints.get(floorIdx).get(i).y - 10,paint);
        }
        paint.setColor(prevColor);
    }

    /*
    loadFloors() - This function  loads all of the floorplan images (which is in res/drawable-nodpi folder) in the floors arraylist.
    It is used to both initialize the bitmap arraylist with all of the images as well as to reset the bitmaps of any modifications
    (such as marking the matched points on the map)
     */
    private void loadFloors(){
        // Create a bitmap array to hold each floorplan image.
        floors.clear();
        prevZoom = fAttacher.getDisplayMatrix();
        for(int i = 0; i < imgArray.length; i++){
            floors.add(BitmapFactory.decodeResource(getResources(),imgArray[i]).copy(Bitmap.Config.ARGB_8888, true));
            Canvas canvas = new Canvas(floors.get(i));
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setTextSize(50);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            int width = floors.get(i).getWidth();
            int height = floors.get(i).getHeight();

            canvas.drawText("Destination", width - 300, height - 35, paint);
            canvas.drawCircle(width - 325, height - 50, 20, paint);
            paint.setColor(Color.RED);
            canvas.drawText("Start", width - 300, height - 85, paint);
            canvas.drawCircle(width - 325, height - 100, 20, paint);
            paint.setColor(Color.BLUE);
            canvas.drawText("QR Code", width - 300, height - 135, paint);
            canvas.drawCircle(width - 325, height - 150, 20, paint);
            //showNodes(i,canvas,paint);
        }
        floorMap.setImageBitmap(floors.get(curMap));
        fAttacher.update();
    }

    /*
    loadCheckpointss() - This function will parse the text file for all of the QR code entries and put the data
    for each code into its corresponding arraylist.
    qrCodes - Holds the qr code string (the data the qr code contains) for every QR code registered.
    FloorIdx - Contains the floor each QR code is located in.
    xCoords - Contains the x-coordinate of the QR code's location
    yCoords - Contains the y-coordinate of the QR code's location
    Locations - Contains the name of key location nearest to the QR code (i.e. NAC Ballroom)
     */
    private void loadCheckpoints(){
        try {
            // The text file is loaded into a BufferedReader, which is used to process each line
            // of the text file and extract the data as a series of strings (one per line)
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("checkpoints.txt")));
            String str;
            // While there is still a valid unread string in the file, we will continue to parse it
            while((str = br.readLine()) != null){
                // First we split the string (which contains all the data separated by whitespaces)
                // into a string array where each element is one of data elements.
                String[] temp_strs = str.split(" ");
                // Then we add each element into its corresponding arraylist
                // qrCodes and Locations are strings, so we can just add them
                // However, the other lists are integer lists, so we use Integer.parseInt() to
                // convert the strings into an integer.
                qrCodes.add(temp_strs[0]);
                FloorIdx.add(Integer.parseInt(temp_strs[1]));
                xCoords.add(Integer.parseInt(temp_strs[2]));
                yCoords.add(Integer.parseInt(temp_strs[3]));
                Locations.add(temp_strs[4].replaceAll("_"," "));
                NodeIdx.add(Integer.parseInt(temp_strs[5]));
            }
            br.close();
            Log.i(TAG, "Finished parsing checkpoints.txt");
        }
        // This catch statement is executed in the case where checkpoints.txt cannot be opened
        catch(IOException e){
            e.printStackTrace();
            Log.e(TAG, "Error opening checkpoints.txt");
        }
    }

    private void loadNodeCoords(){
        for(int floor = 1; floor < 9; floor++) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("floor_" + floor + "_nodelist.txt")));
                String str;
                ArrayList<Point> temp_arraylist = new ArrayList<>();
                while ((str = br.readLine()) != null) {
                    String[] temp_strs = str.split(" ");
                    temp_arraylist.add(new Point(Integer.parseInt(temp_strs[1]), Integer.parseInt(temp_strs[2])));
                }
                nodePoints.add(temp_arraylist);
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error loading node list for floor " + floor);
            }
        }
        Log.i(TAG, "Finished loading node list for all floors!");
    }

    private void loadGraphs(){
        for(int floor = 1; floor < 9; floor++) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("floor_" + floor + "_graph.txt")));
                String str;
                int count = 0;
                ArrayList<Vertex> nodes = new ArrayList<>();
                ArrayList<Edge> edges = new ArrayList<>();
                for (int i = 0; i < nodePoints.get(floor - 1).size(); i++) {
                    Vertex location = new Vertex(Integer.toString(i), "Node_" + i);
                    nodes.add(location);
                }
                while ((str = br.readLine()) != null) {
                    String[] temp_strs = str.split(" ");
                    //edges.add(new Edge(Integer.parseInt(temp_strs[0]), Integer.parseInt(temp_strs[1]), Integer.parseInt(temp_strs[2])));
                    edges.add(new Edge("Edge_" + count, nodes.get(Integer.parseInt(temp_strs[0])), nodes.get(Integer.parseInt(temp_strs[1])), Integer.parseInt(temp_strs[2])));
                    edges.add(new Edge("Edge_" + count, nodes.get(Integer.parseInt(temp_strs[1])), nodes.get(Integer.parseInt(temp_strs[0])), Integer.parseInt(temp_strs[2])));
                    count++;
                }
                Graph g = new Graph(nodes, edges);
                DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(g);
                floorGraph.add(dijkstra);
                nodeArray.add(nodes);
                /*
                dijkstra.execute(nodes.get(4));
                ArrayList<Vertex> pathto = dijkstra.getPath(nodes.get(17));
                Log.i(TAG, "Path is: " + pathto.toString());
                //dijkstra.execute(nodes.get(5));
                //DijkstraAlgorithm d2 = new DijkstraAlgorithm(g);
                dijkstra.execute(nodes.get(6));
                ArrayList<Vertex> path2 = dijkstra.getPath(nodes.get(19));
                Log.i(TAG, "Path is: " + path2.toString());*/
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error loading graph for floor" + floor);
            }
        }
        Log.i(TAG, "Finished loading all floor graphs!");
    }

    /*
    markMaps() - This function will go through the bitmap floorplans and draw circle at specified locations
    These locations are determined beforehand and will be stored in a text file to make the code neater
    Initially, all the QR code locations are marked using the color blue. However, after a match is made, a red
    color is used to denote the point that was matched.
     */
    private void markMaps(){

        // Reset the map view (default zoom/pan) and reload the floor bitmaps.
        //floorMap.resetMatrix();
        loadFloors();

        // Iterate through all of the registered QR codes and mark them on the map.
        // qrCodes, FloorIdx, xCoords, and yCoords are arraylists loaded from checkpoints.txt
        for(int i = 0; i < qrCodes.size(); i++) {
            int curFloor = FloorIdx.get(i) - 1;
            Canvas canvas = new Canvas(floors.get(curFloor));
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            canvas.drawCircle(xCoords.get(i), yCoords.get(i), 20, paint);

            // If a scan has been performed, lastScan will have the index of the matching QR code
            // Now we will mark the matched QR code on the map
            if(lastScan == i) {
                paint.setColor(Color.RED);
                canvas.drawCircle(xCoords.get(i), yCoords.get(i), 30, paint);
                // Now we set curMap (int used to keep track of the current floor) to be the floor
                // where the matched QR code is located. The displayed map and text is updated accordingly.
                curMap = curFloor;
                floorMap.setImageBitmap(floors.get(curMap));
                fAttacher.update();
                //floorTxt.setText("NAC Building Floor " + (curMap + 1));
                getSupportActionBar().setTitle("NAC Building Floor " + (curMap + 1));
                locationTxt.setText("Last scanned location: " + Locations.get(lastScan));
            }
            if(tappedCode == i && tappedCode != lastScan && tappedCode != tappedCodePrev){
                paint.setColor(Color.GREEN);
                canvas.drawCircle(xCoords.get(i), yCoords.get(i), 20, paint);
                fAttacher.setDisplayMatrix(prevZoom);
                fAttacher.update();
            }
            if(tappedCode == tappedCodePrev) {
                tappedCodePrev = -2;
                samePoint = true;
            }
            if (tappedCode >= 0 && lastScan >= 0 && FloorIdx.get(tappedCode) == FloorIdx.get(lastScan) && i == tappedCode && tappedCode != lastScan) {
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(10);

                int source = NodeIdx.get(lastScan);
                int destination = NodeIdx.get(tappedCode);
                int floor = FloorIdx.get(lastScan) - 1;
                floorGraph.get(floor).execute(nodeArray.get(floor).get(source));
                //ArrayList<Integer> path = floorGraph.get(0).getPathIndices(nodeArray.get(0).get(destination));
                ArrayList<Integer> path = floorGraph.get(floor).getPathIndices(nodeArray.get(floor).get(destination));
                if(path == null) {
                    Log.e(TAG, "Error: no path found");
                    return;
                }
                Log.i(TAG, "Path Found is: " + path.toString());
                listDirections.clear();
                if(!curPath)
                    curDirection = 0;
                drawPath(floor, path,canvas,paint);
                //markCurrentPath();
                directionText.setText(listDirections.get(curDirection));
                speakDirections(1);
                isMatch = false;
            }
        }
    }

    private void drawPath(int floor, ArrayList<Integer> path, Canvas canvas, Paint paint){
        route.clear();
        distances.clear();
        String unit;
        if(isMetric){
            unit = "meters";
        }
        else
            unit = "feet";

        int start_x = nodePoints.get(floor).get(path.get(0)).x;
        int start_y = nodePoints.get(floor).get(path.get(0)).y;
        int end_x,end_y;
        for(int index = 1; index < path.size() - 1; index++){
            int prevIdx = path.get(index - 1);
            int curIdx = path.get(index);
            int nextIdx = path.get(index + 1);

            int prev_x = nodePoints.get(floor).get(prevIdx).x;
            int prev_y =  nodePoints.get(floor).get(prevIdx).y;
            int cur_x = nodePoints.get(floor).get(curIdx).x;
            int cur_y = nodePoints.get(floor).get(curIdx).y;
            int next_x = nodePoints.get(floor).get(nextIdx).x;
            int next_y = nodePoints.get(floor).get(nextIdx).y;

            int slope_1 = getSlope(prev_x, prev_y, cur_x, cur_y);
            int slope_2 = getSlope(cur_x, cur_y, next_x, next_y);

            if(slope_1 != slope_2){
                end_x = cur_x;
                end_y = cur_y;
                routePoints.add(new PointF((float)start_x, (float)start_y));
                Path temppath = new Path();
                temppath.moveTo((float) start_x, (float) start_y);
                temppath.lineTo((float) end_x, (float) end_y);
                route.add(temppath);
                //canvas.drawLine((,(float)end_x,(float)end_y,paint);
                //fillArrow(canvas,(float)start_x,(float)start_y,(float)end_x,(float)end_y);
                listDirections.add("Please walk " + getDIstance(start_x, start_y, end_x, end_y) + " " + unit + " " + getDirection(start_x, start_y, end_x, end_y));
                distances.add(getDIstance(start_x, start_y, end_x, end_y));
                start_x = end_x;
                start_y = end_y;
            }
        }
        end_x = nodePoints.get(floor).get(path.get(path.size() - 1)).x;
        end_y = nodePoints.get(floor).get(path.get(path.size() - 1)).y;
        //canvas.drawLine((float)start_x,(float)start_y,(float)end_x,(float)end_y,paint);
        Path temppath = new Path();
        temppath.moveTo((float)start_x,(float)start_y);
        temppath.lineTo((float) end_x, (float) end_y);
        route.add(temppath);
        routePoints.add(new PointF((float) start_x, (float) start_y));
        routePoints.add(new PointF((float) end_x, (float) end_y));
        for(int i = 0; i < route.size(); i++) {
            if((curPath && i == curDirection) || (curDirection == i && i == 0)){
                paint.setColor(Color.CYAN);
                canvas.drawPath(route.get(i),paint);
                paint.setColor(Color.RED);
                continue;
            }
            canvas.drawPath(route.get(i), paint);
        }
        fillArrow(canvas, (float) start_x, (float) start_y, (float) end_x, (float) end_y);
        listDirections.add("Please walk " + getDIstance(start_x, start_y, end_x, end_y) + " " + unit + " " + getDirection(start_x, start_y, end_x, end_y));
        distances.add(getDIstance(start_x, start_y, end_x, end_y));
        listDirections.add("Arrived at the " + Locations.get(tappedCode));
    }

    private int getDIstance(int x1, int y1, int x2, int y2){
        double distanceInPixels = Math.sqrt(Math.pow((double)(x2 - x1),2) + Math.pow((double)(y2 - y1),2));
        if(isMetric)
            return (int)(distanceInPixels*mapScaleMeters);
        else
            return (int)(distanceInPixels*mapScaleFeet);
    }

    private String getDirection(int x1, int y1, int x2, int y2){
        if(x2 > x1 && y2 == y1)
            return "east";
        else if(x2 < x1 && y2 == y1)
            return "west";
        else if(x2 == x1 && y2 > y1)
            return "south";
        else if(x2 == x1 && y2 < y1)
            return "north";
        else if(x2 > x1 && y2 < y1)
            return "northeast";
        else if(x2 < x1 && y2 < y1)
            return "northwest";
        else if(x2 > x1 && y2 > y1)
            return "southeast";
        else if(x2 < x1 && y2 > y1)
            return "southwest";
        else
            return "";
    }

    private int getSlope(int x1, int y1, int x2, int y2){
        if((x2 - x1) != 0)
            return  (y2 - y1) / (x2 - x1);
        else
            return Integer.MAX_VALUE;
    }

    private void fillArrow(Canvas canvas, float x0, float y0, float x1, float y1) {
        Paint paint = new Paint();
        if(curDirection == listDirections.size() + 1)
            paint.setColor(Color.CYAN);
        else
            paint.setColor(Color.RED);

        paint.setStyle(Paint.Style.FILL);

        float deltaX = x1 - x0;
        float deltaY = y1 - y0;
        double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        float frac = (float) (1 / (distance / 30));

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

/*    private void markCurrentPath(){
        if(route.size() > 0 && curDirection < route.size()){
            paint.setColor(Color.RED);
            Paint current_path = new Paint();
            current_path.setColor(Color.CYAN);
            current_path.setStyle(Paint.Style.STROKE);
            current_path.setStrokeWidth(20);
            Path curPath = route.get(curDirection);
            Log.i(TAG, "Curdirection: " + curDirection);
            ///canvas.drawPath(curPath,current_path);
            for(int i = 0; i < route.size(); i++){
                if(i == curDirection){
                    paint.setColor(Color.CYAN);
                    canvas.drawPath(route.get(i),paint);
                    continue;
                }
                paint.setColor(Color.RED);
                canvas.drawPath(route.get(i), paint);
            }
            canvas.drawLine(routePoints.get(curDirection).x,routePoints.get(curDirection).y,routePoints.get(curDirection + 1).x,routePoints.get(curDirection + 1).y,current_path);
        }
    }*/

    /*
    onClick(View v) - This function is called whenever one of the buttons are pressed.
    v.getId() gives us the id of what was clicked. We can use this to set up a case
    for each button press and perform the corresponding action.
     */
    public void onClick(View v){
        // If the "scan qr code" button is clicked, start an intent to grab the QR code
        if(v.getId() == R.id.scan_button){
            // The scanner will only look for QR codes and will automatically finish executing
            // once a valid QR code is scanned or the user presses the back button.
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt("Scan a QR Code");
            integrator.initiateScan();
        }
        if (v.getId() == R.id.map_button_next){
            //The next floor button is pressed
            if(curMap == floors.size() - 1)
                curMap = -1;
            floorMap.setImageBitmap(floors.get(++curMap));
            fAttacher.update();
        }
        if(v.getId() == R.id.map_button_prev){
            //the prev floor button is pressed
            if(curMap == 0)
                curMap = floors.size();
            floorMap.setImageBitmap(floors.get(--curMap));
            fAttacher.update();
        }
        if(v.getId() == R.id.speech_button){
            promptSpeechInput();
        }
        if(v.getId() == R.id.direction_button_next) {
            if(curDirection == listDirections.size() - 1 || listDirections.size() == 0)
                return;
            directionText.setText(listDirections.get(++curDirection));
            //markCurrentPath();
            curPath = true;
            markMaps();
            curPath = false;
            speakDirections(1);
        }
        if(v.getId() == R.id.direction_button_prev){
            if(curDirection == 0)
                return;
            directionText.setText(listDirections.get(--curDirection));
            //markCurrentPath();
            curPath = true;
            markMaps();
            curPath = false;
            speakDirections(1);
        }
        if(v.getId() == R.id.direction_text){
            speakDirections(1);
        }
        //floorTxt.setText("NAC Building Floor " + (curMap + 1));
        getSupportActionBar().setTitle("NAC Building Floor " + (curMap + 1));
    }

    private void speakDirections(int select){
        if(isSpeechOn) {
            if (listDirections.size() > 0 && select == 1)
                tts.speak(listDirections.get(curDirection), TextToSpeech.QUEUE_FLUSH, null);
            //else
             //   tts.speak("No directions have been obtained. Please scan a QR code.", TextToSpeech.QUEUE_ADD, null);

            if(select == 2)
                tts.speak("You have selected the " + Locations.get(tappedCode), TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /*
    onActivityResult(int requestCode, int resultCode, Intent intent) - this function is called after the qr code
    scanner intent finishes executing. We can grab the result of the scan and match it to the qr codes to find a match.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        //retrieve the scanning result
        switch(requestCode) {
            case ZXING_REQUEST_CODE:
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

                if (result != null) {
                    //valid result obtained
                    String scannedContent = result.getContents();
                    if (scannedContent != null) {
                        Log.i(TAG, "QR Code Contents: " + scannedContent);
                        Boolean foundMatch = false;
                        int index = 0;
                        // Check the qrCodes list to see if the scanned code is a match. If it is, record the index of the
                        // matched code and set the bool foundMatch to true.
                        for (int i = 0; i < qrCodes.size(); i++) {
                            if (qrCodes.get(i).equals(scannedContent)) {
                                foundMatch = true;
                                index = i;
                            }
                        }
                        // Match is found, vibrate the device and mark the map with the scanned qr code.
                        if (foundMatch) {
                            if(isVibrationOn)
                                vibrate.vibrate(500);
                            Log.i(TAG, "Found a match!");
                            lastScan = index;
                            isMatch = true;
                            markMaps();
                        }
                        // No match is found, set the text to show the user the result.
                        else {
                            Log.i(TAG, "Scanned code did not match");
                            lastScan = -1;
                            markMaps();
                            //floorTxt.setText("Could not find a match");
                            locationTxt.setText("Please scan another QR code");
                        }
                        toastSuccess.show();
                    } else {
                        toastError.show();
                    }
                } else {
                    //No result or invalid result obtained (i.e. user presses the back button instead of scanning something)
                    toastError.show();
                }
                break;
            case SPEECH_REQUEST_CODE:
                if(resultCode == RESULT_OK && intent != null){
                    ArrayList<String> speechResult = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.i(TAG,speechResult.get(0));

                    for(int i = 0; i < Locations.size(); i++){
                        String location = Locations.get(i);
                        String result_location = speechResult.get(0);
                        if(location.equalsIgnoreCase(result_location)) {
                            destTxt.setText("Destination: " + location);
                            if(isVibrationOn)
                                vibrate.vibrate(100);
                            tappedCode = i;
                            Toast toast = Toast.makeText(MainActivity.this, "You have selected the " + location, Toast.LENGTH_SHORT);
                            speakDirections(2);
                            toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL,0,0);
                            toast.show();
                            markMaps();
                            return;
                        }
                    }
                    Toast.makeText(MainActivity.this, "No location found.", Toast.LENGTH_SHORT).show();
                    break;
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_vibration).setChecked(true);
        menu.findItem(R.id.action_units).setChecked(false);
        menu.findItem(R.id.action_speak).setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            //noinspection SimplifiableIfStatement
            case R.id.action_reset:
                resetAll();
                return true;
            case R.id.action_locations:
                destinationsMenu.show();
                return true;
            case R.id.action_source:
                startPointMenu.show();
                return true;
            case R.id.action_vibration:
                if(item.isChecked()) {
                    item.setChecked(false);
                    isVibrationOn = false;
                }
                else{
                    item.setChecked(true);
                    isVibrationOn = true;
                }
                return true;
            case R.id.action_units:
                if(item.isChecked()) {
                    item.setChecked(false);
                    isMetric = false;
                }
                else{
                    item.setChecked(true);
                    isMetric = true;
                }
                return true;
            case R.id.action_speak:
                if(item.isChecked()) {
                    item.setChecked(false);
                    isSpeechOn = false;
                }
                else{
                    item.setChecked(true);
                    isSpeechOn = true;
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class PhotoTapListener implements PhotoViewAttacher.OnPhotoTapListener {

        @Override
        public void onPhotoTap(View view, float x, float y) {
            x_tap = (int)(x * floors.get(curMap).getWidth());
            y_tap = (int)(y * floors.get(curMap).getHeight());

            for (int i = 0; i < qrCodes.size(); i++) {
                int x_pos = xCoords.get(i);
                int y_pos = yCoords.get(i);
                if (Math.abs(x_tap - x_pos) <= 50 && Math.abs(y_tap - y_pos) <= 50 && curMap == (FloorIdx.get(i) - 1)) {
                    tappedCode = i;
                    if(isVibrationOn)
                        vibrate.vibrate(100);
                    destTxt.setText("Destination: " + Locations.get(tappedCode));
                    Toast toast = Toast.makeText(MainActivity.this, "You have selected the " + Locations.get(tappedCode), Toast.LENGTH_SHORT);
                    if(lastScan < 0)
                        speakDirections(2);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    markMaps();
                    if(!samePoint) {
                        tappedCodePrev = i;
                        samePoint = false;
                    }
                    break;
                }
            }
            //Toast showPos = Toast.makeText(MainActivity.this, "x: " + x_tap + "  y: " + y_tap, Toast.LENGTH_SHORT);
            //showPos.show();
        }

        @Override
        public void onOutsidePhotoTap() {
            // showToast("You have a tap event on the place where out of the photo.");
        }
    }

    public class Vertex {
        final private String id;
        final private String name;


        public Vertex(String id, String name) {
            this.id = id;
            this.name = name;
        }
        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Vertex other = (Vertex) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public class Edge  {
        private final String id;
        private final Vertex source;
        private final Vertex destination;
        private final int weight;

        public Edge(String id, Vertex source, Vertex destination, int weight) {
            this.id = id;
            this.source = source;
            this.destination = destination;
            this.weight = weight;
        }

        public String getId() {
            return id;
        }
        public Vertex getDestination() {
            return destination;
        }

        public Vertex getSource() {
            return source;
        }
        public int getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return source + " " + destination;
        }


    }

    public class Graph {
        private final List<Vertex> vertexes;
        private final List<Edge> edges;

        public Graph(List<Vertex> vertexes, List<Edge> edges) {
            this.vertexes = vertexes;
            this.edges = edges;
        }

        public List<Vertex> getVertexes() {
            return vertexes;
        }

        public List<Edge> getEdges() {
            return edges;
        }



    }

    public class DijkstraAlgorithm {

        private final List<Vertex> nodes;
        private final List<Edge> edges;
        private Set<Vertex> settledNodes;
        private Set<Vertex> unSettledNodes;
        private Map<Vertex, Vertex> predecessors;
        private Map<Vertex, Integer> distance;

        public DijkstraAlgorithm(Graph graph) {
            // create a copy of the array so that we can operate on this array
            this.nodes = new ArrayList<Vertex>(graph.getVertexes());
            this.edges = new ArrayList<Edge>(graph.getEdges());
        }

        public void execute(Vertex source) {
            settledNodes = new HashSet<Vertex>();
            unSettledNodes = new HashSet<Vertex>();
            distance = new HashMap<Vertex, Integer>();
            predecessors = new HashMap<Vertex, Vertex>();
            distance.put(source, 0);
            unSettledNodes.add(source);
            while (unSettledNodes.size() > 0) {
                Vertex node = getMinimum(unSettledNodes);
                settledNodes.add(node);
                unSettledNodes.remove(node);
                findMinimalDistances(node);
            }
        }

        private void findMinimalDistances(Vertex node) {
            List<Vertex> adjacentNodes = getNeighbors(node);
            for (Vertex target : adjacentNodes) {
                if (getShortestDistance(target) > getShortestDistance(node)
                        + getDistance(node, target)) {
                    distance.put(target, getShortestDistance(node)
                            + getDistance(node, target));
                    predecessors.put(target, node);
                    unSettledNodes.add(target);
                }
            }

        }

        private int getDistance(Vertex node, Vertex target) {
            for (Edge edge : edges) {
                if (edge.getSource().equals(node)
                        && edge.getDestination().equals(target)) {
                    return edge.getWeight();
                }
            }
            throw new RuntimeException("Should not happen");
        }

        private List<Vertex> getNeighbors(Vertex node) {
            List<Vertex> neighbors = new ArrayList<Vertex>();
            for (Edge edge : edges) {
                if (edge.getSource().equals(node)
                        && !isSettled(edge.getDestination())) {
                    neighbors.add(edge.getDestination());
                }
            }
            return neighbors;
        }

        private Vertex getMinimum(Set<Vertex> vertexes) {
            Vertex minimum = null;
            for (Vertex vertex : vertexes) {
                if (minimum == null) {
                    minimum = vertex;
                } else {
                    if (getShortestDistance(vertex) < getShortestDistance(minimum)) {
                        minimum = vertex;
                    }
                }
            }
            return minimum;
        }

        private boolean isSettled(Vertex vertex) {
            return settledNodes.contains(vertex);
        }

        private int getShortestDistance(Vertex destination) {
            Integer d = distance.get(destination);
            if (d == null) {
                return Integer.MAX_VALUE;
            } else {
                return d;
            }
        }

        /*
         * This method returns the path from the source to the selected target and
         * NULL if no path exists
         */
        public ArrayList<Vertex> getPath(Vertex target) {
            //LinkedList<Vertex> path = new LinkedList<Vertex>();
            ArrayList<Vertex> path = new ArrayList<>();
            Vertex step = target;
            // check if a path exists
            if (predecessors.get(step) == null) {
                return null;
            }
            path.add(step);
            while (predecessors.get(step) != null) {
                step = predecessors.get(step);
                path.add(step);
            }
            // Put it into the correct order
            Collections.reverse(path);
            return path;
        }

        public ArrayList<Integer> getPathIndices(Vertex target) {
            //LinkedList<Vertex> path = new LinkedList<Vertex>();
            ArrayList<Integer> path = new ArrayList<>();
            Vertex step = target;
            // check if a path exists
            if (predecessors.get(step) == null) {
                return null;
            }
            path.add(Integer.parseInt(step.getId()));
            while (predecessors.get(step) != null) {
                step = predecessors.get(step);
                path.add(Integer.parseInt(step.getId()));
            }
            // Put it into the correct order
            Collections.reverse(path);
            return path;
        }

    }
}

