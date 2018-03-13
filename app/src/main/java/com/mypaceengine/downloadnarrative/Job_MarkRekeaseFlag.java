package com.mypaceengine.downloadnarrative;

/**
 * Created by MypaceEngine on 2018/03/08.
 */

public class Job_MarkRekeaseFlag extends AbstractJobN {
    String flag=null;
    String uuid=null;
    void run() {
        if (dataUtil.getEnableGoogleSync()) {
            dataUtil.saveString("Google_"+flag,uuid);
        }
        if (dataUtil.getEnableLocalSync()){
            dataUtil.saveString("Local_"+flag,uuid);
        }
        if((!shutdownFlg)) {
            this.service.removeJob(this);
        }
    }
    void setFlag(String _flag,String _uuid){
        flag=_flag;
        uuid=_uuid;
    }
}
