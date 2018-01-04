package com.guoguang.runsimtest;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by 40303 on 2017/5/27.
 */

public class WriteLog {
    public void logToFile(String message){
        String filepath = "/mnt/sdcard/logFolder";
        File f = new File(filepath);
        //判断目录是否存在，不存在就创建一个
        if(! f.exists()){
            f.mkdir();
        }
        try{
            String fileName=f.toString()+File.separator+"log_"+paserTime(System.currentTimeMillis())+".txt";
            FileOutputStream fos=new FileOutputStream(fileName);
            fos.write(message.toString().getBytes());
            fos.flush();
            fos.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public String paserTime(long milliseconds) {
        System.setProperty("user.timezone", "Asia/Shanghai");
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(tz);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String times = format.format(new Date(milliseconds));
        return times;
    }

}
