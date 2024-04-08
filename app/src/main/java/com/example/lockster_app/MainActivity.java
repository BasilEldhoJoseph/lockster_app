package com.example.lockster_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.os.Handler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isSecurityCameraEnabled = false;
    private Executor executor = Executors.newSingleThreadExecutor();
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button enableButton = findViewById(R.id.enableButton);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSecurityCamera();
            }
        });

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        ImageCapture.Builder builder = new ImageCapture.Builder();
        imageCapture = builder.build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        cameraProvider.unbindAll();

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleSecurityCamera() {
        if (isSecurityCameraEnabled) {
            disableSecurityCamera();
        } else {
            enableSecurityCamera();
        }
    }

    private void enableSecurityCamera() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            isSecurityCameraEnabled = true;
            Toast.makeText(this, "Security camera enabled.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No accelerometer sensor available.", Toast.LENGTH_SHORT).show();
        }
    }

    private void disableSecurityCamera() {
        sensorManager.unregisterListener(this);
        isSecurityCameraEnabled = false;
        Toast.makeText(this, "Security camera disabled.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission not granted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean capturingImages = false;
    private int capturedImageCount = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        float zAcceleration = event.values[2];

        if (zAcceleration > 9.0f) {
            if (!capturingImages) {
                capturingImages = true;
                capturedImageCount = 0;
                startImageCapture();
            }
        } else {
            capturingImages = false;
        }
    }

    private void startImageCapture() {
        final long captureDelayMillis = 1000;
        final int maxImagesToCapture = 5;

        if (capturedImageCount < maxImagesToCapture) {
            captureImage();
            capturedImageCount++;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startImageCapture();
                }
            }, captureDelayMillis);
        } else {
            capturingImages = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void captureImage() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timestamp + ".jpg";

        File file = new File(getExternalMediaDirs()[0], imageFileName);
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // Handle image saved
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                // Handle error
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}
