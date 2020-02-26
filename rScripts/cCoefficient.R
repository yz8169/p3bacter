# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/4/4
library(optparse)
option_list <- list(
make_option("--m", type = 'character', action = "store", default = "pearson", help = "a character string indicating which correlation coefficient (or covariance) is to be computed")
)
opt <- parse_args(OptionParser(option_list = option_list))

data <- read.table('deal.txt', sep = "\t", header = T, row.names = 1,quote="",check.names=F)
out <- cor(data, use = 'pairwise.complete.obs', method = opt$m)
write.table(out, 'deal.txt', sep = '\t', quote = F,col.names = NA)
