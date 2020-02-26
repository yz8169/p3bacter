#!/usr/bin/perl -w
use strict;
use FindBin qw($Bin);
use Getopt::Long;
my %opts = (outdir=>'.',prefix=>'out',gene=>'CDS',code=>11);
GetOptions(\%opts,"outdir:s","extend:i","prefix:s","gene:s","cnv:s","sv:s","tsize:s","qsize:s",
    "code:i","code_tab:s","tar_gff:s","que_gff:s","pairlist","snp:s","indel:s","tagid:s");
@ARGV || die"Name: sv_annotation.pl(axt2gene.pl)
Description: script to find allelomorph according to  axt result and gff infile
Version: 1.0,  Date: 2012-10-22
Connect: liuwenbin\@genomics.org.cn
Usage: perl sv_annotation.pl <in.axt> <tar.gff> [que.gff] [-optins]
    --outdir <dir>      output directory, default=./
    --prefix <str>      outfile prefix, defualt=out
    --gene <str>        gene type, default=CDS
    --extend <num>      pair gene edge extend distance cutoff, default not set
    --cnv <file>        input cnv result list, default not set
    --sv <file>         input SV result list, default not set
    --tsize <file>      input targe genomics size file for some stat, default not set
    --qsize <file>      input query genomics size file for some stat, default not set
    --pairlist          output gene pairwise list result, default not out
	--tagid				set tagid key word at 9th rank of gff file, e.g: ID,Parent,protein, default not set
    --code_tab <file>   set code table, default=Bin/Code_table
    --code <num>        Code table No(default=11):
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
      12 The Alternative Yeast Nuclear Code\n 
Note: If you not input que.gff, it's likely that the process will do homology genes predicts.\n\n";
#   --snp <file>        input snp file to corr the snp pos at CDS
#   --indel <file>      input indel file to corr the indel pos at CDS
#================================================================================================================================
$opts{code_tab} ||= "$Bin/Code_table";
(-s $opts{code_tab}) || die"error at --code_tab $opts{code_tab}, not an able file, $!";
foreach(@ARGV){
    (-s $_) || die"can't find able infile: $_, $!";
}
my ($inaxt, $tar_gff, $que_gff) = @ARGV;
($opts{tar_gff} && -s $opts{tar_gff}) && ($tar_gff = $opts{tar_gff});
($opts{que_gff} && -s $opts{que_gff}) && ($que_gff = $opts{que_gff});
(($tar_gff && -s $tar_gff) || ($que_gff && -s $que_gff)) || die"error: either --tar_gff or --que_gff must be set, $!";
my (%tar_cds, %que_cds, %tar_cnv_cds, %que_cnv_cds);
my $tar_gene_num = get_gff(\%tar_cds,$tar_gff,$opts{gene},$opts{tagid});#sub1
my $que_gene_num = get_gff(\%que_cds,$que_gff,$opts{gene},$opts{tagid});#sub1
(-d $opts{outdir}) || mkdir($opts{outdir});
## outfile #========================================
my $prefix = "$opts{outdir}/$opts{prefix}";
my $cnv_gene_list = "$prefix.cnv.gene.list";
my $cnv_gene_align = "$prefix.cnv.gene.align";
my $cds_axt = "$prefix.cds.axt";
my $genepair_stat = "$prefix.frame_mutate.stat";
my $indel_gene_list = "$prefix.indel.gene.list";
my $align_gene_stat = "$prefix.align.gene.stat";
my $all_merge_stat = "$prefix.region.cover.stat";
my $snp_anno_list = "$prefix.cds.snp.anno";
my $indel_anno_list = "$prefix.cds.indel.anno";
my $snp_intergenic_anno = "$prefix.intergenic.snp.anno";
my $indel_intergenic_anno = "$prefix.intergenic.indel.anno";
my $snp_indel_stat_list = "$prefix.snp-indel.genemutate.stat";
my $add_tar_gff = "$prefix.taradd.gff";
my $add_que_gff = "$prefix.queadd.gff";
my $pair_list = "$prefix.genepair.list";
#===================================================
my (@cnv_pos, @alg_pos, @indel_pos, @add_gene);
my $out_alg_cnv;
my (@stat,@alg_gene_stat);
my @alg_gene_num = (0) x 6;
my @indel_gene_num = (0) x 12;
my @cnv_gene_num = cnv_gene_annotation($opts{cnv},\%tar_cds,\%que_cds,\%tar_cnv_cds,\%que_cnv_cds,$cnv_gene_list,\@cnv_pos);#sub5
my @sign = qw(Start_syn Stop_syn Synonymous Start_nonsyn Stop_nonsyn Nonsynonymou Premature_stop);
my (%Code, %Star,%End,%snp_stat,%indel_stat);
my (@snpcorr, @indelcorr);
my (%cdsm_snp, %cdsm_indel);
my ($snp_out,$indel_out);
get_code($opts{code_tab},$opts{code},\%Code,\%Star,\%End);#sub0
get_snpindel($opts{snp},\@snpcorr,[0,1,5,6]);#sub13
get_snpindel($opts{indel},\@indelcorr,[0,1,3,4]);#sub13
$opts{pairlist} && (open PL,">$pair_list");
open OUT,">$cds_axt" || die$!;
open AXT,$inaxt || die$!;
$/="\n\n";
while(<AXT>){
    chomp;
    my ($head,$tar_seq,$que_seq) = split /\n/;
    my @head = split /\s+/,$head;
    my (@tar_gene, @que_gene, @pair_gene);
    $alg_gene_num[0] += $head[3]-$head[2]+1;
    push @{${$alg_pos[0]}{$head[1]}},[@head[2,3]];
    if($head[5]=~/,/){
        my ($s1, $e1) = split/,/,$head[5];
        my ($s2, $e2) = split/,/,$head[6];
        push @{${$alg_pos[1]}{$head[4]}},([$s2,$e2],[$s1,$e1]);
        $alg_gene_num[2] += $e1-$s1+$e2+1;
    }else{
        $alg_gene_num[2] += $head[6]-$head[5]+1;
        push @{${$alg_pos[1]}{$head[4]}},[@head[5,6]];
    }
    get_gene(\%tar_cds,\@tar_gene,@head[1,2,3]);#sub2
    get_gene(\%que_cds,\@que_gene,@head[4,5,6]);#sub2
    get_pair_wise(\@tar_gene,\@que_gene,$tar_seq,$que_seq,\@pair_gene,@head[2,5,6,7,9],$opts{extend},\@alg_gene_stat);#sub3
    out_pair_wise(\@pair_gene,\@head,$tar_seq,$que_seq,*OUT,$opts{pairlist}?*PL:0,\%tar_cnv_cds,\%que_cnv_cds,
    \$out_alg_cnv,\@stat,\@alg_gene_num,\@add_gene,\%Code,\%Star,\%End,\@sign,\%snp_stat,\%indel_stat,\$snp_out,
    \$indel_out,\@snpcorr,\@indelcorr,\%cdsm_snp,\%cdsm_indel);#sub4
}
$/="\n";
close AXT;
close OUT;
if($opts{pairlist}){
    out_resgene(\%tar_cds,*PL);   
    out_resgene(\%que_cds,*PL,1);
    close PL;
}
if($snp_out){
    open OUT,">$snp_anno_list" || die$!;
    print OUT '#',join("\t",qw(Tar_Gid Que_Gid Tar_Gregion Que_Gregion Tar_Glen Que_Glen Frame PW_ident
                CDS_ident Annotation)),"\n#",
          join("\t",qw(Tar_ID Tar_pos Que_ID Que_pos PW_pos Tar_Gpos Que_Gpos Tar_base Que_base Phase Codon PW_codon Mutate_Type Harm)),"\n\n";
    print OUT $snp_out;
    close OUT;
    $snp_out = "";
}
if($indel_out){
    open OUT,">$indel_anno_list" || die$!;
    print OUT "#",join("\t",qw(Tar_Gid Que_Gid Tar_Gregion Que_Gregion Tar_Glen Que_Glen Frame PW_ident
                CDS_ident Annotation)),"\n#",
          join("\t",qw(Tar_ID Tar_pos Que_ID Que_pos PW_pos Tar_Gpos Que_Gpos InDel_Type InDel_Len Frame_move Frame_phase Codon_phase
                Phase Codon PW_codon Mutate_Type Harm InDel_Seq)),"\n\n";
    print OUT $indel_out;
    close OUT;
    $indel_out = "";
}
unshift @sign,qw(Unshift Outside_Frame Unknow_Codon Same_Codon Damaged_Start_Codon Damaged_Stop_Codon);
out_snp_indel_stat(\%snp_stat,\%indel_stat,\@sign,$snp_indel_stat_list);#sub10
out_stat(\@stat,$genepair_stat);#sub7
out_align_gene_stat(\@alg_gene_stat,$tar_gene_num,$que_gene_num,$align_gene_stat);#sub8
if($out_alg_cnv){
    open OUT,">$cnv_gene_align";
    print OUT $out_alg_cnv;
    close OUT;
    $out_alg_cnv = "";
}
out_indel_gene($opts{sv},\%tar_cds,\%que_cds,$indel_gene_list,\%tar_cnv_cds,\%que_cnv_cds,\@indel_gene_num,\@indel_pos);#sub6
out_merge_stat(\@cnv_gene_num,\@alg_gene_num,\@indel_gene_num,\@cnv_pos,\@alg_pos,\@indel_pos,$tar_gene_num,$que_gene_num,
        $opts{tsize},$opts{qsize},$all_merge_stat);#sub9
