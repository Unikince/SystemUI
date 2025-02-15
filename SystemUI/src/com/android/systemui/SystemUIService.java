/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.view.OrientationEventListener;

import com.android.systemui.statusbar.phone.PhoneStatusBarView;

public class SystemUIService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();
        //add by mare begin 这里是调用的android.view.OrientationEventListener这个监听屏幕转换方向的类，
        //获取到屏幕的转换的度数之后就可以判断是否左旋还是右旋，并将布尔数值赋给PhoneStatusBarView类中的属性，
        //用户在点击状态栏就会将Bitmap旋转为合适的度数
        startOrientationChangeListener();
        //add by mare end
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        SystemUI[] services = ((SystemUIApplication) getApplication()).getServices();
        if (args == null || args.length == 0) {
            for (SystemUI ui: services) {
                pw.println("dumping service: " + ui.getClass().getName());
                ui.dump(fd, pw, args);
            }
        } else {
            String svc = args[0];
            for (SystemUI ui: services) {
                String name = ui.getClass().getName();
                if (name.endsWith(svc)) {
                    ui.dump(fd, pw, args);
                }
            }
        }
    }
    //add by mare 取到的Bitmap会被旋转，这里需要考虑到将获取到的Bitmap转换方向，
    //此处还必须的考虑是左旋还是右旋到的横屏，转屏的方向不一样的话，那么得到的Bitmap也不一样
	private OrientationEventListener mOrientationListener;
	private boolean mScreenProtrait = true;
	private boolean mCurrentOrient = false;

	private final void startOrientationChangeListener() {
		mOrientationListener = new OrientationEventListener(this) {
			@Override
			public void onOrientationChanged(int rotation) {
				if ((rotation >= 0) && (rotation <= 180)) {// portrait
					mCurrentOrient = true;
					if (mCurrentOrient != mScreenProtrait) {
						mScreenProtrait = mCurrentOrient;
						PhoneStatusBarView.leftOrRightLandscape = true;
					}
				} else if ((rotation > 180) && (rotation < 360)) {// landscape
					mCurrentOrient = false;
					if (mCurrentOrient != mScreenProtrait) {
						mScreenProtrait = mCurrentOrient;
						PhoneStatusBarView.leftOrRightLandscape = false;
					}
				}
			}
		};
                 
		mOrientationListener.enable();
		IntentFilter filter = new IntentFilter();
	        filter.addAction(Intent.ACTION_SCREEN_ON);
	        filter.addAction(Intent.ACTION_SCREEN_OFF);
	        this.registerReceiver(mReceiver, filter);
	}
	
	
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            String action = i.getAction();
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
            	Log.e("qucii", "onOrientationChanged ACTION_SCREEN_ON");
        		mOrientationListener.enable();
            }
            else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            	Log.e("qucii", "onOrientationChanged ACTION_SCREEN_OFF");
            	mOrientationListener.disable();
            }
        }
    };
	//add by mare end

	@Override
	public void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}
}

