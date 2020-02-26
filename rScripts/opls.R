# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/3/20

library(optparse)
option_list <- list(
make_option("--v", type = 'character', action = "store", default = "1", help = "vip Cutoff"),
make_option("--l", type = 'logical', action = "store", default = "F", help = "log 10 transform"),
make_option("--s", type = 'character', action = "store", default = "pareto", help = "Scaling"),
make_option("--p", type = 'integer', action = "store", default = "20", help = "Number of permutations"),
make_option("--t", type = 'character', action = "store", default = "summary", help = "Graphic type"),
make_option("--pa", type = 'logical', action = "store", default = "T", help = "Group ecllipse "),
make_option("--par", type = 'numeric', action = "store", default = "0.8", help = "Amount by which plotting text should be magnified relative to the default "),
make_option("--lo", type = 'numeric', action = "store", default = "1", help = "logFC threshold")
)
opt <- parse_args(OptionParser(option_list = option_list))

xMN <- read.table(quote="","deal.txt",
check.names = FALSE,
header = TRUE,
row.names = 1,
sep = "\t",
comment.char = "")

tmpDf = xMN

samDF <- read.table(quote="","group.txt",
check.names = FALSE,
header = T,
row.names = 1,
com = '',
sep = "\t")
sampleNames = rownames(samDF)
sampleSize = length(sampleNames)

xMN = xMN[rownames(samDF)]
xMN = t(xMN)
yMCN <- matrix(samDF[, "Group"], ncol = 1, dimnames = list(rownames(xMN), "Group"))
predI = 1
algoC = "default"
#can set value
orthoI = NA
crossvalI = sampleSize
log10L = opt$l
scaleC = opt$s
permI = opt$p

library(ropls)

ropLs <- opls(x = xMN,
y = yMCN,
predI = predI,
orthoI = orthoI,
algoC = algoC ,
crossvalI = crossvalI,
log10L = log10L,
permI = permI,
scaleC = scaleC,
subset = NULL,
printL = FALSE,
plotL = FALSE)

modC <- ropLs@typeC
sumDF <- getSummaryDF(ropLs)
desMC <- ropLs@descriptionMC
scoreMN <- getScoreMN(ropLs)
loadingMN <- getLoadingMN(ropLs)

vipVn <- coeMN <- orthoScoreMN <- orthoLoadingMN <- orthoVipVn <- NULL
vipVn <- getVipVn(ropLs)
coeMN <- coef(ropLs)

orthoScoreMN <- getScoreMN(ropLs, orthoL = TRUE)
orthoLoadingMN <- getLoadingMN(ropLs, orthoL = TRUE)
orthoVipVn <- getVipVn(ropLs, orthoL = TRUE)
parCompVi <- c(1, 2)
#plot
parAsColFcVn = NA
parCexN = opt$par
parEllipsesL = opt$pa
parLabVc = NA
ploC = opt$t

plot(ropLs,
typeVc = ploC,
parAsColFcVn = parAsColFcVn,
parCexN = parCexN,
parCompVi = parCompVi,
parEllipsesL = parEllipsesL,
parLabVc = parLabVc,
file.pdfC = "figure.pdf")

rspModC <- gsub("-", "", modC)
rspModC <- paste0("", rspModC)

if (sumDF[, "pre"] + sumDF[, "ort"] < 2) {
    tCompMN <- scoreMN
    pCompMN <- loadingMN
} else {
    if (sumDF[, "ort"] > 0) {
        if (parCompVi[2] > sumDF[, "ort"] + 1)
        stop("Selected orthogonal component for plotting (ordinate) exceeds the total number of orthogonal components of the model", call. = FALSE)
        tCompMN <- cbind(scoreMN[, 1], orthoScoreMN[, parCompVi[2] - 1])
        pCompMN <- cbind(loadingMN[, 1], orthoLoadingMN[, parCompVi[2] - 1])
        colnames(pCompMN) <- colnames(tCompMN) <- c("h1", paste("o", parCompVi[2] - 1, sep = ""))
    } else {
        if (max(parCompVi) > sumDF[, "pre"])
        stop("Selected component for plotting as ordinate exceeds the total number of predictive components of the model", call. = FALSE)
        tCompMN <- scoreMN[, parCompVi, drop = FALSE]
        pCompMN <- loadingMN[, parCompVi, drop = FALSE]
    }
}

