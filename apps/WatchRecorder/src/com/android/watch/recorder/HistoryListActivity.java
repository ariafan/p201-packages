package com.android.watch.recorder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.DateTimeKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryListActivity extends Activity{
    public static ListView listView;
    AudioManager audiomanage;
    long s=0;
	long m=0;
    MyBaseAdater myBaseAdater=new MyBaseAdater();
    MainActivity me= new MainActivity();
    ArrayList<String> files=new ArrayList<String>();
    private List<Item> list; 
    ImageView imageView;
    LinearLayout ll;
    TextView name;
    TextView starttime;
    TextView endtime;
    TextView timesitem;
    SeekBar seekBar1;
    ImageView seekStart;
    private File myPlayFile;
    MediaPlayer mediaPlayer;
    boolean isPause=false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.historylist);
		setVolumeControlStream(AudioManager.STREAM_MUSIC); 
		ll= (LinearLayout) this.findViewById(R.id.l123);
		listView=(ListView) this.findViewById(R.id.listView1);
	    listView.setAdapter(myBaseAdater);
	    list = new ArrayList<Item>();  
	    starttime=(TextView) this.findViewById(R.id.startime);
	    endtime=(TextView) this.findViewById(R.id.endtime);
	    seekStart=(ImageView) this.findViewById(R.id.seekStart);
	    seekBar1=(SeekBar) this.findViewById(R.id.seekBar1);
	    imageView=(ImageView) this.findViewById(R.id.imageView2);
	    myPlayFile=null;
	    audiomanage = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
	    imageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent deleteinIntent=new Intent(HistoryListActivity.this,DeleteListActivity.class);
				startActivityForResult(deleteinIntent, 1);
			}
		});
	    seekStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mediaPlayer!=null){
					if(isPause){
						seekStart.setImageResource(R.drawable.seekstart);
						mediaPlayer.start();
						startTime();
						isPause=false;
					}else{
						seekStart.setImageResource(R.drawable.start);
						mediaPlayer.pause();
						stopTime();
						isPause=true;
					}
				}
			}
		});
	    myBaseAdater.notifyDataSetChanged();
	    listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				//每次开始时先清除信息、、、、、、
				init();
				TextView textView=(TextView) arg1.findViewById(R.id.name);
				myPlayFile = new File(MainActivity.myRecAudioDir.getAbsolutePath()
						+ File.separator
						+ textView.getText().toString());
				try {
					if (mediaPlayer != null) {
		                  mediaPlayer.stop();
		               }
					//获取焦点
					int result = audiomanage.requestAudioFocus(afChangeListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
					if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
						//audiomanage.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
						play(myPlayFile);   
					} 
					
					//play(myPlayFile);
					ll.setVisibility(View.VISIBLE);
					seekStart.setVisibility(View.VISIBLE);
					seekBar1.setVisibility(View.VISIBLE);
					starttime.setVisibility(View.VISIBLE);
					endtime.setVisibility(View.VISIBLE);
					ll.setBackgroundColor(Color.parseColor("#B7B7B7"));
					audiomanage.abandonAudioFocus(afChangeListener); 
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	    
	    seekBar1.setOnSeekBarChangeListener(onSeekBarChangeListener);
	}
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(mediaPlayer!=null){
			mediaPlayer.release();
			mediaPlayer = null;
			stopTime();
		}
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		   // me.recordFiles=data.getStringArrayListExtra("nodeletes");
		if(requestCode==1){
			myBaseAdater.notifyDataSetChanged();
		}
			
	}
	OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			if (focusChange ==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
				seekStart.setImageResource(R.drawable.start);
				mediaPlayer.pause();
				stopTime();
				isPause=true;
			}else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
				seekStart.setImageResource(R.drawable.seekstart);
				mediaPlayer.start();
				startTime();
				isPause=false;
			}else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
				//audiomanage.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
				audiomanage.abandonAudioFocus(afChangeListener);
				if(mediaPlayer!=null){
					mediaPlayer.release();
					mediaPlayer = null;
					stopTime();
				}
			}
			
		}
	};
	OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {
		// 摸完了
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {

			if (mediaPlayer == null) { return; }
			// 跳到给定刻度
			mediaPlayer.seekTo(seekBar.getProgress());
			startTime();
		}

		// 开始摸
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			stopTime();
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
		}
	};
    
	OnCompletionListener onCompletionListener = new OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mp) {
			// mediaPlayer播放结束事件[mediaPlayer对象自己会销毁]
			init();
			seekStart.setVisibility(View.INVISIBLE);
			seekBar1.setVisibility(View.INVISIBLE);
			starttime.setVisibility(View.INVISIBLE);
			endtime.setVisibility(View.INVISIBLE);
			ll.setVisibility(View.INVISIBLE);
		}
	};
	// 所有组件初始化
		private void init() {
			// 所有初始化
			starttime.setText("00:00");
			endtime.setText("00:00");
			seekBar1.setMax(0);
			seekBar1.setProgress(0);
			if (mediaPlayer != null) {
				mediaPlayer.release();
				mediaPlayer = null;
			    //ll.setVisibility(View.INVISIBLE);
			}
			stopTime();
		}
	protected void play(File f) throws IllegalArgumentException, SecurityException, IOException {
		try {
			// 开始播放
			seekStart.setImageResource(R.drawable.seekstart);
			if (mediaPlayer == null) {
				// 初始化，包含准备和加载数据源
				//mediaPlayer=MediaPlayer.create(this, f.getAbsolutePath());
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setDataSource(f.getAbsolutePath());
				// 播放结束后的事件
				mediaPlayer.setOnCompletionListener(onCompletionListener);
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			     	"mm:ss");
				mediaPlayer.prepare();
				mediaPlayer.start();
				// mediaPlayer.getDuration()获取当前歌曲的总时间【毫秒】
				
//				Date date = new Date(mediaPlayer.getDuration());
//				String ss=simpleDateFormat.format(date)+"";
				long g=mediaPlayer.getDuration();
				//long h=g/3600;
				String mm=null,ss=null;
				long m=g/(1000*60);
				long s=(g%(1000*60))/1000;
				if(m<10){
					mm="0"+m;
				}
				else{
					mm=m+"";
				}
				if(s<10){
					ss="0"+s;
				}
				else{
					ss=s+"";
				}
//				long a=mediaPlayer.getDuration()/(60*60);
//				long l=(mediaPlayer.getDuration()%(60*60));
//				long s=(mediaPlayer.getDuration()%(60*60))/(60);
				endtime.setText(mm+":"+ss);     //simpleDateFormat.format(date)+""
				// 设置总刻度
				seekBar1.setMax(mediaPlayer.getDuration());
				startTime();
			}
			
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}
	// 计数器
		Timer timer;
		// 执行线程任务
		class MyTimerTask extends TimerTask {
			@Override
			public void run() {
				// 次线程
				handler.obtainMessage().sendToTarget();
			}
		}
		private void startTime() {
			if (timer == null) {
				timer = new Timer();
				// 100000毫秒后执行一次
				// timer.schedule(new MyTimerTask(), 100000);
				// 每过1秒钟执行一次任务
				timer.schedule(new MyTimerTask(), 1000, 1000);
			}

		}
		
