package com.mypaceengine.downloadnarrative;

import android.os.AsyncTask;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class JobTask extends AsyncTask<AbstractJobN,AbstractJobN,AbstractJobN> {

    SyncJobService service=null;
    AbstractJobN job=null;
    public AbstractJobN getJob(){
        return job;
    }

    public void initialize(SyncJobService _service){
        service=_service;
    }
    @Override
    protected AbstractJobN doInBackground(AbstractJobN... params) {
        AbstractJobN job= params[0];
        job.run();
        return job;
    }

    @Override
    protected void onPostExecute(AbstractJobN job) {
        job.stop();
        service.removeTask(this);
        service.execNextTask();
    }

    protected void onCancelled(AbstractJobN job){
        try {
            job.stop();
        }catch (Exception ex){ex.printStackTrace();}
    }
}
