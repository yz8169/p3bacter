#!/usr/bin/perl -w
use strict;
use FindBin qw($Bin);
use Cwd qw(abs_path);
use Getopt::Long;
my %opt = (vf=>'3G',sleept=>300,prefix=>"final",minLen=>50,p_value=>1.0);
GetOptions(\%opt,"outdir:s","shdir:s","reads:s","nucmer_opts:s","soap:s","lzopts:s","RLen:i","extemd:i","nocomp", "nncomp",
    "sdf:f","dcut:i","qual:i","p_value:f","cfg:s","sleept:i","prefix:s","verbose","qopts:s","nomove","minLen:i",
    "tmask:s","qmask:s","cnv","cover:f","rpdcut:f","extend:i","dupsnp","tar_gff:s","que_gff:s","code:i",
    "code_tab:s","tfix:s","qfix:s","vf:s","locate","phred:i","cpseq_gff");
my $Lib = "$Bin/../lib";
$opt{cfg} ||= "$Bin/Add.txt";
my ($cnv_p, $nucmer_p, $lastz_p, $soap_p, $soap_map, $snp_check, $indel_check, $cnv_check, $sv_check, $make_cns, $pair_review,#pair_review contain formatdb and blastall
    $super_worker, $sv_annotation, $bar_svg, $lendis_svg, $venn_svg, $cycle_svg, $svg_xxx, $snp_indel_dis, $report, $sv_split) = 
get_path($opt{cfg},[qw(cnv_p nucmer_p lastz_p soap_p soapmap snp_check indel_check cnv_check sv_check make_cns pair_review
    super_worker sv_annotation bar_svg lendis_svg venn_svg cycle_svg svg_xxx snp_indel_dis report sv_split)],$Bin,$Lib);#sub1
foreach(['lzopts',\$lastz_p],['nucmer_opts',\$nucmer_p],['soap',\$soap_p]){
    $opt{$_->[0]} && (${$_->[1]} .= " --$_->[0]=\"$opt{$_->[0]}\"");
}
foreach($super_worker,$soap_p,$cnv_p){
    $_ .= " --qopts=\"$opt{qopts}\"" if ($opt{qopts});
}
foreach(qw(RLen extend p_value minLen)){(defined $opt{$_}) && ($sv_check .= " --$_ $opt{$_}");}
foreach(qw(sdf dcut qual phred)){(defined $opt{$_}) && ($snp_check .= " --$_ $opt{$_}");}
foreach(qw(code_tab code)){(defined $opt{$_}) && ($sv_annotation .= " --$_ $opt{$_}");}
foreach(qw(tfix qfix)){(defined $opt{$_}) && ($lastz_p .= " --$_ $opt{$_}");}
(defined $opt{dcut}) && ($indel_check .= " --dcut $opt{dcut}");
(defined $opt{rpdcut}) && ($cnv_check .= " --rpdcut $opt{rpdcut}");
$lastz_p .= " --dup $opt{minLen}";
$opt{nomove} && ($lastz_p .= " --nomove");
$opt{cover} && ($cnv_p .= " --cover $opt{cover}");
$opt{dupsnp} && ($snp_check .= " --dupsnp");
$opt{tfix} ||= "Target";
$opt{qfix} ||= "Query";
#==============================================================================================================
our $TESTING = 0;
(@ARGV == 2) || die"Name: SV_finder.pl
Description:
    Pipeline to find SNP,InDel,SV(Deletion,Insertion,Inversion,Translation,ComplexInDel,Trans+Inver) with Lastz
    Whold genomics alignment in Complete map assembly leveal.
    The SV result can also examimed by reads mapping result in kruskal wallis test
Author:
  Liuwenbin, liuwenbin\@genomics.cn
  Lihang1, lihang1\@genomics.cn
  Qianzhou, zhouqian\@genomics.cn
Version: 1.0,  Date: 2012-09-03
Usage: perl SV_finder.pl <ref.fa> <query.fa> [-options]
    --outdir <dir>        output directory, defualt=./
    --prefix <str>        final result prefix, default=final
    --shdir <dir>         work shell directory, default=outdir/Shell
    --reads <file>        input reads.lst for SV result check
    --tar_gff <file>      input reference gff file for annotation
    --que_gff <file>      input query gff file for anotation
    --cpseq_gff           copy sequence and gff files of ref and query to outdir
    --tfix <str>          target prefix, default=Target
    --qfix <str>          query prefix, default=Query
    --nocomp              infile not be complete map
    --nncomp              infile not complete map, then not do analy change. default sort sequence
    --cnv                 find CNV and mask repeat with N
    --cover <flo>         coverage cutoff at a repeat family at CNV
    --nucmer_opts <str>   nucmer options, defualt='-c 65 -l 20'
