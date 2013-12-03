package com.arbor.client;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class ArborClient extends Activity {
	
	private static final String TAG = "ArborClient";
	
	private TextView output, cacheHits, cacheMisses, falsePositive;
	
	private LocationManager locationManager;
	
	SensorManager sensorManager;
	private Sensor sensorAccelerometer;
	private Sensor sensorMagneticField;
	SensorEventListener sensorListener;
	
	private MapPoint lastDifferentLocation;
	private Location lastKnownLocation;
	private double lastKnownOrientation;
	
	private int hits;
	private int misses;
	
	private float[] valuesAccelerometer;
	private float[] valuesMagneticField;
	 
	private float[] matrixR;
	private float[] matrixI;
	private float[] matrixValues;
	
	LocationListener locationListener;
	
	private Handler guiThread;
	private ExecutorService networkThread;
	
	//private Future<?> networkPending;
	
	private Runnable dataBlockTask;
	
	private Database database;
	private CacheService cache;
	
	// Readable names for various services
	private static final String[] S = { "out of service", "temporarily unavailable", "available" };
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
     
        output = (TextView) findViewById(R.id.output);
        cacheHits = (TextView) findViewById(R.id.hit);
        cacheMisses = (TextView) findViewById(R.id.miss);
        falsePositive = (TextView) findViewById(R.id.positive);

        
        setupHandlers();
        setupListeners();
        setupManagers();
        setupDatabase();
        
        hits = 0;
        misses = 0;
    }
    
    @Override
    public void onBackPressed() {
    	System.exit(0);
    }
    
    private void setupDatabase() {
    	database = new Database(getApplicationContext());
    	cache = new CacheService( this, database );
    	
    	// for testing purposes
    	cache.clearCache();
    }
    
    private void setupHandlers() {
    	this.guiThread = new Handler();
    	this.networkThread = Executors.newSingleThreadExecutor();
    	dataBlockTask = new Runnable(){
    		public void run() {
    			
				DeviceData dev = ArborClient.this.prepareDataBlock();
				
				// we only need to fetch data if the block isn't valid
				if ( !cache.validCache( dev ) ) {
					if(lastDifferentLocation != null)
						misses++;
					
					//Log.v(TAG, "cache miss! " + misses++);
				}
				else {					
					DataBlock d = cache.readBlock(lastKnownLocation.getLatitude(),
							lastKnownLocation.getLongitude());
					
					MapPoint current = d.getLocation();
					
					if(lastDifferentLocation != null) {
						if(!current.equals(lastDifferentLocation))
							hits++;
					}
					else {
						hits++;
					}

					lastDifferentLocation = current;
					//Log.v(TAG, "cache hit! " + hits++);
				}
				
				
				int used = cache.cacheCount(true);
				int all = cache.cacheCount(false);
				
				falsePositive.setText("false positives: " + (all - used));
				cacheHits.setText("cache hits: " + hits);
				cacheMisses.setText("cache misses: " + misses);
    		}
    	};
    	
    }
    
    public DeviceData prepareDataBlock() {
    	return new DeviceData(this.lastKnownLocation, this.lastKnownOrientation);
    }
    
    public void retrieveData(final DeviceData dev) {
    	guiThread.post(new Runnable() {
    		public void run() {
    			try {
//    				if(networkPending != null)
//						networkPending.cancel(true);
    				
    				NetworkTask networkTask = new NetworkTask(
    					ArborClient.this,
    					dev
    				);
    				//networkPending = 
    				networkThread.submit( networkTask );
    			
    			}
    			catch (RejectedExecutionException e) {
    				log("failed to queue network block request");
    			}
    		}
    	});
    }
    
    public void retrieveData(final List<MapPoint> blocks) {
    	guiThread.post(new Runnable() {
    		public void run() {
    			try {
    				NetworkTask networkTask = new NetworkTask(
    					ArborClient.this,
    					blocks
    				);
    				//networkPending = 
					networkThread.submit( networkTask );
    			
    			}
    			catch (RejectedExecutionException e) {
    				log("failed to queue network block request");
    			}
    		}
    	});
    }
    
    private void setupManagers() {
    	// Acquire a reference to the system Location Manager
    	locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String best = this.locationManager.getBestProvider(criteria, true);
        
        log("Location providers:");
        dumpProvidersInformation();
        
        log("Best Location Provider is: " + best);
        
    	// Register the listener with the Location Manager to receive location updates
    	locationManager.requestLocationUpdates(best, 0, 0, locationListener);
    	
    	// setup the sensor manager to calculate azimuth (device orientation)
    	valuesAccelerometer = new float[3];
    	valuesMagneticField = new float[3];
 
    	matrixR = new float[9];
    	matrixI = new float[9];
    	matrixValues = new float[3];
    	
    	Log.v(TAG, "setting up sensor manager");
    	sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
    	sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    	sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    	
		sensorManager.registerListener(sensorListener,
				sensorAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener,
				sensorMagneticField,
				SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    @Override
    protected void onResume() {
    	sensorManager.registerListener(sensorListener,
    			sensorAccelerometer,
    			SensorManager.SENSOR_DELAY_NORMAL);
    	sensorManager.registerListener(sensorListener,
    			sensorMagneticField,
    			SensorManager.SENSOR_DELAY_NORMAL);
    	
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String best = this.locationManager.getBestProvider(criteria, true);        
    	// Register the listener with the Location Manager to receive location updates
    	locationManager.requestLocationUpdates(best, 0, 0, locationListener);
    	
    	super.onResume();
    	
    }
    
    @Override
    protected void onPause() {
    	sensorManager.unregisterListener(sensorListener,
    			sensorAccelerometer);
    	sensorManager.unregisterListener(sensorListener,
    			sensorMagneticField);
    	locationManager.removeUpdates(locationListener);
    	super.onPause();
    }
    
    private void setupListeners() {
    	// Define a listener that responds to location updates
    	locationListener = new LocationListener() {
    	    public void onLocationChanged(Location location) {
	    		// Called when a new location is found by the network location provider.
	    		updateDeviceLocation(location);
    	    }

    	    public void onStatusChanged(String provider, int status, Bundle extras) {
    	    	// Indicates whether location services are available
    	    	Log.v(TAG, "Provider status changed: " + provider + ", status=" 
    	    			+ S[status] + ", extras=" + extras);
    	    }

    	    public void onProviderEnabled(String provider) {
    	    	Log.v(TAG, "Provider enabled: " + provider);
    	    }

    	    public void onProviderDisabled(String provider) {
    	    	Log.v(TAG, "Provider disabled: " + provider);
    	    }
    	  };
    	  
  		sensorListener = new SensorEventListener() {
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				return;
			}
			
			public void onSensorChanged(SensorEvent event) {
				switch(event.sensor.getType()){
					case Sensor.TYPE_ACCELEROMETER:
						for(int i =0; i < 3; i++){
							valuesAccelerometer[i] = event.values[i];
						}
						break;
					case Sensor.TYPE_MAGNETIC_FIELD:
						for(int i =0; i < 3; i++){
							valuesMagneticField[i] = event.values[i];
						}
						break;
			  	}
				   
			  	boolean success = SensorManager.getRotationMatrix(
				       matrixR,
				       matrixI,
				       valuesAccelerometer,
				       valuesMagneticField);
			   
			  	if(success) {
			  		SensorManager.getOrientation(matrixR, matrixValues);
			  		lastKnownOrientation = Math.toDegrees(matrixValues[0]);
			  	}
			}
		};
    }
    
    /***
     * Handles updating the location for the device when the GPS has new data to report
     * @param location the new location of the device
     */
    public void updateDeviceLocation(Location location) {
    	if(location == null) {
    		Log.v(TAG, "Location[unknown]???");
    	}
    	else {
    		//Log.v(TAG, "Latitude: " + location.getLatitude() + ", Longitude: "
    		//			+ location.getLongitude()  );
            
            guiThread.removeCallbacks(dataBlockTask);
          
            this.lastKnownLocation = location;
            
            guiThread.post(dataBlockTask);
    	}
    }
    
    private void dumpProvidersInformation() {
    	List<String> providers = locationManager.getAllProviders();
    	for(String provider: providers) {
    		dumpProvider(provider);
    	}
    }
    
    private void dumpProvider(String provider) {
    	LocationProvider info = locationManager.getProvider( provider );
    	StringBuilder builder = new StringBuilder();
    	builder.append("LocationProvider[")
    		.append("name=")
    		.append(info.getName())
    		.append(", enabled=")
    		.append(locationManager.isProviderEnabled(provider))
    		.append("]");
    	log(builder.toString());
    }
    
    public void log(String string) {
    	output.append(string + "\n");
    }
    
    // run local heuristics
    public void notifyBlockList(final List<DataBlock> data) {
    	guiThread.post(new Runnable() {
    		public void run() {
    			if (data == null ) {
    				appendLog(" the data returned was null?");
    				return;
    			}
    			Iterator<DataBlock> it = data.iterator();
    			while(it.hasNext()) {
    				DataBlock block = it.next();
    				cache.writeBlock(block);
    				appendLog(" finished writing block " + block.getId());
    				// queue block fetch
    				
    				try {
    						BlockData d = new BlockData();
    						d.addBlockList(block.getId());
		    				NetworkTask networkTask = new NetworkTask(
								ArborClient.this,
								d
							);
		    				//networkPending = 
    						networkThread.submit( networkTask );
	    				
	    			}
	    			catch (RejectedExecutionException e) {
	    				log("failed to queue network block request");
	    			}
    			}
    		}
    	});
    }
    
    // store the blocks into the database
    public void notifyBlockData(final List<Data> data) {
    	guiThread.post(new Runnable() {
    		public void run() {
    			for(Data d : data) {
    				cache.writeBlockData(d);
    			}
    		}
    	});
    }
    
    // TODO Consider alternate views, logger object
    public void appendLog(final String string) {
    	guiThread.post(new Runnable() {
    		public void run() {
    			output.append(string + "\n");
    		}
    	});
    }
}