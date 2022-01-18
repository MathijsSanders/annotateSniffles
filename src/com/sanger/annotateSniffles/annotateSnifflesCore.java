package com.sanger.annotateSniffles;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.*;
import java.util.stream.*;
import com.sanger.intervalTree.*;


import htsjdk.samtools.*;

import java.util.concurrent.*;
import java.util.function.*;
public class annotateSnifflesCore {
	
	private final Pattern pattern = Pattern.compile("[\\]\\[](.*):");
	private final Pattern locPat = Pattern.compile(":([0-9]+)");
	private String header = null;
	private HashMap<String, Boolean> headerMap = new HashMap<String, Boolean>(50, 0.9999f);
	
	public annotateSnifflesCore(File inputBam, File inputSniffles, String outputSniffles, File[] sharedNormals, File[] sharedControls, File[] sharedBamNormals, File[] sharedBamControls, double overlapFraction, int minOverlap, int maxOverlap, int extractWidth, int threads) {
		System.out.println("Version: 0.02");
		System.out.println("Retrieving SVs...");
		var intervalMap = retrieveStructuralVariants(inputSniffles, sharedNormals, sharedControls, threads, overlapFraction, minOverlap, maxOverlap);
		System.out.println("Reviewing header PacBio BAM files");
		reviewHeader(sharedBamNormals, sharedBamControls);
		System.out.println("First annotation step by interval trees...");
		firstAnnotationStep(intervalMap, threads);
		System.out.println("Second annotation step by screening PacBio BAM files...");
		secondAnnotationStep(intervalMap.getPrimary(), sharedBamNormals, sharedBamControls, threads, extractWidth);
		System.out.println("Write results...");
		try {
			writeResults(outputSniffles, intervalMap.getPrimary());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-5);
		}
	}
	private void reviewHeader(File[] sharedBamNormals, File[] sharedBamControls) {
		Arrays.stream(sharedBamNormals).map(i -> bHeader(i)).forEach(i -> headerMap.put(i.name, i.state));
		Arrays.stream(sharedBamControls).map(i -> bHeader(i)).forEach(i -> headerMap.put(i.name, i.state));
	}
	private hResult bHeader(File bam) {
		var inputSam = SamReaderFactory.make().enable(SamReaderFactory.Option.DONT_MEMORY_MAP_INDEX).validationStringency(ValidationStringency.LENIENT).samRecordFactory(DefaultSAMRecordFactory.getInstance()).open(bam);
		var contigList = inputSam.getFileHeader().getSequenceDictionary().getSequences();
		try {
			inputSam.close();
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-7);
		}
		return new hResult(bam.getName(), contigList.stream().map(i -> i.getSequenceName()).filter(i -> i.startsWith("chr")).count() > 0);
	}
	private void writeResults(String output, ArrayList<svInfo> primary) throws IOException {
		var data = new ArrayList<String>(Arrays.asList(header));
		data.addAll(primary.stream().map(i -> i.getLine()).collect(Collectors.toCollection(ArrayList::new)));
		Files.write(Paths.get(output), data, Charset.defaultCharset());
	}
	private svContainer retrieveStructuralVariants(File inputSniffles, File[] sharedNormals, File[] sharedControls, int threads, double oFrac, int minOl, int maxOl) {
		return new svContainer(readPrimary(inputSniffles, oFrac, minOl, maxOl), readSecondary(sharedNormals, threads), readSecondary(sharedControls, threads));
	}
	private void firstAnnotationStep(svContainer iMap, int threads) {
		var primary = iMap.getPrimary();
		var forkJoinPool = new ForkJoinPool(threads);
		try {
			forkJoinPool.submit(() -> primary.parallelStream().forEach(i -> overlapSV(i, j -> j.increaseNormal(), iMap.getNormals()))).get();
			forkJoinPool.submit(() -> primary.parallelStream().forEach(i -> overlapSV(i, j -> j.increaseControl(), iMap.getControls()))).get();
		} catch(InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.exit(-3);
		}
	}
	private void secondAnnotationStep(ArrayList<svInfo> primary, File[] bamNormals, File[] bamControls, int threads, int ew) {
		var forkJoinPool = new ForkJoinPool(threads);
		try {
			System.out.println("Reviewing normals");
			forkJoinPool.submit(() -> primary.parallelStream().forEach(i -> overlapBam(i, j -> j.increaseBamNormal(), bamNormals, ew))).get();
			System.out.println("Reviewing controls");
			forkJoinPool.submit(() -> primary.parallelStream().forEach(i -> overlapBam(i, j -> j.increaseBamControl(), bamControls, ew))).get();
		} catch(InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.exit(-6);
		}
	}
	private void overlapSV(svInfo sv, Consumer<svInfo> fun, ConcurrentHashMap<String, HashMap<String, IntervalST<svInfo>>> cmap) {
		cmap.keySet().stream().map(i -> cmap.get(i).get(sv.getLeftChrom())).map(i -> isOverlap(sv, i)).filter(b -> b).forEach(i -> fun.accept(sv));
	}
	private void overlapBam(svInfo sv, Consumer<svInfo> fun, File[] bams, int ew) {
		System.out.println(sv.getLeftChrom() + " - " + sv.getStartLeft() + " - " + sv.getStartRight());
		Arrays.stream(bams).map(i -> reviewSV(sv, i, ew)).filter(b -> b).forEach(i -> fun.accept(sv));
	}
	private boolean reviewSV(svInfo sv, File bam, int ew) {
		var inputSam = SamReaderFactory.make().enable(SamReaderFactory.Option.DONT_MEMORY_MAP_INDEX).validationStringency(ValidationStringency.LENIENT).samRecordFactory(DefaultSAMRecordFactory.getInstance()).open(bam);
		SAMRecordIterator it = null;
		SAMRecord currentRecord = null;
		try {
			var intervals = QueryInterval.optimizeIntervals(new QueryInterval[]{new QueryInterval(inputSam.getFileHeader().getSequenceIndex(transform(sv.getLeftChrom(), bam)), sv.getStartLeft() - ew, sv.getStartLeft() + ew), new QueryInterval(inputSam.getFileHeader().getSequenceIndex(transform(sv.getRightChrom(), bam)), sv.getStartRight() - ew, sv.getStartRight() + ew)});
			it = inputSam.queryOverlapping(intervals);
			while(it.hasNext()) {
				currentRecord = it.next();
				if(fitSV(currentRecord, sv, bam)) {
					inputSam.close();
					return true;
				} else if(sv.isIndel() && reviewCigar(currentRecord, sv)) {
					inputSam.close();
					return true;
				}
			}
			inputSam.close();
		} catch(Exception e) {
			System.out.println(String.join(" - ", sv.getLeftChrom(), sv.getRightChrom()));
			e.printStackTrace();
			System.exit(-8);
		}
		return false;
	}
	private String transform(String chr, File bam) {
		return (headerMap.get(bam.getName())) ? String.join("", "chr", chr) : chr;
	}
	private boolean fitSV(SAMRecord current, svInfo sv, File bam) {
		var searchList = new ArrayList<search>(100);
		var sa = current.getStringAttribute("SA");
		boolean fitLeft = false, fitRight = false;
		if(sa == null || sa.equals(""))
			return false;
		searchList.add(new search(current.getReferenceName(), current.getAlignmentStart(), current.getAlignmentEnd()));
		Arrays.stream(sa.split("\t")).map(i -> i.split(",")).forEach(i -> searchList.add(new search(i[0], Integer.parseInt(i[1]), Integer.parseInt(i[1]) + TextCigarCodec.decode(i[3]).getReferenceLength())));
		for(final search el : searchList) {
			if(transform(sv.getLeftChrom(), bam).equals(el.chr) && (Math.abs(sv.getStartLeft() - el.start) <= sv.getSearchWidth() || Math.abs(sv.getStartLeft() - el.end) <= sv.getSearchWidth()))
				fitLeft = true;
			else if(transform(sv.getRightChrom(), bam).equals(el.chr) && (Math.abs(sv.getStartRight() - el.start) <= sv.getSearchWidth() || Math.abs(sv.getStartRight() - el.end) <= sv.getSearchWidth()))
				fitRight = true;
			if(fitLeft && fitRight)
				return true;
		}
		return false;
	}
	private boolean reviewCigar(SAMRecord current, svInfo sv) {
		var trackerRef = current.getAlignmentStart();
		var it = current.getCigar().getCigarElements().iterator();
		CigarElement tel = null;
		while(it.hasNext()) {
			tel = it.next();
			if(tel.getOperator().toString().equals("H"))
				continue;
			else if(tel.getOperator().toString().equals("S"))
				continue;
			else if(tel.getOperator().toString().equals("M"))
				trackerRef += tel.getLength();
			else if(tel.getOperator().toString().equals("I")) {
				if(Math.abs(sv.getStartLeft() - trackerRef) <= sv.getSearchWidth() && Math.abs(sv.getStartRight() - (trackerRef + tel.getLength())) <= sv.getSearchWidth())
					return true;
			}
			else if(tel.getOperator().toString().equals("D")) {
				if(Math.abs(sv.getStartLeft() - trackerRef) <= sv.getSearchWidth() && Math.abs(sv.getStartRight() - (trackerRef + tel.getLength())) <= sv.getSearchWidth())
					return true;
				trackerRef += tel.getLength();
			} else {
				System.out.println("You are missing the following operator: " + tel.getOperator().toString());
				System.exit(0);
			}
		}
		return false;
	}
	private boolean isOverlap(svInfo sv, IntervalST<svInfo> iTree) {
		if(iTree == null)
			return false;
		var result = iTree.searchAllList(new Interval1D(sv.lowLeft(), sv.highLeft()));
		if(result.size() > 0) {
			var rTree = new IntervalST<svInfo>();
			result.stream().map(i -> iTree.get(i)).filter(i -> i.getRightChrom().equals(sv.getRightChrom())).forEach(i -> rTree.put(new Interval1D(i.getStartRight(), i.getStartRight() + 1), i));
			return (rTree.searchAllList(new Interval1D(sv.lowRight(), sv.highRight())).size() > 0);
		}
		return false;
	}
	private ArrayList<svInfo> readPrimary(File input, double oFrac, int minOl, int maxOl){
		Supplier<Stream<String>> streamSupplier = () -> {try{return Files.lines(input.toPath());}catch(IOException e){e.printStackTrace();} return null;};
		header = String.join("\t", streamSupplier.get().findFirst().get(), "SnifflesNormals", "SnifflesControls", "BamNormals", "BamControls");
		return streamSupplier.get().skip(1).map(i -> i.split("\t")).map(i -> extractStructuralVariant(i, oFrac, minOl, maxOl)).collect(Collectors.toCollection(ArrayList::new));
	}
	private ConcurrentHashMap<String, HashMap<String, IntervalST<svInfo>>> readSecondary(File[] shared, int threads) {
		var map = new ConcurrentHashMap<String, HashMap<String, IntervalST<svInfo>>>(50, 0.9999f, threads);
		var forkJoinPool = new ForkJoinPool(threads);
		try {
			forkJoinPool.submit(() -> Arrays.stream(shared).parallel().forEach(i -> map.put(i.getName().replace("\\.txt", ""), buildMap(i)))).get();
		} catch(InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.exit(-3);
		}
		return map;
	}
	private HashMap<String, IntervalST<svInfo>> buildMap(File input) {
		var iMap = new HashMap<String, IntervalST<svInfo>>(10000, 0.9999f);
		try {
			Files.lines(input.toPath()).skip(1).map(i -> i.split("\t")).forEach(i -> {if(!iMap.containsKey(i[1])){iMap.put(i[1], new IntervalST<svInfo>());}; iMap.get(i[1]).put(new Interval1D(Integer.parseInt(i[2]),  Integer.parseInt(i[2]) + 1), extractStructuralVariant(i, 1, 1, 1));});
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-4);
		}
		return iMap;
	}
	private svInfo extractStructuralVariant(String[] tokens, double oFrac, int minOl, int maxOl) {
		if(tokens[5].contains("BND")) {
			var match = pattern.matcher(tokens[8]);
			var matchLoc = locPat.matcher(tokens[8]);
			return new svInfo(tokens, tokens[1], Integer.parseInt(tokens[2]), (match.find()) ? match.group(1).replace("chr", "") : "NA" , (matchLoc.find()) ? Integer.parseInt(matchLoc.group(1)) : -1, tokens[5], 2500.0);
		}
		return new svInfo(tokens, tokens[1], Integer.parseInt(tokens[2]), tokens[1], Integer.parseInt(tokens[3]), tokens[5], Math.min(Math.max(Math.abs(Double.parseDouble(tokens[4])) * oFrac, minOl), maxOl));
	}
}

class search {
	public String chr = null;
	public Integer start = -1;
	public Integer end = -1;
	public search(String chr, Integer start, Integer end) {
		this.chr = chr;
		this.start = start;
		this.end = end;
	}
}
class hResult {
	public String name = null;
	public Boolean state = false;
	public hResult(String name, Boolean state) {
		this.name = name;
		this.state = state;
	}
}
