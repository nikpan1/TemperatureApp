package com.example.temperatureapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.slider.Slider;

public class MainActivity extends AppCompatActivity
{

    int samplingFrequency = 6000;
    int frequency = 2800;
    int hanningSize = 2048;

    double[] x, y, amplitudes;
    double Y_MAX = 0;

    ImageView iv;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;
    TextView maxTextArea, SamplingTextArea, FrequencyTextArea;

    FFT _fft = new FFT(hanningSize);



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        x = new double[hanningSize];
        y = new double[hanningSize];
        amplitudes = new double[hanningSize /2];

        iv = this.findViewById(R.id.PlotCanvas);
        bitmap = Bitmap.createBitmap(hanningSize /2,520,Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        iv.setImageBitmap(bitmap);
        canvas.drawColor(Color.RED);

        Button buttonStart = findViewById(R.id.StartButton);
        Button buttonStop = findViewById(R.id.StopButton);
        Slider FSslider = findViewById(R.id.SamplingSlider);
        Slider Fslider = findViewById(R.id.FrequencySlider);
        maxTextArea = findViewById(R.id.MaxTextArea);
        SamplingTextArea = findViewById(R.id.SamplingTextArea);
        FrequencyTextArea = findViewById(R.id.FrequencyTextArea);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                ComputeAmpl();
                DrawChart();
            }
        });
        buttonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                canvas.drawColor(Color.BLACK);
            }
        });

        FSslider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                samplingFrequency = (int)value;
                SamplingTextArea.setText("samplingFrequency: " + samplingFrequency);
            }
        });

        Fslider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(Slider slider, float value, boolean fromUser) {
                frequency = (int)value;
                FrequencyTextArea.setText("F: " + frequency);
            }
        });


    }

    public void GenerateSignal()
    {
        for(int i = 0; i< hanningSize; i++)
        {
            x[i] = Math.sin(2*Math.PI* frequency *((double)i/samplingFrequency));
            y[i] = 0;
        }
    }

    public void ComputeAmpl()
    {
        GenerateSignal();
        _fft.fft(x,y);
        Y_MAX =0;
        for(int i = 0; i< hanningSize /2; i++)
        {
            amplitudes[i]=x[i]*x[i]+y[i]*y[i];
            if(amplitudes[i]> Y_MAX) Y_MAX = amplitudes[i];
        }
        for(int i = 0; i< hanningSize /2; i++)
        {
            amplitudes[i]= amplitudes[i]*500/ Y_MAX;
        }
        maxTextArea.setText("Value: " + Y_MAX);
    }
    public void DrawChart()
    {
        canvas.drawColor(Color.BLACK);
        paint.setColor(Color.GREEN);
        for(int i = 0; i< amplitudes.length; i++)
        {
            int downy = 510;
            int upy = 510 - (int) amplitudes[i];
            canvas.drawLine(i,downy,i,upy,paint);
        }
    }
}