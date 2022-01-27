package com.sanger.annotateSniffles;

import com.sanger.intervalTree.*;
import java.util.*;


public class svInfo {

	private Integer normals = 0;
	private Integer controls = 0;
	private Integer bamNormals = 0;
	private Integer bamControls = 0;
	private String[] lineTokens = null;
	private String leftChrom = null;
	private Integer startLeft = -1;
	private String rightChrom = null;
	private Integer startRight = -1;
	private String type = null;
	private Double sw = -1.0;
	private HashMap<String, IntervalST<Integer>> sTreeMapLeft = new HashMap<String, IntervalST<Integer>>(100, 0.999f);
	private HashMap<String, IntervalST<Integer>> sTreeMapRight = new HashMap<String, IntervalST<Integer>>(100, 0.999f);
	Boolean activeLeft = null;
	public svInfo(String[] lineTokens, String leftChrom, Integer startLeft, String rightChrom, Integer startRight, String type, double sw) {
		this.lineTokens = lineTokens;
		this.leftChrom = leftChrom;
		this.startLeft = startLeft;
		this.rightChrom = rightChrom;
		this.startRight = startRight;
		this.type = type;
		this.sw = sw;
	}
	public int getSearchWidth() {
		return sw.intValue();
	}
	public boolean isIndel() {
		return (type.contains("DEL") || type.contains("DUP") || type.contains("INS"));
	}
	public String getLine() {
		return String.join("\t", String.join("\t", lineTokens), normals.toString(), controls.toString(), bamNormals.toString(), bamControls.toString(), countEvents(sTreeMapLeft).toString(), countEvents(sTreeMapRight).toString());
	}
	private Integer countEvents(HashMap<String, IntervalST<Integer>> sTreeMap) {
		if(sTreeMap.values().size() == 0)
			return 0;
		sTreeMap.keySet().stream().forEach(i -> {System.out.println(i); var tree = sTreeMap.get(i); tree.getIntervals().stream().forEach(j -> System.out.println(String.join(" - ", Integer.toString(j.low), Integer.toString(j.high))));});
		return sTreeMap.values().stream().map(i -> i.size()).reduce(0, Integer::sum);
	}
	public int lowLeft() {
		return Math.max(startLeft - sw.intValue(), 0);
	}
	public int highLeft() {
		return startLeft + sw.intValue();
	}
	public int lowRight() {
		return Math.max(startRight - sw.intValue(), 0);
	}
	public int highRight() {
		return startRight + sw.intValue();
	}
	public void increaseNormal() {
		normals++;
	}
	public void increaseControl() {
		controls++;
	}
	public void increaseBamNormal() {
		bamNormals++;
	}
	public void increaseBamControl() {
		bamControls++;
	}
	public int getNormals() {
		return normals;
	}
	public int getControls() {
		return controls;
	}
	public int getBamNormals() {
		return bamNormals;
	}
	public int getBamControls() {
		return bamControls;
	}
	public String getLeftChrom() {
		return leftChrom;
	}
	public String getRightChrom() {
		return rightChrom;
	}
	public Integer getStartLeft() {
		return startLeft;
	}
	public Integer getStartRight() {
		return startRight;
	}
	public String getType() {
		return type;
	}
	public void addIntervalLeft(String cChrom, Interval1D interval) {
		if(!sTreeMapLeft.containsKey(cChrom))
			sTreeMapLeft.put(cChrom, new IntervalST<Integer>());
		var sTreeLeft = sTreeMapLeft.get(cChrom);
		var search = sTreeLeft.searchAllList(interval);
		Interval1D cInterval = null;
		Integer cCount = null;
		if(search.size() == 0)
			sTreeLeft.put(interval, 1);
		else if(search.size() == 1) {
			cInterval = search.getFirst();
			cCount = sTreeLeft.remove(cInterval);
			var nInterval = new Interval1D((interval.low < cInterval.low) ? interval.low : cInterval.low, (interval.high > cInterval.high) ? interval.high : cInterval.high);
			sTreeLeft.put(nInterval, ++cCount);
		} else {
			cCount = search.stream().map(i -> sTreeLeft.remove(i)).reduce(0, Integer::sum);
			search.add(interval);
			var min = search.stream().map(i -> i.low).min(Integer::compare).get();
			var max = search.stream().map(i -> i.high).max(Integer::compare).get();
			var nInterval = new Interval1D(min, max);
			sTreeLeft.put(nInterval, ++cCount);
		}
	}
	public void addIntervalRight(String cChrom, Interval1D interval) {
		if(!sTreeMapRight.containsKey(cChrom))
			sTreeMapRight.put(cChrom, new IntervalST<Integer>());
		var sTreeRight = sTreeMapRight.get(cChrom);
		var search = sTreeRight.searchAllList(interval);
		Interval1D cInterval = null;
		Integer cCount = null;
		if(search.size() == 0)
			sTreeRight.put(interval, 1);
		else if(search.size() == 1) {
			cInterval = search.getFirst();
			cCount = sTreeRight.remove(cInterval);
			var nInterval = new Interval1D((interval.low < cInterval.low) ? interval.low : cInterval.low, (interval.high > cInterval.high) ? interval.high : cInterval.high);
			sTreeRight.put(nInterval, ++cCount);
		} else {
			cCount = search.stream().map(i -> sTreeRight.remove(i)).reduce(0, Integer::sum);
			search.add(interval);
			var min = search.stream().map(i -> i.low).min(Integer::compare).get();
			var max = search.stream().map(i -> i.high).max(Integer::compare).get();
			var nInterval = new Interval1D(min, max);
			sTreeRight.put(nInterval, ++cCount);
		}
	}
	public svInfo setLeft() {
		activeLeft = true;
		return this;
	}
	public svInfo setRight() {
		activeLeft = false;
		return this;
	}
	public String getActiveChrom() {
		return (activeLeft) ? leftChrom : rightChrom;
	}
	public Integer getActiveStart() {
		return (activeLeft) ? startLeft : startRight;
	}
}
