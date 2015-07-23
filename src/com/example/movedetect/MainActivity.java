
package com.example.movedetect;

import com.example.movedetect.R;
import com.example.movedetect.BallPanel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

/**
 * 
 * 
 *autor: Carla Marisa Lobo Simoes, Ana Catarina Marinho Ribeiro 
 */
public class MainActivity extends Activity implements SensorEventListener, OnClickListener{
 
    private TextView tv;
    private TextView tv1;
    private DecimalFormat df;
  
    private SensorManager mSensorManager;
    private Sensor mGyroSensor;
    private Sensor mAccelSensor;
    public static final float EPSILON = 0.000000001f;
    private static final int MIN_SAMPLE_COUNT = 30;
    private static final int MEAN_FILTER_WINDOW = 10;
    private float[] initialRotationMatrix;
    //Uncalibrated 
    private float[] deltaRotationMatrix;
    private float[] currentRotationMatrix;
    private float[] gyroscopeOrientation;
    private float[] deltaRotationVector = new float[4];
    

	// Calibrated maths.
	private float[] currentRotationMatrixCalibrated;
	private float[] deltaRotationMatrixCalibrated;
	private float[] deltaRotationVectorCalibrated;
	private float[] gyroscopeOrientationCalibrated;

    
    static final String LOG_TAG = "GYROCAPTURE";
    static final String SAMPLING_SERVICE_ACTIVATED_KEY = "samplingServiceActivated";
	static final String GYROACCEL_KEY = "sampleCounter";

 // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    
    private float timestamp;
    
    private Button bAbout;
    private Button bSeeAxis;
    private Button bExit;
    
    public double pointX1;
    public double pointX2;
    
    public double pointY1;
    public double pointY2;
    
    public int count=0;
    
    public MediaPlayer mp;
    
    private boolean checkedX=false;
    private boolean checkedY=false;
    
    public static BallPanel ballPanel = null;
    private Handler uiHandler;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

    	
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uiHandler = new Handler();
  
        // Get an instance of the sensor service
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGyroSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccelSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
         
        PackageManager PM= this.getPackageManager();
        boolean gyro = PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        boolean accel = PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
         
        if(gyro && !accel){
        	Toast.makeText(getApplicationContext(),"Gyroscope sensor is present in this device but not accelerometer", Toast.LENGTH_LONG).show();
        }
        else if(gyro && accel){
        	Toast.makeText(getApplicationContext(),"Gyroscope sensor and accelerometer are present in this device", Toast.LENGTH_LONG).show();
        	
        }
        else{
        	Toast.makeText(getApplicationContext(),"Sorry, can't do nothing...Your device doesn't have gyroscope sensor", Toast.LENGTH_LONG).show();
        }
        
