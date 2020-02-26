# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/4/3
library(optparse)
option_list <- list(
make_option("--c", type = 'numeric', action = "store", default = "1.5", help = "data deal iqr")
)
opt <- parse_args(OptionParser(option_list = option_list))
tlog = read.table("data.txt", header = T, com = '', sep = "\t", row.names = 1, check.names = F, quote = "")
ndt01 <- data.frame()
for (i in 1 : dim(tlog)[1]) {
    value <- as.numeric(tlog[i,])
    QL <- quantile(value, probs = 0.25, na.rm = T)
    QU <- quantile(value, probs = 0.75, na.rm = T)
    QU_QL <- QU - QL
    coef <- opt$c
    # QL;QU;QU_QL
    test01 <- value
    out_imp01 <- max(test01[which(test01 <= QU + coef * QU_QL)])
    test01[which(test01 > QU + coef * QU_QL)] <- out_imp01
    dt01 <- as.data.frame(t(test01))
    ndt01 <- rbind(ndt01, dt01)
}
ndt01 <- t1 <- as.matrix(ndt01)
rownames(ndt01) = rownames(tlog)
colnames(ndt01) = colnames(tlog)
write.table(ndt01, "data.txt", quote = FALSE , sep = '\t',col.names = NA)
