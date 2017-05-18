package com.mypaceengine.downloadnarrative;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class Job_LoadVideos  extends Job_LoadNarrativeInfo_Abstract  implements Serializable {
    private static final long serialVersionUID = 000000000000000000001L;

    void run() {
        Log.d("MainServiceTask", "getVideoExec:"+url);
        boolean flag=true;
        try {
            JSONObject json = getNarrativetExec(url);
            this.service.addJobs(createJobInfo(json));
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
                String format = ".mp4";
                String photos_url =null;

                Iterator ite = obj.getJSONObject("renders").keys();
                int width=0;
                int height=0;
                while (ite.hasNext()) {
                    String size_key = (String) ite.next();
                    JSONObject eachObj = obj.getJSONObject("renders").getJSONObject(size_key);
                    JSONObject sizeObj = eachObj.getJSONObject("size");
                    if (sizeObj != null) {
                        int photoWidth = sizeObj.getInt("width");
                        int photoHeight = sizeObj.getInt("height");
                        if (
                                (width < photoWidth) ||
                                        ((width == photoWidth) && (height < photoHeight))
                                ) {
                            photos_url = eachObj.getString("url");
                            if (eachObj.has("format")) {
                                format = "." + eachObj.getString("format");
                            }
                        }
                    }
                }
                if(photos_url!=null) {
                    Job_ChkNeed_DownloadVideo nextJob = new Job_ChkNeed_DownloadVideo();
                    nextJob.setInfo(obj.toString());
                    jobs.add(nextJob);
                }
            }
            String nextUrlStr=jsonObj.getString("next");
            if((nextUrlStr!=null)&&(nextUrlStr.length()>0)&&(!url.equals(nextUrlStr))&&(!nextUrlStr.equals("null"))){
                Log.d("Next","URL:"+nextUrlStr);
                //Thread.sleep(3000);
                Job_LoadVideos nextMoment=new Job_LoadVideos();
                nextMoment.setInfo(nextUrlStr);
                jobs.add(nextMoment);
            }
        return jobs;
    }
}
