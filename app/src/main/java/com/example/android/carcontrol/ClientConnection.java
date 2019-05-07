package com.example.android.carcontrol;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.example.android.carcontrol.SettingsActivity.CLIENT_PORT_KEY;
import static com.example.android.carcontrol.SettingsActivity.IP_DEFAULT;
import static com.example.android.carcontrol.SettingsActivity.IP_KEY;
import static com.example.android.carcontrol.SettingsActivity.PORT_DEFAULT;
import static com.example.android.carcontrol.SettingsActivity.SPEED_KEY;

public class ClientConnection extends Thread implements Change {
    private Context context;
    private Socket socket;
    private String message, prevMessage;
    private String ipAddress;
    private int port;

    ClientConnection(Context c) {
        message = "";
        prevMessage = "";
        context = c;
        retrievePreferences();
    }

    @Override
    public void run() {
        super.run();
        SharedPreferences.OnSharedPreferenceChangeListener interruptListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (!s.equals(SPEED_KEY)) {
                    interrupt();
                }
            }
        };
        getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(interruptListener);
        DataOutputStream DOS;
        while (!Thread.interrupted()) {
            try {
                if (socket != null) {
                    DOS = new DataOutputStream(socket.getOutputStream());
                    if (!prevMessage.equals(message) || message.equals("H\n")) {
                        DOS.flush();
                        DOS.writeChars(message);
                        prevMessage = message;
                        if (message.equals("H\n")) {
                            prevMessage = "";
                            message = "";
                        }
                    }
                } else {
                    socket = new Socket(InetAddress.getByName(ipAddress), port);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Thread.interrupted()) {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    private void retrievePreferences() {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        ipAddress = preferences.getString(IP_KEY, IP_DEFAULT);
        port = preferences.getInt(CLIENT_PORT_KEY, PORT_DEFAULT);
    }

    @Override
    public void onSend(String string) {
        message = string;
    }
}
