package com.szkj.watch.launcher.launcher;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.szkj.watch.launcher.R;
import com.szkj.watch.launcher.provider.LauncherConfig;

public class MyApplication extends Application {
	private static final int MSG_DB_CHANGED = 1;
	public static String[] DAYS_ZH;
	public static String[] DAYS_EN;
	
	private static MyApplication sApp;
	private boolean mDbChanged = false;
	private List<PackageChangedCallback> mCallbacks = 
			new ArrayList<MyApplication.PackageChangedCallback>();
	
	@Override
	public void onCreate() {
		super.onCreate();
		sApp = this;
		getContentResolver().registerContentObserver(
				LauncherConfig.WORKSPACE_CONTENT_URI, 
				true, 
				new WorkspaceTableObserver(mHandler));
		DAYS_ZH = getResources().getStringArray(R.array.days_zh);
		DAYS_EN = getResources().getStringArray(R.array.days_en);
	}
	
	private Handler mHandler = new Handler(new Handler.Callback() {
		
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_DB_CHANGED:
				for (PackageChangedCallback callback : mCallbacks) {
					callback.onChange();
				}
				break;
			}
			return true;
		}
	});
	
	public static MyApplication getApp() {
		return sApp;
	}
	
	private class WorkspaceTableObserver extends ContentObserver {
		public WorkspaceTableObserver(Handler handler) {
			super(handler);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			onChange(selfChange, null);
		}
		
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			mDbChanged = true;
			mHandler.sendEmptyMessage(MSG_DB_CHANGED);
		}	
	}
	
	public boolean isDbChanged() {
		return mDbChanged;
	}
	
	public void setDbChanged(boolean changed) {
		mDbChanged = changed;
	}
	
	public void registerCallback(PackageChangedCallback callback) {
		mCallbacks.add(callback);
	}
	
	public void unregisterCallback(PackageChangedCallback callback) {
		mCallbacks.remove(callback);
	}
	
	public static interface PackageChangedCallback {
		public void onChange();
	}
}
