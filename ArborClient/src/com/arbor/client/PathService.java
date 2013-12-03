package com.arbor.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Point;
import android.util.Log;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class PathService {
	
	private static final String TAG = "PathService";

	private CacheService cache;
	private ArborClient client;
	
	private ArrayList<MapPoint> lastVisited;
	
	private MapPoint lastVisitedNode;
	private MapPoint lastTemporaryNode;
	
	public static final int minimumPathLength = 3;
	public static final int maximumPathLength = 7;
	
	public static final double initialBlockWeight = 1.00d;
	public static final double minimumBlockWeight = 0.01d;
	
	public static final double incrementBlockWeight = 0.10d;
	public static final double decrementBlockWeight = 0.01d;
	
	PathService(ArborClient client, CacheService cache) {
		this.cache = cache;
		this.client = client;
		lastVisited = new ArrayList<MapPoint>();
	}
	
	/***
	 * Return the path that the current user location belongs to if any
	 * @param deviceData
	 * @return
	 */
	public Path belongsToPath(DeviceData deviceData) {
		
		// Get our current block
		DataBlock b = cache.readBlock(deviceData.getLocation().getLatitude(), 
									  deviceData.getLocation().getLongitude());
		
		// Get the path that the block belongs to
		return belongsToPath( b.getLocation() );
	}
	
	/***
	 * Based on a MapPoint, look up the corresponding path, if any.
	 * @param location The path MapPoint location
	 * @return the path it belongs to or null
	 */
	public Path belongsToPath(MapPoint location) {
		SQLiteDatabase sqldb = cache.getDatabase().getReadableDatabase();
		
		Path belongsTo = null;
		
		String query = "SELECT p._id, p.last_used FROM user_path p " +
				"INNER JOIN path_nodes pn ON pn.path_id = p._id " + 
				"WHERE pn.current_x = ? AND pn.current_y = ?";
		
		String sX = String.valueOf(location.x);
		String sY = String.valueOf(location.y);
		
		try {
			
			Cursor cursor = sqldb.rawQuery( query, new String[] { sX, sY } );
			

			if (cursor.getCount() == 0) return null; // no path found
			
			cursor.moveToFirst();
			
			client.appendLog("(" + sX + "," + sY + ") belongs to path");
			
			belongsTo = new Path(cursor.getInt(0), cursor.getString(1));
			
		}
		catch(Exception e) {
			Log.v(TAG, "failure in belongsToPath(MapPoint)" + e.getMessage());
			return null;
		}
		finally {
			sqldb.close();
		}
		return belongsTo;		
	}
	
	/***
	 * Identify if the user is current on a path based on their device location.
	 * @param deviceData
	 * @return True if the device is on a path
	 */
	public boolean onPath(DeviceData deviceData) {
		
		// Get our current block
		DataBlock b = cache.readBlock(deviceData.getLocation().getLatitude(), 
									  deviceData.getLocation().getLongitude());
		
		//client.appendLog("checking if device is on path...");
		// Check if block belongs to a path
		Path path = belongsToPath( b.getLocation() );
		
		if ( path == null ) return false;
		//client.appendLog("device on path!");
		return true;
	}
	
	/***
	 * Identify if the user is current on a path based on latitude and longitude.
	 * @param latitude
	 * @param longitude
	 * @return True if the co-ordinate is on a path
	 */
	public boolean onPath(double latitude, double longitude) {
		
		// Get our current block
		DataBlock b = cache.readBlock(latitude, longitude);
		
		// Check if block belongs to a path
		Path path = belongsToPath( b.getLocation() );
		
		if ( path == null ) return false;
		
		return true;
	}
	
	/***
	 * Decay an entire path, this should occur once per tick.
	 * Once a path has decayed sufficiently either remove it or
	 * split it.
	 * @param pathId
	 */
	public void decayPath(int pathId) {
		SQLiteDatabase sqldb = cache.getDatabase().getWritableDatabase();
		
		try {
			
			String query = "update path_nodes set weight = weight - " 
					+ PathService.decrementBlockWeight
					+ " WHERE path_id = ?";
			
			String sPathId = String.valueOf( pathId );
			
			sqldb.rawQuery( query, new String[] { sPathId } );
			
		}
		finally {
			sqldb.close();
		}
	}
	
	/***
	 * Increment a block by a given weight
	 * @param pathId
	 */
	public void reinforceNode(MapPoint blockLocation) {
		SQLiteDatabase sqldb = cache.getDatabase().getWritableDatabase();
		
		try {
			
			String query = "update path_nodes set weight = weight + " 
					+ PathService.incrementBlockWeight
					+ " WHERE path_id = ? AND current_x = ? AND current_y = ?";
			
			String sBlockX = String.valueOf( blockLocation.x );
			String sBlockY = String.valueOf( blockLocation.y );
			
			sqldb.rawQuery( query, new String[] { sBlockX, sBlockY } );
			
		}
		finally {
			sqldb.close();
		}
	}
	
	/*** 
	 * Check if the path is valid (greater than minimum length, minimum decay)
	 * @param pathId
	 */
	public boolean validatePath(int pathId) {
		SQLiteDatabase sqldb = cache.getDatabase().getReadableDatabase();
		
		try {
			
			String query = "SELECT weight FROM path_nodes WHERE path_id = ?";
			
			String sPathId = String.valueOf( pathId );
			
			Cursor cursor = sqldb.rawQuery( query, new String[] { sPathId } );
			
			// check length
			if ( cursor.getCount() < PathService.minimumPathLength 
					|| cursor.getCount() > PathService.maximumPathLength ) 
						return false;
			
			// check weight
			while( cursor.moveToNext() ) {
				if( cursor.getFloat(0) < PathService.minimumBlockWeight ) 
					return false;
			}
			
		}
		finally {
			sqldb.close();
		}
		
		return true;
	}
	
	/***
	 * Given a path, find all other paths that intersect with it.
	 * @param location
	 * @return
	 */
	private List<MapPoint> intersectingPaths(MapPoint location) {
		SQLiteDatabase sqldb = cache.getDatabase().getReadableDatabase();
		
		List<MapPoint> paths = new ArrayList<MapPoint>();
		
		try {
			
			String sX = String.valueOf( location.x );
			String sY = String.valueOf( location.y );
			
			String query = "SELECT DISTINCT pn.current_x, pn.current_y FROM user_path p" +
					" INNER JOIN path_nodes pn ON pn.path_id = p._id" + 
					" WHERE pn.current_x IN (" + location.x + "," + (location.x + 1) + "," + (location.x - 1) + ")" +
					" AND pn.current_y IN (" + location.y + "," + (location.y + 1) + "," + (location.y - 1) + ")" +
							"AND (pn.current_x != ? AND pn.current_y != ?)";
						
			Cursor cursor = sqldb.rawQuery( query, new String[] { sX, sY } );
			
			while( cursor.moveToNext() ) {
				paths.add(new MapPoint(cursor.getInt(0), cursor.getInt(1)));
			}
			
			cursor.close();
			return paths;
			
		}
		finally {
			sqldb.close();
		}
	}
	
	public List<Path> allPaths() {
		SQLiteDatabase sqldb = cache.getDatabase().getReadableDatabase();
		
		List<Path> paths = new ArrayList<Path>();
		
		try {
			String query = "SELECT p._id, p.last_used FROM user_path p";
			
			Cursor cursor = sqldb.rawQuery( query, new String[]{} );
			
			while( cursor.moveToNext() ) {
				paths.add(new Path(cursor.getInt(0), cursor.getString(1)));
			}
			
			cursor.close();
			return paths;
			
		}
		finally {
			sqldb.close();
		}
	}
	
	/***
	 * Handles predicting the IDs of the blocks that need to be cached
	 * for the user. This may be based on path information or best-guess
	 * approach using linear prediction. This will not consider if a 
	 * block is already cache or not, but will determine the next blocks
	 * that will need to be fetched.
	 * @param device the current location of the device
	 * @return List of MapPoints of the blocks that will need to be fetched.
	 */
	public List<MapPoint> predictNext(DeviceData device) {
		
		List<MapPoint> predictedNodes = new ArrayList<MapPoint>();
		
		Log.v(TAG, "running prediction algorithm");
		
		DataBlock currentBlock = cache.readBlock(device.getLocation().getLatitude(), 
				device.getLocation().getLongitude());
		
		if( onPath(device) ) {
			client.appendLog("user is on a path!");
			List<MapPoint> upcomingIntersections = nextIntersections(
				device,
				currentBlock.getLocation()
			);
			
			MapPoint nextNode = nextPathNode(device, currentBlock.getLocation());
			
			if (nextNode == null ) {
				Log.v(TAG, "on a path but found no next nodes?");
			}
			
			if( upcomingIntersections != null) {
				// check if the user will switch paths
				Log.v(TAG, "there are upcoming intersections");
				Block best = null;
				for(MapPoint intersection : upcomingIntersections) {
					Block current = this.getPathNode(intersection);
					if( best == null || current.weight > best.weight ) {
						best = current;
					}
				}
				predictedNodes.add( best.current );
				
				if (nextNode != null )
					predictedNodes.add(nextNode);
			}
			else {
				// prefetch next nodes in the path
				if (nextNode != null )
					predictedNodes.add(nextNode);
			}
		}
		else {
			client.appendLog("user is not on a path");
			// predict next block basted on heading
			DataBlock b = cache.readBlock(device.getLocation().getLatitude(), 
										  device.getLocation().getLongitude());
			
			double bearing = device.getOrientation();
			
			if (bearing < 0 ) {
				bearing = 360 + bearing;
			}
			
			// TODO consider odd angles when considering position within block
			
			Log.v(TAG, "device bearing: " + bearing);
			
			// mapped from bottom right, up+, west+
			if( (bearing > 315.0 && bearing <= 360) || (bearing > 0 && bearing <= 45.0) ){
				// north
				predictedNodes.add(new MapPoint(b.getX(), b.getY() + 1));
			}
			else if( bearing > 45.0 && bearing <= 135.0) {
				// east
				predictedNodes.add(new MapPoint(b.getX() - 1, b.getY()));
			}
			else if( bearing > 135.0 && bearing <= 225) {
				// south
				predictedNodes.add(new MapPoint(b.getX(), b.getY() - 1));
			}
			else if (bearing > 225 && bearing <= 315) {
				// west todo fix default
				predictedNodes.add(new MapPoint(b.getX() + 1, b.getY()));
			}
		}
		
		// Add block to temporary path and create path if relevant
		this.addVisitedBlock(currentBlock.getLocation());
		
		// Add weight to current block if it is on a path
		if( this.belongsToPath(currentBlock.getLocation()) != null )
			this.reinforceNode(currentBlock.getLocation());
		
		// Handle path decaying and splitting
		for(Path p : allPaths() ) {
			this.decayPath(p.getId());
			
			if( !this.validatePath(p.getId()) ) {
				MapPoint split = this.canSplitPath(p.getId() );
				if(split != null) {
					this.splitPath(p.getId(), split);
				}
			}
		}
		
		return predictedNodes;
	}
	
	/***
	 * Gather the list of the next upcoming intersections for paths that 
	 * may need to be prefetched.
	 * @param device
	 * @param current
	 * @return
	 */
	private List<MapPoint> nextIntersections( DeviceData device, MapPoint current ) {
		
		MapPoint nextBlock = nextPathNode(device, current);
		
		if(nextBlock == null)
			return null;
		
		List<MapPoint> upcomingIntersections = intersectingPaths(nextBlock);
		
		if( upcomingIntersections == null || upcomingIntersections.size() == 0 )
			return null;
		
		return upcomingIntersections;
	}
	
	/***
	 * Based on the current location, determine if it is on a path and if it
	 * is, determine the direction of travel on the path and return.
	 * @param device
	 * @param current
	 * @return
	 */
	public MapPoint nextPathNode( DeviceData device, MapPoint current ) {
		MapPoint nextBlock = null;
		
		SQLiteDatabase sqldb = cache.getDatabase().getReadableDatabase();
		
		
		try {
			
			String query = "SELECT previous_x, previous_y, current_x," +
							" current_y, next_x, next_y, weight, path_id " +
							" FROM path_nodes WHERE current_x = ? AND current_y = ?";
			
			String sX = String.valueOf( current.x );
			String sY = String.valueOf( current.y );
			
			Cursor cursor = sqldb.rawQuery( query, new String[] { sX, sY } );
			
			if( cursor.getCount() == 0 ) return null;
			
			cursor.moveToFirst();
			
			MapPoint previous = null;
			MapPoint next = null;
			
			if(!cursor.isNull(0) && !cursor.isNull(1)) 
				previous = new MapPoint(cursor.getInt(0), cursor.getInt(1));
			
			if(!cursor.isNull(4) && !cursor.isNull(5)) 
				next = new MapPoint(cursor.getInt(4), cursor.getInt(5));

			// TODO Improve algorithm given time
			if( lastVisitedNode != null ) {
				if( previous!= null && previous.equals(lastVisitedNode)) {
					nextBlock = next;
				}
				else if (next != null && next.equals(lastVisitedNode)) {
					nextBlock = previous;
				}
				else {
					// user has jumped	
					nextBlock = next;
				}
			}
			else {
				nextBlock = next;
			}
			
			// assume that we are going from the start of the path
			if(nextBlock == null) {
				query = "SELECT current_x, current_y, " +
						"  next_x, next_y, weight, path_id " +
						" FROM path_nodes WHERE previous_x = null" +
						" AND previous_y = null AND path_id = ?";
				
				String path_id = cursor.getString(7);
				
				cursor = sqldb.rawQuery( query, new String[] { path_id } );
				
				if( cursor.getCount() == 0 ) {
					nextBlock = null;	
				}
				else {
					cursor.moveToFirst();
					
					nextBlock = new MapPoint(cursor.getInt(2), cursor.getInt(3));
				}
				
				
			}
		}
		finally {
			sqldb.close();
		}
		
		lastVisitedNode = current;
		
		return nextBlock;
	}
	
	
	/***
	 * Track the last blocks the user has visited, when conditions are met,
	 * create a path in the database.
	 * @param blockLocation the MapPoint of the block
	 */
	public void addVisitedBlock(MapPoint blockLocation) {
		boolean conditionsMet = false;
		
		//client.appendLog("addVisitedBlock: changed block? " + !blockLocation.equals(lastTemporaryNode));
		
		if(lastTemporaryNode != null && lastTemporaryNode.equals(blockLocation)) 
			return;
		
		client.appendLog("moved to new block (" + blockLocation.x + "," + blockLocation.y + ")" );
		
		lastTemporaryNode = blockLocation;
		
		if( lastVisited.contains( blockLocation ) ) {
			// the user has already visited the block (loops)
			client.appendLog("  user has already see this block");
			if( lastVisited.size() - 1 < PathService.minimumPathLength ) {
				lastVisited = new ArrayList<MapPoint>();
				lastVisited.add(blockLocation);
				conditionsMet = false;
			}
			else {
				conditionsMet = true;
				//lastVisited.remove(lastVisited.size() - 1);
			}
		}
		else if ( belongsToPath( blockLocation ) != null ) {
			// the user has walked onto another path
			client.appendLog("  already belongs to path");
			if( lastVisited.size() - 1 < PathService.minimumPathLength ) {
				lastVisited = new ArrayList<MapPoint>();
				conditionsMet = false;
			}
			else {
				conditionsMet = true;
				//lastVisited.remove(lastVisited.size() - 1);
			}
		}
		else if( lastVisited.size() == PathService.maximumPathLength - 1 ) {
			// at this point it belongs to no path nor have we seen it
			// but we hit max length
			client.appendLog("  reached maximum path length");
			
			// add the node to the visited paths
			lastVisited.add(blockLocation);
			
			conditionsMet = true;
		}
		else {
			
			client.appendLog("  new block in path, adding...");
			// at this point it hasn't been seen, it doesn't belong to a path
			// and the path is not at its maximum length
			lastVisited.add(blockLocation);
			
			conditionsMet = false;
		}
			
		
		client.appendLog("should create path? " + conditionsMet);
			

		if( conditionsMet ) createPath(lastVisited);
	}
	
	private void createPath(List<MapPoint> nodes) {
		
		client.appendLog("creating new path!");
	
		// insert path and nodes
		SQLiteDatabase sqldb = cache.getDatabase().getWritableDatabase();
		
		if( nodes.size() < PathService.minimumPathLength ) return;
		
		try {		
			ContentValues values = new ContentValues();
			values.put("path_length", nodes.size());			
			
			long path_id = sqldb.insertOrThrow("user_path", null, values);
			client.appendLog("created path " + path_id);
			
			for(int i = 0; i < nodes.size(); i++ ) {
				MapPoint previous = i == 0 ? null : nodes.get(i-1);
				MapPoint current = nodes.get(i);
				MapPoint next = i == nodes.size() - 1 ? null : nodes.get(i+1);
			
				values = new ContentValues();
				values.put("path_id", path_id);
				values.put("previous_x", previous == null ? null : previous.x);
				values.put("previous_y", previous == null ? null : previous.y);
				values.put("current_x", current.x);
				values.put("current_y", current.y);
				values.put("next_x", next == null ? null : next.x);
				values.put("next_y", next == null ? null : next.y);
				values.put("weight", PathService.initialBlockWeight);
				
				sqldb.insertOrThrow("path_nodes", null, values);
			}
		}
		catch(Exception e) {
			Log.v(TAG, "failure in createPath(List<?>) " + e.getMessage());

		}
		finally {
			sqldb.close();
			
			// reset the array
			nodes = new ArrayList<MapPoint>();
		}	
	}
	
	private int recreatePath(List<Block> nodes) {
		
		// insert path and nodes
		SQLiteDatabase sqldb = cache.getDatabase().getWritableDatabase();
		
		try {		
			ContentValues values = new ContentValues();
			values.put("path_length", nodes.size());			
			
			long path_id = sqldb.insertOrThrow("user_path", null, values);
			
			for(int i = 0; i < nodes.size(); i++ ) {
				Block b = nodes.get(i);
			
				values = new ContentValues();
				values.put("path_id", path_id);
				values.put("previous_x", b.previous == null ? null : b.previous.x);
				values.put("previous_y",  b.previous == null ? null : b.previous.y);
				values.put("current_x", b.current.x);
				values.put("current_y", b.current.y);
				values.put("next_x",  b.next == null ? null : b.next.x);
				values.put("next_y",  b.next == null ? null : b.next.y);
				values.put("weight", b.weight);
				
				sqldb.insertOrThrow("path_nodes", null, values);
			}
			
			return (int)path_id; //TODO not this awful cast
		}
		finally {
			sqldb.close();
		}	
	}
	
	/***
	 * If a path is invalid, see if it can be split into two smaller paths. 
	 * If a path cannot be split, it should be completely removed.
	 * @param pathId
	 * @return The block with the lowest weight to be split or null if no 
	 * invalid nodes to split on
	 */
	public MapPoint canSplitPath(int pathId) {
		SQLiteDatabase sqldb = cache.getDatabase().getReadableDatabase();
		
		try {
			
			String query = "SELECT current_x, current_y FROM path_nodes WHERE path_id = ?" +
							" AND weight < " + PathService.minimumBlockWeight +
							" ORDER by weight ASC";
			
			String sPathId = String.valueOf( pathId );
			
			Cursor cursor = sqldb.rawQuery( query, new String[] { sPathId } );
			
			if ( cursor.getCount() == 0 ) return null;
			
			cursor.moveToFirst();
			
			return new MapPoint( cursor.getInt(0), cursor.getInt(1));
			
		}
		finally {
			sqldb.close();
		}
	}
	
	/***
	 * Delete a path from the database.
	 * @param pathId
	 */
	public void deletePath(int pathId) {
		SQLiteDatabase sqldb = cache.getDatabase().getWritableDatabase();
		
		try {	
			String queryNodes = "delete from path_nodes where pathId = ?";
			String queryPath = "delete from user_path where pathId = ?";

			String sPathId = String.valueOf( pathId );
			
			sqldb.rawQuery( queryNodes, new String[] { sPathId } );
			sqldb.rawQuery( queryPath, new String[] { sPathId } );
		}
		finally {
			sqldb.close();
		}	
	}
	
	public Block getPathNode( MapPoint point ) {
		SQLiteDatabase sqldb = cache.getDatabase().getReadableDatabase();
		try {
			
			String query = "SELECT previous_x, previous_y, current_x," +
							" current_y, next_x, next_y, weight " +
							" FROM path_nodes WHERE current_x = ? AND current_y = ?";
			
			String sX = String.valueOf( point.x );
			String sY = String.valueOf( point.y );
			
			Cursor cursor = sqldb.rawQuery( query, new String[] { sX, sY } );
			
			cursor.moveToFirst();
			MapPoint previous = null;
			MapPoint next = null;
			MapPoint current = new MapPoint(cursor.getInt(2), cursor.getInt(3));
			
			if(!cursor.isNull(0) && !cursor.isNull(1)) 
				previous = new MapPoint(cursor.getInt(0), cursor.getInt(1));
			
			if(!cursor.isNull(4) && !cursor.isNull(5)) 
				next = new MapPoint(cursor.getInt(4), cursor.getInt(5));
			
			return new Block(
				previous,
				current,
				next,
				cursor.getFloat(6)
			);	
		}
		finally {
			sqldb.close();
		}
	}
	
	/***
	 * Take a path and at its invalid points, split the path into separate parts.
	 * If a segment creates an invalid path, delete that segment.
	 * @param pathId
	 */
	public void splitPath(int pathId, MapPoint splitBlock) {
		List<Block> pathA = new ArrayList<Block>();
		List<Block> pathB = new ArrayList<Block>();
		
		SQLiteDatabase sqldb = cache.getDatabase().getReadableDatabase();
		
		Map<Point, Block> blocks = new HashMap<Point, Block>();
		
		try {
			
			String query = "SELECT previous_x, previous_y, current_x," +
							" current_y, next_x, next_y, weight " +
							" FROM path_nodes WHERE path_id = ?";
			
			String sPathId = String.valueOf( pathId );
			
			Cursor cursor = sqldb.rawQuery( query, new String[] { sPathId } );
			
			while( cursor.moveToNext() ) {
				MapPoint previous = null;
				MapPoint next = null;
				MapPoint current = new MapPoint(cursor.getInt(2), cursor.getInt(3));
				
				if(!cursor.isNull(0) && !cursor.isNull(1)) 
					previous = new MapPoint(cursor.getInt(0), cursor.getInt(1));
				
				if(!cursor.isNull(4) && !cursor.isNull(5)) 
					next = new MapPoint(cursor.getInt(4), cursor.getInt(5));
				
				Block b = new Block(
					previous,
					current,
					next,
					cursor.getFloat(6)
				);
				
				blocks.put(b.current, b);
			}		
		}
		finally {
			sqldb.close();
		}
		
		// determine the starting block
		Block startBlock = null;
		for(Point id : blocks.keySet() ) {
			Block b = blocks.get(id);
			if(b.previous == null) {
				startBlock = b;
				break;
			}
		}
		
		// walk the relationships
		MapPoint nextBlock = startBlock.next;
		boolean passedSplit = false; // for determining whether its path A or B
		while ( nextBlock != null ) {
			Block b = blocks.get(nextBlock);
			if(passedSplit) {
				pathB.add(b);
			}
			else {
				if(b.current.equals(splitBlock)) {
					// skip and change flag
					passedSplit = true;
				}
				else {
					pathA.add(b);
				}
			}
			nextBlock = b.next;
		}
		
		if(pathA.size() > 0)
			pathA.get(pathA.size() - 1).next = null;
		
		if(pathB.size() > 0)
			pathB.get(0).previous = null;
		
		// delete existing path
		deletePath(pathId);
		
		//create new paths with previous values
		int pathIdA = recreatePath(pathA);
		int pathIdB = recreatePath(pathB);
		
		// verify and remove any broken paths 
		verifySplitPath(pathIdA);
		verifySplitPath(pathIdB);
	}
	
	/***
	 * Private method to split paths recursively until a valid path is found
	 * or the path can no longer be split and is deleted.
	 * @param pathId
	 */
	private void verifySplitPath(int pathId) {
		if (!validatePath(pathId)) {
			MapPoint nextSplit = canSplitPath(pathId);
			if( nextSplit != null) {
				splitPath(pathId, nextSplit);
			}
			else {
				deletePath(pathId);
			}
		}
	}
}

/***
 * This defines a logical node block in a path, NOT a data block in the cache.
 * @author Dean Pearce
 *
 */
class Block {
	MapPoint previous;
	MapPoint current;
	MapPoint next;
	Float weight;
	
	Block(MapPoint previous, MapPoint current, MapPoint next, Float w) {
		this.previous = previous;
		this.current = current;
		this.next = next;
		this.weight = w;
	}	
}