package com.guoguang.runsimtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by 40303 on 2017/5/26.
 */

public class BootCompleteReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
       if (intent.getAction().equals(ACTION)){
            Intent newActivity=new Intent(context,MainActivity.class);
            newActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newActivity);
        }
    }
}
