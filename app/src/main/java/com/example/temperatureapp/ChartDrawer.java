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

    public Bitmap DrawChart(double[] amplitudes, int thickness)
    {
        double max = 0;
        for (int i = 0; i < amplitudes.length; i++) {
            if (amplitudes[i] > max) max = amplitudes[i];
        }

        canvas.drawColor(Color.DKGRAY);
        int canvasWidth = canvas.getHeight() - 10;

        // Adjust to canvas size
        for (int i = 0; i < probeWindowSize / 2; i++) {
            amplitudes[i] = amplitudes[i] / (max + Double.MIN_VALUE) * (canvasWidth - 16);
        }

        // Drawing amplitudes
        paint.setColor(Color.MAGENTA);
        paint.setStrokeWidth(thickness); // Set the thickness of the paint
        for (int p = 0; p < amplitudes.length; p++) {
            canvas.drawLine(p, canvasWidth, p, canvasWidth - (int) amplitudes[p], paint);
        }

        // Drawing ticks
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(1); // Set thickness for the ticks
        for (int i = 0; i < amplitudes.length; i++) {
            if (i % 32 == 0) {
                paint.setStrokeWidth(thickness); // Thicker tick
                canvas.drawLine(i, canvasWidth, i, canvasWidth - 8, paint);
            }
            if (i % 128 == 0) {
                paint.setStrokeWidth(thickness * 2); // Even thicker tick
                canvas.drawLine(i, canvasWidth, i, canvasWidth - 32, paint);
            }
        }

        return this.bitmap;
    }
}

