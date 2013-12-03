package com.arbor.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.location.Location;

public class DeviceData {

	private Location location;
	private HashMap<String, String> params;
	private double orientation;

	public DeviceData(Location location, double orientation) {
		this.location = location;
		this.orientation = orientation;
	}
	
	public double getOrientation() {
		return orientation;
	}

	public void setOrientation(double orientation) {
		this.orientation = orientation;
	}

	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public String serialize() throws UnsupportedEncodingException {
		
		params = new HashMap<String, String>();
		
		//addParam("bearing", Float.valueOf(location.getBearing()).toString());
		//addParam("speed", Float.valueOf(location.getSpeed()).toString());
		//addParam("accuracy", Float.valueOf(location.getAccuracy()).toString());
		addParam("latitude", Double.valueOf(location.getLatitude()).toString());
		addParam("longitude", Double.valueOf(location.getLongitude()).toString());
		addParam("altitude", Double.valueOf(location.getAltitude()).toString());

        // add parameters
        String combinedParams = "";
        if (!params.isEmpty())
        {
            combinedParams += "?";
            Iterator<Entry<String, String>> it = params.entrySet().iterator();
            while(it.hasNext())
            {
            	Map.Entry<String, String> pairs = it.next();

        		// build the key-value pair for the URL
                String paramString = pairs.getKey() + "=" 
            		+ URLEncoder.encode(pairs.getValue(), NetworkTask.charset);
                
                if (combinedParams.length() > 1)
                {
                    combinedParams += "&" + paramString;
                }
                else
                {
                    combinedParams += paramString;
                }
            }
        }
		
		return combinedParams;
	}
	
	public void addParam(String key, String value) {
		params.put(key, value);
	}
	
}
