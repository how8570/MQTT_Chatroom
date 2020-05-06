package pers.how8570.mqtt_chatroom;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import pers.how8570.mqtt_chatroom.tools.MQTT;

public class MainActivity extends AppCompatActivity {

    public MQTT publishClient;
    public MQTT subscribeClient;
    public BlockingQueue<byte[]> msgReceiveBuffer = new LinkedBlockingQueue<>();
    public BlockingQueue<byte[]> imgReceiveBuffer = new LinkedBlockingQueue<>();

    public static final int PICK_IMAGE = 1;

    public Button mBtmConn;
    public Button mBtmSubmit;
    public Button mBtnPhoto;

    public ScrollView mScroll;
    public EditText mMsg;
    public LinearLayout mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        try {
            publishClient = new MQTT("room", null, null);
            subscribeClient = new MQTT("room", msgReceiveBuffer, imgReceiveBuffer);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        mBtmConn = findViewById(R.id.mBtmConn);
        mBtmConn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!publishClient.isConnected()){
                    publishClient.start();
                }
                if (!subscribeClient.isConnected()){
                    subscribeClient.start();
                }
            }
        });

        mBtmSubmit = findViewById(R.id.mBtmSubmit);
        mBtmSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = mMsg.getText().toString();
                if (!msg.matches("")){
                    publishClient.publish("room/msg", msg.getBytes());
                }
            }
        });

        mBtnPhoto = findViewById(R.id.mBtmPhoto);
        mBtnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, PICK_IMAGE);
            }
        });

        mMsg = findViewById(R.id.mMsg);
        mMsg.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                Log.d("Keydown", "code: " + keyCode);
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) ){
                    mBtmSubmit.callOnClick();
                    return true;
                }
                return false;
            }
        });
        mLinearLayout = findViewById(R.id.mLinearLayout);

        subscribeClient.subscribe("room/#");

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(10);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // update TextView here!
                                updateMsg();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();

        mScroll = findViewById(R.id.mScroll);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PICK_IMAGE && data != null) {
            Uri uri = data.getData();
            ContentResolver cr = this.getContentResolver();
            try {
                assert uri != null;
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));

                int bytes = bitmap.getByteCount();
                ByteBuffer buf = ByteBuffer.allocate(bytes);
                bitmap.copyPixelsToBuffer(buf);
                byte[] byteArray = buf.array();

                Log.d("main", "byteArray len = " + byteArray.length);
//                String encoded_base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
//                Log.d("main", "encoded_base64 len = " + encoded_base64.length());
                publishClient.publish("room/img", byteArray);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * update message to latest and scroll down
     */
    @SuppressLint("SetTextI18n")
    public void updateMsg() {
        byte[] b;
        while ((b = msgReceiveBuffer.poll()) != null) {

            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, // Width of TextView
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(lp);

            Date currentTime = Calendar.getInstance().getTime();
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String formattedTime = sdf.format(currentTime);
            tv.setText(formattedTime + "\n" + new String(b) + "\n");

            mLinearLayout.addView(tv);
            if (!mScroll.isFocused()){
                // need do take post method to make sure add View finished
                mScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollToBottom(mScroll);
                    }
                });
            }
        }

        while ((b = imgReceiveBuffer.poll()) != null) {

            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, // Width of ImageView
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            iv.setLayoutParams(lp);

            Date currentTime = Calendar.getInstance().getTime();
//            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//            String formattedTime = sdf.format(currentTime);

//            byte[] decodedString = Base64.decode(b.toString(), Base64.DEFAULT);
//            Log.d("main", "decodedString = " + decodedString);
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            iv.setImageBitmap(bitmap);
//            iv.setImageResource(R.drawable.ic_launcher_background);

            mLinearLayout.addView(iv);
            if (!mScroll.isFocused()) {
                // need do take post method to make sure add View finished
                mScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollToBottom(mScroll);
                    }
                });
            }
        }


    }

    /**
     * scroll.fullScroll(View.FOCUS_DOWN) will lead to the change of focus.
     * That will bring some strange behavior when there are more than
     * one focusable views, e.g two EditText.
     * see https://stackoverflow.com/questions/3080402/android-scrollview-force-to-bottom
     * @param sv scrollView to scroll to bottom.
     */
    private void scrollToBottom(ScrollView sv) {
        View lastChild = sv.getChildAt(sv.getChildCount() - 1);
        int bottom = lastChild.getBottom() + sv.getPaddingBottom();
        int sy = sv.getScrollY();
        int sh = sv.getHeight();
        int delta = bottom - (sy + sh);

        sv.smoothScrollBy(0, delta);
    }


}


