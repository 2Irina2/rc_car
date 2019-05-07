package com.example.android.carcontrol;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class SettingsActivity extends AppCompatActivity {
    public static final String IP_KEY = "ip";
    public static final String CLIENT_PORT_KEY = "client_port";
    public static final String SPEED_KEY = "speed";
    public static final String IP_DEFAULT = "192.168.4.1";  // Default esp8266 ip
    public static final int PORT_DEFAULT = 8888;            // Default esp8266 port
    public static final String SPEED_DEFAULT = "SLOW";

    EditText ipEditText;
    EditText portEditText;
    RadioGroup speedRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");

        ipEditText = findViewById(R.id.edittext_ip);
        portEditText = findViewById(R.id.edittext_port);
        speedRadioGroup = findViewById(R.id.radiogroup);

        retrievePreferences();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                updatePreferences();
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    public void retrievePreferences() {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);

        ipEditText.setText(sharedPreferences.getString(IP_KEY, IP_DEFAULT));
        portEditText.setText(Integer.toString(sharedPreferences.getInt(CLIENT_PORT_KEY, PORT_DEFAULT)));
        String speed = sharedPreferences.getString(SPEED_KEY, SPEED_DEFAULT);
        switch (speed) {
            case "SLOW":
                speedRadioGroup.check(R.id.radio_slow);
                break;
            case "MEDIUM":
                speedRadioGroup.check(R.id.radio_medium);
                break;
            case "FAST":
                speedRadioGroup.check(R.id.radio_fast);
        }

    }

    public void updatePreferences() {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();


        String ip = ipEditText.getText().toString();
        if (!ip.isEmpty()) {
            editor.putString(IP_KEY, ip);
        }

        String port = portEditText.getText().toString();
        if (!port.isEmpty()) {
            editor.putInt(CLIENT_PORT_KEY, Integer.valueOf(port));
        }

        int selectedRadioId = speedRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedRadioId);
        String speed = selectedRadioButton.getText().toString();
        if (!speed.isEmpty()) {
            editor.putString(SPEED_KEY, speed);
        }

        editor.commit();
    }
}
