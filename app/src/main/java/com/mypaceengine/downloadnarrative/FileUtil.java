package com.mypaceengine.downloadnarrative;

import com.google.common.io.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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
    public static void directoryMove(File dirFrom, File dirTo)
    {
        if(!dirTo.exists()){
            dirTo.mkdir();
        }
        File[] fromFile = dirFrom.listFiles();

        if(fromFile != null) {

            for(File f : fromFile) {
                if (f.isFile())
                {
                    fileMove(f, dirTo);
                }
                else
                {
                    dirTo = new File(dirTo.getPath() +
                            File.separator + f.getName());

                    directoryMove(f, dirTo);
                }
            }
            dirFrom.delete();
        }
    }

    public static void fileMove(File file, File dir)
    {
        File copyFile = new File(dir.getPath() + File.separator + file.getName());
        FileChannel channelFrom = null;
        FileChannel channelTo = null;

        try
        {
            copyFile.createNewFile();
            channelFrom = new FileInputStream(file).getChannel();
            channelTo = new FileOutputStream(copyFile).getChannel();

            channelFrom.transferTo(0, channelFrom.size(), channelTo);

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (channelFrom != null) { channelFrom.close(); }
                if (channelTo != null) { channelTo.close(); }

                //更新日付もコピー
                copyFile.setLastModified(file.lastModified());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            try{
                file.delete();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public static void fileMoveWithFileName(File file, File copyFile)
    {
        if(copyFile.exists()){
            copyFile.delete();
        }
        try {
            Files.createParentDirs(copyFile);
        }catch (Exception ex){}
        FileChannel channelFrom = null;
        FileChannel channelTo = null;

        try
        {
            copyFile.createNewFile();
            channelFrom = new FileInputStream(file).getChannel();
            channelTo = new FileOutputStream(copyFile).getChannel();

            channelFrom.transferTo(0, channelFrom.size(), channelTo);

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (channelFrom != null) { channelFrom.close(); }
                if (channelTo != null) { channelTo.close(); }

                //更新日付もコピー
                copyFile.setLastModified(file.lastModified());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            try{
                file.delete();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
