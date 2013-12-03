package com.arbor.client;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class BlockData {

	private List<Integer> blockList;

	public List<Integer> getBlockList() {
		return blockList;
	}

	public void setBlockList(List<Integer> blockList) {
		this.blockList = blockList;
	}
	
	public void addBlockList(Integer blockId) {
		if( this.blockList == null )
			blockList = new ArrayList<Integer>();
		
		blockList.add(blockId);
	}
	
	public String serialize() throws Exception {
		if(blockList == null) return null;
		
		JSONObject object = new JSONObject();
		
		JSONArray array = new JSONArray();
		
		for(Integer i : blockList) {
			array.put(i);
		}
		
		object.put("blockList", array);
		
		// encode the data for transmission
		return "?json=" +  URLEncoder.encode(object.toString(), NetworkTask.charset);
	}
}