#--lzopts <str>        lastz options, default='T=2 C=2 H=2000 Y=3400 L=6000 K=2200 --identity=75'
    --lzopts <str>        lastz options, default='--notransition --nochain --gapped --nogfextend --inner=2000 --ydrop=3400 --gappedthresh=6000 --identity=75 --ambiguous=iupac'
    --soap=<str>          soap option, default=' -l 32 -m 460 -x 540 -s 40 -v 2 -r 1'
    --nomove              not move indel ahead at duplication region
    --minLen <num>        minmual SV length for check, default=50
    --RLen <num>          reads length, default=90
    --extend <num>        extend region for check, default=300
    --sdf <flo>           second depth frequence cutoff for SNP check, default=0.45
    --dcut <num>          depth cutoff to support SNP or samll Indel, default=0(not filter)
    --qual <num>          quality cutoff for SNP check, default=15
    --phred <num>         The smallest ASCII value of the characters of quality value(Hiseq:64,Miseq:33), default=64
    --dupsnp              delete SNP at repeat region
    --p_value <flo>       kruskal_wallis test p_value cutoff, default=1.0(not filter)
    --code_tab <file>     set code table, default not set
    --code <num>          Code table No(default=11):
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

    --cfg <file>          script pathway confilg file, default=Bin/Add.txt
    --vf <str>            resource for qsub, default=3G
    --qopts <str>         others qsub options, default=''
    --locate              locate run, not qsub
    --verbose             output runnint information\n
Example:
    perl SV_finder.pl NC20120904.47.fa oursv_fine.fa -cnv -verbose -tar_gff tar.gff -que_gff que.gff
    perl SV_finder.pl NC20120904.47.fa oursv_fine.fa -reads reads.lst --verbose\n\n";
#   --tmask <str>         input repeat masked target fasta file
#   --qmask <str>         input repeat masked query fasta file
#==============================================================================================================
foreach(@ARGV){
    (-s $_) || die"error: can't find able file $_, $!";
    (-w $_) && `dos2unix $_ 2>/dev/null`;
    $_ = abs_path($_);
}
foreach(qw(reads tar_gff que_gff)){
    $opt{$_} &&= abs_path($opt{$_});
}
#($opt{reads} && -s $opt{reads}) || ($sv_check = $sv_split);
$sv_check = $sv_split;
my ($ref,$query)=@ARGV;
my $outdir = ($opt{outdir} || '.');
my $shdir = ($opt{shdir} || "$outdir/Shell");
foreach($outdir,$shdir){
    (-d $_) || mkdir($_);
    $_ = abs_path($_);
}
my ($ref_uc, $ref_new_seq) = &check_uc ($ref);
my ($query_uc, $query_new_seq) = &check_uc ($query);
if ($opt{cpseq_gff}) {
	if ($ref_uc) {
		if (-e "$outdir/$opt{tfix}.seq") { `rm $outdir/$opt{tfix}.seq`; }
		`cp -f $ref $outdir/$opt{tfix}.seq`;
	}
	else {
		open OUT, ">$outdir/$opt{tfix}.seq" || die;
		print OUT $ref_new_seq;
		close OUT;
	}
	if ($query_uc) {
		if (-e "$outdir/$opt{qfix}.seq") { `rm $outdir/$opt{qfix}.seq`; }
		`cp -f $query $outdir/$opt{qfix}.seq`;
	}
	else {
		open OUT, ">$outdir/$opt{qfix}.seq" || die;
		print OUT $query_new_seq;
		close OUT;
	}
	($ref,$query) = ("$outdir/$opt{tfix}.seq", "$outdir/$opt{qfix}.seq");
	if ($opt{tar_gff}) {
		(-e "$outdir/$opt{tfix}.gff") && `rm $outdir/$opt{tfix}.gff`;
		`cp -f $opt{tar_gff} $outdir/$opt{tfix}.gff`;
		 $opt{tar_gff} = "$outdir/$opt{tfix}.gff";
	}
	if ($opt{que_gff}) {
		(-e "$outdir/$opt{qfix}.gff") && `rm $outdir/$opt{qfix}.gff`;
		`cp -f $opt{que_gff} $outdir/$opt{qfix}.gff`;
		 $opt{que_gff} = "$outdir/$opt{qfix}.gff";
	}
}
else {
	if (!$ref_uc) {
		open OUT, ">$outdir/$opt{tfix}.seq" || die;
		print OUT $ref_new_seq;
		close OUT;
		$ref = "$outdir/$opt{tfix}.seq";
	}
	if (!$query_uc) {
		open OUT, ">$outdir/$opt{qfix}.seq" || die;
		print OUT $query_new_seq;
		close OUT;
		$query = "$outdir/$opt{qfix}.seq";
	}
}

