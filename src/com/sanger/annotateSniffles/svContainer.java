package com.sanger.annotateSniffles;

import java.util.*;
import com.sanger.intervalTree.*;
import java.util.concurrent.*;

public class svContainer {

	private ArrayList<svInfo> primary = null;
	private ConcurrentHashMap<String, HashMap<String, IntervalST<svInfo>>> normalMap = null;
	private ConcurrentHashMap<String, HashMap<String, IntervalST<svInfo>>> controlMap = null;
	
	public svContainer(ArrayList<svInfo> primary, ConcurrentHashMap<String, HashMap<String, IntervalST<svInfo>>> normalMap, ConcurrentHashMap<String, HashMap<String, IntervalST<svInfo>>> controlMap) {
		this.primary = primary;
		this.normalMap = normalMap;
		this.controlMap = controlMap;
	}
	
	public ArrayList<svInfo> getPrimary() {
		return primary;
	}
	
	public ConcurrentHashMap<String, HashMap<String, IntervalST<svInfo>>> getNormals() {
		return normalMap;
	}
	
	public ConcurrentHashMap<String, HashMap<String, IntervalST<svInfo>>> getControls() {
		return controlMap;
	}
}
