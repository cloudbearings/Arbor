package com.arbor.client;

import android.graphics.Point;

public class MapPoint extends Point {
	
	public MapPoint(int x, int y) {
		super(x, y);
	}
	
	@Override
	public boolean equals(Object o) {
		
		if( this == o ) {
			return true;
		}
		
		if (!(o instanceof MapPoint)) {       
			return false;     
		}
		
		MapPoint obj = (MapPoint)o;
		
		if( obj.x != this.x || obj.y != this.y ) return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + x;
		result = 31 * result + y;
		return result;
	}

}
