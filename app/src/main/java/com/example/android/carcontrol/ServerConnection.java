package com.example.android.carcontrol;

import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerConnection extends Thread {
    private static final int PORT = 5005;
    private ServerSocket socket;
    private String message;
    private Context context;

    ServerConnection(Context c) {
        message = "";
        context = c;
    }

    @Override
    public void run() {
        super.run();
        try {
            socket = new ServerSocket(PORT);
            Log.e(ServerConnection.class.getName(), "Port open");
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            Log.e(ServerConnection.class.getName(), "Keep listening");
            Socket clientSocket = null;
            try {
                clientSocket = socket.accept();
                Log.e(ServerConnection.class.getName(), "Accepted");
                DataInputStream DIS = new DataInputStream(clientSocket.getInputStream());
                String tmp;
                StringBuilder inputLine = new StringBuilder();
                tmp = DIS.readLine();
                while ( tmp != null && !tmp.isEmpty()) {
                    Log.e(ServerConnection.class.getName(), tmp);
                    inputLine.append(tmp);
                    tmp = DIS.readLine();
                }
                message = inputLine.toString();
                Log.e(ServerConnection.class.getName(), "Obtained message");
                DIS.close();
                try {
                    Capture capture = (Capture) context;
                    capture.onReceive(message);
                    Log.e(ServerConnection.class.getName(), "Called on receive");
                } catch (ClassCastException e) {
                    Log.e(ServerConnection.class.getName(), "Unable to cast context to capture");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Thread.interrupted()) {
                Log.e(ServerConnection.class.getName(), "Interrupted");
                try {
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }
}
