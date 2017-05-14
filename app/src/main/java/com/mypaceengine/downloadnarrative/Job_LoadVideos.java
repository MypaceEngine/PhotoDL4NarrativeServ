package com.mypaceengine.downloadnarrative;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_LoadVideos  extends Job_LoadNarrativeInfo_Abstract {

    List<AbstractJobN> createJobInfo(JSONObject jsonObj) throws Exception{

        List<AbstractJobN> jobs=new ArrayList<AbstractJobN>();
        try {
            JSONArray itemArray = jsonObj.getJSONArray("results");
            int count = itemArray.length();
            for (int i = 0; i < count; i++) {
                JSONObject obj = itemArray.getJSONObject(i);
                String photos_url = obj.getString("photos_url");
                Job_ChkNeed_DownloadVideo nextJob=new Job_ChkNeed_DownloadVideo();
                nextJob.setInfo(obj.toString());
                jobs.add(nextJob);
            }
            String nextUrlStr=jsonObj.getString("next");
            if((nextUrlStr!=null)&&(nextUrlStr.length()>0)&&(!url.equals(nextUrlStr))&&(!nextUrlStr.equals("null"))){
                Log.d("Next","URL:"+nextUrlStr);
                //Thread.sleep(3000);
                Job_LoadVideos nextMoment=new Job_LoadVideos();
                nextMoment.setInfo(nextUrlStr);
                jobs.add(nextMoment);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return jobs;
    }
}
