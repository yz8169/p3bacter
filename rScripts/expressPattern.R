# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/2/7
library(optparse)
option_list <- list(
make_option("--s", type = 'character', action = "store", default = "", help = "the sample names Will be analyzed ,sep=','"),
make_option("--m", type = 'character', action = "store", default = "log2", help = "data transform method"),
make_option("--k", type = 'numeric', action = "store", help = "k value"),
make_option("--kt", type = 'numeric', action = "store", help = "kTree value"),
make_option("--pt", type = 'numeric', action = "store", help = "pTree value"),
make_option("--dm", type = 'character', default = "euclidean", action = "store", help = "dist method"),
make_option("--cm", type = 'character', default = "complete", action = "store", help = "cluster method")
)
opt <- parse_args(OptionParser(option_list = option_list))
data = read.table(quote = "", "deal.txt", header = T, sep = "\t", row.names = 1, check.names = F)
geneData = read.table(quote = "", "gene.txt", header = F, sep = "\t", check.names = F)
m = opt$m
if (m == "log2") {
    data = log2(data + 1)
}else if (m == "log2_center") {
    data = log2(data + 1)
    data = t(scale(t(data), scale = F))
}else if (m == "center") {
    data = t(scale(t(data), scale = F))
}
if (opt$s != "") {
    sampleNames = unlist(strsplit(opt$s, split = ","))
    data = data[sampleNames]
}
genes = geneData$V1
data = data[genes,]
gene_dist = dist(data, method = opt$dm)
hc_genes = hclust(gene_dist, method = opt$cm)
if (! is.null(opt$k)) {
    k = opt$k
    kmeans_clustering <- kmeans(data, centers = k, iter.max = 100, nstart = 5)
    gene_partition_assignments = kmeans_clustering$cluster
}else if (! is.null(opt$kt)) {
    kt = opt$kt
    gene_partition_assignments <- cutree(as.hclust(hc_genes), k = kt)
}else if (! is.null(opt$pt)) {
    pt = opt$pt
    gene_partition_assignments <- cutree(as.hclust(hc_genes), h = pt / 100 * max(hc_genes$height))
}
max_cluster_count = max(gene_partition_assignments)
gene_names = rownames(data)
num_cols = length(data[1,])
for (i in 1 : max_cluster_count) {
    partition_i = (gene_partition_assignments == i)
    partition_data = data[partition_i, , drop = F]
    if (sum(partition_i) == 1) {
        dim(partition_data) = c(1, num_cols)
        colnames(partition_data) = colnames(data)
        rownames(partition_data) = gene_names[partition_i]
    }
    outfile = paste(i, ".matrix.txt", sep = '')
    write.table(partition_data, file = outfile, quote = F, sep = "\t", col.names = NA)
}
