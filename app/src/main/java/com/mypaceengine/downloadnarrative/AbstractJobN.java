package com.mypaceengine.downloadnarrative;

import android.provider.ContactsContract;

import java.io.Serializable;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public abstract class AbstractJobN implements Serializable {
    transient SyncJobService service=null;
    transient DataUtil dataUtil;
    transient boolean shutdownFlg=false;
    void preExecute(SyncJobService _service){
        service=_service;
        dataUtil=new DataUtil(service);
        shutdownFlg=false;
    }
    abstract void run();
    void stop(){
        shutdownFlg=true;
    }
}
