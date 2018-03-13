package com.mypaceengine.downloadnarrative;

import android.content.Context;
import android.os.Environment;

import com.google.common.io.Files;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by MypaceEngine on 2017/05/13.
 */

public class CnvUtil {
    public static Calendar cnvCalender(String dateStr){
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
            Date date = df.parse(dateStr);
            calendar.clear();
            calendar.setTime(date);
        }catch (Exception ex){}
        return calendar;
    }

    public static File cnvFilePathTmp(Context context, String baseFolderName, Calendar cal, String format) throws Exception{

        SimpleDateFormat sdf_Year = new SimpleDateFormat("yyyy");
        SimpleDateFormat sdf_Month = new SimpleDateFormat("MM");
        SimpleDateFormat sdf_Day= new SimpleDateFormat("dd");
        SimpleDateFormat sdf_File= new SimpleDateFormat("hhmmss");

        File result=context.getExternalFilesDir(baseFolderName+ File.separator + sdf_Year.format(cal.getTime())+File.separator+sdf_Month.format(cal.getTime()) + File.separator + sdf_Day.format(cal.getTime()) + File.separator);
        Files.createParentDirs(result);
        return new File(result.getAbsolutePath()+File.separator+ sdf_File.format(cal.getTime()) + format);
    }

    public static File cnvFilePath_Data(String baseFolderName, Calendar cal, String format) throws Exception{

        SimpleDateFormat sdf_Year = new SimpleDateFormat("yyyy");
        SimpleDateFormat sdf_Month = new SimpleDateFormat("MM");
        SimpleDateFormat sdf_Day= new SimpleDateFormat("dd");
        SimpleDateFormat sdf_File= new SimpleDateFormat("hhmmss");

        File result=new File(baseFolderName + sdf_Year.format(cal.getTime())+File.separator+sdf_Month.format(cal.getTime()) + File.separator + sdf_Day.format(cal.getTime()) + File.separator);
        Files.createParentDirs(result);
        return new File(result.getAbsolutePath()+File.separator+ sdf_File.format(cal.getTime()) + format);
    }


    public static String latlong2GeoFormat (double latlong) {
        // doubleからintへ変換
        Double _latlong = latlong;
        int num1 = _latlong.intValue();
        double num2d = ((_latlong - (double)num1) * 60);
        int num2 = (int)num2d;
        double num3d = ((num2d - (double)num2) * 60 * 100000);
        int num3 = (int)num3d;
        // フォーマット num1/denom1,num2/denom2,num3,denom3
        return String.format("%d/1,%d/1,%d/100000", num1, num2, num3);
    }

    public static String getProgramLocalFilePath(Context context){
        return context.getExternalFilesDir(Conf.PhotoFolderName).getAbsolutePath()+File.separator;
    }
    public static String getDCIMFilePath(){
        return Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator+Conf.DCIMFolderName+File.separator;
    }
    public static String getPictureFilePath(){
        return Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_PICTURES + File.separator+Conf.DCIMFolderName+File.separator;
    }
    public static String getFilePathFromType(Context context,int type){
        if(type==DataUtil.FOLDER_DCIM){
            return getDCIMFilePath();
        }else if(type==DataUtil.FOLDER_PIC){
            return getPictureFilePath();
        }else{
            return getProgramLocalFilePath(context);
        }
    }
}
