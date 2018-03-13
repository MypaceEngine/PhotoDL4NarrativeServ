package com.mypaceengine.downloadnarrative;

import java.io.File;

/**
 * Created by MypaceEngine on 2018/03/06.
 */

public class Job_Movefolder extends AbstractJobN {
    void run(){
        if(dataUtil.getFileMoveRequire()) {
            int dirfrom = dataUtil.getPreviousFolderType();
            int dirto = dataUtil.getFolderType();
            if (dirfrom != dirto) {
                service.showExecutingFolderCopy();
                File dirFromFile = new File(CnvUtil.getFilePathFromType(this.service, dirfrom));
                File dirToFile = new File(CnvUtil.getFilePathFromType(this.service, dirto));
                if (dirFromFile.exists()) {
                    FileUtil.directoryMove(dirFromFile, dirToFile);
                }
                dataUtil.setPreviousFolderType(dataUtil.getFolderType());
            }
            dataUtil.setFileMoveRequire(false);
        }
        if((!shutdownFlg)){
            this.service.removeJob(this);
        }
    }
}