        bSeeAxis= (Button)findViewById(R.id.button1);
        bSeeAxis.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchStart();
				}
		});
        
        bAbout= (Button) findViewById(R.id.button2);
        bAbout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchAbout();
				}
		});
        
        bExit= (Button) findViewById(R.id.button3);
        bExit.setOnClickListener(this);
        
       //ballPanel.setVisibility( View.VISIBLE);
        
       ballPanel = (BallPanel)findViewById( R.id.ball);
        
        CheckBox cbX = (CheckBox)findViewById( R.id.checkBox2 );
        cbX.setOnClickListener( new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				CheckBox cbX = (CheckBox)v;
				boolean isChecked = cbX.isChecked();
				
				if( isChecked  && !checkedY) {
					checkedX=true;
					Log.d( LOG_TAG, "sampling activated" );
					
					verifyYMove();
				} else {
					checkedX=false;
					Log.d( LOG_TAG, "sampling deactivated" );
					
				
				}
				
			}
		});
        
        
        CheckBox cbY = (CheckBox)findViewById( R.id.checkBox1 );
        cbY.setOnClickListener( new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				CheckBox cbY = (CheckBox)v;
				boolean isChecked = cbY.isChecked();
				if( isChecked ) {
					Log.d( LOG_TAG, "sampling activated" );
					verifyYMove();
					checkedY=true;
				} else {
					Log.d( LOG_TAG, "sampling deactivated" );
					checkedY=false;
				}
				
			}
		});
        
        //if(checkedX && checkedY) tv1.setText("\n Please unselect one of the checkbox.");
        //if(!checkedX && !checkedY) tv1.setText("\n Please select one of the checkbox and start.");

        Log.d( LOG_TAG, "onCreate");
       
        tv= (TextView)findViewById(R.id.txt2);
        
        tv1= (TextView)findViewById(R.id.txt4);
        
        initMaths();
     
        
        
    }
    public void launchAbout(){
    	Intent i= new Intent(this, About.class);
    	startActivity(i);
    }
    public void launchStart(){
    	Intent i= new Intent(this, Start.class);
    	startActivity(i);
    	//setContentView(R.layout.start);
    }
    public void launchExit(){
    	
    	onResume();
    }
 
   
    public final boolean checkCoordinates(){
    	
    	if(deltaRotationVector[2]==0.00 || deltaRotationVector[2]==-0.00 && deltaRotationVector[1]==0.00 || deltaRotationVector[1]==-0.00 && deltaRotationVector[0]==0.00 || deltaRotationVector[0]==-0.00 ){
    		return true;
    	}
    	return false;
    }
  

 
    @Override
    public final void onSensorChanged(SensorEvent event) {
    	
    	if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
		{
    		int i=0;
    		 
       	 if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                double axisX = event.values[0];
                double axisY = event.values[1];
                double axisZ = event.values[2];
                double funct =(axisX*axisX + axisY*axisY + axisZ*axisZ);
                // Calculate the angular speed of the sample
                double omegaMagnitude = Math.sqrt(funct);
                
                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > EPSILON) {
                  axisX /= omegaMagnitude;
                  axisY /= omegaMagnitude;
                  axisZ /= omegaMagnitude;
                }
                
                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                double thetaOverTwo =  (omegaMagnitude * dT / 2.0f);
                //
                double sinThetaOverTwo = Math.sin(thetaOverTwo);
                
                double cosThetaOverTwo = Math.cos(thetaOverTwo);
                //deltarotationVector in rad2/s
                deltaRotationVectorCalibrated[0] = (float) (sinThetaOverTwo * axisX);
                deltaRotationVectorCalibrated[1] = (float) (sinThetaOverTwo * axisY);
                deltaRotationVectorCalibrated[2] = (float) (sinThetaOverTwo * axisZ);
                deltaRotationVectorCalibrated[3] = (float) cosThetaOverTwo;
              }
              timestamp = event.timestamp;
              
              
          	SensorManager.getRotationMatrixFromVector(
					deltaRotationMatrixCalibrated,
					deltaRotationVectorCalibrated);

			currentRotationMatrixCalibrated = matrixMultiplication(
					currentRotationMatrixCalibrated,
					deltaRotationMatrixCalibrated);

			SensorManager.getOrientation(currentRotationMatrixCalibrated,
					gyroscopeOrientationCalibrated);
			
			
			diff(gyroscopeOrientationCalibrated[0],gyroscopeOrientationCalibrated[1],gyroscopeOrientationCalibrated[2]);
			
			
			tv.setText("\n\n\n X(Roll):" + (new DecimalFormat("#.##").format((double)gyroscopeOrientationCalibrated[0]))
                    + " " + " Y(Pitch):"
                    + (new DecimalFormat("#.##").format((double)gyroscopeOrientationCalibrated[1]))+ " "
                    + " Z(Yaw):" + (new DecimalFormat("#.##").format((double)gyroscopeOrientationCalibrated[2])));
			
              if(i==0){
           	   pointX2=(double)gyroscopeOrientationCalibrated[0];
           	   pointY2=(double)gyroscopeOrientationCalibrated[1];
           	tv.setText("\n\n\n X(Roll):" + (new DecimalFormat("#.##").format((double)gyroscopeOrientationCalibrated[0]))
                    + " " + " Y(Pitch):"
                    + (new DecimalFormat("#.##").format((double)gyroscopeOrientationCalibrated[1]))+ " "
                    + " Z(Yaw):" + (new DecimalFormat("#.##").format((double)gyroscopeOrientationCalibrated[2])));
           	   
           	   i++;
              }
              else{
           	   		pointX1=pointX2;
           	   		pointY1=pointY2;
           	   		pointX2=(double)gyroscopeOrientationCalibrated[0];
           	   		pointY2=(double)gyroscopeOrientationCalibrated[1];
           	   	tv.setText("\n\n\nX(Roll):" + (new DecimalFormat("#.##").format((double)gyroscopeOrientationCalibrated[0]))
                        + " " + " Y(Pitch):"
                        + (new DecimalFormat("#.##").format((double)gyroscopeOrientationCalibrated[1]))+ " "
                        + " Z(Yaw):" + (new DecimalFormat("#.##").format((double)gyroscopeOrientationCalibrated[2])));
              	   
              }
		}
    	
    }
    
    public void diff(double x, double y, double z){
  		if( ballPanel != null ) {
  			SurfaceHolder holder = ballPanel.getHolder();
  			Canvas c = holder.lockCanvas();
  			if( c != null ) {
  				ballPanel.drawBall(c, true, (float)x, (float)y, (float)z);
  				
  				holder.unlockCanvasAndPost(c);
  			}
  		}
  	}
   
    public void verifyXMove(){
    	
    	double diffX;
    	
    	if(Math.abs(pointX2-pointX1)>=0.15 && Math.abs(pointX2-pointX1)<=0.75 && Math.abs(pointY2-pointY1)<0.17 && count>5){
    		count++;
    		diffX=(pointX2-pointX1);
    		
    		tv1.setText("\nOrientation X Diff (Roll) :" + (new DecimalFormat("#.##").format(diffX)) +"\n Count :" + count);
    		soundAllert();
    	}
    	else if(count <5)
    		tv1.setText("\n Try again");
    	else {
    		tv1.setText("\nVery Good!!!Try again with other axis Y !");
    		soundAllert();
    		count=0;
    		
    	}
    	
  
	}
    public void verifyYMove(){
    	
    	double diffY;
    	if(Math.abs(pointY2-pointY1)>=0.10 && Math.abs(pointX2-pointX1)<0.20 && count <5){
    		count++;
    		diffY=(pointY2-pointY1);
    		tv1.setText("\nOrientation Y Diff (Pitch) :" + (new DecimalFormat("#.##").format(diffY)) +"\n Count :" + count);
    		soundAllert();
    	}
    	else if(count <5)
    		tv1.setText("\n Try again");
    	else {
    		tv1.setText("\nVery Good!!!Try again with other axis X !");
    		soundAllert();
    		count=0;
    	}
    	
	}
    
    public void soundAllert(){
    	mp= MediaPlayer.create(this, R.raw.audio);
        mp.start();
    }
    /**
	 * Initialize the data structures required for the maths.
	 */
	private void initMaths()
	{
	
		initialRotationMatrix = new float[9];

		deltaRotationVectorCalibrated = new float[4];
		deltaRotationMatrixCalibrated = new float[9];
		currentRotationMatrixCalibrated = new float[9];
		gyroscopeOrientationCalibrated = new float[3];
		
		deltaRotationVector = new float[4];
		deltaRotationMatrix = new float[9];
		currentRotationMatrix = new float[9];
		gyroscopeOrientation = new float[3];

		// Initialize the current rotation matrix as an identity matrix...
		currentRotationMatrixCalibrated[0] = 1.0f;
		currentRotationMatrixCalibrated[4] = 1.0f;
		currentRotationMatrixCalibrated[8] = 1.0f;

		// Initialize the current rotation matrix as an identity matrix...
		currentRotationMatrix[0] = 1.0f;
		currentRotationMatrix[4] = 1.0f;
		currentRotationMatrix[8] = 1.0f;
	}
	
 
    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
 
    @Override
    protected void onPause() {
        // important to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    /**
	 * Multiply matrix a by b. Android gives us matrices results in
	 * one-dimensional arrays instead of two, so instead of using some (O)2 to
	 * transfer to a two-dimensional array and then an (O)3 algorithm to
	 * multiply, we just use a static linear time method.
	 * 
	 * @param a
	 * @param b
	 * @return a*b
	 */
	private float[] matrixMultiplication(float[] a, float[] b)
	{
		float[] result = new float[9];

		result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
		result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
		result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];

		result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
		result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
		result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];

		result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
		result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
		result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

		return result;
	}
	

	
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	

	public void onClick(View v) {
		this.finish();
		
	};
 
}
