package com.arbor.client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class CacheService {

	private Database db;
	private PathService ps;
	private ArborClient client;
	
	private static final String TAG = "CacheService";
	
	public CacheService(ArborClient client, Database db) {
		this.db = db;
		this.client = client;
		this.ps = new PathService(client, this);
		
		Log.v(TAG, "initialized the cache service");
	}
	
	public Database getDatabase() {
		return db;
	}
	
	public int cacheCount( boolean only_count_used ) {
		SQLiteDatabase sqldb = db.getReadableDatabase();

		// fetch the data for the block
		final String[] FROM = { "_id" };
		
		String WHERE = "";
		if(only_count_used) WHERE = "is_used = 1";
		
		try {
			
			Cursor c1 = sqldb.query( "block", FROM, WHERE, 
				new String[] { }, 
				null, null, null );
			
			Log.v(TAG, "running cache count " + c1.getCount());
			
			return c1.getCount();	
			
		}
		catch(Exception e) {
			Log.v(TAG, "an error occurred in getting cache count " + e.getMessage());
			return -1;
		}
		finally {
			sqldb.close();
		}
	}
	
	public void clearCache() {
		
		
		SQLiteDatabase sqldb = db.getWritableDatabase();
		try {
			sqldb.beginTransaction();
			sqldb.delete("data_variables", null, null);
			sqldb.delete("block_data", null, null);
			sqldb.delete("block", null, null);
			sqldb.setTransactionSuccessful();
			sqldb.endTransaction();
			
			Log.v(TAG, "cache clear is complete!");
		}
		catch(Exception e) {
			Log.v(TAG, "an error occurred in clearing cache: " + e.getMessage());
		}
		finally {
			sqldb.close();
		}
	}
	
	/***
	 * Given the device's current location check that
	 * the relevant block data is valid and cached.
	 * @param device the DeviceData block representing the users' location
	 * @return True if there is a block cached at the current location
	 */
	public boolean validCache( DeviceData device ) {
		
		boolean return_value = true;
		
		DataBlock d = readBlock(device.getLocation().getLatitude(), 
							    device.getLocation().getLongitude());
		
		if( d == null ) {
			return_value = false;
		}
		else {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				Calendar now = Calendar.getInstance();
				
				Date blockExpiry = format.parse(d.getLastUpdated());
				
				TimeZone UTC = TimeZone.getTimeZone("UTC");
				Calendar block = Calendar.getInstance();
				block.setTime(blockExpiry);
				block.set(Calendar.MINUTE, block.get(Calendar.MINUTE) + 15);
				block.setTimeZone(UTC);
				
				Log.v(TAG, "expired? " + block.getTime().toString() + " < " + now.getTime().toString() );
	
				if ( block.before(now) ) return_value = false;
				
			}
			catch(ParseException e) {
				return_value = false;
			}
		}
		
		if( d != null ) {
			// predict explicity future blocks
			Log.v(TAG, "current block is cached, block is (" + d.getX() + "," + d.getY() + ")");
			
			SQLiteDatabase sqldb = db.getWritableDatabase();
			
			ContentValues values = new ContentValues();
			values.put("is_used", 1);
			
			try {
				sqldb.update("block", values, "_id=?", new String[]{ String.valueOf(d.getId()) });
			}
			finally {
				sqldb.close();
			}
		}
		
		if( return_value ) {
			client.appendLog("\nblock (" + d.getX() + "," + d.getY() + ") is cached, running predictive algorithm");
			List<MapPoint> predictedPoints = ps.predictNext(device);
			
			// check if point is already cached, if so, remove it
			Iterator<MapPoint> it = predictedPoints.iterator();
			while(it.hasNext() ) {
				
				MapPoint current = it.next();
				Log.v(TAG, "predicted " + current.toString());
				if( readBlock(current) != null) {
					client.appendLog("predicted " + current.toString() + 
							" is already cached");
					it.remove();
				}
					
			}
			
			if(predictedPoints.size() > 0)
				client.retrieveData(predictedPoints);
		}
		else {
			// fetch current block as well as future
			client.appendLog("current block not cached! retrieving...");
			client.retrieveData(device);
		}
		return return_value;
	}
	
	/***
	 * Read a block at a given latitude and longitude from the cache
	 * if it is available. If not return null.
	 * @param latitude Latitude of the device
	 * @param longitude Longitude of the device
	 * @return The data block if it is cached, null otherwise
	 */
	public DataBlock readBlock( double latitude, double longitude ) {
		SQLiteDatabase sqldb = db.getReadableDatabase();

		// fetch the data for the block
		final String[] FROM = { "_id", "longitudea", "latitudea", "longitudeb",
			"latitudeb", "last_updated", "x", "y"
		};
		
		String WHERE = "longitudea <= ? and latitudea >= ? and " + 
						"longitudeb >= ? and latitudeb <= ?";
		
		String sLatitude = String.valueOf(latitude);
		String sLongitude = String.valueOf(longitude);
		
		try {
			
			Cursor c1 = sqldb.query( "block", FROM, WHERE, 
				new String[] { sLongitude, sLatitude, sLongitude, sLatitude }, 
				null, null, null );
			

			if (c1.getCount() == 0) return null; // no block data found?
			
			// TODO GET NEIGHBOURS!?!?!! 
			
			c1.moveToFirst();
			
			// Count of data points
			Cursor c2 = sqldb.query( "block_data", new String[]{ "_id" }, 
				"block_id=?", new String[] { String.valueOf(c1.getInt(0)) }, 
				null, null, null );
			
			return new DataBlock(
				c1.getInt(0),
				c2.getCount(),
				c1.getString(5),
				c1.getDouble(1),
				c1.getDouble(2),
				c1.getDouble(3),
				c1.getDouble(4),
				c1.getInt(6),
				c1.getInt(7)
			);
			
		}
		catch(Exception e) {
			Log.v(TAG, "an error occurred in readBlock(double, double) " + e.getMessage());
			return null;
		}
		finally {
			sqldb.close();
		}
	}
	
	public DataBlock readBlock( MapPoint point ) {
		SQLiteDatabase sqldb = db.getReadableDatabase();

		// fetch the data for the block
		final String[] FROM = { "_id", "longitudea", "latitudea", "longitudeb",
			"latitudeb", "last_updated", "x", "y"
		};
		
		String WHERE = "x = ? AND y = ?";
		
		String sX = String.valueOf(point.x);
		String sY = String.valueOf(point.y);
		
		try {
			
			Cursor c1 = sqldb.query( "block", FROM, WHERE, 
				new String[] { sX, sY }, 
				null, null, null );
			

			if (c1.getCount() == 0) return null; // no block data found?
			
			c1.moveToFirst();
			
			// Count of data points
			Cursor c2 = sqldb.query( "block_data", new String[]{ "_id" }, 
				"block_id=?", new String[] { String.valueOf(c1.getInt(0)) }, 
				null, null, null );
			
			return new DataBlock(
				c1.getInt(0),
				c2.getCount(),
				c1.getString(5),
				c1.getDouble(1),
				c1.getDouble(2),
				c1.getDouble(3),
				c1.getDouble(4),
				c1.getInt(6),
				c1.getInt(7)
			);
			
		}
		catch(Exception e) {
			Log.v(TAG, "an error occurred in readBlock(MapPoint) " + e.getMessage());
			return null;
		}
		finally {
			sqldb.close();
		}
	}
	

	
	/***
	 * Save a block to the cache database.
	 * @param block The DataBlock to write to the database
	 */
	public void writeBlock( DataBlock block ) {
		SQLiteDatabase sqldb = db.getWritableDatabase();
		
		try {
			// delete old data
			String id = String.valueOf(block.getId());
			
			// fetch the data for the block
			final String[] FROM = { "_id" };
			
			// delete point metadata
			try {
			
				Cursor cursor = sqldb.query("block_data", FROM, "block_id=?", 
						new String[] { id }, null, null, null);
				
				while( cursor.moveToNext() ) {
					sqldb.delete("data_variables", "data_id=?", 
							new String[] { cursor.getString(0) });
				}
			}
			catch(Exception e) {
				// do something with this
			}
			
			// delete points and block
			sqldb.delete("block_data", "block_id=?", new String[] { id });
			sqldb.delete("block", "_id=?", new String[] { id });
			
			// create the data
			ContentValues values = new ContentValues();
			values.put("_id", block.getId());
			values.put("longitudea", block.getLongitudeA());
			values.put("latitudea", block.getLatitudeA());
			values.put("longitudeb", block.getLongitudeB());
			values.put("latitudeb", block.getLatitudeB());
			values.put("x", block.getX());
			values.put("y", block.getY());
			
			sqldb.insertOrThrow("block", null, values);
		}
		catch(Exception e) {
			Log.v(TAG, "failure in writeBlock(DataBlock) " + e.getMessage());
		}
		finally {
			sqldb.close();
		}	
	}
	
	/***
	 * Reads in all data available for a block from the database. 
	 * This allows cached data to be used instead of always querying the 
	 * network service, allowing for better performance and usage.
	 * @param block_id The ID of the block of data to fetch
	 * @return list of data items that exist within the block
	 */
	public List<Data> readBlockData( int block_id ) {
		List<Data> data = new ArrayList<Data>();
		
		SQLiteDatabase sqldb = db.getReadableDatabase();
		
		// fetch the data for the block
		final String[] FROM = { "_id", "block_id", "name", "longitude",
			"latitude", "altitude"
		};
		
		// fetch metadata for each piece of data TODO optimize this
		final String[] FROM_META = { "key_value", "value_data" };
		
		try {
		
			Cursor cursor = sqldb.query("block_data", FROM, "block_id=?", 
					new String[] { String.valueOf(block_id) }, null, null, null);
			
			while( cursor.moveToNext() ) {
				Data d = new Data(
						cursor.getInt(0), 
						cursor.getInt(1), 
						cursor.getString(2), 
						cursor.getDouble(3), 
						cursor.getDouble(4), 
						cursor.getDouble(5), 
						null // Will be populated on next cursor
				);
				
				data.add(d);
			}
			
	
			
			for(Data d : data) {
				Cursor c = sqldb.query("data_variables", FROM_META, "data_id=?", 
					new String[] { String.valueOf(d.getId()) }, null, null, null);
				
				Map<String, String> metaData = 
					new HashMap<String, String>((int)(c.getCount() * 1.25), 0.75f);
				
				while( cursor.moveToNext() ) {				
					metaData.put(c.getString(0), c.getString(1));
				}
				
				d.setMetaData(metaData);
			}
		}
		catch(Exception e) {
			Log.v(TAG, "failure in readBlockData(int) " + e.getMessage());
		}
		finally {
			if(sqldb != null)
				sqldb.close();
		}
		
		return data;
	}
	
	/***
	 * Write a single piece of data to the database. It will remove
	 * any existing data of the same ID and write new data in its place.
	 * @param data The unit of data and meta-data
	 */
	public void writeBlockData( Data data ) {
		SQLiteDatabase sqldb = db.getWritableDatabase();
		
		// delete old data
		String id = String.valueOf(data.getId());
		sqldb.delete("block_data", "_id=?", new String[] { id });
		sqldb.delete("data_variables", "data_id=?", new String[] { id });
		
		// create the data
		ContentValues values = new ContentValues();
		values.put("_id", data.getId());
		values.put("block_id", data.getBlockId());
		values.put("name", data.getName());
		values.put("longitude", data.getLongitude());
		values.put("latitude", data.getLatitude());
		values.put("altitude", data.getAltitude());
		
		try {
			sqldb.insertOrThrow("block_data", null, values);
			
			// create the metadata
			Map<String, String> metaData = data.getMetaData();
			
			if( metaData != null ) {
				for(String s : metaData.keySet() ) {
					ContentValues metaValues = new ContentValues();
					String value = metaData.get(s);
					
					metaValues.put("data_id", data.getId());
					metaValues.put("key_value", s);
					metaValues.put("value_data", value);
		
					sqldb.insertOrThrow("data_variables", null, metaValues);
				}
			}
		}
		catch(Exception e) {
			Log.v(TAG, "failure in writeBlockData(Data) " + e.getMessage());
		}
		finally {
			sqldb.close();
		}
	}

}
