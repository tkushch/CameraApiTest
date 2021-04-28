package com.example.cameraapitest;

import android.graphics.*;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.Image;
import android.util.Size;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;



public class MainActivity extends AppCompatActivity {

    int w=1920, h=1080;
    public static final String LOG_TAG = "myLogs";


    CameraService[] myCameras = null;

    private CameraManager mCameraManager = null;
    private final int CAMERA1   = 0;
    private final int CAMERA2   = 1;


    private Button mButtonOpenCamera1 = null;
    private Button mButtonOpenCamera2 = null;
    private Button mButtonToMakeShot = null;

    private Button show = null;

    private TextureView mImageView = null;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler = null;



    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }




    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        show = findViewById(R.id.button7);
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "test1.jpg");
                File mFile = new File(getDataDir(), "test1.jpg");
                System.err.println(mFile.exists() + "AAA");
                Bitmap myBitmap = BitmapFactory.decodeFile(mFile.getAbsolutePath());
                setContentView(R.layout.fragment_img);
                ImageView myImage = (ImageView) findViewById(R.id.imageView123);
                myImage.setImageBitmap(myBitmap);
                Log.i(LOG_TAG, "AAA OKEY");
            }
        });

        Log.d(LOG_TAG, "Запрашиваем разрешение");
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                ||
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        )
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }


        mButtonOpenCamera1 =  findViewById(R.id.button4);
        mButtonOpenCamera2 =  findViewById(R.id.button5);
        mButtonToMakeShot =findViewById(R.id.button6);
        mImageView = findViewById(R.id.textureView);

        mButtonOpenCamera1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myCameras[CAMERA2].isOpen()) {myCameras[CAMERA2].closeCamera();}
                if (myCameras[CAMERA1] != null) {
                    if (!myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].openCamera();
                }
            }
        });

        mButtonOpenCamera2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myCameras[CAMERA1].isOpen()) {myCameras[CAMERA1].closeCamera();}
                if (myCameras[CAMERA2] != null) {
                    if (!myCameras[CAMERA2].isOpen()) myCameras[CAMERA2].openCamera();
                }
            }
        });


        mButtonToMakeShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {




                if (myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].makePhoto();
                if (myCameras[CAMERA2].isOpen()) myCameras[CAMERA2].makePhoto();

            }
        });


        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            // Получение списка камер с устройства


            myCameras = new CameraService[mCameraManager.getCameraIdList().length];



            for (String cameraID : mCameraManager.getCameraIdList()) {
                Log.i(LOG_TAG, "cameraID: "+cameraID);
                int id = Integer.parseInt(cameraID);


                // создаем обработчик для камеры
                myCameras[id] = new CameraService(mCameraManager,cameraID);

                // Получениe характеристик камеры
                CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(cameraID);
                // Получения списка выходного формата, который поддерживает камера
                StreamConfigurationMap configurationMap =
                        cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                Size[] sizesJPEG = configurationMap.getOutputSizes(ImageFormat.JPEG);
                w = sizesJPEG[0].getWidth();
                h = sizesJPEG[0].getHeight();





            }
        }
        catch(CameraAccessException e){
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }


    }


    public class CameraService {

//        private File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "test1.jpg");
private File mFile = new File(getDataDir(), "test1.jpg");
        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mCaptureSession;
        private ImageReader mImageReader;




        public CameraService(CameraManager cameraManager, String cameraID) {

            mCameraManager = cameraManager;
            mCameraID = cameraID;

        }

        public void makePhoto (){





            try {
                // This is the CaptureRequest.Builder that we use to take a picture.
                final CaptureRequest.Builder captureBuilder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(mImageReader.getSurface());
                CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {


                    }
                };

                mCaptureSession.stopRepeating();
                mCaptureSession.abortCaptures();
                mCaptureSession.capture(captureBuilder.build(), CaptureCallback, mBackgroundHandler);

            }
            catch (CameraAccessException e) {
                e.printStackTrace();


            }




        }


        private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
                = new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {

                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));


            }

        };


        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(CameraDevice camera) {
                mCameraDevice = camera;
                Log.i(LOG_TAG, "Open camera  with id:"+mCameraDevice.getId());

                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                mCameraDevice.close();

                Log.i(LOG_TAG, "disconnect camera  with id:"+mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.i(LOG_TAG, "error! camera id:"+camera.getId()+" error:"+error);
            }
        };


        private void createCameraPreviewSession() {


            mImageReader = ImageReader.newInstance(w,h, ImageFormat.JPEG,1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);

            SurfaceTexture texture = mImageView.getSurfaceTexture();

            texture.setDefaultBufferSize(w,h);
            Surface surface = new Surface(texture);

            try {
                final CaptureRequest.Builder builder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                builder.addTarget(surface);




                mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mCaptureSession = session;
                                try {
                                    mCaptureSession.setRepeatingRequest(builder.build(),null,mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) { }}, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }




        public boolean isOpen() {
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }

        public void openCamera() {
            try {

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {


                    mCameraManager.openCamera(mCameraID,mCameraCallback,mBackgroundHandler);

                }



            } catch (CameraAccessException e) {
                Log.i(LOG_TAG,e.getMessage());

            }
        }

        public void closeCamera() {

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }



    }


    @Override
    public void onPause() {
        if(myCameras[CAMERA1].isOpen()){myCameras[CAMERA1].closeCamera();}
        if(myCameras[CAMERA2].isOpen()){myCameras[CAMERA2].closeCamera();}
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();


    }


    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}







