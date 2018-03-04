package com.mypaceengine.downloadnarrative;

import java.io.File;

/**
 * Created by MypaceEngine on 2018/02/28.
 */

public class FileUtil {
    /*
     * Delete File and Folder
     */
    public static void deleteFolder(File f)
    {

        if(f.exists() == false) {
            return;
        }

        if(f.isFile()) {
            f.delete();
        } else if(f.isDirectory()){

            File[] files = f.listFiles();

            for(int i=0; i<files.length; i++) {
                deleteFolder( files[i] );
            }
            f.delete();
        }
    }
}
