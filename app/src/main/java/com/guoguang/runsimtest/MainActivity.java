package com.guoguang.runsimtest;

import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import static android.telephony.TelephonyManager.SIM_STATE_READY;

public class MainActivity extends AppCompatActivity {
    private TextView tvShowResult;
    private WriteLog writeLog;
    private ExecShellCmd execShellCmd;
    private static final String ACTION_CONNETCIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private IntentFilter simStateFilter;
    private SimStateReceiver simStateReceiver;
    NetworkInfo.State mobileState = null;
    NetworkInfo.State wifeState = null;
    private static final String TAG = "RunSimTest";
    private int previewState;
    private int currentState;
    private volatile boolean isTimeOut = false;
    private int timeOut = 1000 * 30;
    private volatile boolean isDestory=false;

    //处理msg，在开机一分钟后，判断网络状态
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==1){
                int simState = getSimState(getApplicationContext());
                if (simState == -1) {
                    if (writeLog == null) {
                        writeLog = new WriteLog();
                    }
                    writeLog.logToFile("开机一分钟后移动网络状态为断开");
                    tvShowResult.setText("开机一分钟后移动网络状态为断开");
                    //网络状态为断开时延迟10秒后重启
                    doCmdToLog();
                }
            }else if (msg.what==2){
                //网络状态为断开时延迟10秒后重启
                doRebootCmd();
            }

            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //开机设置初始状态为断开
        previewState = -1;
        currentState = -1;
        execShellCmd = new ExecShellCmd();
        tvShowResult = (TextView) findViewById(R.id.showResult);

        //注册广播
        setSimReceiver();

        //开启两个个子线程，开始计时
        new Thread(new MyThread()).start();
        new Thread(new MyThread2()).start();

    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(simStateReceiver);
        isDestory=true;
        super.onDestroy();
    }

    public void setSimReceiver() {
        simStateFilter = new IntentFilter();
        simStateFilter.addAction(ACTION_CONNETCIVITY_CHANGE);
        simStateFilter.addAction(ACTION_SIM_STATE_CHANGED);
        simStateReceiver = new SimStateReceiver();
        registerReceiver(simStateReceiver, simStateFilter);
    }

    public class SimStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_CONNETCIVITY_CHANGE)) {
                currentState = getSimState(context);
                Log.d(TAG, "currentState=" + currentState);
                writeLogToFile(previewState, currentState);
                previewState = currentState;
                Log.d(TAG, "previewState=" + previewState);
                if (getSimState(context) == 0) {
                    tvShowResult.setText("移动网络连接正常");
                } else {
                    tvShowResult.setText("移动网络连接断开");
                }
            }
        }
    }

    public int getSimState(Context context) {
        int flag;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        mobileState = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        wifeState = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        Log.d(TAG, "mobile状态:" + mobileState);
        if (mobileState != null && NetworkInfo.State.CONNECTED != wifeState && NetworkInfo.State.CONNECTED == mobileState) {
            flag = 0;
        } else if (mobileState != null && NetworkInfo.State.CONNECTED == wifeState && NetworkInfo.State.CONNECTED != mobileState) {
            flag = 1;
        } else {
            flag = -1;
        }
        return flag;
    }

    public void writeLogToFile(int previewState, int currentState) {
        Log.d(TAG, "currentState1=" + currentState);
        Log.d(TAG, "previewState1=" + previewState);
        boolean isTimeout = false;
        boolean isRecovery = false;
        String str = "";
        int time = 1000 * 30;

        if (writeLog == null) {
            writeLog = new WriteLog();
        }
        //由连接变成断开
        if (previewState == 0 && currentState == -1) {
            tvShowResult.setText("网络出现断开情况");
            long startTime = System.currentTimeMillis();
            //记录日志，已时间为日志名称
            doCmdToLog();

            //判断30秒内是否重新连上
            while (!isTimeout && !isRecovery) {
                long endTime = System.currentTimeMillis();
                if (((endTime - startTime) > time) && getSimState(getApplicationContext()) != 0) {
                    str = "移动网络由连接状态变为断开时间超过30秒";
                    writeLog.logToFile(str);
                    isTimeout = true;
                    //doRebootCmd();
                } else if (((endTime - startTime) < time) && getSimState(getApplicationContext()) == 0) {
                    str = "移动网络断开" + (endTime - startTime) / 1000 + "秒";
                    writeLog.logToFile(str);
                    isRecovery = true;
                    //doRebootCmd();
                }
            }

        }
    }

    public void testFirstTimeSimState(Context context) {
        long start_time = System.currentTimeMillis();
        int firstSimState = getSimState(context);
        if (firstSimState == -1) {
            while (!isTimeOut) {
                long end_time = System.currentTimeMillis();
                if ((end_time - start_time) > timeOut) {
                    int secondSimState = getSimState(context);
                    if (secondSimState == -1) {
                        if (writeLog == null) {
                            writeLog = new WriteLog();
                        }
                        writeLog.logToFile("开机后移动网络状态为断开");
                        tvShowResult.setText("开机后移动网络状态为断开");
                    }
                    isTimeOut = true;
                }
            }
        }
    }

    public class MyThread implements Runnable {
        boolean isOver = false;

        @Override
        public void run() {
            while (!isOver&&!isDestory) {
                try {
                    Thread.sleep(1000 * 60);// 线程暂停60秒，单位毫秒
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);// 发送消息
                    isOver = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class MyThread2 implements Runnable {
        boolean isOver = false;

        @Override
        public void run() {
            while (!isOver&&!isDestory) {
                try {
                    Thread.sleep(1000*60*10);// 线程暂停10分钟，单位毫秒
                    Message message = new Message();
                    message.what = 2;
                    handler.sendMessage(message);// 发送消息
                    isOver = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void doRebootCmd() {
        try {
            Thread.sleep(1000 * 5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            boolean isOver = false;
            @Override
            public void run() {
                while (!isOver) {
                    execShellCmd.exec("reboot");
                    isOver = true;
                }
            }
        }).start();
    }

    public void doCmdToLog(){
         final String cmd="logcat -b radio -v time -f /mnt/sdcard/logFolder/radioLog_"+writeLog.paserTime(System.currentTimeMillis());
        new Thread(new Runnable() {
            boolean isOver = false;
            @Override
            public void run() {
                while (!isOver) {
                    execShellCmd.exec(cmd);
                    try {
                        Thread.sleep(1000*40);
                        execShellCmd.exec("reboot");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isOver = true;
                }
            }
        }).start();
    }
}
