# -*- coding: UTF-8 -*-
from Bio import SeqIO
import sys
import re


def writeArray2File(fileName, array):
    with open(fileName, 'w') as f:
        f.write("\n".join(array))


def file2Str(fileName):
    with open(fileName, 'r') as f:
        str = f.read()
    return str


def str2File(fileName, str):
    with open(fileName, 'w') as f:
        f.write(str)


infos = []
dict = {}
infos.append(
    "\t".join(["sample", "sequence_length", "molecule_type", "data_file_division", "description",
               "source", "organism", "taxonomy", "topology"]))
geneInfos = []
geneInfos.append(
    "\t".join(["sample", "sequence_name", "start", "end", "strand", "transl_table", "product",
               "protein_id", "gene", "pep"]))
sample = ""
cds = []
pep = []
genes = []
infosContent = ["NA"] * 9
infosContent[1] = "0"
gffInfos = []
i = 1
seqI = 0
for record in SeqIO.parse("data.gbff", "genbank"):
    seqI += 1
    genes.append(">" + record.name)
    infosContent[1] = str((len(record.seq)) + int(infosContent[1]))
    infosContent[2] = (record.annotations.get("molecule_type", "NA"))
    infosContent[3] = (record.annotations.get("data_file_division", "NA"))
    infosContent[4] = (record.description)
    infosContent[5] = (record.annotations.get("source", "NA"))
    infosContent[6] = (record.annotations.get("organism", "NA"))
    organism = []
    organism.extend(record.annotations.get("taxonomy", []))
    organism.append(record.annotations.get("organism", "NA"))
    if (seqI == 1):
        if len(organism) != 7:
            sys.exit(u"分类学信息错误(必须包含6种分类学信息)!")
        infosContent[7] = (",".join(organism))
        for v in record.dbxrefs:
            if v.lower().startswith("biosample"):
                infosContent[0] = v.split(":")[1]
                sample = v.split(":")[1]
                if re.search(r'\W', sample):
                    sys.exit(u"BioSample只能由数字、字母、下划线组成!")

    infosContent[8] = record.annotations.get("topology", "NA")
    genes.append(str(record.seq))
    for f in record.features:
        if f.type == "CDS":
            line = [sample]
            translation = f.qualifiers.get("translation", ["NA"])[0]
            if translation == "NA":
                continue
            seqName = record.name
            start = str(f.location.start.position + 1)
            end = str(f.location.end.position)
            strand = "-"
            product = f.qualifiers.get("product", ["NA"])[0]
            if (f.location.strand > 0):
                strand = "+"
            proteinId = f.qualifiers.get("protein_id", ["NA"])[0]
            locusName = f.qualifiers.get("locus_tag", ["NA"])[0]
            if (proteinId == "NA"):
                proteinId = locusName
            if (proteinId == "NA"):
                proteinId = "mygene" + str(i)
                i += 1

            m = 1
            tmpProteinId = proteinId
            while (dict.has_key(tmpProteinId)):
                tmpProteinId = proteinId + str(m)
                m += 1
            proteinId = tmpProteinId

            dict[proteinId] = 1
            proteinIdWithInfo = proteinId + " locus=" + seqName + ":" + start + ":" + end + ":" + strand
            cds.append(">" + proteinIdWithInfo)
            pep.append(">" + proteinIdWithInfo)
            line.append(seqName)
            line.append(start)
            line.append(end)
            line.append(strand)
            cds.append(str(f.extract(record.seq)))
            line.append(f.qualifiers.get("transl_table", ["NA"])[0])
            line.append(product)
            line.append(proteinId)
            line.append(str(f.extract(record.seq)))
            line.append(translation)
            pep.append(translation)
            geneInfos.append("\t".join(line))
            gff1 = [record.name, "GenBank", "mRNA", start, end, ".", strand, ".",
                    "ID=" + proteinId + ";product=\"" + product + "\""]
            gff2 = [record.name, "GenBank", "CDS", start, end, ".", strand, ".",
                    "Parent=" + proteinId + ";product=\"" + product + "\""]
            gffInfos.append("\t".join(gff1))
            gffInfos.append("\t".join(gff2))
infos.append("\t".join(infosContent))
if sample == "" or int(infosContent[1]) == 0:
    sys.exit("no sample found!")
writeArray2File("geneInfo.txt", geneInfos)
writeArray2File(sample + ".cds.fa", cds)
writeArray2File(sample + ".pep.fa", pep)
writeArray2File(sample + ".genome.fa", genes)
writeArray2File("info.txt", infos)
writeArray2File(sample + ".gff", gffInfos)
str = file2Str("data.gbff")
str2File(sample + ".gbff", str)
