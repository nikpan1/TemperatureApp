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
    Context context;

    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int audioMediatype = MediaRecorder.AudioSource.MIC;
    private AudioRecord recorder;


    private boolean isRecording = false;
    int sampling, probeWindowSize;
    public double[] x;

    public AudioHandler(Activity _activity, Context _context, Object _lock, int _sampling, int _probeWindowSize, double[] _x)
    {
        activity = _activity;
        context = _context;
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
    }


    public void startRecording() {
        // checks if permissions granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            String[] perms = {Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(activity, perms, 0);
        }

        int bufferSize = AudioRecord.getMinBufferSize(sampling, channelConfiguration, audioEncoding);
        recorder = new AudioRecord(audioMediatype, sampling, channelConfiguration, audioEncoding, bufferSize);

        recorder.startRecording();
        isRecording = true;
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
        byte[] buffer = new byte[probeWindowSize];
        Arrays.fill(buffer, (byte) 0);

        while (isRecording) {
            synchronized (lock) {
                // --------
                readData(buffer);
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
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void readData(byte[] buffer)
    {
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