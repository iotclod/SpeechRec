package com.continuesvoicerecognition;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import static com.continuesvoicerecognition.BuildConfig.DEBUG;


/*
        Copyright (c) <2015> <Gal Rom>

        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:



        The above copyright notice and this permission notice shall be included in
        all copies or substantial portions of the Software.



        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
        THE SOFTWARE.
*/

/**
 * Android Docs say very clearly that SpeechRecognition is not intended to use as Continues Speech Recognition.
 * You should try PocketSphinx, a very good library that react to "magic" word and react. the combination between the pocket
 * Sphinx and this implementation is a very good idea.
 * check out PocketSphinx here: https://github.com/cmusphinx/pocketsphinx-android-demo
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private TextView result_tv;
    private Button start_listen_btn, stop_listen_btn, mute;

    private int mBindFlag;
    private Messenger mServiceMessenger;
    private SpeechRecognizerService mSpeechRecognizerService;
    private BroadcastReceiver receiver;

    private final static String TAG = "SpeechRecognizerActivit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent service = new Intent(getBaseContext(), SpeechRecognizerService.class);
        getBaseContext().startService(service);
        mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 0 : Context.BIND_ABOVE_CLIENT;

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(SpeechRecognizerService.RECOGNIZER_MESSAGE);
                result_tv.setText(s);
            }
        };

        setContentView(R.layout.activity_main);
        findViews();
        setClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //bindService(new Intent(this, SpeechRecognizerService.class), mServiceConnection, mBindFlag);
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(SpeechRecognizerService.RECOGNIZER_RESULT)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        if (mServiceMessenger != null) {
            //unbindService(mServiceConnection);
            mServiceMessenger = null;
        }
        super.onStop();
    }

    private void findViews() {
        result_tv = (TextView) findViewById(R.id.result_tv);
        start_listen_btn = (Button) findViewById(R.id.start_listen_btn);
        stop_listen_btn = (Button) findViewById(R.id.stop_listen_btn);
        mute = (Button) findViewById(R.id.mute);
    }


    private void setClickListeners() {
        start_listen_btn.setOnClickListener(this);
        stop_listen_btn.setOnClickListener(this);
        mute.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (PermissionHandler.checkPermission(this, PermissionHandler.RECORD_AUDIO)) {
            Message msg = new Message();

            switch (v.getId()) {
                case R.id.start_listen_btn:
                    msg.what = SpeechRecognizerService.MSG_RECOGNIZER_START_LISTENING;

                    try {
                        mServiceMessenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    result_tv.setText(getString(R.string.you_may_speak));
                    break;
                case R.id.stop_listen_btn:
                    msg.what = SpeechRecognizerService.MSG_RECOGNIZER_STOP_LISTENING;

                    try {
                        mServiceMessenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.mute:
                    msg.what = SpeechRecognizerService.MSG_RECOGNIZER_TOGGLE_MUTE;

                    try {
                        mServiceMessenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else {
            PermissionHandler.askForPermission(PermissionHandler.RECORD_AUDIO, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionHandler.RECORD_AUDIO:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        start_listen_btn.performClick();
                    }
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (DEBUG) {
                Log.d(TAG, "onServiceConnected");
            }
            mServiceMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (DEBUG) {
                Log.d(TAG, "onServiceDisconnected");
            }
            mServiceMessenger = null;
        }
    };
}
