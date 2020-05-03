package pers.how8570.mqtt_chatroom;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import pers.how8570.mqtt_chatroom.tools.MQTT;

public class MainActivity extends AppCompatActivity {

    public MQTT publishClient, subscribeClient;
    public BlockingQueue<byte[]> receiveBuffer = new LinkedBlockingQueue<>();

    public Button mBtmConn;
    public Button mBtmSubmit;
    public ScrollView mScroll;
    public EditText mMsg;
    public LinearLayout mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        try {
            publishClient = new MQTT("room", null);
            subscribeClient = new MQTT("room", receiveBuffer);
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

        mMsg = findViewById(R.id.mMsg);
        mMsg.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                Log.e("Keydown", "code: " + keyCode);
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
                }
            }
        };
        t.start();

        mScroll = findViewById(R.id.mScroll);
    }


    /**
     * update message to latest and scroll down
     */
    @SuppressLint("SetTextI18n")
    public void updateMsg() {
        byte[] b;
        while ( (b = receiveBuffer.poll()) != null){

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
