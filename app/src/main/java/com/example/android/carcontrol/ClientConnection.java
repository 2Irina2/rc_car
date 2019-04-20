package com.example.android.carcontrol;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ClientConnection extends Thread implements Change {
    // Default esp8266 address is 192.168.4.1
    private static final String IP_ADDRESS = "10.42.0.1";
    private static final int PORT = 5000;
    private Socket socket;
    private String message, prevMessage;


    ClientConnection(){
        message = "";
        prevMessage = "";
    }

    @Override
    public void run() {
        super.run();
        try {
            socket = new Socket(InetAddress.getByName(IP_ADDRESS), PORT);
            if(socket.isConnected()){
                Log.e(ClientConnection.class.getName(), "Socket connected");
            } else {
                Log.e(ClientConnection.class.getName(), "Socket failed to connect");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataOutputStream DOS = null;
        try {
            DOS = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(socket.isConnected()){
            try {
                if (DOS != null && (!prevMessage.equals(message) || message.equals("H\n"))) {
                    DOS.flush();
                    DOS.writeChars(message);
                    prevMessage = message;
                    if(message.equals("H\n")){
                        prevMessage = "";
                        message = "";
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(Thread.interrupted()){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    @Override
    public void onSend(String string) {
        message = string;
    }
}
