package com.example.temperatureapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.slider.Slider;

import java.util.Arrays;
import android.util.Log;
public class MainActivity extends AppCompatActivity
{
    public static final double a = -3622566.8654117114, b = 918.16;
    public int sampling = 12000, frequency = 2800, probeWindowSize = 2048, startPos = 510;
    double temperature = 0;


    int avgWindow = 10;
    double[] readings = new double[avgWindow];
    public double[] x = new double[probeWindowSize];
    public double[] y = new double[probeWindowSize];
    public double[] amplitudes = new double[probeWindowSize / 2];

    public Boolean startClicked = false;


    public ImageView imageview;
    public Button buttonStart;
    public Slider samplingSlider, frequencySlider;
    public TextView maxTextArea, temperatureTextArea, SamplingTextArea, FrequencyTextArea;


    private Object lock = new Object();
    private Thread recordingThread, drawingThread;
    FFT fft = new FFT(probeWindowSize);
    ChartDrawer chartDrawer;
    AudioHandler audioHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageview = findViewById(R.id.PlotCanvas);
        buttonStart = findViewById(R.id.StartButton);

        maxTextArea = findViewById(R.id.maxTextArea);
        FrequencyTextArea = findViewById(R.id.FrequencyTextArea);
        maxTextArea = findViewById(R.id.maxTextArea);
        temperatureTextArea = findViewById(R.id.temperatureTextArea);

        frequencySlider = findViewById(R.id.FrequencySlider);
        frequencySlider.setValueFrom(0.0f);
        frequencySlider.setValueTo(16000.0f);
        frequencySlider.setValue((float) frequency);
        FrequencyTextArea = findViewById(R.id.FrequencyTextArea);
        FrequencyTextArea.setText(String.valueOf(frequency));

        samplingSlider = findViewById(R.id.SamplingSlider);
        samplingSlider.setValueFrom(0.0f);
        samplingSlider.setValueTo(16000.0f);
        samplingSlider.setValue((float) sampling);
        SamplingTextArea = findViewById(R.id.SamplingTextArea);
        SamplingTextArea.setText(String.valueOf(sampling));

        // fill all arrays with zeros
        Arrays.fill(x, 0);
        Arrays.fill(y, 0);
        Arrays.fill(amplitudes, 0);
        Arrays.fill(readings, 0);


        // initializing chart and audio handler
        chartDrawer = new ChartDrawer(imageview, probeWindowSize);
        audioHandler = new AudioHandler(this, this, lock, sampling, probeWindowSize, x);


        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                startClicked = !startClicked;
                buttonStart.setText(startClicked ? "Stop" : "Start");

                if(startClicked)
                {
                    recordingThread = new Thread(audioHandler.runnable);
                    audioHandler.startRecording();
                    if(!recordingThread.isAlive()) recordingThread.start();

                    drawingThread = new Thread(() -> MainLoop());
                    if(!drawingThread.isAlive()) drawingThread.start();
                }
                else
                {
                    audioHandler.stopRecording();
                    if(recordingThread.isAlive()) recordingThread.interrupt();
                    if(drawingThread.isAlive()) drawingThread.interrupt();

                    try {
                        recordingThread.join();
                        drawingThread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    recordingThread = null;
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

    void MainLoop()
    {
        double Y_MAX, avg;
        while (startClicked) {
            synchronized (lock) {
                // --------

                //GenerateSignal();
                fft.fft(x, y);
                amplitudes = CalculateAmplitude();
                Y_MAX = GetMaxAmplitude(amplitudes);

                avg = MovingAverage(Y_MAX);
                temperature = (a * avg) + b;

                chartDrawer.DrawChart(amplitudes);
                maxTextArea.setText(String.valueOf(Y_MAX));
                temperatureTextArea.setText(String.valueOf(temperature));

                // --------
                lock.notify();

                // Wait for the first thread to complete
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Sleep to give the second thread a chance to run
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private double[] CalculateAmplitude()
    {
        // we are interested only in the first half of the array
        for(int i = 0; i < probeWindowSize / 2; i ++)
        {
            amplitudes[i] = (x[i] * x[i]) + (y[i] * y[i]);
        }

        return amplitudes;
    }

    private double GetMaxAmplitude(double[] amps)
    {
        double result = 0;
        for(int i = 0; i < probeWindowSize / 2; i ++)
        {
            if (amps[i] > result) result = amps[i];
        }
        return result;
    }



    private double MovingAverage(double newValue)
    {
        double sum = 0;

        // shift the array to the left
        for (int i = 0; i < readings.length - 1; i++) {
            readings[i] = readings[i + 1];
        }

        // add the new value to the end
        readings[readings.length - 1] = newValue;

        // calculate the latest average
        for (double reading : readings) {
            sum += reading;
        }

        return sum / readings.length;
    }

    // this was used only for testing purposes
    private void GenerateSignal()
    {
        for(int i = 0; i < probeWindowSize; i ++)
        {
            x[i] = Math.sin(2 * Math.PI * frequency * ((double)i / sampling));
            y[i] = 0;
        }
    }
}