//		long m;
		Handler handler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				// 每过1秒钟这里需要获得消息
//		        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
				// mediaPlayer.getCurrentPosition()获取当前歌曲的当前时间【毫秒】
//				Date date = new Date(mediaPlayer.getCurrentPosition());
				long gg=mediaPlayer.getCurrentPosition();
				String mm=null;
				String ss=null;
				m=gg/(1000*60);
				if(m<10){
					mm="0"+m;
				}
				else{
					mm=m+"";
				}
				s=(gg%(1000*60))/(1000);
				if(s<10){
					ss="0"+s;
				}
				else{
					ss=s+"";
				}
				starttime.setText(mm+":"+ss);              //simpleDateFormat.format(date)
				seekBar1.setProgress(mediaPlayer.getCurrentPosition());
			}
		};
		private void stopTime() {
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
		}
	class MyBaseAdater extends BaseAdapter{
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return me.recordFiles.size();
		}

		@Override
		public String getItem(int arg0) {
			// TODO Auto-generated method stub
			return me.recordFiles.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return arg0;
		}

		@Override
		public View getView(int arg0, View view, ViewGroup arg2) {
			// TODO Auto-generated method stub
			view=LayoutInflater.from(getApplicationContext()).inflate(R.layout.historylist_item, null);
			name=(TextView) view.findViewById(R.id.name);
			name.setText(MainActivity.recordFiles.get(arg0));
//			name.setText((ArrayList)MainActivity.map.get("recordFiles").);
			timesitem=(TextView) view.findViewById(R.id.timesitem);
			//设置时间
//			Item item=(Item) getItem(arg0);
//			timesitem.setText(item.times);
			return view;
		}
		
	}
	
}
