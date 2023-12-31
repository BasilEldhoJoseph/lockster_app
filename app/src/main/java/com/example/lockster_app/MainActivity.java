package com.example.lockster_app;

import androidx.appcompat.app.AppCompatActivity;//AppCompatActivity is a base class for activities that use the support library action bar features.

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

import android.os.Handler;//needed for handling delayed tasks using a Handler
import java.text.SimpleDateFormat;//to get date format to name images (here)
import java.util.Date;//
import java.util.*;


//for email
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;





public class MainActivity extends AppCompatActivity implements SensorEventListener
{

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isSecurityCameraEnabled = false;
    private Executor executor = Executors.newSingleThreadExecutor();
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;


    // Email configuration
    private static final String EMAIL_USERNAME = "your_email@gmail.com";
    private static final String EMAIL_PASSWORD = "your_email_password";
    private static final String EMAIL_TO = "basil.eldho1414@gmail.com";
    private static final String EMAIL_SUBJECT = "Security Camera Photo";

    // Session object for email sending
    private Session session;




    @Override// intended to override a method in the parent class (in this case, AppCompatActivity).
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState); //calls the onCreate method of the parent class
        // (AppCompatActivity) to perform necessary setup. It's essential to call this to
        // ensure that the activity's initialization is handled properly.
        setContentView(R.layout.activity_main);


        // Initialize email sending properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        //Properties for configuring the email sending process (SMTP settings) were added in the onCreate method

        // Initialize email session
        session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD);
            }
        });

        Button enableButton = findViewById(R.id.enableButton);//initializes a Button object by finding the button with the ID "enableButton" in the currently set content view. It prepares to listen for clicks on this button.java


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        enableButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
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
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)//selecting front camera
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
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
            // Phone is picked up (you can adjust the threshold)
            if (!capturingImages) {
                capturingImages = true;
                capturedImageCount = 0;
                startImageCapture(); //calls the image capture function each time accelerometer value changes
            }
        } else {
            capturingImages = false;
        }
    }

    private void startImageCapture() {
        final long captureDelayMillis = 1000; // 1 second
        final int maxImagesToCapture = 5;

        if (capturedImageCount < maxImagesToCapture) {
            // Capture an image and schedule the next capture after the delay
            captureImage();
            capturedImageCount++;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startImageCapture();  //loop
                }
            }, captureDelayMillis);
        } else {
            capturingImages = false;
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //storing of image
    private void captureImage()
    {
        // Generate a unique filename based on the current time
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
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // This is where you should call the sendEmail method after capturing the image.
                File savedImageFile = new File(outputFileResults.getSavedUri().getPath());
                sendEmail(savedImageFile); // Send email after capturing the image
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
                // Handle error
            }
        });
    }
    // New method for sending email
    private void sendEmail(File attachment)
    {
        try {
            // Create MimeMessage object
            Message message = new MimeMessage(session);
            // Set sender email address
            message.setFrom(new InternetAddress(EMAIL_USERNAME));
            // Set recipient email address
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO));
            // Set email subject
            message.setSubject(EMAIL_SUBJECT);

            // Create MimeBodyPart for attachment
            BodyPart messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachment);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(attachment.getName());

            // Create Multipart object
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // Set the content of the message
            message.setContent(multipart);

            // Send the email
            Transport.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}