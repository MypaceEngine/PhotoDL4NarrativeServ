package com.mypaceengine.downloadnarrative;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.StatFs;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SyncJobService extends JobService {

    JobTask currentTask = null;
    List<AbstractJobN> jobList = null;
    List<GSPContainer> gpsList=null;
    Object syncObje = new Object();
    JobParameters mJobParam=null;
    DataUtil dataUtil=null;
    final static String DataFile = "DataFile.dat";
    final static String GPSFile = "GPSFile.dat";
    boolean isCanceled=false;
    @Override
    public boolean onStartJob(JobParameters params) {
        mJobParam=params;
        dataUtil=new DataUtil(this);
        if(chkEnableTask()) {
            loadGPSList();
            List<AbstractJobN> list = loadJobList();
            if ((list != null) && (list.size() > 0)) {
                execNextTask();
            } else {
                execFirstTask();
            }
        }
        return true;
    }

    public void execFirstTask() {
        if(dataUtil.getEnableLocalSync()||(dataUtil.getEnableGoogleSync()&&(dataUtil.getGoogleAccount()!=null))) {
            Log.d("NarrativeDwnService","First Task Start");
            Job_LoadMoments firstTask = new Job_LoadMoments();
            firstTask.setInfo("https://narrativeapp.com/api/v2/moments/");
            this.addJob(firstTask);
            Job_LoadVideos videoTask = new Job_LoadVideos();
            videoTask.setInfo("https://narrativeapp.com/api/v2/videos/");
            this.addJob(videoTask);

            currentTask = new JobTask();
            firstTask.preExecute(this);
            currentTask.execute(firstTask, firstTask, firstTask);
        }else{
            Log.d("NarrativeDwnService","First Task Not Start");
            jobFinished(mJobParam, false);
        }
    }

    public void execNextTask() {
        AbstractJobN nextJob =pickupJob();
        if(!isCanceled) {
            if (!dataUtil.getEnableGoogleSync() && !dataUtil.getEnableLocalSync()) {
                if (nextJob != null) {
                    Log.d("NarrativeDwnService", "Next Task Start");
                    currentTask = new JobTask();
                    nextJob.preExecute(this);
                    currentTask.execute(nextJob, nextJob, nextJob);
                } else {
                    Log.d("NarrativeDwnService", "Next Task Not Start (Empty)");
                    jobFinished(mJobParam, false);
                }
            } else {
                Log.d("NarrativeDwnService", "Next Task Not Start (OFF)");
                jobFinished(mJobParam, false);
            }
        }
    }
    public boolean chkEnableTask(){
        Intent batteryInfo = this.registerReceiver(
                null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryInfo.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int max = batteryInfo.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        Log.d("Battery : ", "Charged = " + level + "   Max" + max);
        int batteryPercent=(level*100)/max;

        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();

        StatFs sf = new StatFs(this.getApplicationContext().getExternalFilesDir(Conf.TempolaryFolderName).getAbsolutePath());
        long kb = sf.getFreeBytes();
        Log.d("Storage Free Capacity",  kb+"byte");
        if(nInfo!=null)
            Log.d("Network", "Connected:"+nInfo.isConnected()+" TYPE:"+ nInfo.getTypeName()+" CELLER_AVAILABLE:"+dataUtil.getEnableCelSync());
        String key=dataUtil.getNarrativeKey();
        boolean reauthNeed=dataUtil.getNarrativeReauthNeed();
        Log.d("Narrative","ID:"+key+" ReauthNeed:"+ reauthNeed);
        reauthNeed=false;
        return
                (key!=null)&&
                        (!reauthNeed)&&
                        (batteryPercent>0)&&
                        (nInfo!=null)&&(nInfo.isConnected())&&
                        ((nInfo.getType()==ConnectivityManager.TYPE_WIFI)||dataUtil.getEnableCelSync())&&
                        (kb>1024*1024*1024)&&
                        (dataUtil.getEnableLocalSync()||dataUtil.getEnableGoogleSync());
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
            this.deleteFile(DataFile);
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
        jobs.add(1,job);
        addJobFirst(jobs);
    }
    public void addJobFirst(List<AbstractJobN> jobs) {
        if(!isCanceled) {
            synchronized (syncObje) {
                jobList.addAll(jobs);
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
    @Override
    public boolean onStopJob(JobParameters params) {
        if(currentTask!=null){
            currentTask.cancel(true);
        }
        boolean result=isAvailableJobList();
        if(result){
            this.saveJobList();
            jobFinished(mJobParam, true);
        }else{
            clearJobList();
            jobFinished(mJobParam, false);
        }
        saveGPSList();
        isCanceled=true;
        return result;
    }

}
