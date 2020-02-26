from Bio import SeqIO
import sys


def writeArray2File(fileName, array):
    with open(fileName, 'w') as f:
        f.write("\n".join(array))


infos = []
infos.append(
    "\t".join(["sample", "sequence_length", "molecule_type", "data_file_division", "description",
               "source", "organism", "taxonomy"]))
geneInfos = []
geneInfos.append(
    "\t".join(["sample", "locus_tag", "gene_name", "sequence_name", "start", "end", "strand", "transl_table", "product",
               "protein_id", "gene", "pep"]))
sample = ""
cds = []
pep = []
genes = []
infosContent = ["NA"] * 8
infosContent[1] = "0"
for record in SeqIO.parse("data.gbff", "genbank"):
    for v in record.dbxrefs:
        if v.lower().startswith("biosample"):
            sample = v.split(":")[1]
    if sample != "":
        break
    else:
        for f in record.features:
            if f.type == "source":
                taxStr = f.qualifiers["db_xref"]
                sample = taxStr[0].split(":")[1]
                break

for record in SeqIO.parse("data.gbff", "genbank"):
    genes.append(">" + record.name)
    infosContent[0] = sample
    infosContent[1] = str((len(record.seq)) + int(infosContent[1]))
    infosContent[2] = (record.annotations.get("molecule_type", "NA"))
    infosContent[3] = (record.annotations.get("data_file_division", "NA"))
    infosContent[4] = (record.description)
    infosContent[5] = (record.annotations.get("source", "NA"))
    infosContent[6] = (record.annotations.get("organism", "NA"))
    organism = []
    organism.extend(record.annotations.get("taxonomy", []))
    organism.append(record.annotations.get("organism", "NA"))
    infosContent[7] = (",".join(organism))
    genes.append(str(record.seq))
    for f in record.features:
        if f.type == "CDS":
            line = [sample]
            translation = f.qualifiers.get("translation", ["NA"])[0]
            if translation == "NA":
                continue
            locusName = f.qualifiers.get("locus_tag", ["NA"])[0]
            geneName = f.qualifiers.get("gene", ["NA"])[0]
            line.append(locusName)
            line.append(geneName)
            seqName = record.name
            start = str(f.location.start.position)
            end = str(f.location.end.position)
            strand = "-"
            product = f.qualifiers.get("product", ["NA"])[0]
            if (f.location.strand > 0):
                strand = "+"
            proteinId = f.qualifiers.get("protein_id", ["NA"])[0]
            proteinIdWithInfo = proteinId + " " + geneName + " " + seqName + " " + start + " " + end + " " + strand
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
infos.append("\t".join(infosContent))
if int(infosContent[1]) == 0:
    sys.exit("sequence length must greater than 0!")
writeArray2File("geneInfo.txt", geneInfos)
print(sample)
writeArray2File(sample + ".cds.fa", cds)
writeArray2File(sample + ".pep.fa", pep)
writeArray2File(sample + ".genes.fa", genes)
print(infosContent)
writeArray2File("info.txt", infos)