my $splits = '\n\n';
#$super_worker .= " --resource $opt{vf} -splits=\"$splits\" -sleept $opt{sleept}";
$super_worker .= " --multi --resource $opt{vf} -splits=\"$splits\" -sleept $opt{sleept}";
#==============================================================================================================
#step0 CNV
if($opt{cnv}){
    open SH,">$shdir/step0_cnv.sh" || die$!;
    print SH "cd $outdir; $cnv_p $ref $query -outdir 00.CNV -shdir $shdir/CNV -mask -locate\n";
    close SH;
    if(!(-s "$outdir/00.CNV/ref_query.cnv")){
		if ($TESTING) {print STDERR "start CNV? check file: $shdir/runCNV.log\n"; while (!-e "$shdir/runCNV.log") { sleep 30;}} #for testing
        $opt{verbose} && (print STDERR localtime() . " --> star running CNV\n");
        $opt{locate} ? system"cd $shdir ; sh step0_cnv.sh" : system"cd $shdir; $super_worker step0_cnv.sh -prefix cnv";
        $opt{verbose} && (print STDERR localtime() . " --> finish running CNV\n");
		
    }else{
        $opt{verbose} && (print STDERR localtime() . " --> CNV result has been found at 00.CNV/ref_query.cnv\n");
    }
    $opt{tmask} ||= "$outdir/00.CNV/ref.mask.fa";
    $opt{qmask} ||= "$outdir/00.CNV/query.mask.fa";
}
#==============================================================================================================
#step1 alignment
if(!$opt{nocomp}){
	chomp(my $ref_seqn = `grep -c "^>" $ref`);
	chomp(my $que_seqn = `grep -c "^>" $query`);
	($ref_seqn > 1 || $que_seqn > 1) && ($opt{nocomp} = 1);
#	$nucmer_p .= " -nocomp";
}
$nucmer_p .= ($opt{nocomp}) ? (($opt{nncomp}) ?  " --nocomp --nncomp" : " --nocomp") : ""; # add 20141215
#$opt{nocomp} && ($nucmer_p .= " --nocomp"); # to sort
#$opt{nocomp} && ($nucmer_p .= " --nocomp --nncomp"); # mask sorting.
my $real_ref = $ref;
my $real_query = $opt{nocomp} ? $query : "$outdir/01.Nucmer/all.new_query.fa";
if($opt{tmask} && -s $opt{tmask} && $opt{qmask} && -s $opt{qmask}){
    $opt{tmask} = abs_path($opt{tmask});
    $opt{qmask} = abs_path($opt{qmask});
    $nucmer_p .= " -maskfa $opt{qmask}";
    $lastz_p .= " --tunmask $ref --qunmask $real_query";
    $real_ref = $opt{tmask};
    $real_query = $opt{nocomp} ? $opt{qmask} : "01.Nucmer/all.masked_query.fa";
}
if ($TESTING) {print STDERR "Writing shell file: step1_align.sh\n"; } #for testing
open SH,">$shdir/step1_align.sh" || die$!;
print SH "cd $outdir\n$nucmer_p $ref $query -outdir 01.Nucmer 2> $shdir/numcer.log\n",
"$lastz_p $real_ref $real_query --change_log 01.Nucmer/log -step 1235 -outdir 02.Lastz -best 2 -shdir $shdir/lastz -locate",
($opt{nocomp} ? (($opt{nncomp}) ? " --tsize $outdir/01.Nucmer/target.size --qsize $outdir/01.Nucmer/query.size" : " --tsize $outdir/01.Nucmer/target.size.sort --qsize $outdir/01.Nucmer/query.size.sort") : ""), "\n", #add at 20141224
($opt{nocomp} ? "cd 02.Lastz; ln -s ../01.Nucmer/target.size; ln -s ../01.Nucmer/query.size" : ""), "\n\n"; #add at 20141224
if($opt{reads} && -s $opt{reads}){
    (-d "$outdir/03.Soap") || mkdir"$outdir/03.Soap";
    $soap_p .= " -subdir 01.Soapalign,02.Coverage";
    print SH "$soap_p $ref $opt{reads} -outdir $outdir/03.Soap/ref_soap -shdir $shdir/ref_soap -locate\n\n",
    "$soap_p $query $opt{reads} -outdir $outdir/03.Soap/ass_soap -shdir $shdir/ass_soap -locate\n\n";
}
close SH;
if ($TESTING) {print STDERR "start alignment? check file: $shdir/runAlignment.log\n"; while (!-e "$shdir/runAlignment.log") { sleep 30;}} #for testing
$opt{verbose} && (print STDERR localtime() . " --> star running SV alignment\n");
$opt{locate} ? system "cd $shdir; sh step1_align.sh >& step1_align.sh.log" : system "cd $shdir; $super_worker step1_align.sh -prefix svalign";
$opt{verbose} && (print STDERR localtime() . " --> finish running SV alignment\n");
my @result = (["04.Test/snpcheck.snp",'snp',1],["04.Test/svcheck.confident.SV",'sv',1],
    ["02.Lastz/all.identity.list",'identity.list',0],["02.Lastz/all.cover",'cover',0],
    ["02.Lastz/all.sv.axt",'axt',0],["02.Lastz/all.sv.alg",'alg',0],
    ["02.Lastz/all.tar.miss",'tar.miss',0],["02.Lastz/all.que.miss",'que.miss',0]);
