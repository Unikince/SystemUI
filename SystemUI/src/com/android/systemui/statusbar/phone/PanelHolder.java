/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.EventLog;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.android.systemui.EventLogTags;

public class PanelHolder extends FrameLayout {
    public static final boolean DEBUG_GESTURES = true;

    private int mSelectedPanelIndex = -1;
    private PanelBar mBar;
    NotificationPanelView mNotificationPanel;

    public PanelHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setChildrenDrawingOrderEnabled(true);
        //add by mare 2017/3/15 begin
        //========================>
        //获得mNotificationPanel
        mNotificationPanel = (NotificationPanelView) getChildAt(0);
        //add by mare 2017/3/15 end
    }

    public int getPanelIndex(PanelView pv) {
        final int N = getChildCount();
        for (int i=0; i<N; i++) {
            final PanelView v = (PanelView) getChildAt(i);
            if (pv == v) return i;
        }
        return -1;
    }

    public void setSelectedPanel(PanelView pv) {
        mSelectedPanelIndex = getPanelIndex(pv);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mSelectedPanelIndex == -1) {
            return i;
        } else {
            if (i == childCount - 1) {
                return mSelectedPanelIndex;
            } else if (i >= mSelectedPanelIndex) {
                return i + 1;
            } else {
                return i;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (DEBUG_GESTURES) {
            if (event.getActionMasked() != MotionEvent.ACTION_MOVE) {
                EventLog.writeEvent(EventLogTags.SYSUI_PANELHOLDER_TOUCH,
                        event.getActionMasked(), (int) event.getX(), (int) event.getY());
            }
        }
        return false;
    }

    public void setBar(PanelBar panelBar) {
        mBar = panelBar;
    }
    //add by mare 2017/3/21 begin
    //========================>
    //这里刷新layout时不改变mNotificationPanel的位置，加入悬浮通知状况
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	super.onLayout(changed, left, top, right, bottom);
    	if(mNotificationPanel!=null&&mNotificationPanel.is_Qucii_Headup()){
    		mNotificationPanel.layout(left, top, right, bottom);
    	}else if(mNotificationPanel!=null&&mNotificationPanel.getIsTouchOrAnimationOrBarState()){
    		mNotificationPanel.layout(left, top, right, mNotificationPanel.getChangedBottom());
    	}
    }
    //add by mare 2017/3/21 end
}
