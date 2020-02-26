# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/4/3
library(optparse)
option_list <- list(
make_option("--k", type = 'numeric', action = "store", default = "10", help = "kNN value")
)
opt <- parse_args(OptionParser(option_list = option_list))

library(DMwR)
data = read.table('data.txt',sep="\t",header=T,row.names=1,quote="",check.names=F)
data<-knnImputation(data,k=opt$k,scale=F)
write.table(data,"data.txt",quote=FALSE ,sep='\t',col.names = NA)

