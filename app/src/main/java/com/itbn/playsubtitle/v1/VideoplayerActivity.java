package com.itbn.playsubtitle.v1;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.Context;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.os.Vibrator;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.cardview.*;
import com.google.gson.*;
import com.sun.jna.*;
import io.github.nailik.androidresampler.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import kotlin.*;
import okhttp3.*;
import okio.*;
import org.json.*;
import org.vosk.*;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.MediaPlayer.TrackInfo;
import android.preference.PreferenceManager;
import com.itbn.playsubtitle.v1.utilities.SubtitleView;

public class VideoplayerActivity extends Activity implements SurfaceHolder.Callback, OnCompletionListener, OnPreparedListener, OnSeekCompleteListener, OnVideoSizeChangedListener {
	
	private SurfaceHolder holder;
	private int count;
	
	private MediaPlayer mediaPlayer = new MediaPlayer();
	private Handler handler = new Handler();
	private SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(SketchApplication.getContext());
	private boolean repeatMode = false;
	
	private SurfaceView videoScreenLayout;
	private LinearLayout mainLayout;
	private LinearLayout playerUI1;
	private LinearLayout linear1;
	private ImageView back;
	private TextView deviceTime;
	private ImageView repeat;
	private SubtitleView subtitleView;
	private LinearLayout playerUI2;
	private LinearLayout linear3;
	private LinearLayout linear2;
	private TextView progress;
	private SeekBar seekbar1;
	private TextView duration;
	private ImageView play_pause;
	
	private Vibrator playerUI_btn;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
_hideSystemUI();
		setContentView(R.layout.videoplayer);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		videoScreenLayout = findViewById(R.id.videoScreenLayout);
		mainLayout = findViewById(R.id.mainLayout);
		playerUI1 = findViewById(R.id.playerUI1);
		linear1 = findViewById(R.id.linear1);
		back = findViewById(R.id.back);
		deviceTime = findViewById(R.id.deviceTime);
		repeat = findViewById(R.id.repeat);
		subtitleView = findViewById(R.id.subtitleView);
		playerUI2 = findViewById(R.id.playerUI2);
		linear3 = findViewById(R.id.linear3);
		linear2 = findViewById(R.id.linear2);
		progress = findViewById(R.id.progress);
		seekbar1 = findViewById(R.id.seekbar1);
		duration = findViewById(R.id.duration);
		play_pause = findViewById(R.id.play_pause);
		playerUI_btn = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		linear1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (playerUI2.getVisibility() == View.GONE) {
					_showDeviceTime();
					playerUI2.setVisibility(View.VISIBLE);
					playerUI1.setVisibility(View.VISIBLE);
				}
				else {
					playerUI2.setVisibility(View.GONE);
					playerUI1.setVisibility(View.GONE);
				}
			}
		});
		
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_setVibration();
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
					finish();
				}
				else {
					finish();
				}
			}
		});
		
		repeat.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_setVibration();
				if (!repeatMode) {
					repeatMode = true;
					repeat.setBackgroundColor(0xFFF44336);
				}
				else {
					repeatMode = false;
					repeat.setBackgroundColor(Color.TRANSPARENT);
				}
			}
		});
		
		seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar _param1, int _param2, boolean _param3) {
				final int _progressValue = _param2;
				if (mediaPlayer != null && _param3) {
					mediaPlayer.seekTo(_progressValue * 1000);
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar _param1) {
				mediaPlayer.pause();
				play_pause.setImageResource(R.drawable.ic_play_arrow_white);
			}
			
			@Override
			public void onStopTrackingTouch(SeekBar _param2) {
				mediaPlayer.start();
				play_pause.setImageResource(R.drawable.ic_pause_white);
			}
		});
		
		play_pause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_setVibration();
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.pause();
					play_pause.setImageResource(R.drawable.ic_play_arrow_white);
				}
				else {
					mediaPlayer.start();
					play_pause.setImageResource(R.drawable.ic_pause_white);
				}
			}
		});
	}
	
	private void initializeLogic() {
		
		holder = videoScreenLayout.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		videoScreenLayout.setZOrderMediaOverlay(true);
		
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
		//mediaPlayer.setOnErrorListener(this);
		//mediaPlayer.setOnInfoListener(this);
		mediaPlayer.setOnVideoSizeChangedListener(this);
		
		subtitleView.setPlayer(mediaPlayer);
		subtitleView.setSubSource(getIntent().getStringExtra("subtitle"), MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);
		subtitleView.setTextSize((int)preference.getInt(getString(R.string.font_size_key), 20));
		playerUI2.setVisibility(View.GONE);
		playerUI1.setVisibility(View.GONE);
	}
	
	 @Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mediaPlayer.setDataSource(getIntent().getStringExtra("play_video"));
			mediaPlayer.setDisplay(holder);
			mediaPlayer.setScreenOnWhilePlaying(true);
			mediaPlayer.prepare();
		} catch (Exception e) {
			SketchwareUtil.showMessage(getApplicationContext(), e.getMessage());
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		    mediaPlayer.stop();
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		    int videoWidth = mp.getVideoWidth();
		    int videoHeight = mp.getVideoHeight();
		if (videoHeight < videoWidth) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			mp.start();
		}
		else {
			mp.start();
		}
		seekbar1.setMax((int)mp.getDuration() / 1000);
		duration.setText(ActivityHelper.formatDuration(mp.getDuration()));
		runOnUiThread(new Runnable() {
			public void run() {
				if (mp != null) {
					seekbar1.setProgress((int)mp.getCurrentPosition() / 1000);
					progress.setText(ActivityHelper.formatDuration(mp.getCurrentPosition()));
				}
				handler.postDelayed(this, 1000);
			}
		});
	}
	
	/*public boolean onError(MediaPlayer mp, int whatError, int extra) {
}
public boolean onInfo(MediaPlayer mp, int whatInfo, int extra) {
}*/
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		if (repeatMode) {
			mediaPlayer.start();
		}
		else {
			play_pause.setImageResource(R.drawable.ic_play_arrow_white);
		}
	}
	
	public void onSeekComplete(MediaPlayer mp) {
	}
	
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
	}
	
	public void _hideSystemUI() {
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);
	}
	
	
	public void _showDeviceTime() {
		String currentTime = "";
		Date systemTime = new Date();
		if (preference.getBoolean(getString(R.string.timeformat_key), false)) {
			SimpleDateFormat formatted = new SimpleDateFormat("hh:mm a");
			currentTime = formatted.format(systemTime);
		}
		else {
			SimpleDateFormat formatted = new SimpleDateFormat("HH:mm");
			currentTime = formatted.format(systemTime);
		}
		deviceTime.setText(currentTime);
	}
	
	
	public void _setVibration() {
		if (preference.getBoolean(getString(R.string.vibration_switch_key), true)) {
			playerUI_btn.vibrate((long)(50));
		}
	}
	
}