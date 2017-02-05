package com.mypaceengine.downloadnarrative;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SyncService extends IntentService {

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        SyncService getService() {
            return SyncService.this;
        }
    }
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "photodwn4narrative.photodwn.narrative.mypaceengine.com.downloadnarrative.action.FOO";
    private static final String ACTION_BAZ = "photodwn4narrative.photodwn.narrative.mypaceengine.com.downloadnarrative.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "photodwn4narrative.photodwn.narrative.mypaceengine.com.downloadnarrative.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "photodwn4narrative.photodwn.narrative.mypaceengine.com.downloadnarrative.extra.PARAM2";

    static final int NOTIFICATION_ID=R.layout.activity_main;
    public SyncService() {
        super("SyncService");

    }
    NotificationManager mNotificationManager;
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
        registerFirstTask();
        Intent intent =new Intent(this,SyncTask.class);
        PendingIntent pendingIntent=PendingIntent.getService(this,-1,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alermManager=(AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        alermManager.setInexactRepeating(AlarmManager.RTC,System.currentTimeMillis(),5000,pendingIntent);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        //サービスの停止時、通知内容を破棄する
        mNotificationManager.cancel(NOTIFICATION_ID);
        startService(new Intent(this,SyncService.class));
    }
    @Override
    public int onStartCommand(Intent intent,int flags,int startid){
        return START_STICKY;
    }
    NotificationCompat.Builder  notificationBuilder=null;
    public void showNotification() {

        String headText="";
        String text="";
        if(task!=null) {
            if(task.shutdownFlg) {
                headText = "Wait...";
                text = "";
            }else if(!task.isDoing()){
                headText="Wait...";
                text="";
            }else {
                headText="Downloading... ("+task.parcent+"%)";
                ArrayList<JSONObject> momentArr = this.task.momentArr;
 //               ArrayList photoListArr = this.task.photoListArr;
                ArrayList<JSONObject> videoArr = this.task.videoArr;
                int progressMoment = 0;
                int progressVideo = 0;
                int progressPhotoList = 0;
                if (momentArr != null) {
                    progressMoment = momentArr.size();
                }
   //             if (photoListArr != null) {
   //                 progressPhotoList = photoListArr.size();
   //             }
                if (videoArr != null) {
                    progressVideo = videoArr.size();
                }
                text=
                        "Moment: "+task.count_momentList+"/"+progressMoment+" "+
     //                   "Photo: "+task.count_moment+"/"+progressPhotoList+"
                                "Photo: "+task.photo_count+" "+
                                "Video: "+task.count_video+"/"+progressVideo+" ";
            }
        }else{
            mNotificationManager.cancel(NOTIFICATION_ID);
            return;
        }

        // RecieverからMainActivityを起動させる
        Intent intent2 = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, intent2, 0);

        if(notificationBuilder==null) {
            notificationBuilder = new NotificationCompat.Builder(this);
        }
                    Notification notification = notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
//                .setTicker("時間です")
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(headText)
                    .setContentText(text)
                    // 音、バイブレート、LEDで通知
                    //               .setDefaults(Notification.DEFAULT_ALL)
                    // 通知をタップした時にMainActivityを立ち上げる
                    .setContentIntent(pendingIntent)
                    .build();
        // 古い通知を削除
//        mNotificationManager.cancelAll();
        // 通知
        mNotificationManager.notify(R.string.app_name, notification);
        startForeground(R.string.app_name, notification);
    }
    public void registerFirstTask() {
        Log.d("MainService","First Task Register");
        this.mainTimer = new Timer();
        //タスククラスインスタンス生成
        this.mainTimerTask = new MainTimerTask();
        //タイマースケジュール設定＆開始
        this.mainTimer.schedule(mainTimerTask, 1000);
    }

    SyncTask task=null;
    private Timer mainTimer;					//タイマー用
    private MainTimerTask mainTimerTask;
    public void registerNextTask(){
        if(mainTimer!=null){
            mainTimer.cancel();
        }
        if(mainTimerTask!=null) {
            mainTimerTask.cancel();
        }
        if((task!=null)&&(task.shutdownFlg)){
            task=null;
        }
        if((task!=null)&&(task.allTaskCompleteFlg)){
            task=null;
        }
        //タイマーインスタンス生成
        this.mainTimer = new Timer();
        //タスククラスインスタンス生成
        this.mainTimerTask = new MainTimerTask();
        //タイマースケジュール設定＆開始
        this.mainTimer.schedule(mainTimerTask, 300000);
       // this.mainTimer.schedule(mainTimerTask, 1000);
    }
    public class MainTimerTask extends TimerTask {
        @Override
        public void run() {
            executeTask();
        }
    }
    public void executeTask(){
        Log.d("MainService","Task Start");
        if(task!=null){
            if(task.allTaskCompleteFlg) {
                task.setShutdown();
                task = null;
            }else if(task.shutdownFlg){
                task=null;
//            }else if(!task.isDoing()){
//                registerNextTask();
//                Log.d("MainService","Reschedule Task!");
//                return;
            }
        }
        if(getEnableLocalSync()||(getEnableGoogleSync()&&(getGoogleAccount()!=null))) {
            if (task == null) {
                if (getNarrativeKey() != null) {
                    task = new SyncTask();
                    task.initializeTask(this);
                }
            }
            if((task!=null)&&(!task.isDoing())) {
                Log.d("Service","Start");
                task.startExecution();
            }
        }else{
            Log.d("Service","Not Start");
        }
    }

    public void resetTask(){
        if(task!=null){
            if(!getEnableGoogleSync()&&!getEnableLocalSync()) {

                task.setShutdown();
                task=null;
            }

        }
        if(getEnableGoogleSync()||getEnableLocalSync()) {
            this.registerFirstTask();
        }
    }


    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    final static String NARRATIVE_KEY_CODE="NARRATIVE_KEY_CODE";
    final static String NARRATIVE_KEY_CREATE_TIME="NARRATIVE_KEY_CREATE_TIME";
    final static String NARRATIVE_REAUTH_REQUIRE="NARRATIVE_REAUTH_REQUIRE";
    final static String GOOGLE_ACCOUNT="GOOGLE_ACCOUNT";
    final static String GOOGLE_VALIDATION_TIME="GOOGLE_VALIDATION_TIME";
    final static String GOOGLE_SYNC_FLAG="GOOGLE_SYNC_FLAG";
    final static String LOCAL_SYNC_FLAG="LOCAL_SYNC_FLAG";
    final static String NAS_SYNC_FLAG="NAS_SYNC_FLAG";
    final static String CEL_SYNC_FLAG="CEL_SYNC_FLAG";

    public final static String NARRATIVE_COOKIE="NARRATIVE_COOKIE";

    public void setNarrativeKey(String key){
        saveString(NARRATIVE_KEY_CODE,key);
        saveLong(NARRATIVE_KEY_CREATE_TIME, System.currentTimeMillis());
    }
    public String getNarrativeKey(){
       return loadString(NARRATIVE_KEY_CODE,null);
    }
    public long getNarrativeKey_CreateTime(){
        return loadLong(NARRATIVE_KEY_CREATE_TIME,-1);
    }

    public void setGoogleAccounty(String account){
        saveString(GOOGLE_ACCOUNT,account);
        saveLong(GOOGLE_VALIDATION_TIME, System.currentTimeMillis());
    }

    public String getGoogleAccount(){
        return loadString(GOOGLE_ACCOUNT,null);
    }
    public long getGoogleValidationTime(){
        return loadLong(GOOGLE_VALIDATION_TIME,-1);
    }

    public void setEnableGoogleSync(boolean flag){
        saveBoolean(GOOGLE_SYNC_FLAG,flag);
    }
    public void setEnableLocalSync(boolean flag){
        saveBoolean(LOCAL_SYNC_FLAG,flag);
    }
    public void setEnableNasSync(boolean flag){
        saveBoolean(NAS_SYNC_FLAG,flag);
    }
    public void setEnableCelSync(boolean flag){
        saveBoolean(CEL_SYNC_FLAG,flag);
    }
    public void setNarrativeReauthNeed(boolean flag){
        saveBoolean(NARRATIVE_REAUTH_REQUIRE,flag);
    }
    public void setNarrativeCookie(ArrayList<String> list){
        saveList(NARRATIVE_COOKIE,list);
    }

    public boolean getEnableGoogleSync(){
        return loadBoolean(GOOGLE_SYNC_FLAG,false);
    }
    public boolean getEnableLocalSync(){
        return loadBoolean(LOCAL_SYNC_FLAG,false);
    }
    public boolean getEnableNasSync(){
        return loadBoolean(NAS_SYNC_FLAG,false);
    }
    public boolean getEnableCelSync(){
        return loadBoolean(CEL_SYNC_FLAG,false);
    }
    public boolean getNarrativeReauthNeed(){
        return loadBoolean(NARRATIVE_REAUTH_REQUIRE,true);
    }
    public ArrayList<String> getNarrativeCookie(){
        return loadList(NARRATIVE_COOKIE);
    }
    SharedPreferences pref;
    public SharedPreferences getPreference(){
        if(pref==null) {
            pref= getSharedPreferences("settings", Context.MODE_PRIVATE);
        }
        return pref;
    }

    public void saveString(String key,String value){
        synchronized (this){
            SharedPreferences.Editor editor =  getPreference().edit();
            editor.putString(key,value);
            editor.apply();
        }
    }

    public void saveLong(String key,long value){
        synchronized (this){
            SharedPreferences.Editor editor =  getPreference().edit();
            editor.putLong(key,value);
            editor.apply();
        }
    }
    public void saveBoolean(String key,boolean value){
        synchronized (this){
            SharedPreferences.Editor editor =  getPreference().edit();
            editor.putBoolean(key,value);
            editor.apply();
        }
    }
    public void saveList(String key,List<String> list){
        synchronized (this){
            JSONArray array = new JSONArray();
            for (int i = 0, length = list.size(); i < length; i++) {
                try {
                    array.put(i, list.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            saveString(key,array.toString());
        }
    }

    public boolean loadBoolean(String key,boolean defaultValue){
        synchronized (this){
            return getPreference().getBoolean(key,defaultValue);
        }
    }
    public String loadString(String key,String defaultValue){
        synchronized (this){
            return getPreference().getString(key,defaultValue);
        }
    }
    public long loadLong(String key,long defaultValue){
        synchronized (this){
            return getPreference().getLong(key,defaultValue);
        }
    }
    public ArrayList<String> loadList(String key){
        synchronized (this) {
            String stringList = loadString(key, null);
            ArrayList<String> list = new ArrayList<String>();
            try {
                if (stringList != null) {
                    JSONArray array = new JSONArray(stringList);
                    for (int i = 0, length = array.length(); i < length; i++) {
                        list.add(array.optString(i));
                    }
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            return list;
        }
    }
}
