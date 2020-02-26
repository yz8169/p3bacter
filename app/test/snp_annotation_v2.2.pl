#!/usr/bin/perl -w

=head1 Name

  snp_annation.pl (ori_script: soap_cds_snp_final.pl)  -- caculate synonymous and nonsynonymous SNP rate

=head1 Description

  read the sample snp.tab and reference: CDS.fasta or genomis.{fasta,gff}, generate sample cds file, and the snp information file.
  infile:
	snp   ->  soapsnp result or population snp matrix result
	fasta -> genomics fasta file or CDS fasta file(ffn), when ffn ID line can be:
	  >gi|299883432|ref|NC_014301.1|:43500-44459,1-3192 Halalkalicoccus jeotgali B3 plasmid 4, complete sequence
	  >gi|299883432|ref|NC_014301.1|:c13917-13708 Halalkalicoccus jeotgali B3 plasmid 4, complete sequence
	  >GHMM000004   NC_014301.1:13917-13708:- complete sequence
	  >GHMM000004   locus_tag=NC_014301.1:13917-13708:-
	  >GHMM000004   Sequencing:NC_014301.1:13917-13708:-
	  Other form can get chr,region,gene_id[,stand] info use -id_pos (rank split by /[\|\s:]+/) e.g:
		>chr1|00272012|:c3330-3878 HLA_1M_GL000003 hypothetical protein VCF_002037 [Vibrio cholerae BX 330286]
	  then set -id_pos 0,2,3(judjed stand from region)
	gff   -> gene annotation file at gff3 form, set it if fasta file is gemonics. if ffn and ID not be geneid, you can ues -gff to get gene ID
  outfile:
	*.cds               -> output sample CDS fasta
	*.cds.axt           -> axt of ref and sample, input for KaKsCaculater
	*.cds.info          -> annatation result for CDS region
	*.other.info        -> annatation result for NonCDS gene region(mRNA or 5UTR,Intron,Exon,3UTR, only out when genomics fasta and gff set);
	*.intergenic.info   -> annoatation result for Intergenic region;
	*.chr.stat          -> SNP distribution region stat for eatch chr(Intergenic,Syn,Nosyn or Intergenic,5UTR.Intron,Syn,Nosyn,3UTR);
	*.cds.stat          -> SNP at CDS region mutate type stat(Start_nonsyn Stop_nonsyn Premature_stop Nonsynonymous Start_syn Stop_syn Synonymous);

=head1 Version

  Version: 1.0,  Date: 2006-12-6
  Author: Fan Wei, fanw@genomics.org.cn
  Version: 1.2,  Date: 2012-03-30
  Modify by Wenbin Liu, add function for statting Gene type SNP and mutil codon table
  Version: 2.0,  Date: 2012-04-23
  Update: add function to annatation eatch sample(result at subdir name after sample name)
  Version: 2.1,  Date: 2013-03-26
  Update: add --anno option to show gff annotation information at *.info

=head1 Usage

  perl snp_annation.pl <snp> <fasta> [gff] 
  <snp> <fasta> [gff]   if fasta to be CDS(ffn), gff file can ignore, for all gzip is allowed
  --gff <file>      set gff to get geneid when ffn do not contail geneid, just set when fasta if ffn.
  --id_pos <str>    set chr_id,regin_pos,gene_id[,stand] rank info at ffn id line split by /[\|\s:]+/, default not use
  --verbose         output running progress information to screen
  --outdir <dir>    output directory, default=./
  --medir <dir>     merge directory for mutil sample, defualt= outdir/merge
  --code_tab <file>  set code table, default=Bin/Code_table
  --code <num>       translation table type, default=1
						1 The Standard Code
						2 The Vertebrate Mitochondrial Code
						3 The Yeast Mitochondrial Code
						4 The Mold, Protozoan, and Coelenterate Mitochondrial Code and the Mycoplasma/Spiroplasma Code
						5 The Invertebrate Mitochondrial Code
						6 The Ciliate, Dasycladacean and Hexamita Nuclear Code
						7,8 Tables 7 and 8 have been deleted, so do not select it
						9 The Echinoderm and Flatworm Mitochondrial Code
					   10 The Euplotid Nuclear Code
					   11 The Bacterial, Archaeal and Plant Plastid Code
					   12 The Alternative Yeast Nuclear Code
  --chrsel <str>    chromosome select, default stat all chromosome.
  --tagid <str>     set tagid key word at 9th rank of gff file, e.g: ID,Parent,protein, default not set
  --anno            add gff 9th rank annotation information at *.info, default not add
  --anno_list <str> input annotation list: gene_id\t[geen_type\t]annotation_descripe, default not set
  --chrmask         mask the geneid head with chromosome name.
  --prefix <str>    outfile prefix, default=snp basename
  --genesel <str>   gene select for annation, default="3UTR,5UTR,mRNA,intron,CDS,exon,gene"
  --checklen        to filter gene with length not 3X, default not filter.
  --fsize <file>    sequence length file for draw SNP chr plot, default not set.
  --windl <num>     window length for SNP chr plot, default=10000
  --merge           to make merge stat table for one sample infile
  --help            output help information to screen

=head1 Note

  1 Outfile with prefix of <snp> infile basename(default).
  2 You can use -code 12 to set yeast codon table, -code 11 for Bacterial.
  3 At --genesel 3UTR can stand for 3-UTR, ,3_UTR, 3'UTR 3'-UTR and three_prime_UTR;
	  5UTR can stand for 5-UTR, 5_UTR, 5'UTR 5'-UTR and five_prime_UTR.
  4 At --id_pos when stand rank not set it will judge from rejion_pos

=head1 Exmple

  1 do snp annatation for Yeast:
	perl snp_annation.pl yeast.snp yeast.fa yeast.gff -code 12 -outdir snpann/
  2 do snp annatation whith CDS file(ffn):
	perl snp_annatation.pl HCL.snp.maxtion HCL.ffn -code 11 -gff HCL.gff
  3 just annatation with CDS file(ffn):
	perl snp_annatation.pl xx.soapsnp.cns xx.CDS.fasta

=cut

#--subdir <dir>     set subdir name at sample_name/ to store reuslt

use strict;
use warnings;
use PerlIO::gzip;
use Cwd qw(abs_path);
use Getopt::Long;
use FindBin qw($Bin $Script);
use File::Basename qw(basename dirname);
use Data::Dumper;
use File::Path;  ## function " mkpath" and "rmtree" deal with directory
use lib "$Bin/../lib";
use PGAP qw(parse_config);

##get options from command line into variables and set default values
my ($Verbose,$Help,$Outdir,$code_tab,$code,$chrsel,$prefix,$genesel,$gff_set,$id_pos,
	$checkL,$subdir,$windl,$fsize,$medir,$chrmask,$Merge,$anno,$tagid,$anno_list);
