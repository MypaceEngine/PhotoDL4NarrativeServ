package com.mypaceengine.downloadnarrative;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_LoadMoments extends Job_LoadNarrativeInfo_Abstract  implements Serializable {
    private static final long serialVersionUID = 000000000000000000001L;
    boolean firstFlag=false;
    void run() {
        service.showExecutingNotification_LoadMoment();
        Log.d("MainServiceTask", "getMomentExec:"+url);
        boolean flag=true;
        try {
            JSONObject json = getNarrativetExec(url);
            this.service.addJobFirst(createJobInfo(json));
        } catch (Exception ex) {
            ex.printStackTrace();
            flag=false;
        }
        if((flag)&&(!shutdownFlg)){
            this.service.removeJob(this);
        }
    }

    void setFirstFlag(){
        firstFlag=true;
    }
    List<AbstractJobN> createJobInfo(JSONObject jsonObj) throws Exception{

        List<AbstractJobN> jobs=new ArrayList<AbstractJobN>();
            JSONArray itemArray = jsonObj.getJSONArray("results");
            int count = itemArray.length();
        boolean skip=false;
            for (int i = 0; i < count; i++) {

                JSONObject obj = itemArray.getJSONObject(i);
                String photos_url = obj.getString("photos_url");
                String uuid=obj.getString("uuid");

                if(
                        ( (!dataUtil.getEnableLocalSync())||(dataUtil.getEnableLocalSync()&&dataUtil.loadString("Local_PICTURE_UUID","").equals(uuid)))&&
                                ( (!dataUtil.getEnableGoogleSync())||(dataUtil.getEnableGoogleSync()&&dataUtil.loadString("Google_PICTURE_UUID","").equals(uuid)))
                        ){
                    skip=true;
                    break;
                }
                if((i==0)&&(firstFlag)){
                    Job_MarkRekeaseFlag job=new Job_MarkRekeaseFlag();
                    job.setFlag("PICTURE_UUID",uuid);
                    service.addJob(job);
                }
                if(
                        ((dataUtil.getEnableGoogleSync())&&(!dataUtil.loadBooleanHistory("Google_"+uuid)))||
                                ((dataUtil.getEnableLocalSync())&&(!dataUtil.loadBooleanHistory("Local_"+uuid)))
                        ){
                    Job_LoadPhotos nextJob=new Job_LoadPhotos();
                    nextJob.setInfo(photos_url,obj.toString());
                    jobs.add(nextJob);
                }

            }
            String nextUrlStr=jsonObj.getString("next");
            if((nextUrlStr!=null)&&(nextUrlStr.length()>0)&&(!url.equals(nextUrlStr))&&(!nextUrlStr.equals("null"))&&(!skip)){
                Log.d("Next","URL:"+nextUrlStr);
                //Thread.sleep(3000);
                Job_LoadMoments nextMoment=new Job_LoadMoments();
                nextMoment.setInfo(nextUrlStr);
                jobs.add(nextMoment);
            }else{
                Job_LoadVideos videoTask = new Job_LoadVideos();
                videoTask.setInfo("https://narrativeapp.com/api/v2/videos/");
                videoTask.setFirstFlag();
                jobs.add(videoTask);
            }
        return jobs;
    }
}
