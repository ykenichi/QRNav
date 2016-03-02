package capstone.project.qrnav;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Objects;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static final String TAG = "QRNavigation";
    private Button scanButton, mapButton;
    private TextView contentTxt;
    private Toast toastError, toastSuccess;
    private ImageViewTouch floorMap;
    private Bitmap[] floors;
    private ArrayList<String> qrCodes;
    private int lastScan = 0;
    private int curMap = 0;
    private Vibrator vibrate;

    public final static int WHITE = 0xFFFFFFFF;
    public final static int BLACK = 0xFF000000;
    public final static int WIDTH = 800;
    public final static int HEIGHT = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        vibrate = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        scanButton = (Button)findViewById(R.id.scan_button);
        mapButton = (Button)findViewById(R.id.map_button);
        contentTxt = (TextView)findViewById(R.id.scan_content);
        contentTxt.setText("Please Scan a QR Code");
        floorMap = (ImageViewTouch)findViewById(R.id.map);
        floorMap.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
        toastError = Toast.makeText(getApplicationContext(), "QR code was unsuccessfully scanned", Toast.LENGTH_SHORT);
        toastSuccess = Toast.makeText(getApplicationContext(), "QR code was successfully scanned!", Toast.LENGTH_SHORT);
        int[] imgArray = {R.drawable.floor_1, R.drawable.floor_2, R.drawable.floor_3, R.drawable.floor_4, R.drawable.floor_5,
        R.drawable.floor_6, R.drawable.floor_7, R.drawable.floor_8};
        floors = new Bitmap[imgArray.length];
        for(int i = 0; i < imgArray.length; i++){
            floors[i] = BitmapFactory.decodeResource(getResources(),imgArray[i]).copy(Bitmap.Config.ARGB_8888, true);
        }
        floorMap.setImageBitmap(floors[curMap]);
        markMaps();
        qrCodes = new ArrayList<>();
        for(int i = 0; i < 4; i++){
            qrCodes.add("floor_1_code_" + (i+1));
        }
        scanButton.setOnClickListener(this);
        mapButton.setOnClickListener(this);
    }

    private void markMaps(){
        floorMap.resetMatrix();
        Canvas canvas = new Canvas(floors[0]);
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        canvas.drawCircle(190 + 110, 295 + 175, 10, paint);
        canvas.drawCircle(310 + 180, 290 + 175, 10, paint);
        canvas.drawCircle(320 + 180, 220 + 175, 10, paint);
        canvas.drawCircle(220 + 110, 220 + 175, 10, paint);
        paint.setColor(Color.RED);

        switch(lastScan){
            case 1:
                canvas.drawCircle(190 + 110, 295 + 175, 10, paint);
                contentTxt.setText("Scanned QR Code #1");
                break;
            case 2:
                canvas.drawCircle(310 + 180, 290 + 175, 10, paint);
                contentTxt.setText("Scanned QR Code #2");
                break;
            case 3:
                canvas.drawCircle(320 + 180, 220 + 175, 10, paint);
                contentTxt.setText("Scanned QR Code #3");
                break;
            case 4:
                canvas.drawCircle(220 + 110, 220 + 175, 10, paint);
                contentTxt.setText("Scanned QR Code #4");
                break;
        }
    }

    public void onClick(View v){
        // If the "scan qr code" button is clicked
        if(v.getId() == R.id.scan_button){
            //scan button is pressed
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt("Scan a QR Code");
            integrator.initiateScan();
        }
        if(v.getId() == R.id.map_button){
            //map button is pressed
            if(curMap == floors.length - 1)
                curMap = -1;
            floorMap.setImageBitmap(floors[++curMap]);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        //retrieve the scanning result
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if(result != null){
            //valid result obtained
            String scannedContent = result.getContents();
            if(scannedContent != null) {
                //contentTxt.setText("Content: " + scannedContent);
                Log.i(TAG, "QR Code Contents: " + scannedContent);
                Boolean foundMatch = false;
                int index = 0;
                for(int i = 0; i < qrCodes.size(); i++) {
                    if(qrCodes.get(i).equals(scannedContent)) {
                        foundMatch = true;
                        index = i + 1;
                    }
                }
                lastScan = index;
                if(foundMatch){
                    vibrate.vibrate(500);
                    Log.i(TAG, "Found a match!");
                    markMaps();
                }
                else{
                    Log.i(TAG, "Scanned code did not match");
                    markMaps();
                    contentTxt.setText("Could not find match");
                }
                toastSuccess.show();
            }
            else{
                toastError.show();
            }
        }
        else{
            //no result or invalid result obtained
            toastError.show();
        }
    }

    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, HEIGHT, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
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