GetOptions(
	"merge"=>\$Merge,
	"windl:i"=>\$windl,
	"fsize:s"=>\$fsize,
	"chrsel:s"=>\$chrsel,
	"medir:s"=>\$medir,
	"prefix:s"=>\$prefix,
	"gff:s"=>\$gff_set,
	"id_pos:s"=>\$id_pos,
	"code_tab:s"=>\$code_tab,
	"code:i"=>\$code,
	"checklen"=>\$checkL,
	"outdir:s"=>\$Outdir,
	"subdir:s"=>\$subdir,
	"verbose"=>\$Verbose,
	"genesel:s"=>\$genesel,
	"help"=>\$Help,
	"chrmask"=>\$chrmask,
	"anno"=>\$anno,
	"tagid:s"=>\$tagid,
	"anno_list:s"=>\$anno_list
);
$Outdir ||= "./";
die `pod2text $0` if (@ARGV == 0 || $Help);
#===================================================================================================================================
#check error:
foreach(@ARGV){(-s $_) || die"error: can't find able file $_, $!";}

my ($default_code_tab, $draw_cycle, $SNP_chr_plot, $draw_rays) = parse_config ("$Bin/../config.txt", "$Bin/..", "code_table", "cycle_dis_R", "SNP_chr_plot", "draw_radar");

$code_tab ||= "$default_code_tab";
(-s $draw_cycle) || die "error: can't find: $draw_cycle, $!";

foreach($draw_cycle,$SNP_chr_plot,$draw_rays){(-s $_) || die"can't find file $_,$!";}
$anno_list && !(-s $anno_list) && ($anno_list = 0);
my ($snp_file,$fasta_file,$gff_file) = @ARGV;
$Outdir ||= '.';
(-d $Outdir) || mkdir($Outdir);
$chrmask ||= chrmask_auto($gff_file,$chrsel,$tagid);#sub4.1
$prefix ||= basename($snp_file);
my (@SNP,%CODE,%STAR_CODE,%END_CODE,@tol_chr_snp, @sample_name, @snp_files, @homo_hete);
#my (@all_sample_snp, @all_sample_snp_tol, @sample_name);
my ($is_population,$is_list) = get_snp_hash($snp_file,\@SNP,$chrsel,\@tol_chr_snp,\@sample_name,\@snp_files,\@homo_hete);#sub3
#print Dumper \@sample_name;
#exit;

