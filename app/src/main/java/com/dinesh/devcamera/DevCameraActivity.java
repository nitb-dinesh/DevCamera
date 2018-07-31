package com.dinesh.devcamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import static android.content.ContentValues.TAG;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;


@SuppressWarnings("deprecation")
public class DevCameraActivity extends Activity implements SensorEventListener {
	private Camera mCamera;
	private CameraPreview mPreview;
	private SensorManager sensorManager = null;
	private int orientation;
	private ExifInterface exif;
	private int deviceHeight;
	private int deviceWidth;
	private Button ibRetake;
	private Button ibUse;
	private Button ibCapture;
	private FrameLayout flBtnContainer;
	private File sdRoot;
	private String dir;
	private String fileName;
	private ImageView rotatingImage;
	private int degrees = -1;

	CameraNew cameraNew;
	CameraSupport cameraSupport;

	private boolean isRecording = false;
	String[] mPermission = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
			android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,
			Manifest.permission.CAMERA,
			};
	private static final int REQUEST_CODE_PERMISSION = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (ActivityCompat.checkSelfPermission(this, mPermission[0])
				!= MockPackageManager.PERMISSION_GRANTED ||
				ActivityCompat.checkSelfPermission(this, mPermission[1])
						!= MockPackageManager.PERMISSION_GRANTED ||
				ActivityCompat.checkSelfPermission(this, mPermission[2])
						!= MockPackageManager.PERMISSION_GRANTED||
				ActivityCompat.checkSelfPermission(this, mPermission[3])
						!= MockPackageManager.PERMISSION_GRANTED ) {

			ActivityCompat.requestPermissions(this,
					mPermission, REQUEST_CODE_PERMISSION);

			// If any permission aboe not allowed by user, this condition will execute every tim, else your else part will work
		}

		initUI();
	}

    private void initUI() {
        // new camera2 configuration by Dinesh
		/*cameraNew = new CameraNew(this);
		cameraSupport = cameraNew.open(Camera.CameraInfo.CAMERA_FACING_BACK);*/


        // Setting all the path for the image
        sdRoot = Environment.getExternalStorageDirectory();
        dir = "/DCIM/Camera/";
        dir = Environment.DIRECTORY_PICTURES+"/";

        // Getting all the needed elements from the layout
        rotatingImage = (ImageView) findViewById(R.id.imageView1);
        ibRetake = (Button) findViewById(R.id.ibRetake);
        ibUse = (Button) findViewById(R.id.ibUse);
        ibCapture = (Button) findViewById(R.id.ibCapture);
        flBtnContainer = (FrameLayout) findViewById(R.id.flBtnContainer);

        // Getting the sensor service.
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Selecting the resolution of the Android device so we can create a
        // proportional preview
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        deviceHeight = display.getHeight();
        deviceWidth = display.getWidth();

        // Add a listener to the Capture button
/*		ibCapture.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//mCamera.takePicture(null, null, mPicture);
				//Toast.makeText(DevCameraActivity.this, "you press onclicked ", Toast.LENGTH_SHORT).show();
			}
		});*/

        // Add a listener to the Capture button for video

        ibCapture.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                if (isRecording) {
                    // stop recording and release camera
                    mMediaRecorder.stop();  // stop the recording
                    releaseMediaRecorder(); // release the MediaRecorder object
                    //mCamera.lock();         // take camera access back from MediaRecorder
                    isRecording = false;

                    mCamera.startPreview();
                    // inform the user that recording has stopped
                    //setCaptureButtonText("Capture");
                    //Toast.makeText(DevCameraActivity.this, "Recording stop..", Toast.LENGTH_SHORT).show();

                } else {
                    // initialize video camera
                    if (prepareVideoRecorder()) {
                        // Camera is available and unlocked, MediaRecorder is prepared,
                        // now you can start recording
                        mMediaRecorder.start();

                        // inform the user that recording has started
                        //setCaptureButtonText("Stop");
                        //Toast.makeText(DevCameraActivity.this, "Recording start..", Toast.LENGTH_SHORT).show();
                        isRecording = true;
                    } else {
                        // prepare didn't work, release the camera
                        releaseMediaRecorder();
                        // inform user
                    }
                }
                return true;
            }
        });

        // add a  listner to capture button for video  release touch

        ibCapture.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    //Toast.makeText(DevCameraActivity.this, "You press touch up...", Toast.LENGTH_SHORT).show();
                    // pressed

                    if (isRecording) {
                        // stop recording and release camera
                        mMediaRecorder.stop();  // stop the recording
                        releaseMediaRecorder(); // release the MediaRecorder object
                        //mCamera.lock();         // take camera access back from MediaRecorder

                        releaseCamera();

                        // removing the inserted view - so when we come back to the app we
                        // won't have the views on top of each other.
                        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
                        preview.removeViewAt(0);
                        createCamera();
                        //mCamera.startPreview();
                        // inform the user that recording has stopped
                        //setCaptureButtonText("Capture");
                        //Toast.makeText(DevCameraActivity.this, "Recording stop..", Toast.LENGTH_SHORT).show();
                        isRecording = false;
                    } else {
                        // take pic camera
                        mCamera.takePicture(null, null, mPicture);

                    }

                    //stopRecordingVideo();      // release
                } else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    //Toast.makeText(DevCameraActivity.this, "You press touch Down...", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        // Add a listener to the Retake button
        ibRetake.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Deleting the image from the SD card/
                File discardedPhoto = new File(sdRoot, dir + fileName);
                discardedPhoto.delete();

                // Restart the camera preview.
                mCamera.startPreview();

                // Reorganize the buttons on the screen
                flBtnContainer.setVisibility(LinearLayout.VISIBLE);
                ibRetake.setVisibility(LinearLayout.GONE);
                ibUse.setVisibility(LinearLayout.GONE);
            }
        });

        // Add a listener to the Use button
        ibUse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Everything is saved so we can quit the app.

                mCamera.startPreview();

                // Reorganize the buttons on the screen
                flBtnContainer.setVisibility(LinearLayout.VISIBLE);
                ibRetake.setVisibility(LinearLayout.GONE);
                ibUse.setVisibility(LinearLayout.GONE);
                //finish();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
	private void createCamera() {
		// Create an instance of Camera
		mCamera = getCameraInstance();

		// Setting the right parameters in the camera

		Camera.Parameters params = mCamera.getParameters();
		//android.hardware.Camera.Parameters parameters = camera.getParameters();
		//Camera.Size size = params.getPictureSize();


		List<Camera.Size> sizes = params.getSupportedPictureSizes();
		Camera.Size size = sizes.get(0);
//Camera.Size size1 = sizes.get(0);
		for(int i=0;i<sizes.size();i++)
		{

			if(sizes.get(i).width > size.width)
				size = sizes.get(i);


		}


		int height = size.height;
		int width = size.width;

		params.setPictureSize(width, height);
		params.setPictureFormat(PixelFormat.JPEG);
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		//params.setPictureSize(size.width, size.height);
		//params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
		//params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
		params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
		params.setExposureCompensation(0);
		params.setPictureFormat(ImageFormat.JPEG);
		params.setJpegQuality(10);

		if(this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
			params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
		}
		params.setJpegQuality(100);
		mCamera.setParameters(params);

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

		// Calculating the width of the preview so it is proportional.
		/*float widthFloat = (float) (deviceHeight) * 4 / 3;
		int widthApx = Math.round(widthFloat);*/

		// Resizing the LinearLayout so we can make a proportional preview. This
		// approach is not 100% perfect because on devices with a really small
		// screen the the image will still be distorted - there is place for
		// improvment.
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int)(deviceWidth*.92), (int)(deviceHeight*1));
		preview.setLayoutParams(layoutParams);

		// Adding the camera preview after the FrameLayout and before the button
		// as a separated element.
		preview.addView(mPreview, 0);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		Log.e("Req Code", "" + requestCode);
		if (requestCode == REQUEST_CODE_PERMISSION) {
			if (grantResults.length == 4 &&
					grantResults[0] == MockPackageManager.PERMISSION_GRANTED &&
					grantResults[1] == MockPackageManager.PERMISSION_GRANTED &&
					grantResults[2] == MockPackageManager.PERMISSION_GRANTED&&
					grantResults[3] == MockPackageManager.PERMISSION_GRANTED
					)

			{
				//timer.schedule(tt, SPLASH_TIME);
				//new Handler().postDelayed(mRunnable, SPLASH_TIME_OUT);

			} else {
				//timer.schedule(tt, SPLASH_TIME);
				// new Handler().postDelayed(mRunnable, SPLASH_TIME_OUT);
			}

		}
	}


	@Override
	protected void onResume() {
		super.onResume();

		// Test if there is a camera on the device and if the SD card is
		// mounted.
		if (!checkCameraHardware(this)) {
			Intent i = new Intent(this, NoCamera.class);
			startActivity(i);
			finish();
		} else if (!checkSDCard()) {
			Intent i = new Intent(this, NoSDCard.class);
			startActivity(i);
			finish();
		}

		// Creating the camera
		createCamera();

		// Register this class as a listener for the accelerometer sensor
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// release the camera immediately on pause event
		releaseCamera();

		// removing the inserted view - so when we come back to the app we
		// won't have the views on top of each other.
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.removeViewAt(0);
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	/** Check if this device has a camera */
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	private boolean checkSDCard() {
		boolean state = false;

		String sd = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(sd)) {
			state = true;
		}

		return state;
	}

	/**
	 * A safe way to get an instance of the Camera object.
	 */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			// attempt to get a Camera instance
			c = Camera.open();
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}

		// returns null if camera is unavailable
		return c;
	}

	private PictureCallback mPicture = new PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {

			if(isRecording){
				return;
			}

			// Replacing the button after a photho was taken.
			flBtnContainer.setVisibility(View.GONE);
			ibRetake.setVisibility(View.VISIBLE);
			ibUse.setVisibility(View.VISIBLE);

			// File name of the image that we just took.
			fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).toString() + ".jpg";

			// Creating the directory where to save the image. Sadly in older
			// version of Android we can not get the Media catalog name
			File mkDir = new File(sdRoot, dir);
			mkDir.mkdirs();

			// Main file where to save the data that we recive from the camera
			File pictureFile = new File(sdRoot, dir + fileName);

			try {
				FileOutputStream purge = new FileOutputStream(pictureFile);
				purge.write(data);
				purge.close();
				//Toast.makeText(DevCameraActivity.this, pictureFile.toString(), Toast.LENGTH_SHORT).show();
			} catch (FileNotFoundException e) {
				Log.d("DG_DEBUG", "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d("DG_DEBUG", "Error accessing file: " + e.getMessage());
			}

			// Adding Exif data for the orientation. For some strange reason the
			// ExifInterface class takes a string instead of a file.
			try {
				exif = new ExifInterface("/sdcard/" + dir + fileName);
				exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + orientation);
				exif.saveAttributes();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	};

	/**
	 * Putting in place a listener so we can get the sensor data only when
	 * something changes.
	 */
	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				RotateAnimation animation = null;
				if (event.values[0] < 4 && event.values[0] > -4) {
					if (event.values[1] > 0 && orientation != ExifInterface.ORIENTATION_ROTATE_90) {
						// UP
						orientation = ExifInterface.ORIENTATION_ROTATE_90;
						animation = getRotateAnimation(270);
						degrees = 270;
					} else if (event.values[1] < 0 && orientation != ExifInterface.ORIENTATION_ROTATE_270) {
						// UP SIDE DOWN
						orientation = ExifInterface.ORIENTATION_ROTATE_270;
						animation = getRotateAnimation(90);
						degrees = 90;
					}
				} else if (event.values[1] < 4 && event.values[1] > -4) {
					if (event.values[0] > 0 && orientation != ExifInterface.ORIENTATION_NORMAL) {
						// LEFT
						orientation = ExifInterface.ORIENTATION_NORMAL;
						animation = getRotateAnimation(0);
						degrees = 0;
					} else if (event.values[0] < 0 && orientation != ExifInterface.ORIENTATION_ROTATE_180) {
						// RIGHT
						orientation = ExifInterface.ORIENTATION_ROTATE_180;
						animation = getRotateAnimation(180);
						degrees = 180;
					}
				}
				if (animation != null) {
					rotatingImage.startAnimation(animation);
				}
			}

		}
	}

	/**
	 * Calculating the degrees needed to rotate the image imposed on the button
	 * so it is always facing the user in the right direction
	 *
	 * @param toDegrees
	 * @return
	 */
	private RotateAnimation getRotateAnimation(float toDegrees) {
		float compensation = 0;

		if (Math.abs(degrees - toDegrees) > 180) {
			compensation = 360;
		}

		// When the device is being held on the left side (default position for
		// a camera) we need to add, not subtract from the toDegrees.
		if (toDegrees == 0) {
			compensation = -compensation;
		}

		// Creating the animation and the RELATIVE_TO_SELF means that he image
		// will rotate on it center instead of a corner.
		RotateAnimation animation = new RotateAnimation(degrees, toDegrees - compensation, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

		// Adding the time needed to rotate the image
		animation.setDuration(250);

		// Set the animation to stop after reaching the desired position. With
		// out this it would return to the original state.
		animation.setFillAfter(true);

		return animation;
	}

	/**
	 * STUFF THAT WE DON'T NEED BUT MUST BE HEAR FOR THE COMPILER TO BE HAPPY.
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private MediaRecorder mMediaRecorder;



	@TargetApi(Build.VERSION_CODES.FROYO)
	private boolean prepareVideoRecorder(){

		mCamera = getCameraInstance();
		mMediaRecorder = new MediaRecorder();

		// Step 1: Unlock and set camera to MediaRecorder
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);

		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

		// Step 3: Set output format and encoding (for versions prior to API Level 8)
		//mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		//mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		// mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

		//Toast.makeText(this, getOutputMediaFile(MEDIA_TYPE_VIDEO).toString(), Toast.LENGTH_SHORT).show();

		// Step 5: Set the preview output
		mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

		// Step 6: Prepare configured MediaRecorder
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;
	}

	private void releaseMediaRecorder(){
		if (mMediaRecorder != null) {
			mMediaRecorder.reset();   // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			//mCamera.lock();           // lock camera for later use
		}
	}

/*    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }*/

	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
		return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	@TargetApi(Build.VERSION_CODES.FROYO)
	private static File getOutputMediaFile(int type){
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), "MyCameraApp");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE){
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"IMG_"+ timeStamp + ".jpg");
		} else if(type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"VID_"+ timeStamp + ".mp4");
		} else {
			return null;
		}
		return mediaFile;
	}
}