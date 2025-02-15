/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemProperties;
import android.util.AttributeSet;
import com.android.systemui.statusbar.StatusBarState;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.android.systemui.DejankUtils;
import com.android.systemui.EventLogTags;
import com.android.systemui.R;
import com.qucii.systemui.utils.FastBlur;

public class PhoneStatusBarView extends PanelBar {
    private static final String TAG = "PhoneStatusBarView";
    private static final boolean DEBUG = PhoneStatusBar.DEBUG;
    private static final boolean DEBUG_GESTURES = false;

    PhoneStatusBar mBar;

    PanelView mLastFullyOpenedPanel = null;
    PanelView mNotificationPanel;
    private final PhoneStatusBarTransitions mBarTransitions;
    private ScrimController mScrimController;
    private float mMinFraction;
    private float mPanelFraction;
    //add by mare 2017/3/4 begin
    private int mIconSize;
    //add by mare 2017/3/4 end
    private Runnable mHideExpandedRunnable = new Runnable() {
        @Override
        public void run() {
            mBar.makeExpandedInvisible();
        }
    };

    public PhoneStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = getContext().getResources();
        mBarTransitions = new PhoneStatusBarTransitions(this);
        
        //add by mare 2017/3/4 begin
        //=======================>
        //通知栏小图标宽度和中间时钟一半宽度
        mIconSize = res.getDimensionPixelSize(
                R.dimen.status_bar_icon_size);
        //<=======================
        //add by mare 2017/3/4 end
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public void setBar(PhoneStatusBar bar) {
        mBar = bar;
    }

    public void setScrimController(ScrimController scrimController) {
        mScrimController = scrimController;
    }

    //add by mare 2017/3/6 begin
    //=======================>
    //这里判断左侧大于时钟左边，然后截取部分显示
    private View mNotificationIconsParent,mSystemIcons;
    private View centerClock;
    Rect outRect=new Rect();
    Rect outRectSys=new Rect();
    //add by mare 2017/3/6 end
    @Override
    public void onFinishInflate() {
        mBarTransitions.init();
        //add by mare 2017/3/6 begin
        //=======================>
        //这里判断左侧大于时钟左边，然后截取部分显示
        mNotificationIconsParent=findViewById(R.id.notification_icon_area_inner);
        mSystemIcons=findViewById(R.id.statusIcons);
        centerClock=findViewById(R.id.center_clock);
        //add by mare 2017/3/6 end
        
    }
    //add by mare 2017/3/6 begin
    //=======================>
    //这里判断左侧大于时钟左边，然后截取部分显示
   
