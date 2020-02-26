# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/3/27

library(biosigner)

library(optparse)
option_list <- list(
make_option("--m", type = 'character', action = "store", default = "all", help = "Classification method(s)"),
make_option("--b", type = 'integer', action = "store", default = "5", help = "Number of bootstraps"),
make_option("--t", type = 'character', action = "store", default = "S", help = "Number of bootstraps"),
make_option("--p", type = 'numeric', action = "store", default = "0.05", help = "p-value threshold"),
make_option("--s", type = 'integer', action = "store", default = "123", help = "Select an integer (e.g., 123)
if you want to obtain exactly the same signatures when re-running the algorithm; 0 means that no seed is selected"),
make_option("--l", type = 'numeric', action = "store", default = "1", help = "logFC threshold")
)
opt <- parse_args(OptionParser(option_list = option_list))

xMN <- read.table(quote="","deal.txt",
check.names = FALSE,
header = TRUE,
row.names = 1,
sep = "\t")

tmpDf = xMN

samDF <- read.table(quote="","group.txt",
check.names = FALSE,
header = T,
row.names = 1,
com = '',
sep = "\t")

std <- function(x) sd(x) / sqrt(length(x))
uniq.group <- as.character(unique(samDF$Group))
group1 <- rownames(subset(samDF, Group == uniq.group[1]))
group2 <- rownames(subset(samDF, Group == uniq.group[2]))
group1 <- as.character(group1)
group2 <- as.character(group2)
data = tmpDf
mean1 = apply(data[, group1], 1, mean)
okk = as.data.frame(mean1)
rownames(okk) = rownames(data)
okk$std1 = apply(data[, group1], 1, std)
okk$mean2 = apply(data[, group2], 1, mean)
okk$std2 = apply(data[, group2], 1, std)
okk$logFC = log2(okk$mean2 / okk$mean1)

names(okk)[names(okk) == "mean1"] = paste("mean(", uniq.group[1], ")", sep = "")
names(okk)[names(okk) == "std1"] = paste("stderr(", uniq.group[1], ")", sep = "")
names(okk)[names(okk) == "mean2"] = paste("mean(", uniq.group[2], ")", sep = "")
names(okk)[names(okk) == "std2"] = paste("stderr(", uniq.group[2], ")", sep = "")


xMN = xMN[rownames(samDF)]
xMN = t(xMN)

respC = "Group"
tierMaxC <- opt$t
pvalN <- opt$p
seedI = opt$s
methodVc = opt$m
bootI = opt$b

respVc <- samDF[, respC]
respFc <- factor(respVc)
set.seed(seedI)
bsnLs <- biosign(x = xMN,
y = respFc,
methodVc = methodVc,
bootI = bootI,
pvalN = pvalN,
printL = FALSE,
plotL = FALSE)


set.seed(NULL)
tierMC <- bsnLs@tierMC
if (! is.null(tierMC)) {
    plot(bsnLs,
    tierMaxC = tierMaxC,
    file.pdfC = "tier.pdf")
    plot(bsnLs,
    tierMaxC = tierMaxC,
    typeC = "boxplot",
    file.pdfC = "boxplot.pdf")
} else {
    pdf("tier.pdf")
    plot(1, bty = "n", type = "n",
    xaxt = "n", yaxt = "n", xlab = "", ylab = "")
    text(mean(par("usr")[1 : 2]), mean(par("usr")[3 : 4]),
    labels = "No significant variable to display")
    dev.off()
    pdf("boxplot.pdf")
    plot(1, bty = "n", type = "n",
    xaxt = "n", yaxt = "n", xlab = "", ylab = "")
    text(mean(par("usr")[1 : 2]), mean(par("usr")[3 : 4]),
    labels = "No significant variable to display")
    dev.off()
}
tierFullVc <- c("S", LETTERS[1 : 5])
tierVc <- tierFullVc[1 : which(tierFullVc == tierMaxC)]


if (! is.null(tierMC)) {
    tierDF <- data.frame(tier = sapply(rownames(tmpDf),
    function(varC) {
        varTirVc <- tierMC[varC,]
        varTirVc <- names(varTirVc)[varTirVc %in% tierVc]
        paste(varTirVc, collapse = "|")
    }),
    stringsAsFactors = FALSE)
    colnames(tierDF) <- paste(respC,
    colnames(tierDF),
    paste(tierVc, collapse = ""),
    sep = "_")
}


outDf = cbind(okk, tierDF,data[, group1],data[, group2])
tmpNames=names(outDf[,group1])
names=paste(tmpNames,"(",uniq.group[1],")",sep="")
names(outDf)[which(names(outDf) %in% tmpNames)]=names
tmpNames=names(outDf[,group2])
names=paste(tmpNames,"(",uniq.group[2],")",sep="")
names(outDf)[which(names(outDf) %in% tmpNames)]=names

lastName = colnames(tierDF)
outDf = outDf[outDf[[lastName]] != "",]
outDf = subset(outDf, abs(logFC) > opt$l)
names(outDf)[names(outDf) == "logFC"] = paste("log2FC(", uniq.group[2], "/", uniq.group[1], ")", sep = "")
write.table(outDf,
file = "out.txt",
quote = FALSE,
row.names = T,
col.names = NA,
sep = "\t")


