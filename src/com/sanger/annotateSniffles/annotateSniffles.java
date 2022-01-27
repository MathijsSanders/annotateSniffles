package com.sanger.annotateSniffles;

import java.io.*;
import java.util.*;
import com.beust.jcommander.*;
import com.beust.jcommander.validators.PositiveInteger;

public class annotateSniffles {
	private static String vn = "0.1";
	@Parameter
	private List<String> parameters = new ArrayList<String>();
	
	@Parameter(names = "--input-bam-file", description = "Input PacBio BAM file.", required = true, converter = FileConverter.class, validateWith = FileValidator.class, order=0)
	public File input_bam_file = null;
		
	@Parameter(names = "--input-sniffles-file", description = "Input Sniffles file to filter.", required = true, converter = FileConverter.class, validateWith=FileValidator.class, order=1)
	public File input_sniffles_file = null;
	
	@Parameter(names = "--output-sniffles-file", description = "Output annotated Sniffles file.", required = true, order=2)
	public String output_sniffles_file = null;
	
	@Parameter(names = "--shared-sniffles-normal-files", description = "Input Sniffles files of normals.", converter = CommaFileConverter.class, validateWith = CommaFileValidator.class, order=3)
	public File[] shared_normals = null;
	
	@Parameter(names = "--shared-sniffles-control-files", description = "Input Sniffles files of other controls (not specifically normal).", converter = CommaFileConverter.class, validateWith = CommaFileValidator.class, order=4)
	public File[] shared_controls = null;
	
	@Parameter(names = "--shared-bam-normal-files", description = "Input PacBio BAM files of normals.", converter = CommaFileConverter.class, validateWith = CommaFileValidator.class, order=3)
	public File[] shared_bam_normals = null;
	
	@Parameter(names = "--shared-bam-control-files", description = "Input PacBio BAM of other controls (not specifically normal).", converter = CommaFileConverter.class, validateWith = CommaFileValidator.class, order=4)
	public File[] shared_bam_controls = null;
	
	@Parameter(names = "--overlap-fraction", description = "Percentage of SV length to define overlap in interval tree (default: 0.05).", validateWith = PositiveDoubleValidator.class, converter = PositiveDoubleConverter.class, order=5)
	public Double overlap_fraction = 0.05;
	
	@Parameter(names = "--minimum-overlap", description = "Minimum width for detecting overlap in interval tree (default: 20nt).", validateWith = PositiveInteger.class, order=6)
	public Integer minimum_overlap = 20;
	
	@Parameter(names = "--maximum-overlap", description = "Maximum width for detecting overlap in interval tree (default: 20000nt).", validateWith = PositiveInteger.class, order=7)
	public Integer max_overlap = 20000;
	
	@Parameter(names = "--extract-width", description = "Window to extract reads (mutation_position +- width) (default: 2500nt).", validateWith = PositiveInteger.class, order=8)
	public Integer extract_width = 2500;
	
	@Parameter(names = "--supplementary-width", description = "Width for identifying linked alignments (default: 250nt).", validateWith = PositiveInteger.class, order=9)
	public Integer supp_width = 250;
	
	@Parameter(names = "--threads", description = "Number of threads (default: 1).", validateWith = PositiveInteger.class, order=10)
	public Integer threads = 1;
	
	@Parameter(names = {"--help","-help"}, help = true, description = "Get usage information", order=11)
	private boolean help;
	
	@Parameter(names = {"--version","-version"}, description = "Get current version", order=12)
	private Boolean version = null;
	
	public static void main(String[] args) {
		var as  = new annotateSniffles();
		JCommander jCommander = new JCommander(as);
		jCommander.setProgramName("annotateSniffles.jar");
		JCommander.newBuilder().addObject(as).build().parse(args);
		if(as.version != null && as.version) {
			System.out.printf("Annotate Sniffles output: %f", vn);
			System.exit(0);
		}
		else if(as.help) {
			jCommander.usage();
			System.exit(0);
		} else {
			var nThreads = Runtime.getRuntime().availableProcessors();
			if(as.threads > nThreads)
				System.out.println("Warning: Number of threads exceeds number of available cores");
			as.shared_normals = (as.shared_normals != null) ? exclude(as.shared_normals, as.input_sniffles_file, as.shared_controls) : null;
			as.shared_controls = (as.shared_controls != null) ? exclude(as.shared_controls, as.input_sniffles_file, as.shared_normals) : null;
			new annotateSnifflesCore(as.input_bam_file, as.input_sniffles_file, as.output_sniffles_file, as.shared_normals, as.shared_controls, as.shared_bam_normals, as.shared_bam_controls, as.overlap_fraction, as.minimum_overlap, as.max_overlap, as.extract_width, as.supp_width, as.threads);
		}
	}
	private static File[] exclude(File[] total, File input, File[] shared) {
		var set = new HashSet<String>(100, 0.9999f);
		set.add(input.getAbsolutePath());
		Arrays.stream(shared).map(i -> i.getAbsolutePath()).forEach(i -> set.add(i));
		return Arrays.stream(total).filter(i -> !set.contains(i.getAbsolutePath())).toArray(File[]::new);
	}
}
