# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/2/8
library(optparse)
option_list <- list(
make_option("--m", type = 'character', action = "store", default = "log2", help = "data transform method")
)
opt <- parse_args(OptionParser(option_list = option_list))
data = read.table(quote="","deal.txt", header = T, com = '', sep = "\t", row.names = 1, check.names = F)
m = opt$m
if (m == "log2") {
    data = log2(data + 1)
}else if (m == "log2_center") {
    data = log2(data + 1)
    data = t(scale(t(data), scale = F))
}else if (m == "center") {
    data = t(scale(t(data), scale = F))
}
write.table(data, 'deal.txt', sep = '\t', quote = F, col.names = NA)

