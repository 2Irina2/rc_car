package com.example.android.carcontrol;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import io.github.controlwear.virtual.joystick.android.JoystickView;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.example.android.carcontrol.SettingsActivity.CLIENT_PORT_KEY;
import static com.example.android.carcontrol.SettingsActivity.IP_KEY;
import static com.example.android.carcontrol.SettingsActivity.SPEED_DEFAULT;
import static com.example.android.carcontrol.SettingsActivity.SPEED_KEY;

public class MainActivity extends AppCompatActivity implements Capture {
    private static final String networkSSID = "2ASUS2";
    private static final String networkPass = "I7fp3Afs";
    private WifiManager wifiManager;
    private Change commandChange;

    private static final int MAX_PWM = 255;
    private static final int MOTOR_COMMAND_MAX = 142;
    private static final int LIGHT_COMMAND_MAX = 100;

    int speedRatio;

    TextView batteryTextView;
    SharedPreferences.OnSharedPreferenceChangeListener speedAndConnectionListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        batteryTextView = findViewById(R.id.textview_battery_value);
        batteryTextView.setText("~");

        initSharedPreferences();
        handleClientConnection();
        handleServerConnection();

        handleJoyStick();
        handleHonk();
        handleLights();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(speedAndConnectionListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.button_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void initSharedPreferences() {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);
        String speed = sharedPreferences.getString(SPEED_KEY, SPEED_DEFAULT);
        getSpeedRatio(speed);
        speedAndConnectionListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(SPEED_KEY)) {
                    String speed = sharedPreferences.getString(key, SPEED_DEFAULT);
                    getSpeedRatio(speed);
                }
                if (key.equals(CLIENT_PORT_KEY) || key.equals(IP_KEY)) {
                    handleClientConnection();
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(speedAndConnectionListener);
    }

    private void handleClientConnection() {
        turnOnWifi(getApplicationContext());
        configureWifi();

        ClientConnection conn = new ClientConnection(getApplicationContext());
        commandChange = conn;
        conn.start();
    }

    private void handleServerConnection() {
        ServerConnection serverConnection = new ServerConnection(this);
        serverConnection.start();
    }

    private void turnOnWifi(Context context) {
        wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            wifiManager.setWifiEnabled(true);
        }
    }

    private void configureWifi() {
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
    }

    private void handleJoyStick() {
        JoystickView joystickView = findViewById(R.id.joystickView);
        joystickView.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                String commands = buildMotorCommand(angle, strength);
                commandChange.onSend(commands);
            }
        });

    }

    private String buildMotorCommand(int angle, int strength) {
        int commandX = (int) Math.round(strength * Math.cos(Math.toRadians(angle)));
        int commandY = (int) Math.round(strength * Math.sin(Math.toRadians(angle)));

        double rawLeft = (commandY + commandX) * MAX_PWM / MOTOR_COMMAND_MAX / speedRatio;
        double rawRight = (commandY - commandX) * MAX_PWM / MOTOR_COMMAND_MAX / speedRatio;

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
                commandChange.onSend(command);
            }
        });
    }

    private String buildHonkCommand() {
        return "H\n";
    }

    private void getSpeedRatio(String speed) {
        switch (speed) {
            case "SLOW":
                speedRatio = 3;
                break;
            case "MEDIUM":
                speedRatio = 2;
                break;
            case "FAST":
                speedRatio = 1;
                break;
        }
    }

    @Override
    public void onReceive(final String command) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                batteryTextView.setText(command + "%");
                Log.e(MainActivity.class.getName(), "Updated ui");
            }
        });
    }
}