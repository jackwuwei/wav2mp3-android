package me.wuwei.wav2mp3;

import android.os.Handler;
import android.os.Message;

public class LibLAME {
	private static final int LAME_OKAY = 0;
	private static final int LAME_GENERICERROR = -1;
	private static final int LAME_NOMEM = -10;
	private static final int LAME_BADBITRATE       = -11;
	private static final int LAME_BADSAMPFREQ      = -12;
	private static final int LAME_INTERNALERROR    = -13;

	private static final int FRONTEND_READERROR    = -80;
	private static final int FRONTEND_WRITEERROR   = -81;
	private static final int FRONTEND_FILETOOLARGE = -82;
	
	public static final int EVENT_ID_START_ENCODE = 0;
	public static final int EVENT_ID_ENCODING_PROGRESS = 1;
	public static final int EVENT_ID_ENCODE_FINISHED = 2;
	public static final int EVENT_ID_ENCODE_ABORTED = 3;

	private int mNativeLameGF = 0;
	private Handler mEventHandler = null;
	private int mSampleRate = 44100;
	private int mBitrate = 128;
	private boolean mIsVbr = false;
	private String mInputFilePath;
	private String mOutputFilePath;
	private boolean mEncoding = false;
	private final Object mMutex = new Object();
	
	public LibLAME() {
		mNativeLameGF = nativeLameInit();
	}
	
	@Override
	protected void finalize() throws Throwable {
		nativeLameClose(mNativeLameGF);
		mNativeLameGF = 0;
		
		super.finalize();
	}
	
	public int getSampleRate() {
		return mSampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.mSampleRate = sampleRate;
	}

	public boolean isIsVbr() {
		return mIsVbr;
	}

	public void setIsVbr(boolean isVbr) {
		this.mIsVbr = isVbr;
	}

	public int getBitrate() {
		return mBitrate;
	}

	public void setBitrate(int bitrate) {
		this.mBitrate = bitrate;
	}
	
	public void encode(String inputFilePath, String outputFilePath, Handler handler) {
		mEventHandler = handler;
		mInputFilePath = inputFilePath;
		mOutputFilePath = outputFilePath;
		
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (mNativeLameGF != 0) {
					nativeEncode(mNativeLameGF, mInputFilePath, mOutputFilePath, mIsVbr, mBitrate, mSampleRate);
				}
			}
		});
		thread.start();
	}
	
	public void cancel() {
		synchronized(mMutex) {
			mEncoding = false;
		}
	}
	
	public boolean isEncoding() {
		return mEncoding;
	}
	
	private void Callback(int eventId, int progress) {
		Message msg = new Message();
		msg.what = eventId;
		msg.arg1 = progress;
		
		switch (eventId) {
		case EVENT_ID_START_ENCODE:
			synchronized(mMutex) {
				mEncoding = true;
			}
			break;
		case EVENT_ID_ENCODE_FINISHED:
		case EVENT_ID_ENCODE_ABORTED:
			synchronized(mMutex) {
				mEncoding = false;
			}
			break;
		}
		
		if (mEventHandler != null)
			mEventHandler.sendMessage(msg);
	}
	
	private native int nativeLameInit();
	private native void nativeLameClose(int gf);
	private native int nativeEncode(int gf, String inputFilePath, String outputFilePath, boolean isVbr, int bitrate, int sampleRate);
	
	static {
		System.loadLibrary("lame");
	}
}
