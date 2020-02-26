# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/1/26
library(optparse)
option_list <- list(
make_option("--paired", default = F, type = "logical", help = "T test paired"),
make_option("--pCutoff", default = 0.05, type = "numeric", help = "P value cut off"),
make_option("--qCutoff", default = 1, type = "numeric", help = "fdr value cut off")
)
opt <- parse_args(OptionParser(option_list = option_list))
std <- function(x) sd(x) / sqrt(length(x))
data <- read.table(quote="","deal.txt", header = T, sep = "\t",check.names=F)
group <- read.table(quote="","group.txt", header = F, sep = "\t",check.names=F)
uniq.group <- as.character(unique(group$V2))
id = data[, 1]
okk = as.data.frame(id)
group1 <- subset(group, V2 == uniq.group[1])$V1
group2 <- subset(group, V2 == uniq.group[2])$V1
group1 <- as.character(group1)
group2 <- as.character(group2)
okk$mean1 = apply(data[, group1], 1, mean)
okk$std1 = apply(data[, group1], 1, std)
okk$mean2 = apply(data[, group2], 1, mean)
okk$std2 = apply(data[, group2], 1, std)
okk$logFC = log2(okk$mean2 / okk$mean1)
for (i in 1 : nrow(data)) {
    x = as.numeric(data[i, group1])
    y = as.numeric(data[i, group2])
    okk$p[i] = tryCatch(
    {
        tets = t.test(x , y, alternative = "two.sided", paired = opt$paired)
        tets$p.value
    }, error = function(e){
        1
    }
    )
}
okk$fdr = p.adjust(okk$p, method = "fdr", n = length(okk$p))
okk = okk[order(okk[, "p"]),]
write.table(okk, "allResult.txt", sep = "\t", quote = F, row.names = F)
okk = subset(okk, p < opt$pCutoff)
okk = subset(okk, fdr < opt$qCutoff)
names(okk)[names(okk) == "mean1"] = paste("mean(", uniq.group[1], ")", sep = "")
names(okk)[names(okk) == "std1"] = paste("stderr(", uniq.group[1], ")", sep = "")
names(okk)[names(okk) == "mean2"] = paste("mean(", uniq.group[2], ")", sep = "")
names(okk)[names(okk) == "std2"] = paste("stderr(", uniq.group[2], ")", sep = "")
names(okk)[names(okk) == "logFC"] = paste("logFC(", uniq.group[2], "/", uniq.group[1], ")", sep = "")
write.table(okk, "result.txt", sep = "\t", quote = F, row.names = F)
