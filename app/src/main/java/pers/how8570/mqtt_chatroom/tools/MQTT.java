package pers.how8570.mqtt_chatroom.tools;


import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import pers.how8570.mqtt_chatroom.MainActivity;

public class MQTT extends Thread{

    private static  final String tag = "MQTT";

    private static int qos = 1;
    private static MqttClient mqttClient;
    private static MqttConnectOptions options;
    private static String broker = "tcp://192.168.0.101:1883";
    private static String userName = "admin";

    private BlockingQueue msgReceiveBuffer;
    private BlockingQueue imgReciveBuffer;

    public MQTT(String room, final BlockingQueue<byte[]> msg, final BlockingQueue img) throws MqttException {

        this.msgReceiveBuffer = msg;
        this.imgReciveBuffer = img;

        mqttClient = new MqttClient(broker, room, new MemoryPersistence());
        options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(userName);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(20);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                try {
                    mqttClient.reconnect();
                } catch (MqttException e) {
                    Log.i(tag, "lost connected.");
                    e.printStackTrace();
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                String dataType = topic.substring(topic.lastIndexOf('/') + 1).trim();
                Log.i(tag, "recive msg: " + topic + ": " + Arrays.toString(mqttMessage.getPayload()));
                Log.i(tag, "data type: " + dataType + " .");
                if (dataType.equals("msg")) {
                    msgReceiveBuffer.put(mqttMessage.getPayload());
                } else if (dataType.equals("img")) {
                    Log.d(tag, "received len = " + mqttMessage.getPayload().length);
                    imgReciveBuffer.put(mqttMessage.getPayload());
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.i(tag, "Thread: " + Thread.currentThread()
                        + " deliveryComplete!" + iMqttDeliveryToken.isComplete());
            }
        });
        mqttClient.connect();


    }

    public void release() {
        try {
            mqttClient.close();
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    public void publish(String topic, byte[] msg) {
        MqttMessage message = new MqttMessage(msg);
        message.setQos(qos);
        try {
            mqttClient.publish(topic, message);
            Log.i(tag,"msg published");
        } catch (MqttException e) {
            Log.i(tag, "fail publish message to " + topic);
            e.printStackTrace();
        }
    }

    public void subscribe(String topic){
        try{
            mqttClient.subscribe(topic, qos);
            Log.i(tag,"start subscribe");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


//    這會在背景一直跑
//    @Override
//    public void run(){
//        while(!Thread.interrupted()){
//            try {
//                Log.i("MQTT","Thread:"+Thread.currentThread());
//                if(!mqttClient.isConnected()) {
//                    mqttClient.connect(options);
//                    Log.i("MQTT","connected");
//                }
//                /*ByteBuffer bf = (ByteBuffer) sharedQ.take();
//                Log.i("MQTT",bf.toString());
//                if(bf.remaining()>0){
//                    byte[] myB = new byte[bf.remaining()];
//                    Log.i("MQTT","startSending");
//                    bf.get(myB);
//                    startPub(myB);
//                    Log.i("MQTT","endSending");
//                }*/
////                publishMessage(queue.take());
////                publishMessage("test", "test msg".getBytes());
//                subscribe("#");
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//        }
//    }

}
