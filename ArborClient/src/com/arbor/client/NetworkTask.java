package com.arbor.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class NetworkTask implements Runnable {
	
	public enum TaskType { PING, LIST, BLOCK };
	
	private ArborClient client = null;
	
	// Types of data
	private DeviceData deviceData = null;
	private BlockData blockData = null;
	private List<MapPoint> blocks = null;
	
	private TaskType type = TaskType.PING;
	
	// These should only change at compile time
	public static final String charset = "UTF-8";
	public static final String url_base = "http://sectorzerostudios.com";
	
	NetworkTask(ArborClient client, DeviceData data) {
		this.client = client;
		this.deviceData = data;
		this.type = TaskType.LIST;
	}
	
	NetworkTask(ArborClient client, BlockData data) {
		this.client = client;
		this.blockData = data;
		this.type = TaskType.BLOCK;
	}
	
	NetworkTask(ArborClient client, List<MapPoint> blocks) {
		this.client = client;
		this.blocks = blocks;
		this.type = TaskType.LIST;
	}
	
	NetworkTask(ArborClient client) {
		this.type = TaskType.PING;
	}
	
	public void run() {		
		if(type == TaskType.PING) {
			// Do a simple ping test on the API service
			String result = doPingTest();
			client.appendLog( "Received from API: " + result );
		}
		else if(type == TaskType.LIST) {
			// Fetch the list of suggested blocks by server algorithm
			List<DataBlock> result = null;
			
			if(deviceData != null ) {
				result = doBlockListing();
			}
			else if(blocks != null) {
				result = doVirtualFetch();
			}

			client.notifyBlockList(result);
			client.appendLog( "Successfully fetched data listing! ");
		}
		else if(type == TaskType.BLOCK) {
			// Fetch an individual block of data from the server
			List<Data> result = null;
			result = doBlockFetch();
			client.notifyBlockData(result);
			client.appendLog( "Successfully fetched data blocks! ");
		}
		
	}
	
	/***
	 * Fetches the actual block data from the server by ID
	 * @return List of POIs for the given blocks
	 */
	private List<DataBlock> doVirtualFetch() {
		List<DataBlock> returnData = new ArrayList<DataBlock>();
		
		if( this.blocks == null ) return null;
		
		HttpURLConnection con = null;
		
		client.appendLog("virtual fetch?");
		
		try {			
			JSONObject object = new JSONObject();
			
			JSONArray array = new JSONArray();
			
			for(MapPoint i : blocks) {
				JSONObject point = new JSONObject();
				point.put("x", i.x);
				point.put("y", i.y);
				array.put(point);
			}
			
			object.put("points", array);
			
			// encode the data for transmission
			String query = "?json=" + 
					URLEncoder.encode(object.toString(), NetworkTask.charset);
			
			con = this.getConnection("GET", "/data/virtual/list" + query);
			
			//this.writeSocket(con, query);

			JSONObject jsonObject = this.readSocket(con);
			
			// iterate JSONObjects and build arraylist
			JSONArray blockList = jsonObject.getJSONArray("blockList");
			
			for( int i = 0; i < blockList.length(); i++ ) {
				JSONObject o = blockList.getJSONObject(i);
				
				client.appendLog("virtual fetch block => " + o.getInt("id"));
				
				DataBlock block = new DataBlock(
					o.getInt("id"),
					o.getInt("size"),
					o.getString("last_updated"),
					o.getDouble("longitudeA"),
					o.getDouble("latitudeA"),
					o.getDouble("longitudeB"),
					o.getDouble("latitudeB"),
					o.getInt("x"),
					o.getInt("y")
				);
				
				returnData.add(block); // data processed in another thread
			}

		}
		catch(Exception e) {
			client.appendLog("an exception occurred while processing virtual block list " + e.toString() );
		}
		finally {
			if( con != null )
				con.disconnect();
		}
		
		return returnData;
	}
	
	/***
	 * Fetches the actual block data from the server by ID
	 * @return List of POIs for the given blocks
	 */
	private List<Data> doBlockFetch() {
		List<Data> returnData = new ArrayList<Data>();
		
		if( this.blockData == null ) return null;
		
		HttpURLConnection con = null;
		
		try {
		 
			String query = this.blockData.serialize();
			
			con = this.getConnection("GET", "/data/blocks" + query);
			
			//this.writeSocket(con, query);

			JSONObject jsonObject = this.readSocket(con);
			
			// iterate JSONObjects and build arraylist
			JSONArray blockList = jsonObject.getJSONArray("blockList");

			for( int i = 0; i < blockList.length(); i++ ) {
				JSONObject o = blockList.getJSONObject(i);
				
				Map<String, String> metaData = null;
				
				try {
					JSONObject mData = jsonObject.getJSONObject("metaData");
					
					// set a reasonable initial capacity and load factor for performance
					metaData = new HashMap<String, String>((int)(mData.length() * 1.25), 0.75f);
					
					Iterator<?> keys = mData.keys();
					while( keys.hasNext() ) {
						String key = (String)keys.next();
						
						metaData.put(key, mData.getString(key));
						
					}
				}
				catch(Exception e) {
					// no metadata? 
				}
				
				Data block = new Data(
					o.getInt("id"),
					o.getInt("blockId"),
					o.getString("name"),
					o.getDouble("latitude"),
					o.getDouble("longitude"),
					o.getDouble("altitude"),
					metaData
				);
				
				returnData.add(block); // this will be processed in another thread
			}

		}
		catch(Exception e) {
			client.appendLog("an exception occurred while processing data blocks " + e.toString() );
		}
		finally {
			if( con != null )
				con.disconnect();
		}
		
		return returnData;
	}
	
	/***
	 * Requests a list of available blocks from the server in a given area.
	 * @return The list of DataBlock Objects describing the block metadata
	 */
	private List<DataBlock> doBlockListing() {
		List<DataBlock> returnData = new ArrayList<DataBlock>();
		
		if( this.deviceData == null ) return null;
		
		HttpURLConnection con = null;
		
		try {
		 
			String query = this.deviceData.serialize();
			
			con = this.getConnection("GET", "/data/block/list" + query);
			
			//this.writeSocket(con, query);

			JSONObject jsonObject = this.readSocket(con);
			
			// iterate JSONObjects and build arraylist
			JSONArray blockList = jsonObject.getJSONArray("blockList");
			
			for( int i = 0; i < blockList.length(); i++ ) {
				JSONObject o = blockList.getJSONObject(i);
				
				DataBlock block = new DataBlock(
					o.getInt("id"),
					o.getInt("size"),
					o.getString("last_updated"),
					o.getDouble("longitudeA"),
					o.getDouble("latitudeA"),
					o.getDouble("longitudeB"),
					o.getDouble("latitudeB"),
					o.getInt("x"),
					o.getInt("y")
				);
				
				returnData.add(block); // data processed in another thread
			}

		}
		catch(Exception e) {
			client.appendLog("an exception occurred while processing block list " + e.toString() );
		}
		finally {
			if( con != null )
				con.disconnect();
		}
		
		return returnData;
	}
	
	// Faking some data for testing purposes
	private String doPingTest() {
		HttpURLConnection con = null;
		
		String result = "no data received?";
		
		try {
		 
			String query = String.format("testData=%s",
					URLEncoder.encode("ping", charset));
			
			con = this.getConnection("POST", "/data/ping");
			
			this.writeSocket(con, query);

			JSONObject jsonObject = this.readSocket(con);

			// Parse out some data to ensure data is well-formed
			if(jsonObject != null)
				result = jsonObject.getString( "pong" );

		}
		catch(Exception e) {
			client.appendLog("an error occurred: could not ping service" );
		}
		finally {
			if( con != null )
				con.disconnect();
		}
		
		return result;
	}
	
	private HttpURLConnection getConnection(String method, String path) throws Exception {
		HttpURLConnection con = null;
		
		URL url = new URL(url_base + path);
		con = (HttpURLConnection) url.openConnection();
		
		// Build connection configuration
		con.setReadTimeout(5000); //milliseconds
		con.setConnectTimeout(10000); //milliseconds
		
		con.setRequestMethod(method);
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setRequestProperty("Accept-Charset", NetworkTask.charset);
		con.setRequestProperty("Content-Type", "application/json");
		
		return con;
	}
	
	private void writeSocket(HttpURLConnection con, String data) throws IOException {
		// Start the data writing process
		DataOutputStream output = null;
		
		try {
		     output = new DataOutputStream( con.getOutputStream() );
		     output.write(data.getBytes(NetworkTask.charset));
		} finally {
		     if (output != null) {
		    	 try { 
		    		 output.flush();
		    		 output.close(); 
	    		 } 
		    	 catch (IOException ioe) {
		    		throw ioe;
		    	 }
		     }
		}
	}
	
	private JSONObject readSocket(HttpURLConnection con) throws Exception {
		// Start the data reading process
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(con.getInputStream(), charset)
		);
		
		String payload = reader.readLine();
		reader.close();
		
		return new JSONObject(payload);
	}
}
