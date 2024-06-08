package com.example.temperatureapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Debug;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.Slider;

public class MainActivity extends AppCompatActivity
{

    int sampling = 6000;
    int frequency = 2800;
    int hanningSize = 2048;

    double[] x, y, amplitudes;
    double Y_MAX = 0;
    int startPos = 510;

    ImageView imageview;
    Button buttonStart;
    Slider samplingSlider, frequencySlider;
    TextView maxTextArea, temperatureTextArea, SamplingTextArea, FrequencyTextArea;

    Bitmap bitmap;
    Canvas canvas;
    Paint paint;


    FFT _fft = new FFT(hanningSize);
    double temperature = 0;
    Boolean startClicked = false;
    String[] buttonTexts = {"Start", "Stop"};
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int audioMediatype = MediaRecorder.AudioSource.MIC;

    private AudioRecord recorder;
    private boolean isRecording = false;
    private Thread recordingThread, drawingThread;
    boolean isDataReady = false;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        x = new double[hanningSize];
        y = new double[hanningSize];
        amplitudes = new double[hanningSize /2];

        imageview = this.findViewById(R.id.PlotCanvas);
        bitmap = Bitmap.createBitmap(hanningSize /2,664,Bitmap.Config.ARGB_8888); // was earlier 520
        canvas = new Canvas(bitmap);
        paint = new Paint();
        imageview.setImageBitmap(bitmap);
        canvas.drawColor(Color.RED);

        buttonStart = findViewById(R.id.StartButton);
        samplingSlider = findViewById(R.id.SamplingSlider);
        frequencySlider = findViewById(R.id.FrequencySlider);
        maxTextArea = findViewById(R.id.maxTextArea);
        SamplingTextArea = findViewById(R.id.SamplingTextArea);
        FrequencyTextArea = findViewById(R.id.FrequencyTextArea);
        maxTextArea = findViewById(R.id.maxTextArea);
        temperatureTextArea = findViewById(R.id.temperatureTextArea);

        canvas.drawColor(Color.DKGRAY);
        buttonStart.setText(buttonTexts[0]);


        frequencySlider.setValueFrom(0.0f);
        frequencySlider.setValueTo(10000.0f);
        frequencySlider.setValue((float) frequency);
        FrequencyTextArea.setText(String.valueOf(frequency));

        samplingSlider.setValueFrom(0.0f);
        samplingSlider.setValueTo(10000.0f);
        samplingSlider.setValue((float) sampling);
        SamplingTextArea.setText(String.valueOf(sampling));


        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                drawingThread = new Thread(() -> {
                    DrawChart();
                    while(startClicked) {
                        if(isDataReady) {
                            DrawChart();
                        }
                    }
                });

                startClicked = !startClicked;
                buttonStart.setText(buttonTexts[startClicked ? 1 : 0]);

                if(startClicked) {
                    startRecording();
                    drawingThread.start();
                }
                else {
                    stopRecording();
                    drawingThread = null;
                }



            }
        });

        samplingSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                sampling = (int)value;
                SamplingTextArea.setText(String.valueOf(sampling));
            }
        });

        frequencySlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                frequency = (int)value;
                FrequencyTextArea.setText(String.valueOf(frequency));
            }
        });


    }



    public void CalculateAmplitude()
    {
        //GenerateSignal();
        // _fft.fft(x,y);

        _fft.fft(x, y);
        Y_MAX = 0;

        // show only the first half of the generated fft
        for(int i = 0; i < hanningSize / 2; i ++)
        {
            amplitudes[i] = (x[i] * x[i]) + (y[i] * y[i]);
            if (amplitudes[i]> Y_MAX) Y_MAX = amplitudes[i];
        }

        // scale accordingly to the canvas
        // avoiding Y_MAX = 0
        for(int i = 0; i < hanningSize / 2; i ++)
            amplitudes[i]= amplitudes[i] * ((canvas.getHeight() * 0.9) / (Y_MAX + 0.001));
    }


    public void DrawChart()
    {
        canvas.drawColor(Color.DKGRAY);
        paint.setColor(Color.MAGENTA);

        int canvasWidth = canvas.getHeight() - 10;

        for (int p = 0; p < amplitudes.length; p++) {
            canvas.drawLine(p, canvasWidth, p, canvasWidth - (int) amplitudes[p], paint);
        }

        //maxTextArea.setText(String.valueOf(Y_MAX));
        //temperatureTextArea.setText(String.valueOf(temperature));
    }



    private void startRecording() {
        // checks if permissions granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            String[] perms = {Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(this, perms, 0);
        }

        int bufferSize = AudioRecord.getMinBufferSize(sampling, channelConfiguration, audioEncoding);
        recorder = new AudioRecord(audioMediatype, sampling, channelConfiguration, audioEncoding, bufferSize);

        recorder.startRecording();
        isRecording = true;


        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                isDataReady = false;
                int read = recorder.read(buffer, 0, buffer.length);
                if (read > 0) {

                    // normalize given output before fft
                    for (int i = 0; i < hanningSize && i < read; i++) {
                        x[i] = (double) buffer[i] / 32768.0;
                    }
                    CalculateAmplitude();

                }

                isDataReady = true;

                int aSize = 5;
                double [] readings = new double[aSize];
                double avg = 0;

                for (int i = 0; i < readings.length - 1; i++) {
                    readings[i] = readings[i + 1];
                }

                readings[readings.length - 1] = Y_MAX;

                for (int i = 0; i < readings.length; i ++)
                {
                    avg += readings[i];
                }
                temperature = (int)((avg/ aSize) * 1000);

                // thread sleep needs to be in try catch
                try
                {
                    Thread.sleep(1000);
                } catch (Exception e)
                {
                    break;
                }
            }
        });

        recordingThread.start();
    }


    private void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();  // Ensure recording is stopped
    }

    public void GenerateSignal()
    {
        for(int i = 0; i < hanningSize; i++)
        {
            x[i] = Math.sin(2 * Math.PI * frequency * ((double)i / sampling));
            y[i] = 0;
        }
    }
}