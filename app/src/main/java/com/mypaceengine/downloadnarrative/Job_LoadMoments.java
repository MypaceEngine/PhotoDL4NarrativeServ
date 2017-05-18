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
    void run() {
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
    List<AbstractJobN> createJobInfo(JSONObject jsonObj) throws Exception{

        List<AbstractJobN> jobs=new ArrayList<AbstractJobN>();
            JSONArray itemArray = jsonObj.getJSONArray("results");
            int count = itemArray.length();
            for (int i = 0; i < count; i++) {
                JSONObject obj = itemArray.getJSONObject(i);
                String photos_url = obj.getString("photos_url");
                Job_LoadPhotos nextJob=new Job_LoadPhotos();
                nextJob.setInfo(photos_url,obj.toString());
                jobs.add(nextJob);
            }
            String nextUrlStr=jsonObj.getString("next");
            if((nextUrlStr!=null)&&(nextUrlStr.length()>0)&&(!url.equals(nextUrlStr))&&(!nextUrlStr.equals("null"))){
                Log.d("Next","URL:"+nextUrlStr);
                //Thread.sleep(3000);
                Job_LoadMoments nextMoment=new Job_LoadMoments();
                nextMoment.setInfo(nextUrlStr);
                jobs.add(nextMoment);
            }else{
                Job_LoadVideos videoTask = new Job_LoadVideos();
                videoTask.setInfo("https://narrativeapp.com/api/v2/videos/");
                jobs.add(videoTask);

            }
        return jobs;
    }
}
