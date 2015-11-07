package org.steelsquid.alarmarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.util.List;

public class MainActivity extends ActionBarActivity implements LocationListener {
    public static final String SHARED_PREFERENCE_KEY = "org.steelsquid.alarmarm";
    public static final String PREFERENCE_IP = "PREFERENCE_IP";
    public static final String PREFERENCE_USER = "PREFERENCE_USER";
    public static final String PREFERENCE_PASSWORD = "PREFERENCE_PASSWORD";
    public static final String PREFERENCE_METERS = "PREFERENCE_METERS";
    public static final String PREFERENCE_LAT = "PREFERENCE_LAT";
    public static final String PREFERENCE_LONG = "PREFERENCE_LONG";
    public static final String PREFERENCE_WIFI = "PREFERENCE_WIFI";
    public static final String PREFERENCE_ENABLED = "PREFERENCE_ENABLED";

    private SharedPreferences sharedPref = null;
    private LocationManager locationManager = null;
    public WifiManager mWifiManager;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, BackgroundService.class);
        this.startService(intent);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mWifiManager =(WifiManager)getSystemService(Context.WIFI_SERVICE);
        sharedPref = this.getSharedPreferences(SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        EditText editTextIp = (EditText)findViewById(R.id.editTextIp);
        EditText editTextUser = (EditText)findViewById(R.id.editTextUser);
        EditText editTextPassword = (EditText)findViewById(R.id.editTextPassword);
        EditText editTextMeters = (EditText)findViewById(R.id.editTextMeters);
        EditText editTextLat = (EditText)findViewById(R.id.editTextLat);
        EditText editTextLong = (EditText)findViewById(R.id.editTextLong);
        TextView textViewWifi = (TextView)findViewById(R.id.textViewWifi);
        editTextIp.setText(sharedPref.getString(PREFERENCE_IP, "http://<IP>"));
        editTextUser.setText(sharedPref.getString(PREFERENCE_USER, "<USERNAME>"));
        editTextPassword.setText(sharedPref.getString(PREFERENCE_PASSWORD, "<PASSWORD>"));
        editTextMeters.setText(sharedPref.getString(PREFERENCE_METERS, "100"));
        editTextLat.setText(sharedPref.getString(PREFERENCE_LAT, "<Latitude>"));
        editTextLong.setText(sharedPref.getString(PREFERENCE_LONG, "<Longitude>"));
        textViewWifi.setText(sharedPref.getString(PREFERENCE_WIFI, "No home network selected"));

        if(sharedPref.getString(PREFERENCE_METERS, null)==null){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(PREFERENCE_METERS, "100");
            editor.commit();
        }

        boolean enabled = sharedPref.getBoolean(PREFERENCE_ENABLED, false);
        setAlarmArmStatus(enabled, true);

        editTextIp.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(!sharedPref.getString(PREFERENCE_IP, "").equals(s.toString())) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREFERENCE_IP, s.toString());
                    editor.commit();
                    setAlarmArmStatus(false, false);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        editTextUser.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(!sharedPref.getString(PREFERENCE_USER, "").equals(s.toString())) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREFERENCE_USER, s.toString());
                    editor.commit();
                    setAlarmArmStatus(false, false);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        editTextPassword.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(!sharedPref.getString(PREFERENCE_PASSWORD, "").equals(s.toString())) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREFERENCE_PASSWORD, s.toString());
                    editor.commit();
                    setAlarmArmStatus(false, false);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        editTextMeters.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(!sharedPref.getString(PREFERENCE_METERS, "").equals(s.toString())) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREFERENCE_METERS, s.toString());
                    editor.commit();
                    setAlarmArmStatus(false, false);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        editTextLat.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(!sharedPref.getString(PREFERENCE_LAT, "").equals(s.toString())) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREFERENCE_LAT, s.toString());
                    editor.commit();
                    setAlarmArmStatus(false, false);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        editTextLong.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(!sharedPref.getString(PREFERENCE_LONG, "").equals(s.toString())) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(PREFERENCE_LONG, s.toString());
                    editor.commit();
                    setAlarmArmStatus(false, false);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void onGetLocation(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager)  this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        locationManager.requestSingleUpdate(criteria, this, null);
        dialog=ProgressDialog.show(MainActivity.this, "", "Please wait!");
    }

    public void onWifi(View view) {
        List<WifiConfiguration> netConfList = mWifiManager.getConfiguredNetworks();
        final CharSequence wlist[] = new CharSequence[netConfList.size()];
        int i =0;
        for (WifiConfiguration wifi: netConfList){
            wlist[i]=wifi.SSID.replace("\"", "");
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a wifi");
        builder.setItems(wlist, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setAlarmArmStatus(false, false);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(PREFERENCE_WIFI, wlist[which].toString());
                editor.commit();
                TextView textViewWifi = (TextView) findViewById(R.id.textViewWifi);
                textViewWifi.setText(wlist[which].toString());
            }
        });
        builder.show();
    }

    public void onEnable(View view) {
        boolean enabled = sharedPref.getBoolean(PREFERENCE_ENABLED, false);
        if(enabled){
            setAlarmArmStatus(false, true);
        }
        else {
            try {
                InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            }
            catch(Exception ee){

            }
            String ip = sharedPref.getString(PREFERENCE_IP, null);
            String user = sharedPref.getString(PREFERENCE_USER, null);
            String password = sharedPref.getString(PREFERENCE_PASSWORD, null);
            String meters = sharedPref.getString(PREFERENCE_METERS, null);
            String lat = sharedPref.getString(PREFERENCE_LAT, null);
            String longi = sharedPref.getString(PREFERENCE_LONG, null);
            if (ip == null || user == null || password == null || meters == null || lat == null || longi == null) {
                new AlertDialog.Builder(this).setMessage("Fill out all the fields above!").setPositiveButton("OK", null).show();
            } else if (ip.equals("<IP>")) {
                new AlertDialog.Builder(this).setMessage("Fill out all the fields above!").setPositiveButton("OK", null).show();
            } else if (user.equals("<Username>")) {
                new AlertDialog.Builder(this).setMessage("Fill out all the fields above!").setPositiveButton("OK", null).show();
            } else if (password.equals("<Password>")) {
                new AlertDialog.Builder(this).setMessage("Fill out all the fields above!").setPositiveButton("OK", null).show();
            } else if (!isInteger(meters)) {
                new AlertDialog.Builder(this).setMessage("Meters must be a number!").setPositiveButton("OK", null).show();
            } else if (!isDouble(lat) || !isDouble(longi)) {
                new AlertDialog.Builder(this).setMessage("Latitude and/or Longitude errror!").setPositiveButton("OK", null).show();
            } else {
                new TestConnectionTask().execute(ip, user, password);
            }
        }
    }

    public void setAlarmArmStatus(boolean enable, boolean hideKeyboard) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREFERENCE_ENABLED, enable);
        editor.commit();
        TextView textViewStatus = (TextView) findViewById(R.id.textViewWidgetStatus);
        Button buttonEnable = (Button) findViewById(R.id.buttonWidgetEnable);
        if(enable) {
            textViewStatus.setTextColor(getResources().getColor(R.color.ok));
            textViewStatus.setText("Alarm Arm is Enabled");
            buttonEnable.setText("Disable Alarm Arm");
            Intent intent = new Intent(this, BackgroundService.class);
            this.startService(intent);
        }
        else{
            textViewStatus.setTextColor(getResources().getColor(R.color.error));
            textViewStatus.setText("Alarm Arm is Disabled");
            buttonEnable.setText("Enable Alarm Arm");
            Intent intent = new Intent(this, BackgroundService.class);
            this.stopService(intent);
        }
        if(hideKeyboard) {
            try {
                InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            } catch (Exception ee) {

            }
        }
        if (null != AlarmArmWidget.Widget) AlarmArmWidget.Widget.onUpdate(null, null, null);
    }


    class TestConnectionTask extends AsyncTask<String, Void, Exception> {
        private ProgressDialog dialog;
        protected void onPreExecute() {
            dialog=ProgressDialog.show(MainActivity.this, "", "Please wait!");
        }
        protected Exception doInBackground(String... ipUserPassword) {
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpParams params = httpClient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, 10000);
                HttpConnectionParams.setSoTimeout(params, 10000);
                HttpPost httpPost = new HttpPost(ipUserPassword[0]+"/alarm_app");

                httpPost.addHeader(BasicScheme.authenticate(
                        new UsernamePasswordCredentials((String) ipUserPassword[1], (String) ipUserPassword[2]),
                        "UTF-8", false));

                ResponseHandler<String> responseHandler=new BasicResponseHandler();
                String answer = httpClient.execute(httpPost, responseHandler);
                if(answer.equals("True")){
                    BackgroundService.isArmed(true);
                }
                else{
                    BackgroundService.isArmed(false);
                }
            } catch(Exception e){
                return e;
            }
            return null;
        }
        protected void onPostExecute(Exception error) {
            try{
                dialog.dismiss();
            }
            catch(Exception e){

            }
            if(error==null){
                MainActivity.this.setAlarmArmStatus(true, true);
            }
            else{
                new AlertDialog.Builder(MainActivity.this).setMessage(error.toString()).setPositiveButton("OK", null).show();
                MainActivity.this.setAlarmArmStatus(false, true);
            }
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        dialog.dismiss();
        TextView editTextLat = (TextView) findViewById(R.id.editTextLat);
        TextView editTextLong = (TextView) findViewById(R.id.editTextLong);
        editTextLat.setText(String.valueOf(location.getLatitude()));
        editTextLong.setText(String.valueOf(location.getLongitude()));
    }

    @Override
    public void onProviderDisabled(String provider) {
        Intent i = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(i);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public void onHelpServer(View view) {
        StringBuilder sb = new StringBuilder();
        sb.append("The address, user and password to the Steelsquid Kiss OS alarm device.");
        new AlertDialog.Builder(this).setMessage(sb.toString()).setPositiveButton("OK", null).show();
    }

    public void onHelpDistance(View view) {
        StringBuilder sb = new StringBuilder();
        sb.append("If this phone is further away than this the location below alarm will arm.\n");
        sb.append("And if it is closer it will disarm the alarm.");
        new AlertDialog.Builder(this).setMessage(sb.toString()).setPositiveButton("OK", null).show();
    }

    public void onHelpLocation(View view) {
        StringBuilder sb = new StringBuilder();
        sb.append("The location of you Steelsquid Kiss OS alarm device.");
        new AlertDialog.Builder(this).setMessage(sb.toString()).setPositiveButton("OK", null).show();
    }

    public void onHelpWifi(View view) {
        StringBuilder sb = new StringBuilder();
        sb.append("If you ar connected to this wifi disarm the device.\n");
        sb.append("This is important to fill in this otherwise, it will draw much power from your phone.\n");
        sb.append("When this is in use the app do not need to check you gps.");
        new AlertDialog.Builder(this).setMessage(sb.toString()).setPositiveButton("OK", null).show();
    }

    public void onHelpActivate(View view) {
        StringBuilder sb = new StringBuilder();
        sb.append("When actvated the phone will automatically arm and disarm depending on you location and wifi connection.\n");
        sb.append("If you are close to you home and not connected to your wifi for a long time it can be wise to disable or the battery will drain fast.");
        new AlertDialog.Builder(this).setMessage(sb.toString()).setPositiveButton("OK", null).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            locationManager.removeUpdates(this);
        }
        catch(Exception e){

        }
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        return true;
    }
    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        return true;
    }


}
