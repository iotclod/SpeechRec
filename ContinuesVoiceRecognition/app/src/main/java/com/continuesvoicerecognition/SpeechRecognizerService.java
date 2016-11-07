package com.continuesvoicerecognition;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SpeechRecognizerService extends Service {

    private SpeechRecognizerManager mSpeechManager;

    private LocalBroadcastManager broadcaster;

    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_STOP_LISTENING = 2;
    static final int MSG_RECOGNIZER_TOGGLE_MUTE = 3;

    static final public String RECOGNIZER_RESULT = "com.continuesvoicerecognition.REQUEST_PROCESSED";
    static final public String RECOGNIZER_MESSAGE = "com.continuesvoicerecognition.CVR_MSG";

    private final static String TAG = "SpeechRecognizerService";

    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
        Message msg = new Message();
        msg.what = SpeechRecognizerService.MSG_RECOGNIZER_START_LISTENING;
        try {
            mServerMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void sendResult(final String message) {
        Intent intent = new Intent(RECOGNIZER_RESULT);
        if (message != null)
            intent.putExtra(RECOGNIZER_MESSAGE, message);
        broadcaster.sendBroadcast(intent);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startListening() {
        sendResult("Locked and Loaded");
        if (mSpeechManager == null) {
            SetSpeechListener();
        } else if (!mSpeechManager.ismIsListening()) {
            mSpeechManager.destroy();
            SetSpeechListener();
        }
    }

    private void stopListening() {
        if (mSpeechManager != null) {
            sendResult(getString(R.string.destroied));
            mSpeechManager.destroy();
            mSpeechManager = null;
        }
    }

    private void toggleMute() {
        if (mSpeechManager != null) {
            if (mSpeechManager.isInMuteMode()) {
                sendResult(getString(R.string.mute));
                mSpeechManager.mute(false);
            } else {
                sendResult(getString(R.string.un_mute));
                mSpeechManager.mute(true);
            }
        }
    }

    protected static class IncomingHandler extends Handler {
        private WeakReference<SpeechRecognizerService> mtarget;

        IncomingHandler(SpeechRecognizerService target) {
            mtarget = new WeakReference<SpeechRecognizerService>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            final SpeechRecognizerService target = mtarget.get();

            switch (msg.what) {
                case MSG_RECOGNIZER_START_LISTENING:
                    target.startListening();
                    break;

                case MSG_RECOGNIZER_STOP_LISTENING:
                    target.stopListening();
                    break;

                case MSG_RECOGNIZER_TOGGLE_MUTE:
                    target.toggleMute();
                    break;
            }
        }
    }

    private void SetSpeechListener() {
        mSpeechManager = new SpeechRecognizerManager(this, new SpeechRecognizerManager.onResultsReady() {
            @Override
            public void onResults(ArrayList<String> results) {
            if (results != null && results.size() > 0) {
                StringBuilder sb = new StringBuilder();
                if (results.size() > 5) {
                    results = (ArrayList<String>) results.subList(0, 5);
                }
                for (String result : results) {
                    sb.append(result).append("\n");
                    sendResult(sb.toString());
                    if ("stop self" == sb.toString()) {
                        sendResult("Sayonara");
                        stopSelf();
                    }
                }
            } else {
                sendResult(getString(R.string.no_results_found));
            }
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service Destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mServerMessenger.getBinder();
    }
}
