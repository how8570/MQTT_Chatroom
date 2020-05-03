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

    private static BlockingQueue receiveBuffer;

    public MQTT(String room, final BlockingQueue<byte[]> buffer) throws MqttException {

        this.receiveBuffer = buffer;

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
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                Log.i(tag, "recive msg: " + s + ": " + Arrays.toString(mqttMessage.getPayload()));
                receiveBuffer.put(mqttMessage.getPayload());
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
