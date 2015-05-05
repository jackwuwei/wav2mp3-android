package me.wuwei.wav2mp3;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String INPUT_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/test.wav";
	private static final String OUTPUT_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/test.mp3";
	private LibLAME mLibLame = null;
	private Button mBtnStart = null;
	private ProgressBar mPrgEncode = null;
	private Handler mEventHandler = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		TextView tv = (TextView)findViewById(R.id.textViewInfo);
		tv.setText(String.format(getString(R.string.title_textview_info), INPUT_FILE_PATH, OUTPUT_FILE_PATH));
		
		mBtnStart = (Button)findViewById(R.id.button_start);
		mBtnStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mLibLame.isEncoding()) {
					stopEncode();
				} else {
					startEncode();
				}
			}
		});
		
		mPrgEncode = (ProgressBar)findViewById(R.id.progressBarEncode);
		
		mLibLame = new LibLAME();
		
		mEventHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
				case LibLAME.EVENT_ID_ENCODE_FINISHED:
				case LibLAME.EVENT_ID_ENCODE_ABORTED:
					mPrgEncode.setProgress(0);
				case LibLAME.EVENT_ID_START_ENCODE:
					refreshUI();
					break;
				case LibLAME.EVENT_ID_ENCODING_PROGRESS:
					mPrgEncode.setProgress(msg.arg1);
					break;
				}
				super.handleMessage(msg);
			}
			
		};
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void startEncode() {
		changeLameParam();
		mLibLame.encode(INPUT_FILE_PATH, OUTPUT_FILE_PATH, mEventHandler);
	}
	
	private void stopEncode() {
		mLibLame.cancel();
	}
	
	private void refreshUI() {
		if (mLibLame.isEncoding()) {
			mBtnStart.setText(R.string.title_button_stop);
		} else {
			mBtnStart.setText(R.string.title_button_start);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent intent = new Intent(this, PreferencesActivity.class);
            startActivity(intent);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void changeLameParam() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int samplerate = Integer.parseInt(prefs.getString("sample_rate", "44100"));
		int bitrate = Integer.parseInt(prefs.getString("bitrate", "128"));
		boolean isVbr = prefs.getBoolean("is_vbr", false);
		
		if (mLibLame != null) {
			mLibLame.setBitrate(bitrate);
			mLibLame.setSampleRate(samplerate);
			mLibLame.setIsVbr(isVbr);
		}
	}

	@Override
	protected void onDestroy() {
		mLibLame = null;
		super.onDestroy();
	}
}
