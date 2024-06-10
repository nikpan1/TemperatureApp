package com.example.temperatureapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.ImageView;

public class ChartDrawer {
    Canvas canvas;
    Bitmap bitmap;
    Paint paint;
    ImageView imageview;
    int probeWindowSize;

    public ChartDrawer(ImageView _imageView, int _probeWindowsSize) {
        imageview = _imageView;
        probeWindowSize = _probeWindowsSize;

        paint = new Paint();
        bitmap = Bitmap.createBitmap( probeWindowSize / 2,664, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        canvas.drawColor(Color.DKGRAY);

        imageview.setImageBitmap(bitmap);
    }

    public Bitmap DrawChart(double[] amplitudes)
    {
        double max = 0;
        for (int i = 0; i < amplitudes.length; i ++) {
            if(amplitudes[i] > max) max = amplitudes[i];
        }

        canvas.drawColor(Color.DKGRAY);
        int canvasWidth = canvas.getHeight() - 10;

        // adjust to canvas size
        for(int i = 0; i < probeWindowSize / 2; i ++) {
            amplitudes[i] = amplitudes[i] / (max + Double.MIN_VALUE) * (canvasWidth - 16);
        }

        // drawing amplitudes
        paint.setColor(Color.MAGENTA);
        for (int p = 0; p < amplitudes.length; p ++) {
            canvas.drawLine(p, canvasWidth, p, canvasWidth - (int) amplitudes[p], paint);
        }

        // Drawing ticks
        paint.setColor(Color.GREEN);
        for (int i = 0; i < amplitudes.length; i ++) {
            if (i % 32 == 0) {
                canvas.drawLine(i, canvasWidth, i, canvasWidth - 8, paint);
            }
            if (i % 128 == 0) {
                canvas.drawLine(i, canvasWidth, i, canvasWidth - 32, paint);
            }
        }

        return this.bitmap;
    }
}
