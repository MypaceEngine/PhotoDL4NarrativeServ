package com.mypaceengine.downloadnarrative;

import android.provider.ContactsContract;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public abstract class AbstractJobN {
    SyncJobService service=null;
    DataUtil dataUtil;
    boolean shutdownFlg=false;
    void preExecute(SyncJobService _service){
        service=_service;
        dataUtil=new DataUtil(service);
    }
    abstract void run();
    void stop(){
        shutdownFlg=true;
    }
}
