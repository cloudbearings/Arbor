package com.arbor.client;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "arbor.db";
	private static final int DATABASE_VERSION = 1;

	/***
	 * Create a database for use  in the mobile application
	 * @param ctx The Activity context in which it is being created
	 */
	public Database(Context ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Handle upgrades in here -- drop tables etc
		String tables[] = { "block", "block_data", "data_variables" };
		for(String s : tables ) {
			db.execSQL("DROP TABLE IF EXISTS ?", new String[]{ s });
		}
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		// Stores logical blocks
		db.execSQL("CREATE TABLE block ( " +
			"_id integer primary key, " +
			"longitudeA float not null, " +
			"latitudeA float not null, " +
			"longitudeB float not null, " +
			"latitudeB float not null, " +
			"x integer not null, " +
			"y integer not null, " +
			"last_updated datetime default CURRENT_TIMESTAMP," +
			"is_used integer not null default 0);"
		);
		
		// Stores data within blocks
		db.execSQL("CREATE TABLE block_data ( " +
			"_id integer primary key autoincrement, " +
			"block_id integer, " +
			"name varchar(128) not null, " +
			"longitude float not null, " +
			"latitude float not null, " +
			"altitude float, " +
			"last_updated datetime default CURRENT_TIMESTAMP, " +
			"FOREIGN KEY(block_id) REFERENCES block(id)); "	
		);
		
		// Stores metadata for data points
		db.execSQL("CREATE TABLE data_variables ( " +
			"_id integer primary key autoincrement, " +
			"data_id integer, " +
			"key_value varchar(128) not null, " +
			"value_data clob, " +
			"FOREIGN KEY(data_id) REFERENCES block_data(id)); "
		);
		
		// User paths
		db.execSQL("CREATE TABLE user_path (" +
			" _id integer primary key autoincrement," +
			" path_length integer," +
			" last_used datetime default CURRENT_TIMESTAMP" +
			");"
		);
		
		// User path nodes
		db.execSQL("CREATE TABLE path_nodes (" +
			" path_id integer," +
			" previous_x integer," +
			" previous_y integer," +
			" current_x integer not null," +
			" current_y integer not null," +
			" next_x integer," +
			" next_y integer," +
			" weight float not null," +
			" FOREIGN KEY(path_id) REFERENCES user_path(_id)" +
			");"
		);
	}
}