if($opt{reads} && -s $opt{reads}){ ### Has reads for check
#==============================================================================================================
#stpe2 soapmap
	if ($TESTING) {print STDERR "Writing shell file: step2_soapmap.sh\n"; } #for testing
    open SH,">$shdir/step2_soapmap.sh" || die$!;
    print SH "cd $outdir/03.Soap/ref_soap/; $soap_map soap.lst -outdir 02.Soapmap -size $outdir/02.Lastz/target.size ",
      "-plot $outdir/01.Nucmer/all.snp,$outdir/02.Lastz/all.snp\n\n";
    print SH "cd $outdir/03.Soap/ass_soap/; $soap_map soap.lst -outdir 02.Soapmap -size $outdir/02.Lastz/query.size ",
      "-plot $outdir/01.Nucmer/all.snp,$outdir/02.Lastz/all.snp -rank 5,6\n\n";
    close SH;
	if ($TESTING) {print STDERR "start soapmap? check file: $shdir/runSoapmap.log\n"; while (!-e "$shdir/runSoapmap.log") { sleep 30;}} #for testing
    $opt{verbose} && (print STDERR localtime() . " --> star running soapmap\n");
    $opt{locate} ? system "cd $shdir; sh step2_soapmap.sh >& S2.log" : system "cd $shdir; $super_worker step2_soapmap.sh -prefix soapmap -sleept 100";
    $opt{verbose} && (print STDERR localtime() . " --> finish running soapmap\n");
#==============================================================================================================
#step3 Test
    (-d "$outdir/04.Test") || mkdir"$outdir/04.Test";
	if ($TESTING) {print STDERR "Writing shell file: step3_svcheck.sh\n"; } #for testing
    open SH,">$shdir/step3_svcheck.sh" || die$!;
    print SH "cd $outdir/04.Test\n$sv_check $outdir/02.Lastz/all.{block.sv,indel} -ref_seq $ref -ref_map ",
    "$outdir/03.Soap/ref_soap/02.Soapmap -ass_seq $query -ass_map $outdir/03.Soap/ass_soap/02.Soapmap\n\n",
    "$snp_check $outdir/03.Soap/ref_soap/02.Soapmap/soap.SNP.depth $outdir/{02.Lastz,01.Nucmer}/all.snp ",
    "-ass_depth $outdir/03.Soap/ass_soap/02.Soapmap/soap.SNP.depth > snpcheck.snp 2> base.review.log\n\n",
    "$make_cns $ref -snp snpcheck.snp -rank1 0,1,3,4 -indel svcheck.small.indel --rank2 0,1,2,7,11 > ref.cns 2>ref_base.log\n",
    "$soap_p ref.cns $opt{reads} -outdir $outdir/03.Soap/cns_soap -shdir $shdir/cns_soap -cover -vf $opt{vf}\n",
    "$indel_check svcheck.small.indel ref_base.log $outdir/03.Soap/cns_soap/02.Coverage/soap.coverage.depthsingle > indelcheck.small.indel\n\n";
    push @result ,["04.Test/indelcheck.small.indel",'indel',1];
    if($opt{cnv}){
        print SH "$cnv_check $outdir/00.CNV/ref_query.cnv $outdir/03.Soap/{ref_soap,ass_soap}/02.Soapmap/soap.RP.depth > cnvcheck.cnv\n";
        push @result,["04.Test/cnvcheck.cnv",'cnv',1];
    }
    close SH;
}else{  ### Do not has reads for check
    (-d "$outdir/04.Test") || mkdir"$outdir/04.Test";
	if ($TESTING) {print STDERR "Writing shell file: step3_svcheck.sh\n"; } #for testing
    open SH,">$shdir/step3_svcheck.sh" || die$!;
    print SH "cd $outdir/04.Test\n$sv_check --nomap $outdir/02.Lastz/all.{block.sv,indel} -ref_seq $ref -ass_seq $query\n",
    "$snp_check 0 $outdir/{02.Lastz,01.Nucmer}/all.snp > snpcheck.snp\n";
    push @result,["04.Test/svcheck.small.indel",'indel',1];
    if($opt{cnv}){
        print SH "$cnv_check $outdir/00.CNV/ref_query.cnv --nomap  > cnvcheck.cnv\n";
        push @result,["04.Test/cnvcheck.cnv",'cnv',1];
    }
}
if ($TESTING) {print STDERR "start checking? check file: $shdir/runCheck.log\n"; while (!-e "$shdir/runCheck.log") { sleep 30;}} #for testing
$opt{verbose} && (print STDERR localtime() . " --> star testing SV result\n");
#system"cd $shdir; sh step3_svcheck.sh >& S3.log";
system"cd $shdir; sh step3_svcheck.sh > step3_svcheck.sh.o 2> step3_svcheck.sh.e"; #modify at 20141224
$opt{verbose} && (print STDERR localtime() . " --> finish testing SV result\n");
#==============================================================================================================
#final result
my $res_dir = "$outdir/Result";
(-d $res_dir) || mkdir($res_dir);
(-d "$res_dir/Report") || system"cp -r $report $res_dir/Report";
my @d = ("01.Synteny","02.Variation","03.Annotation");
foreach(@d[0,1]){(-s "$res_dir/$_") || mkdir"$res_dir/$_";}
open SH,">$shdir/cp.sh" || die$!;
open DR,">$shdir/step5_draw.sh" || die"$!";
print DR "#!/usr/bin/bash\ncd $res_dir/$d[1]\n";
foreach(@result){
    print SH "if [ -f $outdir/$_->[0] ] ; then cp -f $outdir/$_->[0] $res_dir/$d[$_->[2]]/$opt{prefix}.$_->[1] ; fi\n";
}
print SH "cp $outdir/02.Lastz/*.{svg,png} $res_dir/$d[0]\n";
$cycle_svg .= " ../$d[0]/$opt{prefix}.alg $opt{prefix}.sv";
if(($opt{tar_gff} && -s $opt{tar_gff}) || ($opt{que_gff} && -s $opt{que_gff})){
    (-d "$res_dir/$d[2]") || mkdir"$res_dir/$d[2]";
    foreach(qw(tar_gff que_gff)){($opt{$_} && -s $opt{$_}) && ($sv_annotation .= " --$_ $opt{$_}");}
    $opt{cnv} && ($sv_annotation .= " --cnv $opt{prefix}.cnv");
    open ANN,">$shdir/step4_annotation.sh" || die$!;
    print ANN "cd $res_dir/$d[1]\n",
          # "$sv_annotation ../$d[0]/$opt{prefix}.axt --prefix $opt{prefix} -sv $opt{prefix}.sv ",
          "$sv_annotation ../$d[0]/$opt{prefix}.axt --prefix $opt{prefix}  ",
          "-tsize $outdir/02.Lastz/target.size -qsize $outdir/02.Lastz/query.size --outdir ../$d[2] --pairlist ",
          "--snp $outdir/02.Lastz/all.snp --indel $outdir/02.Lastz/all.indel\n",
          "$pair_review $res_dir/$d[2]/$opt{prefix}.genepair.list $ref $query $res_dir/$d[2]/$opt{prefix}.genepair.list\n";
#          "--snp $opt{prefix}.snp --indel $opt{prefix}.indel\n";
    close ANN;
    print SH "sh $shdir/step4_annotation.sh\n";
    $cycle_svg .= " $res_dir/$d[2]/$opt{prefix}.genepair.list.review";
    (-d "$res_dir/$d[2]/stat") || mkdir"$res_dir/$d[2]/stat";
    print DR "cd $res_dir/$d[2]\n",
    "$bar_svg $opt{prefix}.snp-indel.genemutate.stat -style 2 -tranxy -symbol SNP,InDel -row=-1 --nohead 2 -ranks 0,1,3 -colors ",
    "crimson,royalblue -grid -x_title \"SNP Number\" -y_title2 \"InDel Number\" -vice > $opt{prefix}.snp-indel.genemutate.svg\n",
    "$svg_xxx $opt{prefix}.snp-indel.genemutate.svg\n",
    "$bar_svg $opt{prefix}.frame_mutate.stat --rotate=\"-45\" -symbol \"Identity=100%,Identity<100%\" -row=\"-1\" --nohead 2 -ranks 0,1,3 ",
    "-colors crimson,royalblue -grid -y_title \"Gene Pairwise Numbers\" -size_yt 20 > $opt{prefix}.frame_mutate.svg\n",
    "$svg_xxx $opt{prefix}.frame_mutate.svg\n",
    "$venn_svg $opt{prefix}.region.cover.stat $opt{tfix} $opt{qfix} > $opt{prefix}.region.cover.svg\n",
    "$svg_xxx $opt{prefix}.region.cover.svg\n",
    "$lendis_svg $opt{prefix}.genepair.list > $opt{prefix}.genepair.terminal.svg\n",
    "$svg_xxx $opt{prefix}.genepair.terminal.svg\n",
    "mv *.{stat,png,svg} stat/\ncd ../$d[1]\n";
}
print DR "$cycle_svg -tsize $outdir/02.Lastz/target.size -qsize $outdir/02.Lastz/query.size -avg > SV_cycle.svg\n",
      "$svg_xxx SV_cycle.svg\n",
      "$snp_indel_dis $opt{prefix}.snp $opt{prefix}.indel $outdir/02.Lastz/target.size -vice 1 ",
      "-prefix final.snp_indel.dis -symbol \"SNP,InDel\"\n";
