package com.example.temperatureapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.Context;
import android.app.Activity;
import android.util.Log;

import java.util.Arrays;

public class AudioHandler {
    Activity activity;
    Object lock;
    Runnable runnable;

    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int audioMediatype = MediaRecorder.AudioSource.DEFAULT;
    private AudioRecord recorder;


    private boolean isRecording = false;
    int sampling, probeWindowSize;
    public double[] x;

    public AudioHandler(Activity _activity, Object _lock, int _sampling, int _probeWindowSize, double[] _x)
    {
        activity = _activity;
        lock = _lock;

        sampling = _sampling;
        probeWindowSize = _probeWindowSize;
        x = _x;

        runnable = new Runnable() {
            @Override
            public void run() {
                recording();
            }
        };

        checkPermissions();
    }


    public void startRecording() {
        checkPermissions();
        int bufferSize = AudioRecord.getMinBufferSize(sampling, channelConfiguration, audioEncoding);
        recorder = new AudioRecord(audioMediatype, sampling, channelConfiguration, audioEncoding, bufferSize);

        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            recorder.startRecording();
            isRecording = true;
        } else {
            Log.e("AudioHandler", "AudioRecord initialization failed");
        }
    }

    public void checkPermissions() {
        // Check if permission is granted
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.RECORD_AUDIO}, 0);
            return; // Exit the function to wait for permission result
        }
    }

    public void stopRecording() {
        isRecording = false;
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }


    public void recording()
    {
        while (isRecording) {
            synchronized (lock) {
                // --------
                readData();
                // --------

                lock.notify();

                // Wait for the second thread to complete
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Sleep to give the second thread a chance to run
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void readData()
    {
        byte[] buffer = new byte[probeWindowSize];
        Arrays.fill(buffer, (byte) 0);

        int read = recorder.read(buffer, 0, buffer.length);
        if (read > 0) {
            // normalize given output before fft
            for (int i = 0; i < probeWindowSize && i < read; i++) {
                x[i] = (double) buffer[i] / 32768.0;
            }
        }
        else {
            Arrays.fill(x, 0);
        }
    }
}