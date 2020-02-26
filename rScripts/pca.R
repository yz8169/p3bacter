# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/2/1
library(optparse)
option_list <- list(
make_option("--s", type = 'character', action = "store", default = "", help = "Will pca sample names,sep=','")
)
opt <- parse_args(OptionParser(option_list = option_list))

data <- read.table(quote="",'deal.txt', sep = "\t", header = T, row.names = 1,check.names=F)
if (opt$s != "") {
    sampleNames = unlist(strsplit(opt$s, split = ","))
    data = data[sampleNames]
}

data <- t(data)
pca <- prcomp(data, scal = FALSE)
write.table(pca$x, 'out.txt', sep = '\t', quote = F, col.names = NA)
write.table(summary(pca)$importance[2,], 'impoPath.txt', sep = '\t', quote = F, col.names = NA)
