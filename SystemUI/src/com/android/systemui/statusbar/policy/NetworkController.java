/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionInfo;

import com.android.settingslib.wifi.AccessPoint;

import java.util.List;

public interface NetworkController {

    boolean hasMobileDataFeature();
    void addSignalCallback(SignalCallback cb);
    void removeSignalCallback(SignalCallback cb);
    void setWifiEnabled(boolean enabled);
    void onUserSwitched(int newUserId);
    AccessPointController getAccessPointController();
    MobileDataController getMobileDataController();

    public interface SignalCallback {
        void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
                boolean activityIn, boolean activityOut, String description);

        void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType,boolean showDataIcon,
                int qsType, boolean activityIn, boolean activityOut, int dataActivityId,
                int mobileActivityId, int stackedDataIcon, int stackedVoiceIcon,
                String typeContentDescription, String description,
                boolean isWide, int subId,String networkName,final boolean showNetworkClass,boolean isDelaySignal,boolean isDelayNoService);// modified by yangfan 
        void setSubs(List<SubscriptionInfo> subs);
        void setNoSims(boolean show);
        
//        /**延迟5s显示无服务图标**/
//        void setNoService(boolean show, boolean delay);

        void setEthernetIndicators(IconState icon);

        void setIsAirplaneMode(IconState icon);

        void setMobileDataEnabled(boolean enabled);

        void setNetworkLabelEnable(boolean enable ,boolean noServiceEnable);// added by yangfan 
    }

    public interface SignalCallbackExtended extends SignalCallback {
        void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType,boolean showDataIcon,
                int qsType, boolean activityIn, boolean activityOut, int dataActivityId,
                int mobileActivityId, int stackedDataIcon, int stackedVoiceIcon,
                String typeContentDescription, String description, boolean isWide,
                int subId, int imsIconId, boolean isImsOverWifi, int dataNetworkTypeInRoamingId,
                int embmsIconId,String networkName,final boolean showNetworkClass,boolean isDelaySignal,boolean isDelayNoService);// modified by yangfan 
    }

    public static class IconState {
        public final boolean visible;
        public final int icon;
        public final String contentDescription;

        public IconState(boolean visible, int icon, String contentDescription) {
            this.visible = visible;
            this.icon = icon;
            this.contentDescription = contentDescription;
        }

        public IconState(boolean visible, int icon, int contentDescription,
                Context context) {
            this(visible, icon, context.getString(contentDescription));
        }
    }

    /**
     * Tracks changes in access points.  Allows listening for changes, scanning for new APs,
     * and connecting to new ones.
     */
    public interface AccessPointController {
        void addAccessPointCallback(AccessPointCallback callback);
        void removeAccessPointCallback(AccessPointCallback callback);
        void scanForAccessPoints();
        int getIcon(AccessPoint ap);
        boolean connect(AccessPoint ap);
        boolean canConfigWifi();

        public interface AccessPointCallback {
            void onAccessPointsChanged(List<AccessPoint> accessPoints);
            void onSettingsActivityTriggered(Intent settingsIntent);
        }
    }

    /**
     * Tracks mobile data support and usage.
     */
    public interface MobileDataController {
        boolean isMobileDataSupported();
        boolean isMobileDataEnabled();
        void setMobileDataEnabled(boolean enabled);
        DataUsageInfo getDataUsageInfo();

        public static class DataUsageInfo {
            public String carrier;
            public String period;
            public long limitLevel;
            public long warningLevel;
            public long usageLevel;
        }
    }
}