my $chr_num = keys %{$tol_chr_snp[0]};
@SNP || die"error: can't get anly SNP information, $!";
get_code($code_tab,($code || 1),\%CODE,\%STAR_CODE,\%END_CODE);#sub1
my %Abbrev = &abbrev;
my @g_type = $genesel ? split/,/,$genesel : qw(5UTR mRNA intron CDS exon gene 3UTR);
foreach(@g_type){&stand_type($_);}#sub2
my %snp_stat_num = qw(5UTR 1 intron 2 3UTR 5);
my $nn=0;
foreach(0..$#g_type){($g_type[$_] eq "CDS") && ($nn = $_);}
my (@GENE,%get_gene,%annoh);
my $anno2 = $anno ? 1 : 0;
if($anno_list){
	for(`less $anno_list`){
		/^#/ && next;
		chomp;
		my @annol = /\t/ ? split /\t+/ : split /\s+/;
		if(@annol == 3){
			${$annoh{$annol[1]}}{$annol[0]} = $annol[2];
		}else{
			(@annol > 2) && ($annol[1] = join("; ",@annol[1..$#annol]));
			${$annoh{'CDS'}}{$annol[0]} = $annol[1];
		}
	}
	($anno,$anno2) = (1, 0);
}
my $has_gff = get_gff_info($gff_file,\@g_type,\@GENE,\%get_gene,$chrsel,$chrmask,$tagid,$anno2,\%annoh);#sub4
my %gff_ID;
get_gff_info($gff_set,0,0,\%gff_ID,$chrsel,$chrmask,$tagid,$anno2,\%annoh);#sub4
$has_gff || get_ffn_info($fasta_file,\@GENE,$nn,\%get_gene,$chrsel,\%gff_ID,$id_pos,$chrmask);#sub7
my $get_len = ($has_gff && !$is_population && !($fsize && -s $fsize)) ? 1 : 0;

my @id_pos_sel;
if($id_pos){
	@id_pos_sel = split/,/,$id_pos;
}
my @FASTA;
($fasta_file=~/\.gz/) ? (open(IN,"<:gzip",$fasta_file) || die$!) : (open(IN, $fasta_file) || die ("can not open $fasta_file\n"));
$medir ||= ($Merge || $is_list || ($is_population && @sample_name > 1)) ? "$Outdir/merge" : $Outdir;
my %size_hash;
if($get_len){
	(-d $medir) || mkdir($medir);
	$fsize ||= "$medir/$prefix.genomics.size";
	open SIZ,">$fsize" || die$!;
}elsif($fsize && -s $fsize){
	%size_hash = split/\s+/,`awk '{print \$1,\$2}' $fsize`;
}
$/=">";
<IN>;
while (<IN>){
	chomp;
	my ($chr,$posse,$stand,$gene_id,$seq) = (0) x 5;
	my %ffn_gene_sel;
	$chr = $1 if(/^(\S+)/);
	if(!$has_gff){
		m/^(.+?\n)/;
		($chr,$posse,$stand,$gene_id) = get_ffn_id_line($1,$id_pos,\@id_pos_sel,$chrmask);#sub4.1
		get_cds_gene($chr,$posse,$stand,$gene_id,\%gff_ID,\%ffn_gene_sel);#sub4.2
	}elsif($id_pos){
		m/^(.+?\n)/;
		$chr = (split/[\|:\s]+/,$1)[$id_pos_sel[0]];
	}
	next if(!exists $get_gene{$chr});
	$chrsel && ($chrsel ne $chr) && next;
	s/^.+?\n|\s//g;
	$seq = uc; ## all upper case
	$seq || die"error: there's 0 length sequence at fasta file\n";
	push @FASTA,[$chr,$posse,$stand,$gene_id,$seq,\%ffn_gene_sel];#if genomics is big and only one sample, just use this cycle at foreach(WFASTA).
	if($get_len){
		my $seq_len = length($seq);
		print SIZ "$chr\t$seq_len\n";
		$size_hash{$chr} = $seq_len;
	}
}
close IN;
$get_len && close(SIZ);
$/="\n";

#====>>> 
my $samp_num = (@sample_name > 1) ? $#sample_name : 0;
$medir ||= "$Outdir/merge";
(-d $medir) || mkdir($medir);
$medir = abs_path($medir);
$Outdir = abs_path($Outdir);
my ($outdir0,$prefix0) = ($Outdir,$prefix);
($Merge || $samp_num || $is_list) && (open AXTL,">$medir/$prefix.axt.list" || die$!);
foreach my $hh (0..$samp_num){
	($Merge && !$hh && $is_population && $samp_num==1) && next;
	($SNP[$hh] && %{$SNP[$hh]}) || next;
	my %SNP = %{$SNP[$hh]};
	my %tol_chr_snp = %{$tol_chr_snp[$hh]};
	my %SNP_intergenic;
	my %homo_hete = $homo_hete[$hh] ? %{$homo_hete[$hh]} : ();
	foreach my $chr(keys %SNP){
		foreach my $pos (keys %{$SNP{$chr}}){
			$SNP_intergenic{$chr}{$pos} = $SNP{$chr}{$pos};
		}
	}
	if($hh || (!$is_population && $is_list)){
		$prefix = $sample_name[$hh];
		$Outdir = "$outdir0/$prefix";
		$subdir && ($Outdir .= "/$subdir");
		(-d $Outdir) || `mkdir -p $Outdir`;
		print AXTL "$prefix\t$Outdir/$prefix.cds.axt.txt\n";
	}elsif($is_population && $samp_num){
		$prefix .= ".population";
		$Outdir = $medir;
	}
## ONLY annotation for Fungi
	if(%homo_hete && %size_hash){
		open HOE,">$Outdir/$prefix.hoe.stat.xls" || die$!;
		print HOE "ChrID\tGenome_Size(bp)\t#SNP\trate(%)\t#homo\trate(%)\t#hete\trate(%)\n";
		my @thoe;
		foreach my $chr(sort keys %size_hash){
			my @hoe;
			@hoe[2,4] = ($homo_hete{$chr}->[0] || 0, $homo_hete{$chr}->[1] || 0);
			$hoe[0] += $hoe[2] + $hoe[4];
			foreach(0,2,4){
				$hoe[$_+1] = sprintf "%0.4f",100*$hoe[$_]/$size_hash{$chr};
				$thoe[$_] += $hoe[$_];
			}
			$thoe[6] += $size_hash{$chr};
			print HOE join("\t",$chr,$size_hash{$chr},@hoe),"\n";
		}
		foreach(0,2,4){$thoe[$_+1] = sprintf "%0.4f",100*$thoe[$_]/$thoe[6];}
		print HOE join("\t",'Total',@thoe[6,0..5]),"\n";
		close HOE;
		%homo_hete = ();
		%{$homo_hete[$hh]} = ();
	}


	my $out_inf_file = "$Outdir/$prefix.cds.info.xls";
	my $out_cds_file = "$Outdir/$prefix.cds";
	my $out_axt_file = "$Outdir/$prefix.cds.axt.txt";
	my $out_other_file = "$Outdir/$prefix.other.info.xls";
	open INF,">$out_inf_file" || die$!;
	open CDS,">$out_cds_file" || die$!;
	open AXT,">$out_axt_file" || die$!;
	open OTH,">$out_other_file" || die$!;
	my $has_other_region = 0;
	my %tol_cds_snp_num;
	my %snp_type_num;
	my %mut_type_stat;
	my $inf_head = "ref_id\tposition\tref_base<->sample_base\tsnp_status\tstrand\tcodon_phase\tcodon_mutate\taa_mutate\tmutate_type\tsynonymous\tnonsynonymous\tgene_id\tpos_start\tpos_end";
	$anno && ($inf_head .= "\tannotation");
	print INF $inf_head,"\n";
	##chr10   104221044	    A<->R    Het-one +	729:2   GTA<->GTG		V<->V		0.5		0		NM_024789		104220315	104223314
	my $oth_head = "ref_id\tposition\tref_base\tsample_base\tgene_type\tgene_start\tgene_end\tgene_stand\tgene_name";
	$oth_head .= $anno ? "\tannotation\n" : "\n";
	print OTH $oth_head;

	foreach(@FASTA){
		my ($chr,$posse,$stand,$gene_id,$seq,$ffn_gene_sel) = @{$_};
		$tol_chr_snp{$chr} || next;	
		my @snp_num = (0,0,0,0,0);
		my %cds_snp_gene;
		foreach(0..$#g_type){
			my $num = &loop_region($GENE[$_]->{$chr},$SNP{$chr},$g_type[$_],$chr,\%cds_snp_gene,!$has_gff,$ffn_gene_sel,
				$SNP_intergenic{$chr},\%tol_cds_snp_num,$anno,\%annoh);#sub5
			$num && $snp_stat_num{$g_type[$_]} && 
			($snp_num[$snp_stat_num{$g_type[$_]}-1] += $num, $has_other_region = 1);
		}
		if(!%cds_snp_gene){
			foreach my $i(0..5){$snp_type_num{$chr}->[$i] += ($snp_num[$i] || 0); }
			next;
		}
#	%cds_snp_gene || (goto STASNP);
		foreach my $gene_id (sort {$GENE[$nn]->{$chr}->{$a}->[0]->[0]<=>$GENE[$nn]->{$chr}->{$b}->[0]->[0]} keys %cds_snp_gene){
			my ($gene_cds_str,$stand);
			my @snp_pos;
			my $gene_cds_len_p = 0;
			foreach my $cds_p (@{$GENE[$nn]->{$chr}->{$gene_id}}){
				$has_gff && ($gene_cds_str .= substr($seq,$cds_p->[0]-1,$cds_p->[1]-$cds_p->[0]+1));
				if(@$cds_p > 4){
					push @snp_pos,@{$cds_p}[4..$#$cds_p];
					@{$cds_p} = @{$cds_p}[0..3];
				}
				$stand = $cds_p->[2];
				$gene_cds_len_p += $cds_p->[1]-$cds_p->[0]+1;
			}
			$has_gff || ($gene_cds_str = $seq);
			my $gene_cds_len = length($gene_cds_str);
			if($gene_cds_len != $gene_cds_len_p){
				warn"Note: gene $gene_id at $chr with length $gene_cds_len not equal to pos caculate length $gene_cds_len_p\n";
				next;
			}
			my $un_three;
			if($checkL && $gene_cds_len % 3){
				if($checkL){
					warn"Note: gene $gene_id at $chr with length $gene_cds_len isn't 3X\n";
					next;
				}
				$un_three = 1;
			}
			if($stand eq '-'){
				if($has_gff){
					$gene_cds_str =~ tr/AGCTagctMRWSYK/TCGAtcgaKYWSRM/;
					$gene_cds_str = reverse($gene_cds_str);
				}
				foreach(@snp_pos){
					$_->[0] =~ tr/AGCTagctMRWSYK/TCGAtcgaKYWSRM/;
					$_->[1] =~ tr/AGCTagctMRWSYK/TCGAtcgaKYWSRM/;
					$_->[2] = $gene_cds_len - $_->[2] + 1;
				}
			}
			my ($ref_cds_str,$axt_cds_str,$end_codon) = ($gene_cds_str, $gene_cds_str, int($gene_cds_len/3));
			my @out_cds_pos;
			my %out_cds_mut;
			foreach (@snp_pos){
				my $phase = ($_->[2]-1)%3;
				my $codon = ($_->[2]-1-$phase)/3 + 1;
				my $ref_codon = substr($ref_cds_str,$_->[2]-1-$phase,3);
				my ($new_ref_codon,$str_codon,$mous_nr,$h_type,$mut_type,$mous_codon,$mous_aa,$syn_mous,$nosyn_mous) = 
				homo_hete($_->[0],$_->[1],$ref_codon,$phase,$codon,$end_codon,\$snp_num[2],\$snp_num[3]);#sub6
				substr($gene_cds_str,$_->[2]-1-$phase,3) = $str_codon;
				substr($axt_cds_str,$_->[2]-1-$phase,3) = $str_codon;
				($ref_codon =~ /[MRWSYK]/) && (substr($ref_cds_str,$_->[2]-1-$phase,3) = $new_ref_codon);
#			substr($gene_cds_str,$_->[2]-1,1) = $_->[1];
#			substr($axt_cds_str,$_->[2]-1,1) = $str_nt;
#            ($_->[0] =~ /[MRWSYK]/) && (substr($ref_cds_str,$_->[2]-1,1) = $ref_nt);
				$mut_type_stat{$mut_type}++;
				my @outp = ($chr,$_->[3],$mous_nr,$h_type,$stand,"$codon:$phase",$mous_codon,$mous_aa,
					$mut_type,$syn_mous,$nosyn_mous,$gene_id,@{$_}[4,5]);
				$anno && (push @outp,sanno_str(\%annoh,'CDS',$gene_id));
				print INF join("\t",@outp),"\n";
				my $key = "$_->[4]-$_->[5]";
				(!@out_cds_pos || $key ne $out_cds_pos[-1]) && (push @out_cds_pos,$key);
				$out_cds_mut{$mut_type}++;
			}
			$gene_cds_str =~ s/(.{1,60})/$1\n/g;
			my $temp_cds_snp_num = 0;
			my @out_cds_mut;
			foreach(keys %out_cds_mut){
				push @out_cds_mut , $_ . $out_cds_mut{$_};
				$temp_cds_snp_num += $out_cds_mut{$_};
			}
			my $out_cds_mut = join("|",@out_cds_mut);
			$un_three && ($out_cds_mut .= "|Not3X");
			my $out_gene_id = "$gene_id\t$chr:" . join(",",@out_cds_pos) . ":$stand\t$out_cds_mut\tSNP_num=$temp_cds_snp_num\n";
			print CDS ">$out_gene_id",$gene_cds_str;
			print AXT "$out_gene_id",$ref_cds_str,"\n",$axt_cds_str,"\n\n";
		}
#    STASNP:
		foreach my $i(0..5){
			$snp_type_num{$chr}->[$i] += ($snp_num[$i] || 0);
		}
	}
	close INF;
	close CDS;
	close AXT;
	close OTH;
	(-z $out_other_file) && `rm $out_other_file`;

	foreach (keys %SNP_intergenic){
		%{$SNP_intergenic{$_}} || (delete $SNP_intergenic{$_});
	}
	my %tol_intergenic_num;
	if(%SNP_intergenic){
		my $out_intergenic_file = "$Outdir/$prefix.intergenic.info.xls";
		open INT,">$out_intergenic_file" || die$!;
		my $int_head = "ref_id\tposition\tref_base\tsample_base\tgene_dis\tgene_start\tgene_end\tgene_stand\tgene_name";
		$int_head .= $anno ? "\tannotation\n" : "\n";
		print INT $int_head;
		foreach my $chr (sort keys %SNP_intergenic){
			$tol_intergenic_num{$chr} = keys %{$SNP_intergenic{$chr}};
			foreach my $pos (sort {$a<=>$b} keys %{$SNP_intergenic{$chr}}){
				my ($dis,$start,$end,$stand,$gene_id);
				if($GENE[$nn]{$chr} && %{$GENE[$nn]{$chr}}){
					($dis,$start,$end,$stand,$gene_id) = get_intergenic($pos,$GENE[$nn]{$chr});#sub8
				}else{
					($dis,$start,$end,$stand,$gene_id) = qw(. . . . .);
				}
				my @outp2 = ($chr,$pos,@{$SNP_intergenic{$chr}{$pos}},$dis,$start,$end,$stand,$gene_id);
				$anno && (push @outp2,&sanno_str(\%annoh,'CDS',$gene_id));
				print INT join("\t",@outp2),"\n";
			}
		}
		close INT;
	}

	open STO,">$Outdir/$prefix.chr.stat.xls" || die$!;
#my @signs = qw(#5UTR #Intron #Syn #Non_syn #3UTR);
	my @signs = split/\s+/,"ChrID #Tol_SNP #Intergenic #5UTR #Intron #Syn #Non_syn #3UTR";
	my @num = (0..4);
	if(!$has_other_region){
		@num = (2,3);
		@signs = split/\s+/,'ChrID #Tol_SNP #Intergenic #Syn #Non_syn';
	}

	print STO join("\t",@signs),"\n";
	my @total = (0,0,0,0,0,0,0);
	foreach my $k (sort keys %tol_chr_snp){
		my @chr_stat;
		$chr_stat[5] = ($tol_intergenic_num{$k} || 0);
		foreach my $i(@num){
			$chr_stat[$i] = $snp_type_num{$k} ? ($snp_type_num{$k}->[$i] || 0) : 0;
			$total[$i] += $chr_stat[$i];
		}
		if($snp_type_num{$k}){
			@{$snp_type_num{$k}} = ();
			delete $snp_type_num{$k};
		}
		$total[5] += $tol_chr_snp{$k};
		$total[6] += $chr_stat[5];
		print STO join("\t",$k,$tol_chr_snp{$k},$chr_stat[5],@chr_stat[@num]),"\n";
	}
	$chrsel || (print STO join("\t",'Total',@total[5,6,@num]),"\n");
	close STO;
	if(!$is_population && $chr_num <= 25 && $chr_num > 1){
#-title \"SNP in Genome Distribution\"
		system"perl $draw_cycle  $Outdir/$prefix.chr.stat.xls $Outdir/$prefix.cycle.png";
	}
	#%mut_type_stat || next;
#@signs = qw(Start_nonsyn Stop_nonsyn Premature_stop Nonsynonymous Start_syn Stop_syn Synonymous);
	@signs = qw(Start_syn Stop_syn Start_nonsyn Stop_nonsyn Premature_stop Synonymous Nonsynonymous);
	open STA,">$Outdir/$prefix.cds.stat.xls" || die$!;
	print STA "Mutate_Type\tSNP_Number\tPercentage(%)\n";
	my $percent;
	my $cds_snp_toal = 0;
	foreach(@signs){
		$mut_type_stat{$_} ||= 0;
		$percent = sprintf "%0.4f",($mut_type_stat{$_}/$total[5])*100;
		print STA join("\t",$_,$mut_type_stat{$_},$percent),"\n";
		$cds_snp_toal += $mut_type_stat{$_};
		delete $mut_type_stat{$_};
	}
	$cds_snp_toal = keys %tol_cds_snp_num;
	$percent = sprintf "%0.4f",($cds_snp_toal/$total[5])*100;
	print STA join("\t","Total_CDS_SNP",$cds_snp_toal,$percent),"\n";
	my $intergenic_toal = $total[5] - $cds_snp_toal;
	$percent = sprintf "%0.4f", ($intergenic_toal/$total[5])*100;
	print STA join ("\t", "Intergenic", $intergenic_toal, $percent), "\n";
	print STA "Total_SNP\t$total[5]\n";
	close STA;

}
($Merge || $samp_num || $is_list) && close(AXTL);
($Merge || $samp_num || $is_list) || exit(0);
## <<<=====
$is_population && (shift @sample_name);
$Outdir = $outdir0;
@sample_name = sort @sample_name;
open STA,">$medir/$prefix0.total.chr.stat.xls" || die$!;
my $head = `head -1 $Outdir/$sample_name[0]/$sample_name[0].chr.stat.xls`;
$head =~ s/^\S+/Sample_name/;
foreach(@sample_name){
	my $line = `tail -1 $Outdir/$_/$_.chr.stat.xls`;
	$line =~ s/^\S+/$_/;
	$head .= $line;
}
print STA $head;
close STA;
if(!$is_population){
	open STA,">$medir/$prefix0.total.hoe.stat.xls" || die$!;
	$head = `head -1 $Outdir/$sample_name[0]/$sample_name[0].hoe.stat.xls`;
	$head =~ s/^\S+/Sample_name/;
	foreach(@sample_name){
		my $line = `tail -1 $Outdir/$_/$_.hoe.stat.xls`;
		$line =~ s/^\S+/$_/;
		$head .= $line;
	}
	print STA $head;
	close STA;
}
open STAT,">$medir/$prefix0.total.cds.stat.xls" || die$!;
my @signs = split/\s+/,`awk '{print \$1}' $Outdir/$sample_name[0]/$sample_name[0].cds.stat.xls`;
shift @signs if ($signs[0] eq "Mutate_Type");
print STAT join("\t",qw(Sample_name Type),@signs),"\n";
foreach(@sample_name){
	my (@l1,@l2);
	foreach(`less $Outdir/$_/$_.cds.stat.xls`){
		/^Mutate_Type/ && next;
		my @l = split;
		&comma_add (0, $l[1]) && push @l1,$l[1];
		(@l>2) && (&comma_add (4, $l[2])) && (push @l2,$l[2]);
	}
	print STAT join("\t",$_,'number',@l1),"\n \trate(%)\t",join("\t",@l2),"\t--\n";
}
close STAT;
if(!$is_population && ($Merge || $samp_num || $is_list)){
	if($fsize && -s $fsize){
		$fsize = abs_path($fsize);
		my $symbol = join(",",@sample_name);
		$windl ||= 10000;
#		print "cd $medir; perl $SNP_chr_plot @snp_files -symbol $symbol -onenosym -windl $windl -prefix $prefix0 $fsize\n";
#		sleep 60;
		system"cd $medir; perl $SNP_chr_plot @snp_files -symbol $symbol -onenosym -windl $windl -prefix $prefix0 $fsize";
	}
	@signs = split/\s+/,`awk '{print \$1}' $Outdir/$sample_name[0]/$sample_name[0].chr.stat.xls`;
	shift @signs;
	pop @signs;
	(@signs > 2 && @signs <= 25) || exit(0);
	open ACH,">$medir/$prefix0.snp.chrdis" || die$!;
	print ACH "\t",join("\t",@signs),"\n";
	foreach(@sample_name){
		@signs = split/\s+/,`awk '{print \$2}' $Outdir/$_/$_.chr.stat.xls`;
		shift @signs;
		pop @signs;
		print ACH join("\t",$_,@signs),"\n";
	}
	close ACH;
#-title \"SNP in Genome Distribution\"
	system"perl $draw_rays $medir/$prefix0.snp.chrdis > $medir/$prefix0.snp.chrdis.svg
	`convert $medir/$prefix0.snp.chrdis.svg $medir/$prefix0.snp.chrdis.png`";
}

#===================================================================================================================================
#sub1
#===========
sub get_code
#===========
{
	my ($code_tab,$num,$hash_m,$hash_s,$hash_e) = @_;
	my %hash = split/\s*=\s*|\n\n+/,` awk '(!/^#/)' $code_tab`;
	$hash{$num} || die"error: can't find $num at $code_tab, $!";
	my @l = split/\n/,$hash{$num};
	my $len = length($l[0]) - 1;
	foreach my $i(0..$len){
		my @base;
		foreach my $j(0..4){push @base,substr($l[$j],$i,1);}
		my $cod = join("",@base[2..4]);
		$hash_m->{$cod} = $base[0];
		($hash_s && $base[1] eq "M") && ($hash_s->{$cod} = $base[0]);
		($hash_e && $base[0] eq "*") && ($hash_e->{$cod} = $base[0]);
	}
}
#sub2
# to make gene name at stand
#=============
sub stand_type
#=============
{
# miRNA miRNA_star rRNA TE tRNA ncRNA protein pseudogene pseudogenic_exon pseudogenic_transcript snoRNA snRNA
# transposable_element_gene misc_RNA retrotransposed start_codon stop_codon);
	if($_[0]=~/exon|intron/i && $_[0]=~/UTR/i){#UTR exion&intron stat at UTR
	}elsif($_[0] =~ /3\S*UTR|three_prime_UTR/i){
		$_[0] = '3UTR';
	}elsif($_[0] =~ /5\S*UTR|five_prime_UTR/i){
		$_[0] = '5UTR';
	}elsif($_[0] =~ /exon/i){
		$_[0] = 'exon';
	}elsif($_[0] =~ /intron/i){
		$_[0] = 'intron';
	}elsif($_[0] =~ /\bCDS\b/i){
		$_[0] = 'CDS';
	}
}
#sub8
#==================
sub get_intergenic{
#==================
	my ($pos, $hash) = @_;
	my ($dis,$start,$end,$stand,$gene_id);
	my @id = sort {$hash->{$a}->[0]->[0] <=> $hash->{$b}->[0]->[0]} (keys %{$hash});
	if($pos < $hash->{$id[0]}->[0]->[0]){
		if($hash->{$id[0]}->[0]->[0] > $hash->{$id[-1]}->[-1]->[1] && $pos - $hash->{$id[-1]}->[-1]->[1] < $hash->{$id[0]}->[0]->[0] - $pos){
			$gene_id = $id[-1];
			$dis = $pos - $hash->{$id[-1]}->[-1]->[1];
		}else{
			$gene_id = $id[0];
			$dis = $hash->{$id[0]}->[0]->[0] - $pos;
		}
#    }elsif($pos > $hash->{$id[-1]}->[-1]->[1]){
#        $gene_id = $id[-1]; #here we not caculate cycle chr, need genomics size info
	}else{
		my $j = 0;
		foreach my $i(1..$#id){
			($hash->{$id[$i]}->[0]->[0] < $pos) && next;
			$j = $i;
			last;
		}
		if($j==0){
			$gene_id = $id[-1];
			$dis = $pos - $hash->{$id[-1]}->[-1]->[1];
		}elsif($pos - $hash->{$id[$j-1]}->[-1]->[1] < $hash->{$id[$j]}->[0]->[0] - $pos){
			$gene_id = $id[$j-1];
			$dis = $pos - $hash->{$id[$j-1]}->[-1]->[1];
		}else{
			$gene_id = $id[$j];
			$dis = $hash->{$id[$j]}->[0]->[0] - $pos;
		}
	}
	($start,$stand) = @{$hash->{$gene_id}->[0]}[0,2];
	$end = $hash->{$gene_id}->[-1]->[1];
	($dis,$start,$end,$stand,$gene_id);
}

#sub3
#================
sub get_snp_hash
#================
{
	my ($snp_file0,$SNP,$chrsel,$tol_chr_snp,$sample_name,$sample_file,$homo_hete) = @_;
## Ca21chr1        43483   T       A       99      A       34      32      32      T       0       0       0       32      1.00000 1.00000 0       20
	#
#        ref     VC1191  VC1215  VC1232  VC1242  VC1374  VC1447  VC1449
# HLA_1M_9324     C       C       C       C       C       C       C     
	my @snp_files;
	my %capui = ("AC","M", "CA","M", "GT","K", "TG","K", "CT","Y",
		"TC","Y", "AG","R", "GA","R", "AT","W", "TA","W", "CG","S", "GC","S");
	chomp(my $snp_file_head = `less $snp_file0 | head -1`);
	my $is_list = 0;
	if((-s $snp_file_head) || ($snp_file_head =~ /^(\S+)[\s=]+(\S+)/ && -s $2)){
		$is_list = 1;
		($snp_file0=~/\.gz/) ? (open(IN,"<:gzip",$snp_file0) || die$!) : (open(IN,$snp_file0) || die "can't open the $snp_file0\n");
		while(<IN>){
			chomp;
			if(-s $_){
				push @snp_files,$_;
				push @$sample_name,(split/\//)[-2];
			}elsif(/^(\S+)[\s=]+(\S+)/){
				push @$sample_name,$1;
				push @snp_files,$2;
			}
		}
		close IN;
	}else{
		push @snp_files,$snp_file0;
	}
	my $is_population = 0;
	my $has_depth = 0;
	$sample_file && (@$sample_file = @snp_files);
	foreach my $fn(0..$#snp_files){
		my $snp_file = $snp_files[$fn];
		($snp_file=~/\.gz/) ? (open(IN,"<:gzip",$snp_file) || die$!) : (open(IN,$snp_file) || die "can't open the $snp_file\n");
		my $samp_num = 0;
		while(<IN>){
			/^#/ && next;
			if(/^\s+ref/){
				$is_population = 1;
				s/^\s+//;
				my @h = split;
#				(@h > 2 || $Merge) && (@{$sample_name} = @h, $samp_num = $#h);
				if (@h > 3 || $Merge) {
					if (/\s+\S+\.depth\b/) {
						$has_depth = 1;
						for (my $i=0; $i<=$#h; $i+=3) {
							$h[$i] =~ s/\.depth//;
							push @{$sample_name}, $h[$i];
							$samp_num ++;
						}
					}
					else {
						for (my $i=0; $i<=$#h; $i+=2) {
							$h[$i] =~ s/\.base//;
							push @{$sample_name}, $h[$i];
							$samp_num ++;
						}
					}
					$samp_num --;
				}
				next;
			}
			chomp;
			my @l = split;
			my ($chr,$pos,$ref_type,$str_type);
			if($is_population){
#				my $star_rank = 2;
				my $star_rank = 3; #modify by liangshuqing at 20160126
				if($l[0] =~ /(\S+)_(\d+)/){
					($chr,$pos) = ($1, $2);
					$ref_type = $l[1];
				}else{
					($chr,$pos,$ref_type) = @l[0,1,2];#may be we will change the matrix form at the fiture
					$star_rank = 4;
				}
				$chrsel && ($chrsel ne $chr) && next;
				my ($str1,$str2);
#				@l = @l[$star_rank..$#l];
				my @l_t;
				if (!$has_depth) {
					for (my $i=$star_rank; $i<=$#l; $i+=2) { push @l_t, $l[$i]; }
				}
				else {
					for (my $i=$star_rank; $i<=$#l; $i+=3) { push @l_t, $l[$i]; }
				}
				@l = @l_t;
				foreach my $i(0..$#l){
					($l[$i]=~/[ATCG]/) || next;
					($l[$i] eq $ref_type) && next;
					if($Merge || $samp_num > 1){
						$SNP->[$i+1]->{$chr}->{$pos} = [$ref_type,$l[$i]];
						$tol_chr_snp->[$i+1]->{$chr}++;
					}
					if(!$str1){$str1 = $l[$i]; next;}
					if(!$str2 && $l[$i] ne $str1){$str2 = $l[$i];}
				}
#            $str1 ||= $ref_type;
#            (@l>$star_rank+1) && ($str2 ||= $l[$star_rank]);
#            $str_type = ($str2 && ($str2 ne $str1)) ? ($capui{"$str1$str2"} || $str1) : $str1;
				$str1 && ($str_type =  $str2 ? ($capui{"$str1$str2"} || $str1) : $str1);
			}else{
				($chr,$pos,$ref_type,$str_type) = @l[0,1,2,4];
			}
			$str_type || next;
			$chrsel && ($chrsel ne $chr) && next;
			$tol_chr_snp->[$fn]->{$chr}++;
			$SNP->[$fn]->{$chr}->{$pos} = [$ref_type,$str_type];
			$is_population && next;
			my $hohe = ($str_type =~ /[ATCG]/i) ? 0 : 1;
			$homo_hete->[$fn]->{$chr}->[$hohe]++;
		}
		close(IN);
	}
	($is_population, $is_list);
}
#sub4
#================
sub get_gff_info{
#================
#NC_002516.2     RefSeq  gene    483     2027    .       +       .       ID=gene0;Name=dnaA;Dbxref="GeneID:878417";gbkey=Gene;gene=dnaA;locus_tag=PA0001
	my ($refGene_file,$g_type,$GENE,$get_gene,$chrsel,$chrmask,$tag_id,$do_anno,$annoh) = @_;
	($refGene_file && -s $refGene_file) || return(0);
	my %repeat_region;
	($refGene_file=~/\.gz$/) ? (open(REF,"<:gzip",$refGene_file)||die$!) : (open(REF,$refGene_file) || die"can't open the $refGene_file\n");
	while(<REF>){
#		last if /^##FASTA/;
		/^#/ && next;
		chomp;
		my @temp=split(/\t/);
		next if (@temp < 9);
		my $chr=$temp[0];
		$chrsel && ($chrsel ne $chr) && next;
		my $ID = get_locus_tag($temp[8],$tag_id);#sub4.0
		$chrmask && ($ID = "$chr:$ID");
		my $type=$temp[2];
		&stand_type($type);
		$do_anno && (${$annoh}{$type}{$ID} = $temp[8]);
		if(!$g_type){
			my $key = join(":",$chr,"$temp[3]-$temp[4]",$temp[6]);
			$get_gene->{$key} = $ID;
		}else{
			my $gene_key = join(" ",$chr,$type,@temp[3,4,6],$ID);
			if($repeat_region{$gene_key}){
				$Verbose && warn"Note: $gene_key appear again at gff file\n";
				next;
			}
			$repeat_region{$gene_key} = 1;
			foreach(0..$#$g_type){
				if($g_type->[$_]=~/\b$type\b/i){
					if(!$GENE->[$_]{$chr}{$ID} || $GENE->[$_]{$chr}{$ID}->[-1]->[0] != $temp[3]){
						push @{$GENE->[$_]{$chr}{$ID}}, [@temp[3,4,6],$ID];#[$start,$end,$strand,$ID];
						$get_gene->{$chr} = 1;
					}
				}
			}
		}
	}
	close(REF);
	1;
}
#sub4.0
sub get_locus_tag{
	my ($arrayTmp,$tag_id) = @_;
	$arrayTmp .= " ";
	$arrayTmp =~ s/\s+$/;/;
	if($tag_id && $arrayTmp =~ /$tag_id([\s=]+)(\S+?)[\s;]/){
		return($2);
	}
	my $locus_tag = $1 if (
		$arrayTmp=~/locus_tag[\s=]+(\S+?)[\s;]/ || 
		$arrayTmp=~/Name[\s=]+(\S+?)[\s;]/ ||
		$arrayTmp=~/Parent=(\S+?)[\s;]/ || 
		$arrayTmp=~/ID[\s=]+(\S+?)[\s:;]/ ||
		$arrayTmp=~/protein_id[\s=]+(\S+;)[\s;]/ || 
		$arrayTmp=~/gene_id[\s=]+(\S+?)[\s;]/ ||
		$arrayTmp=~/\bid[\s=]+(\S+?)[\s;]/ || 
		$arrayTmp=~/(\S+?)\s*;/);
	$locus_tag =~ s/^"|"$|;$//g;
	$locus_tag || "Unknow_ID";
}
#sub4.1
#================
sub chrmask_auto{
#================
#NC_002516.2     RefSeq  gene    483     2027    .       +       .       ID=gene0;Name=dnaA;Dbxref="GeneID:878417";gbkey=Gene;gene=dnaA;locus_tag=PA0001
	my ($refGene_file,$chrsel,$tag_id) = @_;
	($refGene_file && -s $refGene_file) || return(0);
	$chrsel && return(0);
	($refGene_file=~/\.gz$/) ? (open(REF,"<:gzip",$refGene_file)||die$!) : (open(REF,$refGene_file) || die"can't open the $refGene_file\n");
	my %uniq;
	while(<REF>){
#		last if /^##FASTA/;
		/^#/ && next;
		chomp;
		my @temp=split(/\t/);
		next if (@temp < 9);
		($temp[2] ne "CDS") && next;
		my $chr = $temp[0];
		my $ID = get_locus_tag($temp[8],$tag_id);#sub4.0
		if($uniq{$ID} && ($uniq{$ID} ne $chr)){
			close REF;
			return(1);
		}else{
			$uniq{$ID} = $chr;
		}
	}
	close REF;
	0;
}
#sub7
#================
sub get_ffn_info
#================
{
#>gi|299883432|ref|NC_014301.1|:43500-44459,1-3192 Halalkalicoccus jeotgali B3 plasmid 4, complete sequence
#>gi|299883432|ref|NC_014301.1|:c13917-13708 Halalkalicoccus jeotgali B3 plasmid 4, complete sequence
#>GHMM000004   NC_014301.1:13917-13708:- complete sequence
#>GHMM000004   Located=NC_014301.1:13917-13708:-
#>GHMM000004   Sequencing:NC_014301.1:13917-13708:-
	my ($fasta_file,$GENE,$nn,$get_gene,$chrsel,$gff_ID,$id_pos,$chrmask) = @_;
	my @id_pos_sel = $id_pos ? (split/,/,$id_pos) : ();
	foreach(`grep \">\" $fasta_file`){
		my ($chr,$posse,$stand,$gene_id) = get_ffn_id_line($_,$id_pos,\@id_pos_sel,$chrmask);#sub4.1
		$get_gene->{$chr} = 1;
		my @out_gene = get_cds_gene($chr,$posse,$stand,$gene_id,$gff_ID);
		foreach(@out_gene){
			push @{$GENE->[$nn]{$chr}{$_->[-1]}},$_;
		}
	}
}
#sub4.1
sub get_ffn_id_line{
	my ($line,$id_pos,$id_pos_sel,$chrmask) = @_;
	$line =~ s/^>//;
	($line =~ m/^(\S+)/) || next;
	my $gene_id = $1;
	my ($chr,$posse,$stand);
	if($id_pos){
		my @l = (split/[\|\s:]+/)[@$id_pos_sel];
		($chr,$posse,$gene_id,$stand) = @l;
		$stand ||= ($posse =~ /c/) ? '-' : '+';
		$posse =~ s/c//g;
	}elsif($gene_id =~ /\S+\|\S+/){
		($chr,$posse) = (split/\|/,$gene_id)[-2,-1];
		$stand = ($posse =~ /c/) ? '-' : '+';
		$posse =~ s/[:c]//g;
	}elsif($line =~ m/(\S+:\S+:\S+)/){
		my @p = split/:|=/,$1;
		(@p == 4) && (shift @p);
		($chr,$posse,$stand) = @p;
	}else{
		die"error at input fasta form, $!";
	}
	$chrmask && ($gene_id = "$chr:$gene_id");
	($chr,$posse,$stand,$gene_id);
}
#sub4.2
sub get_cds_gene{
	my ($chr,$posse,$stand,$gene_id,$gff_ID,$hash) = @_;
	my @out;
	foreach(split/,/,$posse){
		my @pos = split/-/;
		($pos[0]>$pos[1]) && ($stand='-', @pos = @pos[1,0]);
		my $key = join(":",$chr,"$pos[0]-$pos[1]",$stand);
		my $tem_gene_id = ($gff_ID->{$key} || $gene_id);
		$hash ? ($hash->{$tem_gene_id} = 1) : 
		(push @out,[@pos,$stand,$tem_gene_id]);
	}
	@out;
}

#sub5
## loop each region
###########################
#===============
sub loop_region
#===============
{
	my ($GENE, $chr_snp_p, $type,$chr,$get_gene,$is_ffn,$ffn_gene_sel,$snp_inter_genic,$tol_cds_snp_num,$anno,$annoh) = @_;
	($GENE && %{$GENE}) || return(0);
	my ($stat,$ll,$outprint) = (0, 0);
	my $is_cds = ($type eq 'CDS') ? 1 : 0;
	my @key_sel = $is_ffn ? (keys %$ffn_gene_sel) : (keys %$GENE);
	foreach my $gene_id (@key_sel){
#		@$gene_p = sort {$a->[0] <=> $b->[0]} @$gene_p;
		my $gene_p = $GENE->{$gene_id};
		my $len = 0;
		foreach  my $region_p (@$gene_p){ ##loop for each region
			my ($s, $e) = @{$region_p}[0,1];
			foreach my $j($s .. $e) {
				if(!$e || !$s){
					print STDERR "Note: gene: $gene_id miss locate information\n";
					next;
				}
				if (exists $chr_snp_p->{$j}){
					(exists $snp_inter_genic->{$j}) && (delete $snp_inter_genic->{$j});
					if($is_cds){
						push @{$region_p},[@{$chr_snp_p->{$j}},$len+$j-$s+1,$j,$s,$e];
						$get_gene->{$region_p->[3]} = 1;
						my $t_key = "$chr $j";
						$Verbose && $tol_cds_snp_num->{$t_key} && warn"Note: SNP $t_key appear at gene $tol_cds_snp_num->{$t_key} and $region_p->[3]\n";
						$tol_cds_snp_num->{$t_key} = $region_p->[3];
					}else{
						my @outp = ($chr,$j,@{$chr_snp_p->{$j}},$type,@{$region_p});
						$anno && (push @outp,&sanno_str($annoh,$type,$outp[-1]));
						$outprint .= join("\t",@outp)."\n";
						$ll++;
						if(!($ll % 30)){
							$stat += $ll;
							$ll = 0;
							print OTH $outprint;
							$outprint="";
						}
					}
				}
			}
			$len += $e - $s + 1;
		}
	}
	$ll && (print OTH $outprint);
	$stat + $ll;
}
#sub5.1
sub sanno_str{
	my ($annoh,$type,$id) = @_;
	if($annoh->{$type}){
		$anno_list ? ($annoh->{$type}->{$id} || "--") : 
		($annoh->{$type}->{$id} || $annoh->{CDS}->{$id} || '--');
	}else{
		"--";
	}
}
#sub6
#=============
sub homo_hete
#=============
{
	my ($ref_nt0,$str_nt0,$codons,$phase,$tmp_codon,$end_codon,$syn_num,$nosyn_num) = @_;
	my ($syn_mous,$nosyn_mous,$base,$h_type,$mut_type) = (0, 0);
	my ($ref_nt, $str_nt) = ($ref_nt0, $str_nt0);
	my $old_ref_nt = substr($codons,$phase,1);
	if ($old_ref_nt !~ /[ATCG]/){
		chage_abbrev($ref_nt);
		substr($codons,$phase,1) = $ref_nt;
	}elsif($old_ref_nt ne $ref_nt){
#        warn"jiong!!.......";
		$ref_nt = $old_ref_nt;
	}
	chage_abbrev($codons);
	my ($ref_codon,$str_codon) = ($codons, $codons);
	my $ref_aa = ($CODE{$ref_codon} || 'X');
	my $str_aa;
	if ($str_nt =~ /[ACGT]/) {
		($base,$h_type) = ($str_nt,'Homo');
		if($base eq $ref_nt){
			$h_type = 'Same';
			$mut_type = 'Same';
			$str_aa = ($CODE{$str_codon} || 'X');
		}else{
			substr($str_codon,$phase,1) = $base;
			$str_aa = ($CODE{$str_codon} || 'X');
			($str_aa ne $ref_aa) ? ($nosyn_mous = 1) : ($syn_mous = 1);
		}
	}elsif($str_nt =~ /[MRWSYKVHDBXN]/){ #[MRWSYK]
		$base = $Abbrev{$str_nt}[0];
		substr($str_codon,$phase,1) = $base;
		$str_aa = ($CODE{$str_codon} || 'X');
		my $hete_num = 0;
		if($base ne $ref_nt){
			$hete_num++;
			($str_aa ne $ref_aa) ? ($nosyn_mous += 0.5) : ($syn_mous += 0.5);
		}
		if($Abbrev{$str_nt}[1] ne $ref_nt){
			$hete_num++;
			my $base2 = $Abbrev{$str_nt}[1];
			my $str_codon2 = $ref_codon;
			substr($str_codon2,$phase,1) = $base2;
			my $str_aa2 = ($CODE{$str_codon2} || 'X');
			if($str_aa2 ne $ref_aa){
				if($nosyn_mous == 0){
					($base,$str_codon,$str_aa) = ($base2,$str_codon2,$str_aa2);
				}
				$nosyn_mous += 0.5;
			}else{
				$syn_mous += 0.5;
			}
		}
		$h_type = "Het$hete_num";
	}else{
		die "different_base unknown complex character, please check the snp data, $!";
	}
	$nosyn_mous ? ($$nosyn_num++) : ($$syn_num++);
	if($mut_type){
#we do not consider the situation of Same
	}elsif($nosyn_mous){
		if($tmp_codon == 1){
			$mut_type = 'Start_nonsyn';
		}elsif($tmp_codon == $end_codon){
			$mut_type = 'Stop_nonsyn';
		}else{
			$mut_type = $END_CODE{$str_codon} ? 'Premature_stop' : 'Nonsynonymous';
		}
	}else{
		if($tmp_codon == 1){
			$mut_type = 'Start_syn';
		}elsif($tmp_codon == $end_codon){
			$mut_type = 'Stop_syn';
		}else{
			$mut_type = 'Synonymous';
		}
	}
	($ref_codon,$str_codon,"$ref_nt0<->$str_nt0",$h_type,$mut_type,"$ref_codon<->$str_codon","$ref_aa<->$str_aa",$syn_mous,$nosyn_mous);
}
#sub6.1
#================
sub chage_abbrev{
#================
	my $str;
	($_[0] =~ /[MRWSYK]/) || return(0);
	foreach(split//,$_[0]){
		if(/[MRWSYK]/){
			my @str_array = @{$Abbrev{$_}};
			$str .= @str_array[int(rand($#str_array+1))];
		}else{
			$str .= $_;
		}
	}
	$_[0] = $str;
}

sub comma_add {
	my $nu = shift;
	my $arg = "%.${nu}f";
	foreach (@_) {
		$_ = sprintf($arg,$_);
		$_ = /(\d+)\.(\d+)/ ? comma($1) . '.' . $2 : comma($_);
	}
	return 1;
}
sub comma{
	my ($c,$rev) = @_;
	(length($c) > 3) || return($c);
	$rev || ($c = reverse $c);
	$c =~ s/(...)/$1,/g;
	$rev || ($c = reverse $c);
	$rev ? ($c =~ s/,$//) : ($c =~ s/^,//);
	$c;
}

#############################################
sub abbrev{
	my %Abbrev = (
		'A' => [ 'A' ],
		'C' => [ 'C' ],
		'G' => [ 'G' ],
		'T' => [ 'T' ],
		'M' => [ 'A', 'C' ],
		'R' => [ 'A', 'G' ],
		'W' => [ 'A', 'T' ],
		'S' => [ 'C', 'G' ],
		'Y' => [ 'C', 'T' ],
		'K' => [ 'G', 'T' ],
		'V' => [ 'A', 'C', 'G' ],
		'H' => [ 'A', 'C', 'T' ],
		'D' => [ 'A', 'G', 'T' ],
		'B' => [ 'C', 'G', 'T' ],
		'X' => [ 'A', 'C', 'G', 'T' ],
		'N' => [ 'A', 'C', 'G', 'T' ]
	);
	%Abbrev;
}
#############################################

#===================================================================================================================================
