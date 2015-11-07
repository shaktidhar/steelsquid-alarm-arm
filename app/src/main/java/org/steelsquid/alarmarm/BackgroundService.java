package org.steelsquid.alarmarm;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * This wille xecute in the background to check if phone is away from the house
 */
public class BackgroundService extends Service  implements LocationListener, SensorEventListener {
    private static Boolean isDeviceArmed=null;
    private Handler handler;
    private SharedPreferences sharedPref = null;
    private LocationManager locationManager = null;
    private String androidId;
    private boolean enabled;
    private String ip;
    private String user;
    private String password;
    private int meters;
    private String wifi;
    private SendTask sendTask;
    private SensorManager sensorManager;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 40;
    private Boolean homeWifi=null;
    private Location homeLocation;
    private Boolean lastStatus = null;
    private Long lastDistance=null;
    private int movingCounter = 0;
    private boolean isMoving=true;
    private int updateCounter = 0;
    private boolean continueRear;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sharedPref = this.getSharedPreferences(MainActivity.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        handler = new Handler();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        enabled = sharedPref.getBoolean(MainActivity.PREFERENCE_ENABLED, false);
        if(enabled) {
            ip = sharedPref.getString(MainActivity.PREFERENCE_IP, null);
            user = sharedPref.getString(MainActivity.PREFERENCE_USER, null);
            password = sharedPref.getString(MainActivity.PREFERENCE_PASSWORD, null);
            wifi = sharedPref.getString(MainActivity.PREFERENCE_WIFI, null);
            meters = Integer.parseInt(sharedPref.getString(MainActivity.PREFERENCE_METERS, null));
            double latitude = Double.parseDouble(sharedPref.getString(MainActivity.PREFERENCE_LAT, null));
            double longitude = Double.parseDouble(sharedPref.getString(MainActivity.PREFERENCE_LONG, null));
            homeLocation = new Location("home");
            homeLocation.setLatitude(latitude);
            homeLocation.setLongitude(longitude);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            lastStatus = null;
            homeWifi=null;
            lastDistance=null;
            setGpsUpdate(true);
            handler.postDelayed(updater, 1);
        }
        else{
            try {
                locationManager.removeUpdates(this);
            }
            catch(Exception e){

            }
        }
        return Service.START_STICKY;
    }


    public synchronized void setGpsUpdate(boolean continueToRear) {
        long timeSeconds= 10;
        if(lastDistance!=null) {
            timeSeconds = lastDistance/33;
            if (timeSeconds<10){
                timeSeconds=10;
            }
            else if(timeSeconds>300){
                timeSeconds=300;
            }
        }
        continueRear=continueToRear;
        try {
            handler.removeCallbacks(locationUpdater);
        }
        catch(Exception e){

        }
        continueRear=continueToRear;
        handler.postDelayed(locationUpdater, timeSeconds*1000);
    }

    Runnable locationUpdater = new Runnable() {
        @Override
        public void run() {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            locationManager.requestSingleUpdate(criteria, BackgroundService.this, null);
        }
    };


    @Override
    public void onLocationChanged(Location location) {
        lastDistance = (long)homeLocation.distanceTo(location);
        boolean newStatus = lastDistance>meters;
        if(!continueRear || lastStatus==null || newStatus!=lastStatus){
            lastStatus = newStatus;
            sendLastStatus();
        }
        if(continueRear) {
            setGpsUpdate(true);
        }
    }


    Runnable updater = new Runnable() {
        @Override
        public void run() {
            if(wifi!=null){
                String currentWifi = getCurrentSsid();
                if(currentWifi!=null && currentWifi.equals(wifi)){
                    if(homeWifi==null || !homeWifi) {
                        stopGpsUpdate();
                        homeWifi=true;
                        lastStatus=false;
                        sendLastStatus();
                    }
                }
                else{
                    if(homeWifi==null|| homeWifi) {
                        setGpsUpdate(true);
                        homeWifi = false;
                    }
                }
            }
            if(!homeWifi){
                if(movingCounter == 12){
                    if(isMoving){
                        stopGpsUpdate();
                        isMoving=false;
                        lastStatus=false;
                        sendLastStatus();
                    }
                }
                else{
                    if(!isMoving) {
                        setGpsUpdate(true);
                        isMoving = true;
                    }
                    movingCounter++;
                }
            }
            if(updateCounter==30){
                updateCounter=0;
                setGpsUpdate(false);
            }
            else{
                updateCounter++;
            }
            try {
                handler.removeCallbacks(updater);
            }
            catch(Exception e){

            }
            handler.postDelayed(updater, 10000);
        }
    };


    public void sendLastStatus() {
        try {
            if (lastStatus != null) {
                if (sendTask == null || sendTask.getStatus() == AsyncTask.Status.FINISHED) {
                    sendTask = new SendTask();
                    sendTask.execute(lastStatus);
                }
            }
        }
        catch(Exception eee){

        }
    }

    class SendTask extends AsyncTask<Boolean, Void, Exception> {
        private ProgressDialog dialog;
        protected void onPreExecute() {
        }
        protected Exception doInBackground(Boolean... status) {
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpParams params = httpClient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, 10000);
                HttpConnectionParams.setSoTimeout(params, 10000);
                String statusString="False";
                if(status[0]){
                    statusString="True";
                }
                HttpPost httpPost = new HttpPost(ip+"/alarm_app%7c"+androidId+"%7c"+statusString);

                httpPost.addHeader(BasicScheme.authenticate(
                        new UsernamePasswordCredentials(user, password),
                        "UTF-8", false));

                ResponseHandler<String> responseHandler=new BasicResponseHandler();
                String answer = httpClient.execute(httpPost, responseHandler);
                if(answer.equals("True")){
                    BackgroundService.isArmed(true);
                }
                else{
                    BackgroundService.isArmed(false);
                }
                if (null != AlarmArmWidget.Widget) AlarmArmWidget.Widget.onUpdate(null, null, null);
            }
            catch(Exception e){
                return e;
            }
            return null;
        }
        protected void onPostExecute(Exception error) {
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;
                if (speed > SHAKE_THRESHOLD) {
                    movingCounter=0;
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }

    }

    public void stopGpsUpdate() {
        try {
            locationManager.removeUpdates(this);
        }
        catch(Exception e){
        }
        try {
            handler.removeCallbacks(locationUpdater);
        }
        catch(Exception e){
        }
    }


    @Override
    public void onDestroy() {
        stopGpsUpdate();
        try {
            sensorManager.unregisterListener(this);
        }
        catch(Exception e){
        }
        try {
            handler.removeCallbacks(updater);
        }
        catch(Exception e){

        }
    }



    public String getCurrentSsid() {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID().replace("\"", "");
            }
        }
        return ssid;
    }


    public static Boolean isArmed(){
        return isDeviceArmed;
    }
    public static void isArmed(boolean isArm){
        isDeviceArmed = isArm;
    }
}
