package capstone.project.qrnav;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    // Initialize global variables
    private static final String TAG = "QRNavigation";
    private Button scanButton, mapButtonNext, mapButtonPrev;
    private TextView floorTxt, locationTxt;
    private Toast toastError, toastSuccess;
    private ImageViewTouch floorMap;
    private int[] imgArray;
    private ArrayList<Bitmap> floors = new ArrayList<>();
    private ArrayList<String> qrCodes = new ArrayList<>();
    private ArrayList<Integer> FloorIdx = new ArrayList<>();
    private ArrayList<Integer> xCoords = new ArrayList<>();
    private ArrayList<Integer> yCoords = new ArrayList<>();
    private ArrayList<String> Locations = new ArrayList<>();
    private int lastScan = -1;
    private int curMap = 0;
    private Vibrator vibrate;

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
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        vibrate = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        scanButton = (Button)findViewById(R.id.scan_button);
        mapButtonNext = (Button)findViewById(R.id.map_button_next);
        mapButtonPrev = (Button)findViewById(R.id.map_button_prev);
        floorTxt = (TextView)findViewById(R.id.current_floor);
        locationTxt = (TextView)findViewById(R.id.scan_content);
        floorMap = (ImageViewTouch)findViewById(R.id.map);
        floorMap.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

        // Declare error and success Toasts we use to display whether or not the scan was successful.
        toastError = Toast.makeText(getApplicationContext(), "QR code was unsuccessfully scanned", Toast.LENGTH_SHORT);
        toastSuccess = Toast.makeText(getApplicationContext(), "QR code was successfully scanned!", Toast.LENGTH_SHORT);

        // Get an array of IDs that correspond to each floorplan bitmap in the drawable folder (the name of the drawable is the filename w/o the extension)
        imgArray = new int[] {R.drawable.floor_1, R.drawable.floor_2, R.drawable.floor_3, R.drawable.floor_4,
                R.drawable.floor_5, R.drawable.floor_6, R.drawable.floor_7, R.drawable.floor_8};

        // Load all of the checkpoint (QR code) data from the checkpoints.txt file in the assets folder.
        loadCheckpoints();

        // Set our custom ImageViewTouch element to display the current map element and mark the maps.
        markMaps();

        // Set a listener to both the scan and map buttons. When a button is clicked, it will call the onClick function with the view corresponding to that button.
        scanButton.setOnClickListener(this);
        mapButtonNext.setOnClickListener(this);
        mapButtonPrev.setOnClickListener(this);

        // Set floorTxt with the current floor value (should be 1 at the start of the app). Also set the location text.
        floorTxt.setText("NAC Building Floor " + (curMap + 1));
        locationTxt.setText("Please scan a QR code");
    }

    /*
    loadFloors() - This function  loads all of the floorplan images (which is in res/drawable-nodpi folder) in the floors arraylist.
    It is used to both initialize the bitmap arraylist with all of the images as well as to reset the bitmaps of any modifications
    (such as marking the matched points on the map)
     */
    private void loadFloors(){
        // Create a bitmap array to hold each floorplan image.
        floors.clear();
        for(int i = 0; i < imgArray.length; i++){
            floors.add(BitmapFactory.decodeResource(getResources(),imgArray[i]).copy(Bitmap.Config.ARGB_8888, true));
        }
        floorMap.setImageBitmap(floors.get(curMap));
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
                Locations.add(temp_strs[4]);
            }
            Log.i(TAG, "Finished parsing checkpoints.txt");
        }
        // This catch statement is executed in the case where checkpoints.txt cannot be opened
        catch(IOException e){
            e.printStackTrace();
            Log.e(TAG, "Error opening checkpoints.txt");
        }
    }

    /*
    markMaps() - This function will go through the bitmap floorplans and draw circle at specified locations
    These locations are determined beforehand and will be stored in a text file to make the code neater
    Initially, all the QR code locations are marked using the color blue. However, after a match is made, a red
    color is used to denote the point that was matched.
     */
    private void markMaps(){

        // Reset the map view (default zoom/pan) and reload the floor bitmaps.
        floorMap.resetMatrix();
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
                paint.setTextSize(50);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("You are here", xCoords.get(i) - 135, yCoords.get(i) - 35, paint); // Puts some text above the matched point
                canvas.drawCircle(xCoords.get(i), yCoords.get(i), 30, paint);
                // Now we set curMap (int used to keep track of the current floor) to be the floor
                // where the matched QR code is located. The displayed map and text is updated accordingly.
                curMap = curFloor;
                floorMap.setImageBitmap(floors.get(curMap));
                floorTxt.setText("NAC Building Floor " + (curMap + 1));
                locationTxt.setText("Last scanned location: " + Locations.get(curMap).replaceAll("_"," "));
            }
        }
    }

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
        if(v.getId() == R.id.map_button_next){
            //The next floor button is pressed
            if(curMap == floors.size() - 1)
                curMap = -1;
            floorMap.setImageBitmap(floors.get(++curMap));
        }
        if(v.getId() == R.id.map_button_prev){
            //the prev floor button is pressed
            if(curMap == 0)
                curMap = floors.size();
            floorMap.setImageBitmap(floors.get(--curMap));
        }
        floorTxt.setText("NAC Building Floor " + (curMap + 1));
    }

    /*
    onActivityResult(int requestCode, int resultCode, Intent intent) - this function is called after the qr code
    scanner intent finishes executing. We can grab the result of the scan and match it to the qr codes to find a match.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        //retrieve the scanning result
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if(result != null){
            //valid result obtained
            String scannedContent = result.getContents();
            if(scannedContent != null) {
                Log.i(TAG, "QR Code Contents: " + scannedContent);
                Boolean foundMatch = false;
                int index = 0;
                // Check the qrCodes list to see if the scanned code is a match. If it is, record the index of the
                // matched code and set the bool foundMatch to true.
                for(int i = 0; i < qrCodes.size(); i++) {
                    if(qrCodes.get(i).equals(scannedContent)) {
                        foundMatch = true;
                        index = i;
                    }
                }
                // Match is found, vibrate the device and mark the map with the scanned qr code.
                if(foundMatch){
                    vibrate.vibrate(500);
                    Log.i(TAG, "Found a match!");
                    lastScan = index;
                    markMaps();
                }
                // No match is found, set the text to show the user the result.
                else{
                    Log.i(TAG, "Scanned code did not match");
                    lastScan = -1;
                    markMaps();
                    floorTxt.setText("Could not find a match");
                    locationTxt.setText("Please scan another QR code");
                }
                toastSuccess.show();
            }
            else{
                toastError.show();
            }
        }
        else{
            //No result or invalid result obtained (i.e. user presses the back button instead of scanning something)
            toastError.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