close DR;
close SH;
if ($TESTING) {print STDERR "start cp && draw? check file: $shdir/runCpDraw.log\n"; while (!-e "$shdir/runCpDraw.log") { sleep 30;}} #for testing
system"cd $shdir; sh cp.sh ; sh step5_draw.sh ; chmod 755 -R $res_dir";
$opt{verbose} && (print STDERR localtime() . " --> SV_finder.pl Done!\nResult at $res_dir\n");

###############################################################################################################
## SUB
#sub1
#===========
sub get_path{
    my ($add,$path,$Bin,$lib) = @_;
    (-s $add) || die"error can't find able script pathway cfgfile $add, $!\n";
    my $awk_get = 'awk \'(!/^#/ && $2=="=")\'';
    my %pathway = split/\s+=\s+|\n/,`$awk_get $add`;
    my @out_path;
    $pathway{BIN} && ($Bin = $pathway{BIN});
    $pathway{LIB} && ($lib = $pathway{LIB});
    foreach(@$path){
        $pathway{$_} || die"error: can't find $_ at $add, $!";
        my $script = $pathway{$_};
        $Bin && ($script =~ s#\bBin#$Bin#);
        $lib && ($script =~ s#\bLib#$lib#);
        foreach(split/\s+/,$script){
            /^\// && !(-s $_) && die"error: can't find script: $_, $!\n";
        }
        push @out_path,$script;
    }
    @out_path;
}

sub check_uc {
	my $in_seq = $_[0];
	my ($flag, $new_seq) = (1, "");
	open IN, $in_seq || die;
	while (<IN>) {
		chomp;
		next if (/^\s*$/);
		if (! /^\>/) {
			if (/[a..z]/) {
				$flag = 0;
				$_ = uc $_;
			}
		}
		$new_seq .= "$_\n";
	}
	close IN;
	return ($flag, $new_seq);
}
#==============================================================================================================
