package com.rockchips.mediacenter.activity;

import java.util.List;

import momo.cn.edu.fjnu.androidutils.utils.ToastUtils;

import com.rockchips.mediacenter.bean.LocalDevice;
import com.rockchips.mediacenter.data.ConstData;
import com.rockchips.mediacenter.utils.ActivityExitUtils;
import com.rockchips.mediacenter.utils.DialogUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.rockchips.mediacenter.R;
/**
 * @author GaoFei
 * App基本Activity
 */
public class AppBaseActivity extends Activity{
	private static final String TAG = "AppBaseActivity";
	private DeviceUpDownReceiver mDeviceUpDownReceiver;
	private LocalDevice mCurrDevice;
    /**
     * 定时器处理
     */
    private Handler mTimeHandler;
    /**
     * 定时器任务
     */
    private TimerTask mTimerTask;
    /**
     * 是否超时
     */
    private boolean mIsOverTime;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActivityExitUtils.addActivity(this);
		initData();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		initBaseEvent();
	}
	
	
	private void initData(){
		mCurrDevice = (LocalDevice)getIntent().getSerializableExtra(ConstData.IntentKey.EXTRAL_LOCAL_DEVICE);
		mDeviceUpDownReceiver = new DeviceUpDownReceiver();
		mTimeHandler = new Handler();
		mTimerTask = new TimerTask();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(getClass() != MainActivity.class){
			IntentFilter deviceUpDownFilter = new IntentFilter();
			//注册设备下线广播
			deviceUpDownFilter.addAction(ConstData.BroadCastMsg.DEVICE_DOWN);
			LocalBroadcastManager.getInstance(this).registerReceiver(mDeviceUpDownReceiver, deviceUpDownFilter);
		}
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		if(getClass() != MainActivity.class){
			LocalBroadcastManager.getInstance(this).unregisterReceiver(mDeviceUpDownReceiver);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ActivityExitUtils.removeActivity(this);
	}
	
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		hideNavigationBar();
	}
	
	/**
	 * 隐藏NavigationBar
	 */
	public void hideNavigationBar(){
		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
		              | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(uiOptions);
	}
	
	
	public void initBaseEvent(){
		View decorView = getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener
		        (new View.OnSystemUiVisibilityChangeListener() {
		    @Override
		    public void onSystemUiVisibilityChange(int visibility) {
		        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
		        	hideNavigationBar();
		        } else {
		            
		        }
		    }
		});
	}
	
	
	/**
	 * 启动定时器
	 * @param time 处罚时间
	 */
	public void startTimer(long time){
	    mIsOverTime = false;
	    mTimeHandler.postDelayed(mTimerTask, time);
	}
	
	/**
	 * 结束定时器
	 */
	public void endTimer(){
	    mTimeHandler.removeCallbacks(mTimerTask);
	}
	
	/**
	 * 定时器回调方法
	 */
	public void onTimerArrive(){
	    mIsOverTime = true;
	    DialogUtils.closeLoadingDialog();
	    ToastUtils.showToast(getString(R.string.load_over_time));
	}
	
	/**
	 * 是否超时
	 * @return
	 */
	public boolean isOverTimer(){
	    return mIsOverTime;
	}
	
	class DeviceUpDownReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			String mountPath = intent.getStringExtra(ConstData.IntentKey.EXTRA_DEVICE_PATH);
			if(mCurrDevice != null && mCurrDevice.getMountPath().equals(mountPath)){
				//退出Activity
				List<Activity> allActivities = ActivityExitUtils.getAllActivities();
				if(allActivities != null && allActivities.size() > 0){
					for(int i = allActivities.size() - 1; i >= 0; --i){
						Activity itemActivity = allActivities.get(i);
						if(itemActivity != null && itemActivity.getClass() != MainActivity.class){
							itemActivity.finish();
						}
					}
				}
			}
		}
	}
	
	/**
     * 定时器任务
     * @author GaoFei
     *
     */
    class TimerTask implements Runnable{
        @Override
        public void run() {
           onTimerArrive();
        }
    }
}
