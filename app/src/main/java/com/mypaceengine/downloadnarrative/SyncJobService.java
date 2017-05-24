package com.mypaceengine.downloadnarrative;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.StatFs;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SyncJobService extends JobService {

    List<JobTask> currentTaskList = null;
    List<AbstractJobN> jobList = null;
    List<GSPContainer> gpsList=null;
    Object syncObje = new Object();
    Object syncObjeTask = new Object();
    JobParameters mJobParam=null;
    DataUtil dataUtil=null;
    final static String DataFile = "DataFile.dat";
    final static String GPSFile = "GPSFile.dat";
    boolean isCanceled=false;
    static final int NOTIFICATION_ID=R.layout.activity_main;
    NotificationManager mNotificationManager;
    @Override
    public boolean onStartJob(JobParameters params) {
        mJobParam=params;
        dataUtil=new DataUtil(this);
        currentTaskList=new ArrayList<JobTask>();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if(chkEnableTask()) {
            loadGPSList();
            List<AbstractJobN> list = loadJobList();
            if ((list != null) && (list.size() > 0)) {
                execNextTask();
            } else {
                execFirstTask();
            }
        }else{
            showPendingNotification();
            registJobSchedule(this);
        }
        return true;
    }

    public void execFirstTask() {
        if(dataUtil.getNarrativeReauthNeed()){
            jobFinished(mJobParam, false);
        }else if(dataUtil.getEnableLocalSync()||(dataUtil.getEnableGoogleSync()&&(dataUtil.getGoogleAccount()!=null))) {
            if(this.chkEnableTask()) {
                showExecutingNotification();
                Log.d("NarrativeDwnService", "First Task Start");
                Job_LoadMoments firstTask = new Job_LoadMoments();
                firstTask.setInfo("https://narrativeapp.com/api/v2/moments/");
                this.addJob(firstTask);

                JobTask currentTask = new JobTask();
                currentTask.initialize(this);
                firstTask.preExecute(this);
//                currentTask.execute(firstTask, firstTask, firstTask);
                currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,firstTask, firstTask, firstTask);
                currentTaskList.add(currentTask);
            }else{
                stopExec();
            }
        }else{
            Log.d("NarrativeDwnService","First Task Not Start");
            jobFinished(mJobParam, false);
        }
    }

    public void execNextTask() {

        if(!isCanceled) {
            if(dataUtil.getNarrativeReauthNeed()) {
                jobFinished(mJobParam, false);
            }else {
                showExecutingNotification();
                if(dataUtil.getEnableLocalSync()||(dataUtil.getEnableGoogleSync()&&(dataUtil.getGoogleAccount()!=null))) {
                    if(this.chkEnableTask()) {
                        synchronized (syncObjeTask) {
                            int taskNum = currentTaskList.size();
                            List<AbstractJobN> currentTasks = new ArrayList<AbstractJobN>();
                            for (int i = 0; i < currentTaskList.size(); i++) {
                                currentTasks.add(currentTaskList.get(i).getJob());
                            }

                            List<AbstractJobN> nextJobs = pickupJob(currentTasks, 1 - taskNum);
                            if (nextJobs.size() > 0) {
                                Log.d("NarrativeDwnService", "Next Task Start");
                                for (int j = 0; j < nextJobs.size(); j++) {
                                    JobTask currentTask = new JobTask();
                                    currentTask.initialize(this);
                                    AbstractJobN nextJob = nextJobs.get(j);
                                    nextJob.preExecute(this);
//                                    currentTask.execute(nextJob, nextJob, nextJob);
                                    currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,nextJob, nextJob, nextJob);
                                    currentTaskList.add(currentTask);
                                }
                            } else if (currentTaskList.size() == 0) {
                                Log.d("NarrativeDwnService", "Next Task Not Start (Empty)");
                                jobFinished(mJobParam, false);
                            }
                        }
                    }else{
                        stopExec();
                    }
                } else {
                    Log.d("NarrativeDwnService", "Next Task Not Start (OFF)");
                    jobFinished(mJobParam, false);
                }
            }
        }
    }
    public void removeTask(JobTask currentTask){
        synchronized (syncObjeTask) {
            currentTaskList.remove(currentTask);
        }
    }
    public boolean chkEnableTask(){

        StatFs sf = new StatFs(this.getApplicationContext().getExternalFilesDir(Conf.TempolaryFolderName).getAbsolutePath());
        long kb = sf.getFreeBytes();
        Log.d("Storage Free Capacity",  kb+"byte");

        String key=dataUtil.getNarrativeKey();
        boolean reauthNeed=dataUtil.getNarrativeReauthNeed();
        Log.d("Narrative","ID:"+key+" ReauthNeed:"+ reauthNeed);

        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        if(nInfo!=null)
            Log.d("Network", "Connected:"+nInfo.isConnected()+" TYPE:"+ nInfo.getTypeName());


        reauthNeed=false;
        return
                (key!=null)&&
                        (kb>1024*1024*1024)&&
                        ((nInfo!=null)&&(nInfo.isConnected())&&((nInfo.getType()==ConnectivityManager.TYPE_WIFI)||dataUtil.getEnableCelSync()));

    }

    private List<AbstractJobN> loadJobList() {
        synchronized (syncObje) {
            ObjectInputStream objectInputstream = null;
            FileInputStream fileInputstream = null;
            try {
                fileInputstream = openFileInput(DataFile);
                objectInputstream = new ObjectInputStream(fileInputstream);
                jobList = (ArrayList<AbstractJobN>) objectInputstream.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (objectInputstream != null) {
                        objectInputstream.close();
                    }
                    if (fileInputstream != null) {
                        fileInputstream.close();
                    }
                } catch (Exception ex) {

                }
            }
            if (jobList == null) {
                jobList = new ArrayList<AbstractJobN>();
            }
        }
        return jobList;
    }

    void saveJobList() {
        if(!isCanceled) {
            synchronized (syncObje) {
                if (jobList != null) {
                    ObjectOutputStream objectOutputstream = null;
                    FileOutputStream fileOutputStream = null;
                    try {
                        fileOutputStream = openFileOutput(DataFile, Context.MODE_PRIVATE);
                        objectOutputstream = new ObjectOutputStream(fileOutputStream);
                        objectOutputstream.writeObject(jobList);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (objectOutputstream != null) {
                                objectOutputstream.close();
                            }
                            if (fileOutputStream != null) {
                                fileOutputStream.close();
                            }
                        } catch (Exception ex) {

                        }
                    }
                }
            }
        }
    }

    void clearJobList() {
        synchronized (syncObje) {
            try {
                this.deleteFile(SyncJobService.DataFile);
            }catch (Exception ex){}
        }
        jobList = new ArrayList<AbstractJobN>();
    }

    public boolean isAvailableJobList(){
        boolean result=false;
        synchronized (syncObje) {
            if((jobList!=null)&&(jobList.size()>0)){
                result=true;
            }
        }
        return result;
    }
    public AbstractJobN pickupJob(){
        AbstractJobN result=null;
        synchronized (syncObje) {
            if(isAvailableJobList()) {
                result = jobList.get(0);
            }
        }
        return result;
    }
    public List<AbstractJobN> pickupJob(List<AbstractJobN> ignoreList,int num){
        ArrayList<AbstractJobN> result = new ArrayList<AbstractJobN>();
        synchronized (syncObje) {
            for (int i = 0; i < jobList.size(); i++) {
                if (result.size() >= num) {
                    break;

                }
                boolean flag = true;
                for (int j = 0; j < ignoreList.size(); j++) {
                    if (jobList.get(i) == ignoreList.get(j)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    result.add(jobList.get(i));
                }
            }

        }
        return result;
    }
    public void addJob(AbstractJobN job) {
        List<AbstractJobN> jobs=new ArrayList<AbstractJobN>();
        jobs.add(job);
        addJobs(jobs);
    }
    public void addJobs(List<AbstractJobN> jobs) {
        if(!isCanceled) {
            synchronized (syncObje) {
                jobList.addAll(jobs);
            }
        }
    }
    public void addJobFirst(AbstractJobN job) {
        List<AbstractJobN> jobs=new ArrayList<AbstractJobN>();
        jobs.add(job);
        addJobFirst(jobs);
    }
    public void addJobFirst(List<AbstractJobN> jobs) {
        if(!isCanceled) {
            synchronized (syncObje) {
                jobList.addAll(1,jobs);
            }
        }
    }
    public void removeJob(AbstractJobN job) {
        synchronized (syncObje) {
            jobList.remove(job);
        }
    }
    private List<GSPContainer> loadGPSList() {
        synchronized (syncObje) {
            ObjectInputStream objectInputstream = null;
            FileInputStream fileInputstream = null;
            try {
                fileInputstream = openFileInput(GPSFile);
                objectInputstream = new ObjectInputStream(fileInputstream);
                gpsList = (List<GSPContainer>) objectInputstream.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (objectInputstream != null) {
                        objectInputstream.close();
                    }
                    if (fileInputstream != null) {
                        fileInputstream.close();
                    }
                } catch (Exception ex) {

                }
            }
            if (gpsList == null) {
                gpsList = new ArrayList<GSPContainer>();
            }
        }
        return gpsList;
    }
    List<GSPContainer> getGPSList() {
        return gpsList;
    }
    void addGPSData(GSPContainer data){
        if(gpsList!=null){
            gpsList.add(data);
        }
    }
    void saveGPSList() {
        if(!isCanceled) {
            synchronized (syncObje) {
                if (jobList != null) {
                    ObjectOutputStream objectOutputstream = null;
                    FileOutputStream fileOutputStream = null;
                    try {
                        fileOutputStream = openFileOutput(GPSFile, Context.MODE_PRIVATE);
                        objectOutputstream = new ObjectOutputStream(fileOutputStream);
                        objectOutputstream.writeObject(gpsList);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (objectOutputstream != null) {
                                objectOutputstream.close();
                            }
                            if (fileOutputStream != null) {
                                fileOutputStream.close();
                            }
                        } catch (Exception ex) {

                        }
                    }
                }
            }
        }
    }

    public static void registJobSchedule(Context context){
        DataUtil dataUtil=new DataUtil(context);
        int netType= JobInfo.NETWORK_TYPE_UNMETERED;
        if(dataUtil.getEnableCelSync()){
            netType= JobInfo.NETWORK_TYPE_ANY;
        }
        ComponentName mServiceName= new ComponentName(context, SyncJobService.class);
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(1);
        JobInfo jobInfo = new JobInfo.Builder(1, mServiceName)
                .setMinimumLatency(1 * 1000)
                .setOverrideDeadline(60 * 1000)
                .setRequiredNetworkType(netType)
                .setPersisted(true)
                .setRequiresDeviceIdle(true)
                .setRequiresCharging(true)
                .build();
        scheduler.schedule(jobInfo);
    }

    public void stopExec(){
        synchronized (syncObjeTask) {
            List<AbstractJobN> currentTasks = new ArrayList<AbstractJobN>();
            for (int i = 0; i < currentTaskList.size(); i++) {
                currentTaskList.get(i).cancel(true);
            }
        }
        boolean result=isAvailableJobList();
        if(result){
            showPendingNotification();
            try {
                this.saveJobList();
            }catch (Exception ex){
                ex.printStackTrace();
            }
            jobFinished(mJobParam, false);
            registJobSchedule(this);
        }else{
            showEndNotification();
            clearJobList();
            jobFinished(mJobParam, false);
            JobScheduler scheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.cancel(1);
        }
        try {
            saveGPSList();
        }catch (Exception ex){
            ex.printStackTrace();
        }

        isCanceled=true;

    }
    @Override
    public boolean onStopJob(JobParameters params) {
        stopExec();
        if(dataUtil.getNarrativeReauthNeed()) {
            showReqestNarrativeAuthNotification();
        }
        return false;
    }
    public void showExecutingNotification(){
        Calendar calendar = Calendar.getInstance();
        showNotification("Sync is Executing","Last Updating:"+calendar.getTime().toString());
    }
    public void showEndNotification(){
        Calendar calendar = Calendar.getInstance();
        showNotification("Sync is Completed.","Last Updating:"+calendar.getTime().toString());
    }
    public void showPendingNotification(){
        Calendar calendar = Calendar.getInstance();
        showNotification("Sync is Pending.","Last Updating:"+calendar.getTime().toString());
    }
    public void showReqestNarrativeAuthNotification(){
        Calendar calendar = Calendar.getInstance();
        showNotification("Please reauthenticate to Narrative Service","Last Updating:"+calendar.getTime().toString());
    }

    NotificationCompat.Builder  notificationBuilder=null;
    public void showNotification(String headText,String text) {

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
}
