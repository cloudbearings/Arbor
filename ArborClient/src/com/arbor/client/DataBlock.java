package com.arbor.client;

public class DataBlock {
	private int id; // block id number
	private String lastUpdated; // last time the block was updated on the server
	private int size; // in children
	private double longitudeA;
	private double latitudeA;
	private double longitudeB;
	private double latitudeB;
	
	private MapPoint location;
	
	private int x;
	private int y;
	
	public DataBlock(int id, int size, String last_updated, double longitudeA,
			double latitudeA, double longitudeB, double latitudeB, int x, int y) {
		
		this.id = id;
		this.size = size;
		this.lastUpdated = last_updated;
		this.longitudeA = longitudeA;
		this.latitudeA = latitudeA;
		this.longitudeB = longitudeB;
		this.latitudeB = latitudeB;
		this.x = x;
		this.y = y;
		
		location = new MapPoint(x, y);
	}
	
	public MapPoint getLocation() {
		return this.location;
	}
	
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
		location = new MapPoint(x, y);
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
		location = new MapPoint(x, y);
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public String getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(String last_updated) {
		this.lastUpdated = last_updated;
	}

	public double getLongitudeA() {
		return longitudeA;
	}

	public void setLongitudeA(double longitudeA) {
		this.longitudeA = longitudeA;
	}

	public double getLatitudeA() {
		return latitudeA;
	}

	public void setLatitudeA(double latitudeA) {
		this.latitudeA = latitudeA;
	}

	public double getLongitudeB() {
		return longitudeB;
	}

	public void setLongitudeB(double longitudeB) {
		this.longitudeB = longitudeB;
	}

	public double getLatitudeB() {
		return latitudeB;
	}

	public void setLatitudeB(double latitudeB) {
		this.latitudeB = latitudeB;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
