package org.steelsquid.alarmarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Browser;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Implementation of App Widget functionality.
 */
public class AlarmArmWidget extends AppWidgetProvider {
    public static AlarmArmWidget Widget = null;
    public static Context context;
    public static AppWidgetManager AWM;
    public static int IDs[];

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (null == context) context = AlarmArmWidget.context;
        if (null == appWidgetManager) appWidgetManager = AlarmArmWidget.AWM;
        if (null == appWidgetIds) appWidgetIds = AlarmArmWidget.IDs;
        AlarmArmWidget.Widget = this;
        AlarmArmWidget.context = context;
        AlarmArmWidget.AWM = appWidgetManager;
        AlarmArmWidget.IDs = appWidgetIds;

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }


    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.alarm_arm_widget);
        SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        boolean enabled = sharedPref.getBoolean(MainActivity.PREFERENCE_ENABLED, false);


        if(enabled) {
            views.setTextColor(R.id.textViewWidgetStatus, context.getResources().getColor(R.color.enable));
            views.setTextViewText(R.id.textViewWidgetStatus, "Enabled");
        }
        else{
            views.setTextColor(R.id.textViewWidgetStatus, context.getResources().getColor(R.color.disable));
            views.setTextViewText(R.id.textViewWidgetStatus, "Disabled");
        }
        if(BackgroundService.isArmed()!=null) {
            if (BackgroundService.isArmed()) {
                views.setTextColor(R.id.textViewWidgetArm, context.getResources().getColor(R.color.arm));
                views.setTextViewText(R.id.textViewWidgetArm, "Armed");
                views.setTextViewText(R.id.textViewWidgetTime, BackgroundService.getLastCheck());
            } else {
                views.setTextColor(R.id.textViewWidgetArm, context.getResources().getColor(R.color.disarm));
                views.setTextViewText(R.id.textViewWidgetArm, "Disarmed");
                views.setTextViewText(R.id.textViewWidgetTime, BackgroundService.getLastCheck());
            }
        }

        views.setOnClickPendingIntent(R.id.buttonWidgetArm, getPendingSelfIntent(context, "buttonWidgetArm"));
        views.setOnClickPendingIntent(R.id.buttonWidgetBrowser, getPendingSelfIntent(context, "buttonWidgetBrowser"));
        views.setOnClickPendingIntent(R.id.buttonWidgetDisable, getPendingSelfIntent(context, "buttonWidgetDisable"));
        views.setOnClickPendingIntent(R.id.buttonWidgetDisarm, getPendingSelfIntent(context, "buttonWidgetDisarm"));
        views.setOnClickPendingIntent(R.id.buttonWidgetEnable, getPendingSelfIntent(context, "buttonWidgetEnable"));
        views.setOnClickPendingIntent(R.id.buttonWidgetSettings, getPendingSelfIntent(context, "buttonWidgetSettings"));


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    class ArmTask extends AsyncTask<String, Void, Exception> {
        private ProgressDialog dialog;
        protected void onPreExecute() {
        }
        protected Exception doInBackground(String... ipUserPasswordStatus) {
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpParams params = httpClient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, 10000);
                HttpConnectionParams.setSoTimeout(params, 10000);

                String ip=ipUserPasswordStatus[0];
                String user=ipUserPasswordStatus[1];
                String password=ipUserPasswordStatus[2];
                String statusString=ipUserPasswordStatus[3];

                HttpPost httpPost = new HttpPost(ip+"/alarm_app_arm%7c"+statusString);

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
                Log.e("ArmTask", "ArmTask", e);
            }
            return null;
        }
        protected void onPostExecute(Exception error) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        String ip = sharedPref.getString(MainActivity.PREFERENCE_IP, null);
        String user = sharedPref.getString(MainActivity.PREFERENCE_USER, null);
        String password = sharedPref.getString(MainActivity.PREFERENCE_PASSWORD, null);
        if(ip==null || user==null || password==null){
            Intent myIntent = new Intent(context, MainActivity.class);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);
        }
        else if(intent.getAction().equals("buttonWidgetSettings")) {
            Intent myIntent = new Intent(context, MainActivity.class);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);
        }
        else if(intent.getAction().equals("buttonWidgetBrowser")) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ip+"/utils?alarm"));
            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            browserIntent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            String authorization = user + ":" + password;
            String authorizationBase64 = Base64.encodeToString(authorization.getBytes(), 0);
            Bundle bundle = new Bundle();
            bundle.putString("Authorization", "Basic " + authorizationBase64);
            browserIntent.putExtra(Browser.EXTRA_HEADERS, bundle);
            context.startActivity(browserIntent);
        }
        else if(intent.getAction().equals("buttonWidgetArm")) {
            new ArmTask().execute(ip, user, password, "True");
        }
        else if(intent.getAction().equals("buttonWidgetDisarm")) {
            new ArmTask().execute(ip, user, password, "False");
        }
        else if(intent.getAction().equals("buttonWidgetEnable")) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(MainActivity.PREFERENCE_ENABLED, true);
            editor.commit();
            context.startService(new Intent(context, BackgroundService.class));
            if (null != AlarmArmWidget.Widget) AlarmArmWidget.Widget.onUpdate(null, null, null);
        }
        else if(intent.getAction().equals("buttonWidgetDisable")) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(MainActivity.PREFERENCE_ENABLED, false);
            editor.commit();
            context.startService(new Intent(context, BackgroundService.class));
            if (null != AlarmArmWidget.Widget) AlarmArmWidget.Widget.onUpdate(null, null, null);
        }


    }


    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}

