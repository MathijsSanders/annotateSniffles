# Annotate Sniffles 
Annotate Sniffles output with additional statistics based on a set of unmatched normals or other controls with the purpose of filtering.

## How do I run it?

Align your PacBio data with NGMLR (https://github.com/philres/ngmlr), call SVs with Sniffles (https://github.com/fritzsedlazeck/Sniffles), annotate the called structural variants with AnnotSV (https://github.com/lgmgeo/AnnotSV) including all information you desire and finally use annotateSniffles on your sample of interest. Two types of files can be used as input for the controls:

- Sniffles-AnnotSV files: The control samples underwernt the same cycle of processing resulting in Sniffles output annotated with AnnotSV.
- PacBio BAM files: PacBio BAM files similarly aligned with NGMLR

Our package annotateSniffles uses these two types of information to either determine whether the SV is detected in the controls or find reads in the control PacBio BAM files that support SV of interest. Since long-read sequencing often results in the detection of SVs with imprecise boundaries the algorithm offers some flexibility concerning how proximal breakpoints should be in the controls with respect to the detected SV. The algorithm adds, depending on which type of information is used as input, additional columns specifying how many control samples were positive for the detected SV. This is further dichotomized into the 'normal' and 'control' categories in case certified normals and other possible controls, e.g. other tumours or non-normal controls, are included. 

### The recommended way

The pre-compiled JAR file is included with the repository, but in case the package needs to be recompiled, please run:

```bash
mvn package clean
```

The following command adds a single column to the BRASS input BED file indicating whether the SV is detectable in the PacBio data (0: absent, 1: present).

```bash
java -Xmx4G -jar annotateSniffles.jar --input-bam-file input_PacBio_BAM_file --input-sniffles-file input_Sniffles_AnnotSV_file --output-sniffles-file output_annotated_tsv_file --shared-bam-normal-files comma_separated_PacBio_BAM_files_normals --shared-bam-control-files comma_separated_PacBio_BAM_files_controls --shared-sniffles-normal-files comma_separated_Sniffles_AnnotSV_files_normals --shared-sniffles-control-files comma_separated_Sniffles_AnnotSV_files_controls --overlap-fraction key_determining_window_size --minimum-overlap minimum_window_SV_breakpoints --maximum-overlap maximum_window_SV_breakpoints --extract-width window_read_extraction_PacBio_BAM --threads threads
```

- --input-bam-file*: NGMLR PacBio BAM file of the sample of interest
- --input-sniffles-file*: Sniffles file annotated with AnnotSV for the sample of interest
- --shared-bam-normal-files: Comma-separated paths to NGMLR-aligned PacBio BAM files of the normals
- --shared-bam-control-files: Comma-separated paths to the NGMLR-aligned PacBio BAM files of the controls
- --shared-sniffles-normal-files: Comma-separated paths to the AnnotSV-annotated Sniffles output for the normals
- --shared-sniffles-controls-files: Comma-separated paths to the AnnotSV-annotated Sniffles output for the controls
- --overlap-fraction: Fraction used for determining the window size around the SV breakpoints. SVs in controls samples with breakpoints in both windows are deemed the same. Window size is limited by the mimimum and maximum window size. (default: 0.05)
- --mimimum-overlap: Minimum window size (default: 20nt)
- --maximum-overlap: Maximum window size (default: 20000nt)
- --extract-width: Window around the breakpoints for extracting reads (default: 2500nt)
- --threads: Number of threads (default: 1)
- --help, -help: Get usage information
- --version, -version: Get current version
- \* Required.

*Dependencies*
- Maven version 3+ (For compiling only).
- Java JDK 11+

*Example*
```bash
java -Xmx4G -jar annotateSniffles.jar --input-bam-file ./PacBio_BAM/SU2_pacbio.sorted.bam --input-sniffles-file ./CONTROLS/SU2.txt --output-sniffles-file ./ANNOTATED/SU2_noDGV.txt --shared-bam-normal-files ./PacBio_BAM/RA2408_ND.sorted.bam,./PacBio_BAM/RA3555_ND.sorted.bam,./PacBio_BAM/RA3612_ND.sorted.bam --shared-bam-control-files ./PacBio_BAM/Barcelona.pacbio.sorted.bam,./PacBio_BAM/CB_PacBio_hg19.sorted.bam,./PacBio_BAM/DE36_combined.sorted.bam,./PacBio_BAM/DE84.pacbio.sorted.bam,./PacBio_BAM/OH_Pacbio.hg19.sorted.bam,./PacBio_BAM/PB_Pacbio.sorted.bam,./PacBio_BAM/RALF1_pacbio.sorted.bam,./PacBio_BAM/RF_Normal.sorted.bam,./PacBio_BAM/SU1_pacbio.sorted.bam --shared-sniffles-normal-files ./NORMALS/RA2408.txt,./NORMALS/RA3555.txt,./NORMALS/RA3612.txt --shared-sniffles-control-files ./CONTROLS/Barcelona.txt,./CONTROLS/CB.txt,./CONTROLS/DE36.txt,./CONTROLS/DE84.txt,./CONTROLS/OH.txt,./CONTROLS/PB.txt,./CONTROLS/RALF1.txt,./CONTROLS/RF_Normal.txt,./CONTROLS/SU1.txt --threads 20 --minimum-overlap 50
```
