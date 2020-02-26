# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/2/11
library(optparse)
option_list <- list(
make_option("--paired", default = F, type = "logical", help = "T test paired"),
make_option("--pCutoff", default = 0.05, type = "numeric", help = "P value cut off"),
make_option("--qCutoff", default = 1, type = "numeric", help = "fdr value cut off"),
make_option("--m", default = "tTest", type = "character", help = "test method"),
make_option("--l", default = "1", type = "numeric", help = "logFC cut off"),
make_option("--ve", default = F, type = "logical", help = "var equal")
)
opt <- parse_args(OptionParser(option_list = option_list))
std <- function(x) sd(x) / sqrt(length(x))
data <- read.table("deal.txt", header = T, sep = "\t",check.names=FALSE,row.names=1,quote="")
group <- read.table(quote="",file="group.txt", header = F, sep = "\t",check.names=FALSE)
uniq.group <- as.character(unique(group$V2))
group1 <- subset(group, V2 == uniq.group[1])$V1
group2 <- subset(group, V2 == uniq.group[2])$V1
group1 <- as.character(group1)
group2 <- as.character(group2)
mean1 = apply(data[, group1], 1, mean)
okk = as.data.frame(mean1)
rownames(okk)=rownames(data)
write.table(rownames(data), "dealTest.txt", sep = "\t", quote = F, col.names = NA)
okk$std1 = apply(data[, group1], 1, std)
okk$mean2 = apply(data[, group2], 1, mean)
okk$std2 = apply(data[, group2], 1, std)
okk$logFC = log2(okk$mean2 / okk$mean1)
for (i in 1 : nrow(data)) {
    x = as.numeric(data[i, group1])
    y = as.numeric(data[i, group2])
    okk$p[i] = tryCatch(
    {
        if (opt$m == "tTest") {
            tets = t.test(x , y, alternative = "two.sided", paired = opt$paired,var.equal=opt$ve)
        }else {
            tets = wilcox.test(x , y, alternative = "two.sided", paired = opt$paired)
        }
        tets$p.value
    }, error = function(e){
        1
    }
    )
}
print(head(okk,5))

okk$fdr = p.adjust(okk$p, method = "fdr", n = length(okk$p))
okk = okk[order(okk[, "p"]),]
write.table(okk, "allResult.txt", sep = "\t", quote = F,col.names = NA)
okk = subset(okk, p < opt$pCutoff)
okk = subset(okk, fdr < opt$qCutoff)
okk = subset(okk, abs(logFC) > opt$l)
names(okk)[names(okk) == "mean1"] = paste("mean(", uniq.group[1], ")", sep = "")
names(okk)[names(okk) == "std1"] = paste("stderr(", uniq.group[1], ")", sep = "")
names(okk)[names(okk) == "mean2"] = paste("mean(", uniq.group[2], ")", sep = "")
names(okk)[names(okk) == "std2"] = paste("stderr(", uniq.group[2], ")", sep = "")
names(okk)[names(okk) == "logFC"] = paste("log2FC(", uniq.group[2], "/", uniq.group[1], ")", sep = "")
write.table(okk, "result.txt", sep = "\t", quote = F, col.names = NA)

