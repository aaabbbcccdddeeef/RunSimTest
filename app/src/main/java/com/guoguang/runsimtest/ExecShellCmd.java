package com.guoguang.runsimtest;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by wangjr on 2017/5/27.
 * java执行shell命令
 * 使用socket进行进程间通讯
 */

public class ExecShellCmd {
    private static final String TAG = "ExecShellCmd";
    private static final int SOCKET_PORT = 8090;
    private static final String SOCKET_IP = "127.0.0.1";

    public int exec(String cmd) {
        Log.d(TAG, "exec cmd: " + cmd);
        Socket socket;
        BufferedReader in;
        PrintWriter out;

        try {
            socket = new Socket(SOCKET_IP, SOCKET_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);
            BufferedReader line = new BufferedReader(new InputStreamReader(System.in));
            out.println(cmd);
            Log.d(TAG, "exec cmd success");
            line.close();
            out.close();
            Log.d(TAG, "exec cmd success2");
            in.close();
            socket.close();
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
