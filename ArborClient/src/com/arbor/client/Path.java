package com.arbor.client;

import java.util.Date;

public class Path {

	private int id;
	private Date time;
	
	Path(int id, Date time) {
		this.id = id;
		this.time = time;
	}
	
	Path(int id, String time) {
		//parse the string
		Date date = null;
		
		new Path(id, date);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}
}