out_add_gene($add_gene[0],$add_tar_gff);#sub11
out_add_gene($add_gene[1],$add_que_gff);#sub11
if(($opts{snp} && -s $opts{snp}) || ($opts{indel} && -s $opts{indel})){
    %tar_cds = ();
    %que_cds = ();
    get_gff(\%tar_cds,$tar_gff,$opts{gene},$opts{tagid},$add_tar_gff,1);#sub1
    get_gff(\%que_cds,$que_gff,$opts{gene},$opts{tagid},$add_que_gff,1);#sub1
    out_intergenic_snpindel($opts{snp},[0,1],[5,6],\%cdsm_snp,\%tar_cds,\%que_cds,$snp_intergenic_anno);#sub14
    out_intergenic_snpindel($opts{indel},[0..2],[3..5],\%cdsm_indel,\%tar_cds,\%que_cds,$indel_intergenic_anno,-1);#sub14
}
#==========================================================================================================================
#sub14
#===========================
sub out_intergenic_snpindel
#===========================
{
    my ($inf,$trank,$qrank,$cdsm,$tar_cds,$que_cds,$outf,$splice) = @_;
    ($inf && -s $inf) || return(0);
    open SI,$inf || die$!;
    open OUT,">$outf" || die$!;
    while(<SI>){
        chomp;
        my @l = split;
        my @t = @l[@{$trank}];
        my @q = @l[@{$qrank}];
        $cdsm->{$t[0]} && $cdsm->{$t[0]}->{$t[1]} && next;
        my $tanno = intergenic(\@t,$tar_cds);#sub14.1
        my $qanno = intergenic(\@q,$que_cds);#sub14.1
        ($tanno=~/^-\d+\s+/ && $qanno=~/^-\d+\s+/) && next;
        if($splice){
            splice(@l,$splice,0,$tanno,$qanno);
            print OUT join("\t",@l),"\n";
        }else{
            print OUT join("\t",$_,$tanno,$qanno),"\n";
        }
    }
    close SI;
    close OUT;
    (-s $outf) || `rm $outf`;
}
#sub14.1
#==============
sub intergenic
#==============
{
    my ($pos,$cds) = @_;
    $cds->{$pos->[0]} || return(join("\t",qw(- - - - - -)));
    $pos->[2] ||= $pos->[1];
    my @out;
    push @out,&find_nearly_gene($pos->[1],$cds->{$pos->[0]});#sub14.1.1
    push @out,&find_nearly_gene($pos->[2],$cds->{$pos->[0]},1);
#    (($out[0] ne '-' && $out[0]<0) || ($out[2] ne '-' && $out[2]<0)) ? 0 : join("\t",@out);
    join("\t",@out);
}
#sub14.1.1
#====================
sub find_nearly_gene
#====================
{
    my ($p,$array,$rev) = @_;
    if($rev){
        ($p > $array->[-1]->[3]) && return(qw(- - -));
        foreach my $i(1..$#$array){
            $i = $#$array-$i;
            ($p > $array->[$i]->[3]) && return($array->[$i+1]->[2]-$p,@{$array->[$i+1]}[0,4]);
        }
        return($array->[0]->[2]-$p,@{$array->[0]}[0,4]);
    }else{
        ($p < $array->[0]->[2]) && return(qw(- - -));
        foreach my $i(1..$#$array){
            ($p < $array->[$i]->[2]) && return($p-$array->[$i-1]->[3],@{$array->[$i-1]}[0,4]);
        }
        return($p-$array->[-1]->[3],@{$array->[-1]}[0,4]);
    }
}
#sub13
#================
sub get_snpindel
#================
{
    my ($inf,$hash,$sel) = @_;
    ($inf && -s $inf) || return(0);
    open INF,$inf || die$!;
    while(<INF>){
        my @l = (split)[@$sel];
        push @{${$hash->[0]}{$l[0]}},$l[1];
        push @{${$hash->[1]}{$l[2]}},$l[3];
    }
    close INF;
    foreach (values %{$hash->[0]}){@{$_} = sort {$a <=> $b} @{$_};}
    foreach (values %{$hash->[1]}){@{$_} = sort {$a <=> $b} @{$_};}
}
#sub12
#===============
sub out_resgene
#===============
{
    my ($tar_cds,$handel,$is_que) = @_;
    ($tar_cds && %$tar_cds) || return(0);
    foreach my $id(keys %$tar_cds){
        foreach my $p(@{$tar_cds->{$id}}){
            my ($tlen,$tar_name) = gene_name($id,$p);#sub4.2
            my @out = ($p->[0],'-',$tar_name,'-',$tlen,0);
            $is_que && (@out = @out[1,0,3,2,5,4]);
            print $handel join("\t",@out),"\n";
        }
    }
}
#sub0
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
#sub1
#===========
sub get_gff
#===========
{
    my ($CDS,$gff,$gene_type,$tagid,$add_gff,$mask) = @_;
    if(!($gff && -s $gff)){
       ($add_gff && -s $add_gff) ? ($gff=$add_gff, $add_gff=0) : return(0);
    }
    $gene_type ||= "CDS";
    open GFF, "<$gff" || die "$!";
    my ($scaff_name, $gene_name, $stand);
    my $gene_num = 0;
	my @IDsigns = qw(protein_id gene_id locus_tag Name Parent ID id);
	$tagid && (unshift @IDsigns,$tagid);
    while(<GFF>){
        chomp;
        next if ($_ =~ /^#/);
        my @arrayTmp = split /\t/, $_;
        next unless($arrayTmp[2] =~ /$gene_type/i);
		my $locus_tag;
		for my $s(@IDsigns){
			$locus_tag && last;
			($arrayTmp[-1] =~ /$s[\s=]+(.+?)\s*;/i) || next;
			$locus_tag = $1;
		}
		if(!$locus_tag){
			$locus_tag = $arrayTmp[-1];
#			print STDERR "error: can't find gene id at gff line: $_, $!";
		}
		$locus_tag =~ s/^\s+//;
		$locus_tag =~ s/\s+$//;
		$locus_tag =~ s/^"|"$//g;
#        ($gene_type=~/\|/) && ($locus_tag .= ".$arrayTmp[2]");#add gene mask after GeneID while mutil gene type
        if(!$gene_name || $scaff_name ne $arrayTmp[0] || $gene_name ne $locus_tag || $arrayTmp[6] ne $stand){
            if($mask){
                push @{$CDS->{$arrayTmp[0]}},
                     [$locus_tag,$arrayTmp[6],@arrayTmp[3,4],"$arrayTmp[0]:$arrayTmp[3]-$arrayTmp[4]"];
            }else{
                push @{$CDS->{$arrayTmp[0]}},[$locus_tag,$arrayTmp[6],[@arrayTmp[3,4]]];
            }
            $gene_num++;
        }else{
            if($mask){
				($CDS->{$arrayTmp[0]}->[-1]->[2] < $arrayTmp[3]) && ($CDS->{$arrayTmp[0]}->[-1]->[2] = $arrayTmp[3]);
                ($CDS->{$arrayTmp[0]}->[-1]->[3] < $arrayTmp[4]) && ($CDS->{$arrayTmp[0]}->[-1]->[3] = $arrayTmp[4]);
                $CDS->{$arrayTmp[0]}->[-1]->[-1] .= ",$arrayTmp[3]-$arrayTmp[4]";
            }else{
                push @{$CDS->{$arrayTmp[0]}->[-1]},[@arrayTmp[3,4]];
            }
        }
        ($scaff_name, $gene_name, $stand) = ($arrayTmp[0],$locus_tag,$arrayTmp[6]);
        if(eof GFF && $add_gff && -s $add_gff){
            close GFF;
            open GFF,"<$add_gff" || die$!;
            $add_gff = 0;
        }
    }
    close GFF;
    %{$CDS} || die"error: can't get CDS from gff file: $gff";
    foreach my $v(values %{$CDS}){
        if($mask){
            @{$v} = sort {$a->[2] <=> $b->[2]} @{$v};
            foreach(@{$v}){$_->[-1] .= ":$_->[1]";}
        }else{
            @{$v} = sort {$a->[2]->[0]<=>$b->[2]->[0]} @{$v};
        }
    }
    $gene_num;
}
#sub2
#============
sub get_gene
#============
{
    my ($CDS,$get,$scaf,$star,$end) = @_;
    ($CDS && $CDS->{$scaf}) || return(0);
    my $i = 0;
    my ($s1,$s2,$e1,$e2);
    if($star=~/,/){
        ($s1, $e1) = split/,/,$star;
        ($s2, $e2) = split/,/,$end;
    }
    foreach my $j(0..$#{$CDS->{$scaf}}){
        $j -= $i;
        if($e1){
            ( ($CDS->{$scaf}->[$j]->[2]->[0] >= $s1 && $CDS->{$scaf}->[$j]->[-1]->[1] <= $e1) ||
              ($CDS->{$scaf}->[$j]->[2]->[0] >= $s2 && $CDS->{$scaf}->[$j]->[-1]->[1] <= $e2) ) || next;
        }else{
            ($CDS->{$scaf}->[$j]->[2]->[0] > $end) && last;
            ($CDS->{$scaf}->[$j]->[2]->[0] >= $star && $CDS->{$scaf}->[$j]->[-1]->[1] <= $end) || next;
        }
        $i++;
        push @{$get},splice(@{$CDS->{$scaf}},$j,1);
    }
}
#sub3
#=================
sub get_pair_wise
#=================
{
    my ($tar_gene,$que_gene,$tar_seq,$que_seq,$pair,$tar_star,$que_star,$que_end,$stand,$type,$extend,$alg_gene_stat) = @_;
    my (@tar_fill,@que_fill,@tar_gap,@que_gap);
    real_fill_pos($tar_seq,$tar_gene,$tar_star,0,\@tar_fill,\@tar_gap);#sub3.1
    real_fill_pos($que_seq,$que_gene,$que_star,($stand eq '-') ? $que_end : 0,\@que_fill,\@que_gap);#sub3.1
    my $n1 = (defined $type && $type =~ /.(.)(.)/) ? 2*$1+$2 : 0;
    if(@tar_fill && @que_fill){
#        my $i = 0;
        my (%tuniq,%quniq);
        for my $t(0..$#tar_fill){$tuniq{$t} = 1;}
        for my $q(0..$#que_fill){$quniq{$q} = 1;}
        foreach my $h(0..$#tar_fill){
#            $h -= $i;
            my @sel;
            foreach my $k(0..$#que_fill){
                if($stand eq '-'){
                    ($tar_fill[$h]->[1] eq $que_fill[$k]->[1]) && next;
                }else{
                    ($tar_fill[$h]->[1] ne $que_fill[$k]->[1]) && next;
                }
                ($que_fill[$k]->[-1]->[1] > $tar_fill[$h]->[2]->[0] && 
                 $tar_fill[$h]->[-1]->[1] > $que_fill[$k]->[2]->[0]) || next;
                my $d1 = abs($tar_fill[$h]->[2]->[0]-$que_fill[$k]->[2]->[0]);
                my $d2 = abs($tar_fill[$h]->[-1]->[1]-$que_fill[$k]->[-1]->[1]);
                my $s = ($extend && $d1 > $extend) ? 0 : 1;
                my $e = ($extend && $d2 > $extend) ? 0 : 2;
                if($s || $e){
                    if(!@sel || $d1+$d2<$sel[0]){
                        @sel = ($d1+$d2,$s+$e,$k);
                        $sel[0] || last;
                    }
                }
            }
            @sel || next;
#           $i++;
#           push @{$pair},[$sel[1],splice(@tar_fill,$h,1),splice(@que_fill,$sel[2],1),splice(@$tar_gene,$h,1),splice(@$que_gene,$sel[2],1)];
            push @{$pair},[$sel[1],$tar_fill[$h],$que_fill[$sel[2]],$tar_gene->[$h],$que_gene->[$sel[2]]];#extend_type tar_fill que_fill tar_gene que_gene
            $tuniq{$h} && ($alg_gene_stat->[$n1]->[0]++, delete($tuniq{$h}));
            $quniq{$sel[2]} && ($alg_gene_stat->[$n1]->[3]++, delete($quniq{$sel[2]}));
#            $alg_gene_stat->[$n1]->[0]++;
#            $alg_gene_stat->[$n1]->[3]++;
        }
        if(%tuniq){
            @tar_fill = @tar_fill[keys %tuniq];
            @$tar_gene = @{$tar_gene}[keys %tuniq];
        }else{
            @tar_fill = ();
            @$tar_gene = ();
        }
        if(%quniq){
            @que_fill = @que_fill[keys %quniq];
            @$que_gene = @{$que_gene}[keys %quniq];
        }else{
            @que_fill = ();
            @$que_gene = ();
        }
    }
    $alg_gene_stat->[$n1]->[1] += add_pair_gene(\@tar_fill,$tar_gene,$que_star,($stand eq '-') ? $que_end : 0, \@que_gap,$pair,$stand);#sub3.2
    $alg_gene_stat->[$n1]->[4] += add_pair_gene(\@que_fill,$que_gene,$tar_star,0,\@tar_gap,$pair,$stand,1);#sub3.2
}
#sub3.2
#================
sub add_pair_gene
#================
{
    my ($tar_fill,$tar_gene,$que_star,$que_end,$gap,$pair,$stand,$rev) = @_;
    ($tar_fill && @$tar_fill) || return(0);
    my (@que_fill,@que_gene);
    copy_array($tar_fill,\@que_fill);#sub3.2.1
    foreach my $p(@que_fill){
        $p->[0] .= '.HL';
        ($stand eq '-') && ($p->[1] =~ tr/+-/-+/);
    }
    copy_array(\@que_fill,\@que_gene);#sub3.2.1
    foreach my $p(@que_gene){
        foreach my $q(@{$p}[2..$#$p]){
            foreach (@$q){
                $_ = pos_change($gap,$_,$que_star,$que_end,1);#sub3.1.2
            }
            $que_end && (@$q = @{$q}[1,0]);
        }
        $que_end && (@{$p}[2..$#$p] = reverse @{$p}[2..$#$p]);
    }
    if($rev){
        foreach my $k (0..$#$tar_fill){
            push  @{$pair},[5,$que_fill[$k],$tar_fill->[$k],$que_gene[$k],$tar_gene->[$k]];
        }
    }else{
        foreach my $k (0..$#$tar_fill){
            push  @{$pair},[4,$tar_fill->[$k],$que_fill[$k],$tar_gene->[$k],$que_gene[$k]];
        }
    }
    scalar @que_fill;
}
#sub3.2.1
#==============
sub copy_array
#=============+
{
    my ($tar,$que) = @_;
    foreach my $p(@$tar){
        my @g = @{$p}[0,1];
        foreach my $q(@{$p}[2..$#$p]){
            my @gq = @$q;
            push @g,[@gq];
        }
        push @{$que},[@g];
    }
}
#sub3.1
#================
sub real_fill_pos
#================
{
    my ($seq,$real_pos,$star,$end,$fill_pos,$gap) = @_;
    ($real_pos && @$real_pos) || return(0);
    gap_array($gap,$seq);#sub3.1.1
    copy_array($real_pos,$fill_pos);
    foreach my $p(@{$fill_pos}){
        foreach my $q (@{$p}[2..$#$p]){
            foreach (@$q){
                $_ =  pos_change($gap,$_,$star,$end);#sub3.1.2
            }
            $end && (@$q = @{$q}[1,0]);
        }
        $end && (@{$p}[2..$#$p] = reverse @{$p}[2..$#$p]);
    }
}
#sub3.1.1
#=============
sub gap_array
#=============
{
    my ($tar_gap,$tar_seq) = @_;
    ($tar_seq =~/-/) || return(0);
    my ($real,$dis) = (0, 0);
    push @{$tar_gap},[$real,$dis];
    if($tar_seq =~ /^(-+)/){
        $dis = length $1;
        $tar_seq =~ s/^-+//g;
        push @{$tar_gap},[$real,$dis];
    }
	($tar_seq =~/-/) || return(0);
    my @m = ($tar_seq =~ m/([^-]+)(-+)/g);
    foreach(0..$#m/2){
        my ($n, $g) = splice(@m,0,2);
        $n = length $n;
        $g = length $g;
        $real += $n;
        $dis += $n+$g;
        push @{$tar_gap},[$real,$dis];
    }
}
#sub3.1.2
#==============
sub pos_change
#==============
{
    my ($db,$que,$star,$end,$type) = @_;
    $type ||= 0;# 1:fill->real, 0:real->fill , fill star from 0, real star from 1
    my ($s, $e, $l, $s2, $e2);
    if($star=~/,/){
        ($s, $e) = split/,/,$star;
        $star = $s;
        $l = $e - $s + 1;
        if($end){
           ($s2, $e2) = split/,/,$end;
           $end = $e2;
        }
    }
    if(!$type){
        if($end){
            $que = ($e2 && $que>$e2) ? $e-$que+$e2 : $end-$que;
        }else{
            $que = ($s && $que<$s) ? $que+$l-1 : $que-$star;
        }
    }
    my $out;
    if($db && @$db){
        my $i = 0;
        foreach(0..$#$db){
            ($db->[$_]->[$type] > $que) && last;
            $i = $_;
        }
        $out = $db->[$i]->[1-$type] + $que - $db->[$i]->[$type];
        if($type && $db->[$i+1] && $out > $db->[$i+1]->[0]){
            $out = $db->[$i+1]->[0];
        }
    }else{
        $out = $que;
    }
    if($type){
        if($e2 && $out >= $e2){
            $out = $e-($out-$e2);
        }elsif($end){
            $out = $end - $out;
        }elsif($s && $out >= $l){
            $out -= $l-1;
        }else{
            $out += $star;
        }
    }
    ($out < 0) && ($out = 0);
    $out;
}
#sub4
#================
sub out_pair_wise
#================
{
    my ($pair,$head,$tar_seq,$que_seq,$handel,$handel2,$tar_cnv,$que_cnv,$alg_cnv,$stat,$algn_gene,$add_gene,
        $Code,$Star,$End,$sign,$snp_stat,$indel_stat,$snp_out,$indel_out,$snpcorr,$indelcorr,$cdsm_snp,$cdsm_indel) = @_;
    foreach my $p (@{$pair}){# extend_type tar_fill que_fill tar_gene que_gene
        my @dis = ($p->[1]->[2]->[0] - $p->[2]->[2]->[0], $p->[2]->[-1]->[1] - $p->[1]->[-1]->[1]);#star 1-2, end 2-1
        my $s = ($dis[0] < 0) ? $p->[1]->[2]->[0] : $p->[2]->[2]->[0];
        my $e = ($dis[1] < 0) ? $p->[1]->[-1]->[1] : $p->[2]->[-1]->[1];
        my $ll = length $tar_seq;
		#=================================================================##
		if($e>=$ll){
			print STDERR "substr outside of string at @$head, seq_len: $ll, star: $s, end: $e\n",
			"$p->[1]->[2]->[0]-$p->[1]->[-1]->[1] $p->[2]->[2]->[0]-$p->[2]->[-1]->[1]\n",
			"$p->[3]->[2]->[0]-$p->[3]->[-1]->[1] $p->[4]->[2]->[0]-$p->[4]->[-1]->[1]\n";
			next;
		}
        my $t_seq = substr($tar_seq,$s,$e-$s+1);
        my $q_seq = substr($que_seq,$s,$e-$s+1);
        my @indel;#the exon overlap: [gene_pos,len,seq,tar:0/que:1]
        exon_mask(\$t_seq,$p->[1],$s,$e,\@indel,0);
        exon_mask(\$q_seq,$p->[2],$s,$e,\@indel,1);
        if(@indel){
            @indel = sort {$a->[0] <=> $b->[0]} @indel;
            my $dis = 0;
            foreach(@indel){
                if($_->[0]+$dis+$_->[1]>=length($q_seq)){
                    die"$_->[0]\t$_->[1]\t$dis";
                }
                if($_->[3]){
                    if(substr($q_seq,$_->[0]+$dis,$_->[1]) eq '-' x $_->[1]){
                        substr($q_seq,$_->[0]+$dis,$_->[1]) = $_->[2];
                    }else{
                        substr($t_seq,$_->[0]+$dis,0) = '-' x $_->[1];
                        substr($q_seq,$_->[0]+$dis,0) = $_->[2];
                        $dis += $_->[1];
                    }
                }else{
                    if(substr($t_seq,$_->[0]+$dis,$_->[1]) eq '-' x $_->[1]){
                        substr($t_seq,$_->[0]+$dis,$_->[1]) = $_->[2];
                    }else{
                        substr($q_seq,$_->[0]+$dis,0) = '-' x $_->[1];
                        substr($t_seq,$_->[0]+$dis,0) = $_->[2];
                        $dis += $_->[1];
                    }
                }
            }
        }
        my @tq_se = ($p->[1]->[2]->[0],$p->[1]->[-1]->[1],$p->[2]->[2]->[0],$p->[2]->[-1]->[1]);#fill t_s,e q_s,e
        if($p->[1]->[1] eq '-'){
            $p->[0] =~ tr/12/21/;
            @dis = @dis[1,0];
            foreach($t_seq,$q_seq){
                $_ = reverse;
                tr/ATCGatcg/TAGCtagc/;
            }
            @tq_se = ($e-$tq_se[1], $e-$tq_se[0], $e-$tq_se[3], $e-$tq_se[2]);
        }else{
            foreach(@tq_se){$_ -= $s;}
        }
        if($dis[0] > 0){
            $dis[0] -= (substr($q_seq,0,$dis[0]) =~ s/-/-/g);
        }elsif($dis[0] < 0){
            $dis[0] += (substr($t_seq,0,-$dis[0]) =~ s/-/-/g);
        }
        if($dis[1] > 0){
            $dis[1] -= (substr($q_seq,-$dis[1]) =~ s/-/-/g);
        }elsif($dis[0] < 0){
            $dis[1] += (substr($t_seq,$dis[0]) =~ s/-/-/g);
        }
        my ($tlen,$tar_name) = gene_name($head->[1],$p->[3]);#sub4.2
        my ($qlen,$que_name) = gene_name($head->[4],$p->[4]);#sub4.2
        my $mask = ($p->[0]==4) ? 'DeleteGene' : ($p->[0]==5) ? 'InsertGene' : "$dis[0]/$dis[1]";
        my $t1 = 0;
        if($mask eq 'DeleteGene'){
            $t1 = 3;
        }elsif($mask eq 'InsertGene'){
            $t1 = 4;
        }elsif($dis[0]+$dis[1]){
            $t1 = ($dis[0] % 3) ? 2 : 1;
        }
        my $gene_pair = join("\t",$p->[1]->[0],$p->[2]->[0],$tar_name,$que_name,$tlen,$qlen,$mask);
        my @ident = caculate_identity($t_seq,$q_seq,$Code,$Star,$End,@tq_se,
            $tlen,$qlen,$sign,$gene_pair,$snp_stat,$indel_stat,$snp_out,$indel_out,
            [$head->[1],$p->[3],$head->[4],$p->[4]],$snpcorr,$indelcorr,$cdsm_snp,$cdsm_indel);#sub4.3
        my $t2 = ($ident[0] == 100.00) ? 0 : 2;
        $stat->[$t1]->[$t2]++;
        ($mask eq 'InsertGene') ? ($t2 || push @{$add_gene->[0]},[$head->[1],@{$p->[3]}]) :  ($algn_gene->[1]++);
        ($mask eq 'DeleteGene') ? ($t2 || push @{$add_gene->[1]},[$head->[4],@{$p->[4]}]) :  ($algn_gene->[3]++);
        $ident[2] && ($mask .= "\t".(pop @ident));
        if($tar_cnv->{$p->[1]->[0]} || $que_cnv->{$p->[2]->[0]}){
            $mask = join("\t",$mask,$tar_cnv->{$p->[1]->[0]}||0,$tar_cnv->{$p->[2]->[0]}||0);
            $$alg_cnv .= join("\t",$p->[1]->[0],$p->[2]->[0],$tar_name,$que_name,$tlen,$qlen,@ident,$mask)."\n";
            $tar_cnv->{$p->[1]->[0]} && ($algn_gene->[4]++);
            $que_cnv->{$p->[2]->[0]} && ($algn_gene->[5]++);
        }
        print $handel join("\t",$p->[1]->[0] . '--' . $p->[2]->[0],$tar_name,$que_name,$tlen,$qlen,@ident,"@{$head}[9,10]",$mask),"\n",
              $t_seq,"\n",$q_seq,"\n\n";
        $handel2 && (print $handel2 join("\t",$p->[1]->[0],$p->[2]->[0],$tar_name,$que_name,$tlen,$qlen,@ident,"@{$head}[9,10]",$mask),"\n");
    }
}
#sub7
#============
sub out_stat
#============
{
    my ($stat,$out_statf) = @_;
    if($stat && @$stat){
        foreach(0..4){
            $stat->[5]->[0] += ($stat->[$_]->[0] ||= 0);
            $stat->[5]->[2] += ($stat->[$_]->[2] ||= 0);
        }
        my $tol = $stat->[5]->[0] + $stat->[5]->[2];
        my @sign = qw(No-Frame-Mutate Frame-Unshift Frame-Shift DeleteGene InsertGene Total_pairwise);
        open STA,">$out_statf" || die$!;
        print STA "\t\tIdentity=100%\tIdentity<100%\tTotal\n",
              join("\t","Frame-Mutate","Num","Rate(%)","Num","Rate(%)","Num","Rate(%)"),"\n";
        foreach(0..5){
            $stat->[$_]->[1] = sprintf("%.2f",100*$stat->[$_]->[0]/$tol);
            $stat->[$_]->[3] = sprintf("%.2f",100*$stat->[$_]->[2]/$tol);
            $stat->[$_]->[4] = $stat->[$_]->[0] + $stat->[$_]->[2];
            $stat->[$_]->[5] = sprintf("%.2f",100*$stat->[$_]->[4]/$tol);
            print STA join("\t",$sign[$_],@{$stat->[$_]}),"\n";
        }
        close STA;
    }
}
#sub4.1
#=============
sub exon_mask
#=============
{
    my ($t_seq,$fill,$s,$e,$indel,$rev) = @_;
    (@$fill==3 && $s==$fill->[2]->[0] && $e==$fill->[2]->[1]) && return(0);
    $$t_seq = lc $$t_seq;
    my @pp;
    foreach my $i(2..$#$fill){
        my @p = ($fill->[$i]->[0]-$s, $fill->[$i]->[1]-$s);
        my $sub_seq = uc(substr($$t_seq,$p[0],$p[1]-$p[0]+1));
        substr($$t_seq,$p[0],$p[1]-$p[0]+1) = $sub_seq;
        if(@pp && $p[0] <= $pp[1] && $p[0]>1){#ignore cycle node gene
            my $len = $pp[1]-$p[0]+1;
            push @{$indel},[$pp[1]+1,$len,substr($$t_seq,$p[0],$len),$rev];
        }
        @pp = @p;
    }
}
#sub4.2
#=============
sub gene_name
#=============
{
    my ($scaf,$gene) = @_;
    my @out;
    my $len = 0;
    foreach my $p(@{$gene}[2..$#$gene]){
        push @out,"$p->[0]-$p->[1]";
        $len = $p->[1] - $p->[0] + 1;
    }
    ($len, join(":",$scaf,join(",",@out),$gene->[1]));
}
#sub4.3
#=====================
sub caculate_identity
#=====================
{
    my ($t_seq,$q_seq,$Code,$Star,$End,$ts,$te,$qs,$qe,$t_len,$q_len,$sign,$mask,$snp_stat,$indel_stat,$snp_out,
        $indel_out,$gene_pos,$snpcorr,$indelcorr,$cdsm_snp,$cdsm_indel) = @_;
    my @t = split//,$t_seq;
    my @q = split//,$q_seq;
    my $len = length $t_seq;
    my ($t_pos,$q_pos) = (-1, -1);
    my ($nt,$gt,$glen,$ins_len,$del_len) = (0) x 4;
    my ($ins_num,$del_num,$is_ins,$is_del,$cds_snp,$genomic_snp) = (0) x 6;
    my ($seq,$sub_seq,$adl);
    my (@SNP,@INDEL,@pos);
    my (@real_snp,@real_indel);
    my @real_pos;
#   $gene_pos to be: [tar_star,tar_gene,que_star,que_gene]
    @real_pos[0,1] = ($gene_pos->[1]->[1] eq '-') ? ($gene_pos->[1]->[-1]->[1]+1, -1) : ($gene_pos->[1]->[2]->[0]-1, 1);
    @real_pos[2,3] = ($gene_pos->[3]->[1] eq '-') ? ($gene_pos->[3]->[-1]->[1]+1, -1) : ($gene_pos->[3]->[2]->[0]-1, 1);
    if($ts){#ts,te -> [fill tar_s,tar_e]
        $sub_seq = substr($t_seq,0,$ts);
        $adl = $ts - ($sub_seq =~ s/-/-/g);
#        $real_pos[0] += $real_pos[1]*$adl;
        $real_pos[0] -= $real_pos[1]*$adl;
    }
    if($qs){#qs,qe -> [fill que_s,que_e]
        $sub_seq = substr($q_seq,0,$qs);
        $adl = $qs - ($sub_seq =~ s/-/-/g);
#        $real_pos[2] += $real_pos[3]*$adl;
        $real_pos[2] -= $real_pos[3]*$adl;
    }
    foreach my $i(0..$#t){
        ($t[$i]=~/[ATCGN]/) && ($t_pos++);
        ($q[$i]=~/[ATCGN]/) && ($q_pos++);
        ($t[$i] =~ /-/) || ($real_pos[0] += $real_pos[1]);
        ($q[$i] =~ /-/) || ($real_pos[2] += $real_pos[3]);
        if($t[$i] eq '-' && $q[$i] eq '-'){
            $len--;
        }elsif($t[$i] eq '-' && $q[$i] ne '-'){
            ($q[$i]=~/[ATCGN]/) && ($glen++);
            $is_ins || ($ins_num++);
            ($is_del, $is_ins) = (0, 1);
            push_indel(\@INDEL,\@pos,\$del_len,$is_del,\$seq,'Insertion',$gene_pos,$indelcorr);#sub4.3.1
            $ins_len || (@pos = ($i, $t_pos, $q_pos, @real_pos));
            $seq .= $q[$i];
            $ins_len++;
        }elsif($t[$i] ne '-' && $q[$i] eq '-'){
            ($t[$i]=~/[ATCGN]/) && ($glen++);
            $is_del || ($del_num++);
            ($is_ins, $is_del) = (0, 1);
            push_indel(\@INDEL,\@pos,\$ins_len,$is_ins,\$seq,'Deletion',$gene_pos,$indelcorr);#sub4.3.1
            $del_len || (@pos = ($i, $t_pos, $q_pos, @real_pos));
            $seq .= $t[$i];
            $del_len++;
#        }elsif($t[$i]!~/n/ig || $q[$i]!~/n/ig){
        }else{
            ($is_ins, $is_del) = (0, 0);
            push_indel(\@INDEL,\@pos,\$ins_len,$is_ins,\$seq,'Insertion',$gene_pos,$indelcorr);#sub4.3.1
            push_indel(\@INDEL,\@pos,\$del_len,$is_del,\$seq,'Deletion',$gene_pos,$indelcorr);#sub4.3.1
            my $s = abs(ord($t[$i]) - ord($q[$i]));
            if($t[$i]=~/[ATCGN]/ && $q[$i]=~/[ATCGN]/){
                $glen++;
                if($s){
                    corr_pos($snpcorr->[0],$gene_pos->[0],\$real_pos[0]);#sub4.3.0
                    corr_pos($snpcorr->[1],$gene_pos->[2],\$real_pos[2]);#sub4.3.0
#                    $cdsm_snp->{$gene_pos->[0]}->{$real_pos[0]}++;
                    $cds_snp++;
                    push @SNP,[$gene_pos->[0],$real_pos[0],$gene_pos->[2],$real_pos[2],$i,$t_pos,$q_pos,$t[$i],$q[$i]];
                }else{
                   $gt++;
                }
            }elsif($s == 32){
                $nt++;
            }else{
                corr_pos($snpcorr->[0],$gene_pos->[0],\$real_pos[0]);#sub4.3.0
                corr_pos($snpcorr->[1],$gene_pos->[2],\$real_pos[2]);#sub4.3.0
#                $cdsm_snp->{$gene_pos->[0]}->{$real_pos[0]}++;
                push @SNP,[$gene_pos->[0],$real_pos[0],$gene_pos->[2],$real_pos[2],$i,$t_pos,$q_pos,$t[$i],$q[$i]];
                $genomic_snp++;
            }
#        }else{
#            ($is_ins, $is_del) = (0, 0);
        }
    }
    push_indel(\@INDEL,\@pos,\$ins_len,0,\$seq,'Insertion',$gene_pos,$indelcorr);#sub4.3.1
    push_indel(\@INDEL,\@pos,\$del_len,0,\$seq,'Deletion',$gene_pos,$indelcorr);#sub4.3.1
    $genomic_snp += $cds_snp;
    my @out = (sprintf("%.2f",$len ? 100*($nt+$gt)/$len : 0), sprintf("%.2f",$glen ? 100*$gt/$glen : 0));
    my @mutate;
    foreach([$ins_num,'Insertion_num'],[$del_num,'Deletion_num'],[$cds_snp,'CDS_SNP_num'],[$genomic_snp,'PairWise_SNP_num']){
        $_->[0] && (push @mutate,"$_->[1]=$_->[0]");
    }
    if(@mutate){
        $mask .= "\t".join("\t",@out);
        my ($snp_out_temp, $indel_out_temp);
        my $h1 = snp_anno(\@SNP,$t_seq,$q_seq,$Code,$Star,$End,$ts,$te,$qs,$qe,$t_len,$q_len,$sign,$snp_stat,\$snp_out_temp,$cdsm_snp);#sub4.3.2
        my $h2 = indel_anno(\@INDEL,$t_seq,$q_seq,$Code,$Star,$End,$ts,$te,$qs,$qe,$t_len,$q_len,$sign,$indel_stat,\$indel_out_temp,$cdsm_indel);#sub4.3.2
        push @mutate,"Harm=$h1/$h2";
        push @out,join(" ",@mutate);
        $snp_out_temp && ($$snp_out .= "$mask\t$out[2]\n".$snp_out_temp);
        $indel_out_temp && ($$indel_out .= "$mask\t$out[2]\n".$indel_out_temp);
    }
    @out;
}
#sub4.3.0
#============
sub corr_pos
#============
{
    my ($hash,$id,$pos) = @_;
    ($hash && $hash->{$id} && @{$hash->{$id}}) || return(0);
    my ($s,$e);
    foreach (@{$hash->{$id}}){
        if($_ == $$pos){
            return(1);
        }elsif($_ > $$pos){
            $e = $_;
            last;
        }else{
            $s = $_;
        }
    }
    if(!$s && $e && $e-$$pos<2){
        $$pos = $e;
    }elsif($s){
        $$pos = ($e && (abs($$pos-$s) > abs($$pos-$e))) ? $e : $s;
    }
}

#sub4.3.1
#==============
sub push_indel
#==============
{
    my ($INDEL,$pos,$ins_len,$is_ins,$seq,$sign,$gene_pos,$indelcorr) = @_;
    if($$ins_len && !$is_ins && @$pos){
        if($pos->[4] == -1){
            $pos->[3] -= ($sign =~ /^I/) ? 1 : $$ins_len-1;
        }
        if($pos->[6] == -1){
            $pos->[5] -= ($sign =~ /^D/) ? 1 : $$ins_len-1;
        }
        corr_pos($indelcorr->[0],$gene_pos->[0],\$pos->[3]);#sub4.3.0
        corr_pos($indelcorr->[1],$gene_pos->[2],\$pos->[5]);#sub4.3.0
        push @{$INDEL},[$gene_pos->[0],$pos->[3],$gene_pos->[2],$pos->[5],@{$pos}[0..2],$sign,$$ins_len,$$seq];
        $$ins_len = 0;
        $$seq = "";
    }
}
#sub4.3.3
#===============
sub indel_anno
#===============
{
    my ($INDEL,$t_seq,$q_seq,$Code,$Star,$End,$ts,$te,$qs,$qe,$t_len,$q_len,$sign,$stat,$out,$cdsm_indel) = @_;
    ($INDEL && @$INDEL) || return(0);
    my $dis = 0;
    my ($tseq,$qseq) = ($t_seq,$q_seq);
    $tseq =~ s/[-atcg]//g;
    $qseq =~ s/[-atcg]//g;
    my $tol_harm = 0;
    foreach my $p(@$INDEL){
        $cdsm_indel->{$p->[0]}->{$p->[1]}++;
        my $seq = pop @$p;
        my $move = ($seq =~ s/([ATCG])/$1/g);
        my ($cm, $fm);
        if($p->[7] eq 'Deletion'){
            $dis -= $move;
            $cm = $p->[5] % 3;
        }else{
            $dis += $move;
            $cm = $p->[6] % 3;
        }
        $fm = $dis % 3;
        push @{$p},($move,$fm,$cm);
        my ($type,$harm) = qw(Unshift 0);
        if($fm || $cm){
            ($type,$harm) = code_check($p,$tseq,$qseq,$t_seq,$q_seq,$Code,$Star,$End,$t_len,$q_len,$ts,$te,$qs,$qe,$sign);#sub4.3.2.0
        }else{
            push @{$p},qw(- - -);
        }
        $stat->{$type}++;
        $$out .= " " . join("\t",@{$p},$type,$harm,$seq)."\n";
        $harm && ($tol_harm++);
    }
    $tol_harm;
}

#sub4.3.2
#============
sub snp_anno
#============
{
    my ($SNP,$t_seq,$q_seq,$Code,$Start,$End,$ts,$te,$qs,$qe,$t_len,$q_len,$sign,$stat,$out,$cdsm_snp) = @_;
    ($SNP && @$SNP) || return(0);
    my ($tseq,$qseq) = ($t_seq,$q_seq);
    $tseq =~ s/[-atcg]//g;
    $qseq =~ s/[-atcg]//g;
    my $tol_harm = 0;
    foreach my $p(@$SNP){
        $cdsm_snp->{$p->[0]}->{$p->[1]}++;
        my ($type,$harm) = code_check($p,$tseq,$qseq,$t_seq,$q_seq,$Code,$Start,$End,$t_len,$q_len,$ts,$te,$qs,$qe,$sign);#sub4.3.2.0
        $stat->{$type}++;
        $$out .= " " . join("\t",@{$p},$type,$harm)."\n";
        $harm && ($tol_harm++);
    }
    $tol_harm;
}
#sub4.3.2.0
#==============
sub code_check
#==============
{
    my ($p,$tseq,$qseq,$t_seq,$q_seq,$Code,$Star,$End,$t_len,$q_len,$ts,$te,$qs,$qe,$sign,$stat) = @_;
    my (@t_ph, @q_ph);
    my ($t_se, $q_se, $over, $harm1, $harm2) = (0) x 5;
    code_set(\@t_ph,$tseq,$t_seq,$p->[4],$p->[5],$ts,$te,$t_len,\$t_se,\$over,\$harm1,$Code);#sub4.3.2.1
    code_set(\@q_ph,$qseq,$q_seq,$p->[4],$p->[6],$qs,$qe,$q_len,\$q_se,\$over,\$harm2,$Code,\@t_ph,$t_seq);#sub4.3.2.1
    my $type;
    my $harm = $harm1 + 2*$harm2;
    if($over){
        $type = 'Outside_Frame';
    }elsif($harm){
        $type = 'Unknow_Codon';
    }elsif($t_ph[3] eq $q_ph[3]){
        $type = 'Same_Codon';
    }elsif($harm1 = ($t_se==1 && !$Star->{$t_ph[3]}) ? 1 : 0,
        $harm2 = ($q_se==1 && !$Star->{$q_ph[3]}) ? 1 : 0,
        $harm = $harm1 + 2*$harm2){
        $type = 'Damaged_Start_Codon';
    }elsif($harm1 = ($t_se==2 && !$End->{$t_ph[3]}) ? 1 : 0,
        $harm2 = ($q_se==2 && !$End->{$q_ph[3]}) ? 1 : 0,
        $harm = $harm1 + 2*$harm2){
        $type = 'Damaged_Stop_Codon';
    }else{
        my $n = ($t_se==1 || $q_se==1) ? 0 : ($t_se==2 || $q_se==2) ? 1 : 2;
        my $m = ($t_ph[4] eq $q_ph[4]) ? 0 : 1;
        my $p = $n + 3*$m;
        if($n == 2 && $m){
            $End->{$t_ph[3]} && ($p = 6, $harm = 1);
            $End->{$q_ph[3]} && ($p = 6, $harm = 2);
        }
        $type = $sign->[$p];
    }
    foreach(2,7,8){push @{$p},"$t_ph[$_]<=>$q_ph[$_]";}
    ($type, $harm);
}

#sub4.3.2.1
#============
sub code_set
#============
{
    my ($ph,$seq,$fseq,$i,$pos,$star,$end,$len,$is_se,$over,$harm,$Code,$tph,$tseq) = @_;
    if($i >= $star && $i <= $end){
        $ph->[0] = int($pos / 3);
        $ph->[1] = $pos - $ph->[0]*3;
        $ph->[2] = join(":",++$ph->[0],$ph->[1]);
        ($ph->[0] == 1) && ($$is_se=1);
        ($ph->[0] * 3 == $len) && ($$is_se=2);
        $ph->[3] = substr($seq,3*$ph->[0]-3,3);
        $ph->[4] = ($Code->{$ph->[3]} || 'X');
        ($ph->[4] eq 'X') && ($$harm++);
        @{$ph}[5,6] = find_rna_code($fseq,$i-$ph->[1],$Code);#sub4.3.2.1.1
        $ph->[7] = "$ph->[3]|$ph->[4]";
        $ph->[8] = "$ph->[5]|$ph->[6]";
        if($tph && $tph->[2] =~/[+-]/){
            $tph->[1] = $ph->[1];
            @{$tph}[5,6] = find_rna_code($tseq,$i-$tph->[1],$Code);#sub4.3.2.1.1
            $tph->[7] = '-|-';
            $tph->[8] = "$tph->[5]|$tph->[6]";
        }
    }else{
        $ph->[2] = ($pos > $end) ? '+' : '-';
        @{$ph}[3,4] = qw(- -);
        if($tph && @$tph){
            $ph->[1] = $tph->[1];
        }else{
            $ph->[0] = 0;
            $ph->[1] = ($i-$star) % 3;
        }
        @{$ph}[5,6] = find_rna_code($fseq,$i-$ph->[1],$Code);#sub4.3.2.1.1
        $ph->[7] = '-|-';
        $ph->[8] = "$ph->[5]|$ph->[6]";
        $$over++;
    }
}
#sub4.3.2.1.1
#=================
sub find_rna_code
#=================
{
    my ($seq,$i,$Code) = @_;
    my $code = substr($seq,$i,3);
    if($code =~ /-/){
        $code =~ s/-//g;
        foreach (1.. length($seq)-$i-3){
            my $h = substr($seq,$i+2+$_,1);
            ($h eq '-') && next;
            $code .= $h;
            (length($code) == 3) && last;
        }
        $code ||= '---';
    }
    ($code, $Code->{uc($code)} || 'X');
}
#sub5
#=======================
sub cnv_gene_annotation
#=======================
{
    my ($cnvf,$tar_cds,$que_cds,$tar_cnv_cds,$que_cnv_cds,$outf,$cnv_pos) = @_;
    ($cnvf && -s $cnvf) || return(0,0,0,0);
    my @num;
    open CNV,$cnvf || die$!;
    open OUT,">$outf" || die$!;
    while(<CNV>){
        my @head = split;
        my (%tar_cnv, %que_cnv);
        $num[0] += get_cnv_region(\%tar_cnv,$head[6],\%{$cnv_pos->[0]});#sub5.1
        $num[2] += get_cnv_region(\%que_cnv,$head[7],\%{$cnv_pos->[1]});#sub5.1
        my (@tar_gene, @que_gene);
        my $tn = get_cnv_gene(\%tar_cnv,\%tar_cds,\@tar_gene,$tar_cnv_cds,$head[0]);#sub5.2
        my $qn = get_cnv_gene(\%que_cnv,\%que_cds,\@que_gene,$que_cnv_cds,$head[0]);#sub5.2
        $num[1] += $tn;
        $num[3] += $qn;
        print OUT join("\n ",join("\t",@head[0..5],$tn,$qn,$tn+$qn),@head[6..9]),"\n";
        print OUT join(" ","\t+",$tar_gene[0]->[0],$que_gene[0]->[0],$tar_gene[0]->[0]+$que_gene[0]->[0]),"\n",
              "\t@{$tar_gene[0]}\n\t@{$tar_gene[1]}\n\t@{$que_gene[0]}\n\t@{$que_gene[1]}\n",
              join(" ","\t-",$tar_gene[2]->[0],$que_gene[2]->[0],$tar_gene[2]->[0]+$que_gene[2]->[0]),"\n",
              "\t@{$tar_gene[2]}\n\t@{$tar_gene[3]}\n\t@{$que_gene[2]}\n\t@{$que_gene[3]}\n\n";
    }
    close CNV;
    close OUT;
    @num;
}
#sub5.1
#==================
sub get_cnv_region
#==================
{
    my ($cnv,$region,$hash) = @_;
    ($region eq '-') && return(0);
    foreach my $p (split/;/,$region){
        my ($id,$start,$end,$stand) = split/[:,]/,$p;
        push @{$cnv->{$id}},[$stand,$start,$end];
        $hash && (push @{$hash->{$id}},[$start,$end]);
    }
    my $len = 0;
    foreach my $p (values %{$cnv}){
        my @pos;
        foreach (sort {$a->[0] cmp $b->[0] || $a->[1] <=> $b->[1]} @{$p}){
            if(@pos && $pos[-1]->[0] eq $_->[0] && $_->[1] < $pos[-1]->[2]+2){
                ($pos[-1]->[2] < $_->[2]) && ($pos[-1]->[2] = $_->[2]);
            }else{
                push @pos,[@{$_}];
            }
        }
        @{$p} = @pos;
        my ($s,$e) = (0, 0);
        foreach (sort {$a->[1] <=> $b->[1]} @pos){
            if($e && $_->[1] < $e+2){
                ($_->[2] > $e) && ($e = $_->[2]);
            }else{
                $e && ($len += $e-$s+1);
                ($s, $e) = @{$_}[1,2];
            }
        }
        $e && ($len += $e-$s+1);
    }
    $len;
}
#sub5.2
#================
sub get_cnv_gene
#================
{
    my ($tar_cnv,$tar_cds,$tar_gene,$get,$famid,$coverage) = @_;
#    (%{$tar_cnv} && %{$tar_cds}) || return(0);
    foreach my $id (keys %{$tar_cnv}){
        $tar_cds->{$id} || next;
        foreach my $i (@{$tar_cnv->{$id}}){
            gene_covered($id,@{$i}[1,2],$tar_cds,$tar_gene,$coverage,$i->[0],$famid,$get);#sub5.2.1
        }
    }
    my $gene_num = 0;
    foreach(0..3){
        my $n = $tar_gene->[$_] ? @{$tar_gene->[$_]} : 0;
        unshift @{$tar_gene->[$_]},$n;
        ($_ % 2) || ($gene_num += $n);
    }
    $gene_num;
}
#sub5.2.1
#===============
sub gene_covered
#===============
{
    my ($id,$star,$end,$cds,$out,$coverage,$stand,$famid,$get) = @_;
    ($cds && $cds->{$id} && @{$cds->{$id}}) || return(0);
    $coverage ||= 0.5;
    my $gene_num = 0;
    foreach my $j (@{$cds->{$id}}){
        ($famid && $get->{$j->[0]}) && next;
        if($j->[-1]->[1] <= $star){
            next;
        }elsif($j->[2]->[0]<$end && $j->[-1]->[1]>$star){
            my $cover = ($j->[-1]->[1]<$end?$j->[-1]->[1]:$end) - 
                ($j->[2]->[0] > $star ? $j->[2]->[0] : $star) + 1;
            $cover = sprintf("%.2f",$cover/($j->[-1]->[1]-$j->[2]->[0]+1));
            ($cover < $coverage) || next;
        }elsif($j->[2]->[0] >= $end){
            last;
        }
        my ($len, $gene_name) = gene_name($id,$j);#sub4.2
        $famid && ($get->{$j->[0]} = $famid);
        my $k = ($stand && $stand ne $j->[1]) ? 1 : 0;
        push @{$out->[2*$k]},$j->[0];
        push @{$out->[2*$k+1]},$gene_name;
        $gene_num++;
    }
    $gene_num;
}
#sub6
#==================
sub out_indel_gene
#==================
{
    my ($svf,$tar_cds,$que_cds,$outf,$tar_cnv,$que_cnv,$indel_gene_num,$indel_pos,$coverage) = @_;
    ($svf && -s $svf && (%{$tar_cds} || %{$que_cds})) || return(0);
    open OUT,">$outf" || die$!;
    open SV,$svf || die$!;
#    NC_008533.1     101259  101259  scaffold1       108884  108973  -       DupInser-115    0       90      000     0-6     0.000000
    while(<SV>){
        my @l = split;
        my @indel_gene;
        my $gene_num;
        my @gene_num;
        my @cnv = ($tar_cnv, $que_cnv);
        if($l[7]=~/Dele|Indel/i){
            $gene_num[0] = gene_covered(@l[0..2],$tar_cds,\@{$indel_gene[0]},$coverage);#sub5.2.1
            push @{${$indel_pos->[0]}{$l[0]}},[@l[1,2]];
            $indel_gene_num->[0] += $l[8];
            $indel_gene_num->[1] += $gene_num[0];
            if($l[10]=~/^0/){
                $indel_gene_num->[6] += $l[8];
                $indel_gene_num->[7] += $gene_num[0];
            }
        }
        if($l[7]=~/Insert|Indel/i){
            $gene_num[1] = gene_covered(@l[3..5],$que_cds,\@{$indel_gene[1]},$coverage);#sub5.2.1
            push @{${$indel_pos->[1]}{$l[3]}},[@l[4,5]];
            $indel_gene_num->[2] += $l[9];
            $indel_gene_num->[3] += $gene_num[1];
            if($l[10]=~/^0/){
                $indel_gene_num->[8] += $l[9];
                $indel_gene_num->[9] += $gene_num[1];
            }
        }
        ($gene_num[0] || $gene_num[1]) || next;
        my @cnv_gene_num;
        print OUT;
        foreach my $n(0,1){
            ($gene_num[$n] && %{$cnv[$n]}) || next;
            my @cnv_gene;
            foreach(0..$gene_num[$n]-1){
                $cnv[$n]->{$indel_gene[$n]->[0]->[$_]} || next;
                push @{$cnv_gene[0]},"$indel_gene[$n]->[0]->[$_]:$cnv[$n]->{$indel_gene[$n]->[0]->[$_]}";
                push @{$cnv_gene[1]},$indel_gene[$n]->[1]->[$_];
                $cnv_gene_num[$n]++;
            }
            print OUT join("\t",$n,$gene_num[$n],"@{$indel_gene[$n]->[0]}","@{$indel_gene[$n]->[1]}\n");
            if($cnv_gene_num[$n]){
                print OUT join("\t","C$n",$cnv_gene_num[$n],"@{$cnv_gene[0]}","@{$cnv_gene[1]}\n");
                $indel_gene_num->[4+$n] += $cnv_gene_num[$n];
                ($l[10]=~/^0/) && ($indel_gene_num->[10+$n] += $cnv_gene_num[$n]);
            }
        }
        print OUT "\n";
    }
    close SV;
    close OUT;
    (-s $outf) || `rm $outf`;
}
#sub8
#=======================
sub out_align_gene_stat
#=======================
{
    my ($algn_gene,$tar_gene_num,$que_gene_num,$outf) = @_;
    ($algn_gene && @$algn_gene) || return(0);
    @{$algn_gene->[4]} = (0,0,$tar_gene_num,0,0,$que_gene_num);
    @{$algn_gene->[5]} = @{$algn_gene->[4]};
    foreach my $i(0..3){
        $algn_gene->[$i]->[2] += ($algn_gene->[$i]->[0] ||= 0) + ($algn_gene->[$i]->[1] ||= 0);
        $algn_gene->[$i]->[5] += ($algn_gene->[$i]->[3] ||= 0) + ($algn_gene->[$i]->[4] ||= 0);
        $algn_gene->[4]->[2] -= $algn_gene->[$i]->[2];
        $algn_gene->[4]->[5] -= $algn_gene->[$i]->[5];
        $algn_gene->[5]->[0] += $algn_gene->[$i]->[0];
        $algn_gene->[5]->[3] += $algn_gene->[$i]->[3];
    }
    @{$algn_gene->[4]}[1,4] = @{$algn_gene->[4]}[2,5];
    $algn_gene->[5]->[1] = $tar_gene_num - $algn_gene->[5]->[0];
    $algn_gene->[5]->[4] = $que_gene_num - $algn_gene->[5]->[3];
    my @sign = qw(Normal_synteny Translocation Inversion Tran+inver Other_regions Total_genes);
    open OUT,">$outf" || die$!;
    print OUT "\t\t\tTarget\t\t\tQuery\n\t\tPair\tUnPair\tTotal\tPair\tUnPair\tTotal\n";
    foreach(0..5){
        print OUT join("\t",$sign[$_],@{$algn_gene->[$_]}),"\n";
    }
    close OUT;
}
#sub9
#=================
sub out_merge_stat
#=================
{
    my ($cnv,$alg,$indel,$cnv_pos,$alg_pos,$indel_pos,$tgene,$qgene,$tsize,$qsize,$outf) = @_;
    $tsize = get_size($tsize);#sub0.1
    $qsize = get_size($qsize);#sub0.1
    my @out = ([@{$alg}[0..3]],[],[@{$cnv}],[],[@{$indel}[0..3]],[@{$indel}[6..9]]);
    @{$out[1]} = (overlap_stat($cnv_pos->[0],$alg_pos->[0]),$alg->[4],overlap_stat($cnv_pos->[1],$alg_pos->[1]),$alg->[5]);
    @{$out[3]} = (overlap_stat($cnv_pos->[0],$indel_pos->[0]),$indel->[4],overlap_stat($cnv_pos->[1],$indel_pos->[1]),$indel->[5]);
    @{$out[7]}[1,3] = ($tgene, $qgene);
    $out[6]->[1] = $tgene + $out[5]->[1] - $out[0]->[1] - $out[4]->[1];
    $out[6]->[3] = $qgene + $out[5]->[3] - $out[0]->[3] - $out[4]->[3];
    if($tsize){
        $out[7]->[0] = $tsize;
        $out[6]->[0] = $tsize + $out[5]->[0] - $out[0]->[0] - $out[4]->[0];
    }else{
        ($out[7]->[0], $out[6]->[0]) = qw(- -);
    }
    if($qsize){
        $out[7]->[2] = $qsize;
        $out[6]->[2] = $qsize + $out[5]->[2] - $out[0]->[2] - $out[4]->[2];
    }else{
        ($out[7]->[2], $out[6]->[2]) = qw(- -);
    }
    my @sign = qw(Align_regions Align+Repeat Repeat_regions Repeat+InDel InDel_regions Align+InDel Other_regions Whole_Genomic);
    open OUT,">$outf" || die$!;
    if($tsize && $qsize){
        print OUT "\t\t\t\tTarget\t\t\t\tQuery\n\t\tlen(bp)\trate(%)\t#gene\trate(%)\tlen(bp)\trate(%)\t#gene\trate(%)\n";
        foreach (0..7){
            my @t;
            @t[0,2,4,6] = @{$out[$_]};
            foreach my $i(0,2,4,6){
                $t[$i+1] = sprintf("%.2f", $out[7]->[$i/2] ? 100 * $t[$i]/$out[7]->[$i/2] : 0);
            }
            print OUT join("\t",$sign[$_],@t),"\n";
        }
    }else{
        print OUT "\t\t\tTarget\t\t\tQuery\n\t\tlength(bp)\t#gene\tlength(bp)\t#gene\n";
        foreach (0..7){
            print OUT join("\t",$sign[$_],@{$out[$_]}),"\n";
        }
    }
    close OUT;
}

#sub9.1
#============
sub get_size
#============
{
    my $sizef = shift;
    my $size = 0;
    if($sizef && -s $sizef){
        chomp($size = `awk '{x+=\$2}END{print x}' $sizef`);
    }elsif($sizef && $sizef !~ /\D/){
        $size = $sizef;
    }
    $size;
}
#sub9.2
#================
sub overlap_stat
#================
{
    my ($hash1, $hash2) = @_;
    ($hash1 && %$hash1 && $hash2 && %$hash2) || return(0);
    my $len = 0;
    foreach my $id (keys %$hash1){
        $hash2->{$id} || next;
        foreach my $p (sort {$a->[0] <=> $b->[0]} @{$hash1->{$id}}){
            foreach my $j (sort {$a->[0] <=> $b->[0]} @{$hash2->{$id}}){
                if($j->[1] < $p->[0]){
                    next;
                }elsif($j->[0] <= $p->[1] && $j->[1] >= $p->[0]){
                    $len += ($j->[1]<$p->[1] ? $j->[1] : $p->[1]) - ($j->[0]>$p->[0] ? $j->[0] : $p->[0]) + 1;
                }elsif($j->[0] > $p->[1]){
                    last;
                }
            }
        }
    }
    $len;
}
#sub10
#======================
sub out_snp_indel_stat
#======================
{
    my ($snp_stat,$indel_stat,$sign,$snp_indel_stat_list) = @_;
    (%$snp_stat || %indel_stat) || return(0);
    my @out;
    my $j = @$sign;
    $out[$j]->[0] = 'Total_Mutate';
    foreach my $i(0..$#sign){
        $out[$i]->[0] = $sign->[$i];
        $out[$i]->[1] = ($snp_stat->{$sign->[$i]} || 0);
        $out[$i]->[3] = ($indel_stat->{$sign->[$i]} || 0);
        $out[$i]->[5] = $out[$i]->[1] + $out[$i]->[3];
        $out[$j]->[1] += $out[$i]->[1];
        $out[$j]->[3] += $out[$i]->[3];
    }
    $out[$j]->[5] = $out[$j]->[1] + $out[$j]->[3];
    open OUT,">$snp_indel_stat_list" || die$!;
    print OUT "\t\tSNP\t\tInDel\t\tTotal\nMutate_Type\tnum\trate(%)\tnum\trate(%)\tnum\trate(%)\n";
    foreach my $i(0..$j-1){
        $out[$i]->[2] = sprintf("%.2f",$out[$j]->[1] ? 100*$out[$i]->[1]/$out[$j]->[1] : 0);
        $out[$i]->[4] = sprintf("%.2f",$out[$j]->[3] ? 100*$out[$i]->[3]/$out[$j]->[3] : 0);
        $out[$i]->[6] = sprintf("%.2f",$out[$j]->[5] ? 100*$out[$i]->[5]/$out[$j]->[5] : 0);
        print OUT join("\t",@{$out[$i]}),"\n";
    }
    $out[$j]->[2] = sprintf("%.2f",$out[$j]->[5] ? 100*$out[$j]->[1]/$out[$j]->[5] : 0);
    $out[$j]->[4] = sprintf("%.2f",$out[$j]->[5] ? 100*$out[$j]->[3]/$out[$j]->[5] : 0);
    $out[$j]->[6] = "100.00";
    print OUT join("\t",@{$out[$j]}),"\n";
    close OUT;
}
#sbu11
#================
sub out_add_gene
#================
{
    my ($add_gene,$outf) = @_;
    ($add_gene && @$add_gene) || return(0);
    open GFF,">$outf" || die$!;
    print GFF "##gff-version 3\n";
    foreach my $p (@$add_gene){
        my ($id, $gene_name, $stand) = splice(@{$p},0,3);
        print GFF join("\t",$id,'SVFINDER','gene',$p->[0]->[0],$p->[-1]->[1],'.',$stand,'.',"ID=$gene_name;Name=$gene_name;\n"),
              join("\t",$id,'SVFINDER','mRNA',$p->[0]->[0],$p->[-1]->[1],'.',$stand,'.',"ID=$gene_name;Name=$gene_name;Parent=$gene_name;\n");
        foreach(@{$p}){
            print GFF join("\t",$id,'SVFINDER','CDS',$_->[0],$_->[1],'.',$stand,'.',"Parent=$gene_name;\n");
        }
    }
    close GFF;
}
