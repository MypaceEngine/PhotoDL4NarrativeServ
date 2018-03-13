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
    boolean firstFlag=false;
    void run() {
        service.showExecutingNotification_LoadMoment();
        Log.d("MainServiceTask", "getVideoExec:"+url);
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
        Job_ChkNeed_DownloadVideo nextJob=null;
            int count = itemArray.length();
        boolean skip=false;
            for (int i = 0; i < count; i++) {
                JSONObject obj = itemArray.getJSONObject(i);
                String format = ".mp4";
                String photos_url =null;
                String uuid= obj.getString("uuid");

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
                if(
                        ( (!dataUtil.getEnableLocalSync())||(dataUtil.getEnableLocalSync()&&dataUtil.loadString("Local_VIDEO_UUID","").equals(uuid)))&&
                                ( (!dataUtil.getEnableGoogleSync())||(dataUtil.getEnableGoogleSync()&&dataUtil.loadString("Google_VIDEO_UUID","").equals(uuid)))
                        ){
                    skip=true;
                    break;
                }
                if((i==0)&&(firstFlag)){
                    Job_MarkRekeaseFlag job=new Job_MarkRekeaseFlag();
                    job.setFlag("VIDEO_UUID",uuid);
                    service.addJob(job);
                }
                if(photos_url!=null) {
                    nextJob = new Job_ChkNeed_DownloadVideo();
                    nextJob.setInfo(obj.toString());
                    jobs.add(nextJob);
                }
            }
            String nextUrlStr=jsonObj.getString("next");
            if((nextUrlStr!=null)&&(nextUrlStr.length()>0)&&(!url.equals(nextUrlStr))&&(!nextUrlStr.equals("null"))&&(!skip)){
                Log.d("Next","URL:"+nextUrlStr);
                //Thread.sleep(3000);
                Job_LoadVideos nextMoment=new Job_LoadVideos();
                nextMoment.setInfo(nextUrlStr);
                jobs.add(nextMoment);
            }
        return jobs;
    }
}
