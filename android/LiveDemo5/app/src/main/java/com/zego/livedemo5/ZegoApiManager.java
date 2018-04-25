package com.zego.livedemo5;


import android.text.TextUtils;
import android.widget.Toast;

import com.zego.livedemo5.utils.PreferenceUtil;
import com.zego.livedemo5.utils.SystemUtil;
import com.zego.livedemo5.videocapture.VideoCaptureFactoryDemo;
import com.zego.livedemo5.videofilter.VideoFilterFactoryDemo;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoAvConfig;

/**
 * des: zego api管理器.
 */
public class ZegoApiManager {

    private static ZegoApiManager sInstance = null;

    private ZegoLiveRoom mZegoLiveRoom = null;

    private ZegoAvConfig zegoAvConfig;


    private final int[][] VIDEO_RESOLUTIONS = new int[][]{{320, 240}, {352, 288}, {640, 360},
            {960, 540}, {1280, 720}, {1920, 1080}};

    private long mAppID = 0;
    private byte[] mSignKey = null;

    private ZegoApiManager() {
        mZegoLiveRoom = new ZegoLiveRoom();
    }

    public static ZegoApiManager getInstance() {
        if (sInstance == null) {
            synchronized (ZegoApiManager.class) {
                if (sInstance == null) {
                    sInstance = new ZegoApiManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * 高级功能.
     */
    private void openAdvancedFunctions(){

        // 开启测试环境
        if(PreferenceUtil.getInstance().getTestEnv(false)){
            ZegoLiveRoom.setTestEnv(true);
        }

        // 外部渲染
        if(PreferenceUtil.getInstance().getUseExternalRender(false)){
            // 开启外部渲染
            ZegoLiveRoom.enableExternalRender(true);
        }

        // 外部采集
        if(PreferenceUtil.getInstance().getVideoCapture(false)){
            // 外部采集
            VideoCaptureFactoryDemo factoryDemo = new VideoCaptureFactoryDemo();
            factoryDemo.setContext(ZegoApplication.sApplicationContext);
            ZegoLiveRoom.setVideoCaptureFactory(factoryDemo);
        }

        // 外部滤镜
        if(PreferenceUtil.getInstance().getVideoFilter(false)){
            // 外部滤镜
            VideoFilterFactoryDemo videoFilterFactoryDemo = new VideoFilterFactoryDemo();
            ZegoLiveRoom.setVideoFilterFactory(videoFilterFactoryDemo);
        }
    }

    private void initUserInfo(){
        // 初始化用户信息
        String userID = PreferenceUtil.getInstance().getUserID();
        String userName = PreferenceUtil.getInstance().getUserName();

        if (TextUtils.isEmpty(userID) || TextUtils.isEmpty(userName)) {
            long ms = System.currentTimeMillis();
            userID = ms + "";
            userName = "Android_" + SystemUtil.getOsInfo() + "-" + ms;

            // 保存用户信息
            PreferenceUtil.getInstance().setUserID(userID);
            PreferenceUtil.getInstance().setUserName(userName);
        }
        // 必须设置用户信息
        ZegoLiveRoom.setUser(userID, userName);

    }


    private void init(long appID, byte[] signKey){

        initUserInfo();

        // 开发者根据需求定制
        openAdvancedFunctions();

        mAppID = appID;
        mSignKey = signKey;
        PreferenceUtil.getInstance().setAppId(mAppID);
        PreferenceUtil.getInstance().setAppKey(mSignKey);

        // 初始化sdk
        boolean ret = mZegoLiveRoom.initSDK(appID, signKey, ZegoApplication.sApplicationContext);
        if(!ret){
            // sdk初始化失败
            Toast.makeText(ZegoApplication.sApplicationContext, "Zego SDK初始化失败!", Toast.LENGTH_LONG).show();
        } else {
            int liveQualityLevel=PreferenceUtil.getInstance().getLiveQuality(3);
            switch (liveQualityLevel) {
                case 0:
                    zegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.VeryLow);
                    break;
                case 1:
                    zegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.Low);
                    break;
                case 2:
                    zegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.Generic);
                    break;
                case 3:
                    zegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.High);
                    break;
                case 4:
                    zegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.VeryHigh);
                    break;
                case 5:
                    zegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.SuperHigh);
                    break;
                case 6:
                    // 自定义设置
                    zegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.High);
                    int progress=PreferenceUtil.getInstance().getVideoResolutions(0);
                    zegoAvConfig.setVideoEncodeResolution(VIDEO_RESOLUTIONS[progress][1],VIDEO_RESOLUTIONS[progress][0]);
                    zegoAvConfig.setVideoCaptureResolution(VIDEO_RESOLUTIONS[progress][0], VIDEO_RESOLUTIONS[progress][1]);
                    int videoFps=PreferenceUtil.getInstance().getVideoFps(0);
                    zegoAvConfig.setVideoFPS(videoFps);
                    int videoBitrate=PreferenceUtil.getInstance().getVideoBitrate(0);
                    zegoAvConfig.setVideoBitrate(videoBitrate);
                    break;
            }

            mZegoLiveRoom.setAVConfig(zegoAvConfig);
            // 开发者根据需求定制
            // 硬件编码
            setUseHardwareEncode(PreferenceUtil.getInstance().getHardwareEncode(false));
            // 硬件解码
            setUseHardwareDecode(PreferenceUtil.getInstance().getHardwareDecode(false));
            // 码率控制
            setUseRateControl(PreferenceUtil.getInstance().getEnableRateControl(false));


        }
    }

    /**
     * 此方法是通过 appId 模拟获取与之对应的 signKey，强烈建议 signKey 不要存储在本地，而是加密存储在云端，通过网络接口获取
     *
     * @param appId
     * @return
     */
    private byte[] requestSignKey(long appId) {
        return ZegoAppHelper.requestSignKey(appId);
    }

    /**
     * 初始化sdk.
     */
    public void initSDK(){
        // 即构分配的key与id, 默认使用 UDP 协议的 AppId
        if (mAppID <= 0) {
            long storedAppId = PreferenceUtil.getInstance().getAppId();
            if (storedAppId > 0) {
                mAppID = storedAppId;
                mSignKey = PreferenceUtil.getInstance().getAppKey();
            } else {
                mAppID = ZegoAppHelper.UDP_APP_ID;
                mSignKey = requestSignKey(mAppID);
            }
        }

        init(mAppID, mSignKey);
    }

    public void reInitSDK(long appID, byte[] signKey) {
        init(appID, signKey);
    }

    public void releaseSDK() {
        // 清空高级设置
        ZegoLiveRoom.setTestEnv(false);
        ZegoLiveRoom.enableExternalRender(false);

        // 先置空factory后unintSDK, 或者调换顺序，factory中的destroy方法都会被回调
        ZegoLiveRoom.setVideoCaptureFactory(null);
        ZegoLiveRoom.setVideoFilterFactory(null);
        mZegoLiveRoom.unInitSDK();
    }

    public ZegoLiveRoom getZegoLiveRoom() {
        return mZegoLiveRoom;
    }

    public void setZegoConfig(ZegoAvConfig config) {
        zegoAvConfig = config;
        mZegoLiveRoom.setAVConfig(config);
    }


    public ZegoAvConfig getZegoAvConfig(){
        return  zegoAvConfig;
    }


    public void setUseTestEvn(boolean useTestEvn) {

        PreferenceUtil.getInstance().setUseTestEvn(useTestEvn);
    }

    public boolean isUseExternalRender(){
        return PreferenceUtil.getInstance().getUseExternalRender(false);
    }

    public void setUseExternalRender(boolean useExternalRender){

        PreferenceUtil.getInstance().setExternalRender(useExternalRender);
    }

    public void setUseVideoCapture(boolean useVideoCapture) {

        PreferenceUtil.getInstance().setVideoCapture(useVideoCapture);
    }

    public void setUseVideoFilter(boolean useVideoFilter) {

        PreferenceUtil.getInstance().setVideoFilter(useVideoFilter);
    }

    public boolean isUseVideoCapture() {
        return PreferenceUtil.getInstance().getVideoCapture(false);
    }

    public boolean isUseVideoFilter() {
        return PreferenceUtil.getInstance().getVideoFilter(false);
    }

    public void setUseHardwareEncode(boolean useHardwareEncode) {
        if(useHardwareEncode){
            // 开硬编时, 关闭码率控制
            if(PreferenceUtil.getInstance().getEnableRateControl(false)){
                mZegoLiveRoom.enableRateControl(false);
                PreferenceUtil.getInstance().setEnableRateControl(false);
            }
        }
        ZegoLiveRoom.requireHardwareEncoder(useHardwareEncode);

        PreferenceUtil.getInstance().setRequireHardwareEncoder(useHardwareEncode);
    }

    public void setUseHardwareDecode(boolean useHardwareDecode) {
        ZegoLiveRoom.requireHardwareDecoder(useHardwareDecode);
        PreferenceUtil.getInstance().setRequireHardwareDecoder(useHardwareDecode);
    }

    public void setUseRateControl(boolean useRateControl) {
        if(useRateControl){
            // 开码率控制时, 关硬编
            if(PreferenceUtil.getInstance().getHardwareEncode(false)){
                ZegoLiveRoom.requireHardwareEncoder(false);
                PreferenceUtil.getInstance().setRequireHardwareEncoder(false);
            }
        }
        PreferenceUtil.getInstance().setEnableRateControl(useRateControl);
        mZegoLiveRoom.enableRateControl(useRateControl);
    }

    public long getAppID() {
        return mAppID;
    }

    public byte[] getSignKey() {
        return mSignKey;
    }

    public boolean isUseTestEvn(){
        return PreferenceUtil.getInstance().getTestEnv(false);
    }

}