    @Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if(centerClock.getVisibility()==View.VISIBLE){
			mNotificationIconsParent.getBoundsOnScreen(outRect);
			int totalWidth = centerClock.getLeft();
			// 这里判断左侧大于时钟左边
			if (outRect.right > totalWidth) {
				// 按图标大小截取宽度
				int width = (totalWidth - outRect.left) - (totalWidth - outRect.left) % mIconSize - 10;
				mNotificationIconsParent.layout(mNotificationIconsParent.getLeft(), mNotificationIconsParent.getTop(),
						mNotificationIconsParent.getLeft() + width, mNotificationIconsParent.getBottom());
			}
			//add by mare 2017/3/9 begin
	        //=======================>
	        //这里判断右边部分左侧小于时钟右侧边，就截取部分显示
			mSystemIcons.getBoundsOnScreen(outRectSys);
			
			int totalRight=centerClock.getRight();
			if(totalRight>outRectSys.left){
				int width=outRectSys.right-totalRight-10;
				mSystemIcons.layout(mSystemIcons.getRight()-width, mSystemIcons.getTop(),
						mSystemIcons.getRight(), mSystemIcons.getBottom());
			}
			//add by mare 2017/3/9 end
		}else{
			mNotificationIconsParent.layout(mNotificationIconsParent.getLeft(), mNotificationIconsParent.getTop(),
					mNotificationIconsParent.getLeft() + mNotificationIconsParent.getMeasuredWidth(), mNotificationIconsParent.getBottom());
			mSystemIcons.layout(mSystemIcons.getRight() - mSystemIcons.getMeasuredWidth(), mSystemIcons.getTop(),
					mSystemIcons.getRight(), mSystemIcons.getBottom());
		}
	}
    //add by mare 2017/3/6 end
	@Override
    public void addPanel(PanelView pv) {
        super.addPanel(pv);
        if (pv.getId() == R.id.notification_panel) {
            mNotificationPanel = pv;
        }
    }

    @Override
    public boolean panelsEnabled() {
        return mBar.panelsEnabled();
    }

	@Override
    public boolean onRequestSendAccessibilityEventInternal(View child, AccessibilityEvent event) {
        if (super.onRequestSendAccessibilityEventInternal(child, event)) {
            // The status bar is very small so augment the view that the user is touching
            // with the content of the status bar a whole. This way an accessibility service
            // may announce the current item as well as the entire content if appropriate.
            AccessibilityEvent record = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(record);
            dispatchPopulateAccessibilityEvent(record);
            event.appendRecord(record);
            return true;
        }
        return false;
    }

    @Override
    public PanelView selectPanelForTouch(MotionEvent touch) {
        // No double swiping. If either panel is open, nothing else can be pulled down.
        return mNotificationPanel.getExpandedHeight() > 0
                ? null
                : mNotificationPanel;
    }

    @Override
    public void onPanelPeeked() {
        super.onPanelPeeked();
        mBar.makeExpandedVisible(false);
    }

    @Override
    public void onAllPanelsCollapsed() {
        super.onAllPanelsCollapsed();
        // Close the status bar in the next frame so we can show the end of the animation.
        DejankUtils.postAfterTraversal(mHideExpandedRunnable);
        mLastFullyOpenedPanel = null;
    }

    public void removePendingHideExpandedRunnables() {
        DejankUtils.removeCallbacks(mHideExpandedRunnable);
    }

	@Override
    public void onPanelFullyOpened(PanelView openPanel) {
        super.onPanelFullyOpened(openPanel);
        if (openPanel != mLastFullyOpenedPanel) {
            openPanel.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
        mLastFullyOpenedPanel = openPanel;
    }
    /**
     * *add by mare 截取桌面图片并且做虚化处理 begin
     */
	public static boolean leftOrRightLandscape = true;

	private boolean isNavigationEnable() {
		if (SystemProperties.getBoolean("persist.sys.navg_bar_visible", false)) {
			return true;
		} else {
			return false;
		}
	}
	
	protected Bitmap shot() {
		DisplayMetrics dm = getResources().getDisplayMetrics();
		int width, height;
		int navigationHeight = 108;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			width = dm.widthPixels;
			height = dm.heightPixels+navigationHeight;
		} else {
			if(isNavigationEnable()){
				width = dm.heightPixels;
				height = dm.widthPixels+navigationHeight;
			}else{
				width = dm.heightPixels;
				height = dm.widthPixels;
			}
			
		}
		Bitmap mBitmap = SurfaceControl.screenshot(width, height);
		return mBitmap;
	}

	protected Bitmap fastBlur(Bitmap bkg) {
		float scaleFactor = 7;
		float radius = 10;

		Bitmap overlay = Bitmap.createBitmap(
				(int) (bkg.getWidth() / scaleFactor),
				(int) (bkg.getHeight() / scaleFactor), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(overlay);
		canvas.translate(0, 0);
		canvas.scale(1 / scaleFactor, 1 / scaleFactor);
		Paint paint = new Paint();
		paint.setFlags(Paint.FILTER_BITMAP_FLAG);
		canvas.drawBitmap(bkg, 0, 0, paint);

		overlay = FastBlur.doBlur(overlay, (int) radius, true);
		DisplayMetrics dm = getResources().getDisplayMetrics();
		int width = bkg.getWidth();
		int height = bkg.getHeight();
		Bitmap mBitmap = overlay.createScaledBitmap(overlay, width, height,
				true);
		return mBitmap;
	}

	protected Bitmap darkBitmap(Bitmap srcBitmap) {
		int imgHeight, imgWidth;
		imgHeight = srcBitmap.getHeight();
		imgWidth = srcBitmap.getWidth();

		Bitmap dstBitmap = Bitmap.createBitmap(imgWidth, imgHeight,
				Config.ARGB_8888);
		Bitmap bmp = Bitmap.createBitmap(imgWidth, imgHeight, Config.ARGB_8888);
		float contrast = (float) (35 / 100.0);
		ColorMatrix cMatrix = new ColorMatrix();
		cMatrix.set(new float[] { contrast, 0, 0, 0, 0, 0, contrast, 0, 0, 0,
				0, 0, contrast, 0, 0, 0, 0, 0, 1, 0 });

		Paint paint = new Paint();
		paint.setColorFilter(new ColorMatrixColorFilter(cMatrix));

		Canvas canvas = new Canvas(bmp);
		canvas.drawBitmap(srcBitmap, 0, 0, paint);
		return bmp;
	}

	protected Bitmap rotateBitmap(Bitmap bmp) {
		Bitmap mbmp = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),
				Config.ARGB_8888);
		Canvas canvas = new Canvas(mbmp);
		canvas.drawBitmap(bmp, 0, 0, null);
		Matrix matrix = new Matrix();
		matrix.postScale(1f, 1f);
		if (leftOrRightLandscape == true) {
			matrix.postRotate(90);
		} else {
			matrix.postRotate(-90);
		}
		Bitmap dstbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
				bmp.getHeight(), matrix, true);
		canvas.drawBitmap(dstbmp, 0, 0, null);
		return dstbmp;
	}
	 /**
     * *add by mare 截取桌面图片并且做虚化处理 end
     */
	@Override
    public boolean onTouchEvent(MotionEvent event) {	
		// add by mare 当用户点击最上面的状态栏时就会触发事件，而我们获取到这个事件，并在这时截取屏幕
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			BitmapDrawable mBitmapDrawable;
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				mBitmapDrawable = new BitmapDrawable(getResources(),
						darkBitmap(fastBlur(shot())));
			} else {
				mBitmapDrawable = new BitmapDrawable(getResources(),
						darkBitmap(fastBlur(rotateBitmap(shot()))));
			}
			mBitmapDrawable.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			mBar.mNotificationPanel.setBackground(mBitmapDrawable);
		}
		// add by mare
        boolean barConsumedEvent = mBar.interceptTouchEvent(event);

        if (DEBUG_GESTURES) {
            if (event.getActionMasked() != MotionEvent.ACTION_MOVE) {
                EventLog.writeEvent(EventLogTags.SYSUI_PANELBAR_TOUCH,
                        event.getActionMasked(), (int) event.getX(), (int) event.getY(),
                        barConsumedEvent ? 1 : 0);
            }
        }

        return barConsumedEvent || super.onTouchEvent(event);
    }

    @Override
    public void onTrackingStarted(PanelView panel) {
        super.onTrackingStarted(panel);
        mBar.onTrackingStarted();
        mScrimController.onTrackingStarted();
    }

    @Override
    public void onClosingFinished() {
        super.onClosingFinished();
        mBar.onClosingFinished();
    }

    @Override
    public void onTrackingStopped(PanelView panel, boolean expand) {
        super.onTrackingStopped(panel, expand);
        mBar.onTrackingStopped(expand);
    }

    @Override
    public void onExpandingFinished() {
        super.onExpandingFinished();
        mScrimController.onExpandingFinished();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mBar.interceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public void panelScrimMinFractionChanged(float minFraction) {
        if (mMinFraction != minFraction) {
            mMinFraction = minFraction;
            updateScrimFraction();
        }
    }

    @Override
    public void panelExpansionChanged(PanelView panel, float frac, boolean expanded) {
        super.panelExpansionChanged(panel, frac, expanded);
        mPanelFraction = frac;
        //remove by mare 2017/3/7 begin
        //===========================>
        //这里去掉作用是不改变锁屏滑动解锁时的亮度变化

        if ((mBar.getBarState() != StatusBarState.KEYGUARD)) {
            updateScrimFraction();
        }

        //remove by mare 2017/3/7 end
    }
    //Note by mare 2017/3/7
    //通知前的蒙层画布
    private void updateScrimFraction() {
        float scrimFraction = Math.max(mPanelFraction - mMinFraction / (1.0f - mMinFraction), 0);
        mScrimController.setPanelExpansion(scrimFraction);
    }
    
// show QS page when pull down by yangfan begin   
    @Override
    public void showPage(int target) {
    	super.showPage(target);
    	mBar.mNotificationPanel.showPage(target);
    }
 // show QS page when pull down by yangfan end  

    @Override
    protected boolean isInCallUIActivity() {
    	 return mBar.isInCallUIActivity();
    }
    
}
