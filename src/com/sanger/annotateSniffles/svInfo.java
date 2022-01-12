package com.sanger.annotateSniffles;

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
		return String.join("\t", String.join("\t", lineTokens), normals.toString(), controls.toString(), bamNormals.toString(), bamControls.toString());
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
}
