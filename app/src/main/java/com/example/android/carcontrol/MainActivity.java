package com.example.android.carcontrol;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import io.github.controlwear.virtual.joystick.android.JoystickView;

import static java.lang.Thread.sleep;

public class MainActivity extends Activity {
    private static final String networkSSID = "2ASUS2";
    private static final String networkPass = "I7fp3Afs";
    private static WifiManager wifiManager;
    private Change commandChange;

    private static final int MAX_PWM = 255;
    private static final int MOTOR_COMMAND_MAX = 142;
    private static final int LIGHT_COMMAND_MAX = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handleConnection();

        handleJoyStick();
        handleHonk();
        handleLights();
    }

    private void handleConnection() {
        turnOnWifi(getApplicationContext());
        configureWifi(getApplicationContext());

        try {
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ClientConnection conn = new ClientConnection();
        commandChange = conn;
        conn.start();
    }

    private static void turnOnWifi(Context context) {
        wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            wifiManager.setWifiEnabled(true);
        }
        Toast.makeText(context, "turning on...", Toast.LENGTH_SHORT).show();
    }

    private static void configureWifi(Context context) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";
        conf.preSharedKey = "\"" + networkPass + "\"";
        conf.status = WifiConfiguration.Status.ENABLED;
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        int netid = wifiManager.addNetwork(conf);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netid, true);
        wifiManager.reconnect();
        Toast.makeText(context, "Configured", Toast.LENGTH_SHORT).show();
    }

    private void handleJoyStick() {
        JoystickView joystickView = findViewById(R.id.joystickView);
        joystickView.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                String commands = buildMotorCommand(angle, strength);
                Log.e(MainActivity.class.getName(), commands);
                commandChange.onSend(commands);
            }
        });

    }

    private String buildMotorCommand(int angle, int strength) {
        int commandX = (int) Math.round(strength * Math.cos(Math.toRadians(angle)));
        int commandY = (int) Math.round(strength * Math.sin(Math.toRadians(angle)));

        double rawLeft = (commandY + commandX) * MAX_PWM / MOTOR_COMMAND_MAX;
        double rawRight = (commandY - commandX) * MAX_PWM / MOTOR_COMMAND_MAX;

        return "L:" + String.valueOf(Math.round(rawLeft)) + ";" +
                "R:" + String.valueOf(Math.round(rawRight)) + "\n";
    }

    private void handleLights() {
        final TextView intensity = findViewById(R.id.textview_intensity);
        SeekBar slider = findViewById(R.id.slider_intensity);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                String command = buildLightCommand(progress);
                intensity.setText("Intensity: " + progress);
                Log.e(MainActivity.class.getName(), command);
                commandChange.onSend(command);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private String buildLightCommand(int intensity) {
        return "B:" + intensity * MAX_PWM / LIGHT_COMMAND_MAX + "\n";
    }

    private void handleHonk() {
        Button honkButton = findViewById(R.id.button_honk);
        honkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = buildHonkCommand();
                Log.e(MainActivity.class.getName(), command);
                commandChange.onSend(command);
            }
        });
    }

    private String buildHonkCommand() {
        return "H\n";
    }
}