## x-scores and prediction

colnames(tCompMN) <- paste0(rspModC, "_XSCOR-", colnames(tCompMN))
tCompDF <- as.data.frame(tCompMN)[rownames(samDF), , drop = FALSE]

tesVl <- NULL
fitMCN <- fitted(ropLs)

colnames(fitMCN) <- paste0(rspModC,
"_predictions")
fitDF <- as.data.frame(fitMCN)[rownames(samDF), , drop = FALSE]
tCompDF <- cbind.data.frame(tCompDF, fitDF)
samDF <- cbind.data.frame(samDF, tCompDF)

## x-loadings and VIP
colnames(pCompMN) <- paste0(rspModC, "_XLOAD-", colnames(pCompMN))
if (! is.null(vipVn)) {
    pCompMN <- cbind(pCompMN, vipVn)
    colnames(pCompMN)[ncol(pCompMN)] <- paste0(rspModC,
    "_VIP",
    ifelse(! is.null(orthoVipVn),
    "_pred",
    ""))
    if (! is.null(orthoVipVn)) {
        pCompMN <- cbind(pCompMN, orthoVipVn)
        colnames(pCompMN)[ncol(pCompMN)] <- paste0(rspModC,
        "_VIP_ortho")
    }
}
if (! is.null(coeMN)) {
    pCompMN <- cbind(pCompMN, coeMN)
    if (ncol(coeMN) == 1)
    colnames(pCompMN)[ncol(pCompMN)] <- paste0(rspModC, "_COEFF")
    else
    colnames(pCompMN)[(ncol(pCompMN) - ncol(coeMN) + 1) : ncol(pCompMN)] <- paste0(rspModC, "_", colnames(coeMN), "-COEFF")
}
pCompDF <- as.data.frame(pCompMN)[]

std <- function(x) sd(x) / sqrt(length(x))
data = tmpDf
uniq.group <- as.character(unique(samDF$Group))
group1 <- rownames(subset(samDF, Group == uniq.group[1]))
group2 <- rownames(subset(samDF, Group == uniq.group[2]))
group1 <- as.character(group1)
group2 <- as.character(group2)
mean1 = apply(data[, group1], 1, mean)
okk = as.data.frame(mean1)
rownames(okk) = rownames(data)
okk$std1 = apply(data[, group1], 1, std)
okk$mean2 = apply(data[, group2], 1, mean)
okk$std2 = apply(data[, group2], 1, std)
okk$logFC = log2(okk$mean2 / okk$mean1)



## sampleMetadata
write.table(samDF,
file = "sampleOut.txt",
quote = FALSE,
row.names = T,
col.names = NA,
sep = "\t")


outDf = cbind(okk, pCompDF,data[, group1],data[, group2])
tmpNames=names(outDf[,group1])
names=paste(tmpNames,"(",uniq.group[1],")",sep="")
names(outDf)[which(names(outDf) %in% tmpNames)]=names
tmpNames=names(outDf[,group2])
names=paste(tmpNames,"(",uniq.group[2],")",sep="")
names(outDf)[which(names(outDf) %in% tmpNames)]=names

outDf = subset(outDf, OPLSDA_VIP_pred >= opt$v)
names(outDf)[names(outDf) == "mean1"] = paste("mean(", uniq.group[1], ")", sep = "")
names(outDf)[names(outDf) == "std1"] = paste("stderr(", uniq.group[1], ")", sep = "")
names(outDf)[names(outDf) == "mean2"] = paste("mean(", uniq.group[2], ")", sep = "")
names(outDf)[names(outDf) == "std2"] = paste("stderr(", uniq.group[2], ")", sep = "")
outDf = subset(outDf, abs(logFC) > opt$lo)
names(outDf)[names(outDf) == "logFC"] = paste("log2FC(", uniq.group[2], "/", uniq.group[1], ")", sep = "")

write.table(outDf,
file = "dataOut.txt",
quote = FALSE,
row.names = T,
col.names = NA,
sep = "\t")


