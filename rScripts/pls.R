# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/3/26
da <- read.table(quote="","deal.txt", sep = "\t", head = T, check.names = FALSE)
rownames(da) <- as.character(da[, 1])
da <- da[, - 1]
da <- t(da)

sd <- read.table(quote="","group.txt", head = T, sep = "\t", comment.char = "", check.names = FALSE)
rownames(sd) <- as.character(sd[, 1])
sd[, 1] <- as.character(sd[, 1])
sd$Group <- as.character(sd$Group)
da <- da[as.character(sd[, 1]),]
da <- da[, apply(da, 2, function(x)any(x > 0))]

library(mixOmics)
num <- length(unique(sd$Group))
plsda.otu <- plsda(da, sd$Group, ncomp = num, near.zero.var = T)
pls.sites <- plsda.otu$variates$X
pls.rotat <- plsda.otu$loadings$X
pls.impo <- plsda.otu$loadings$Y
write.table(pls.sites, "site.txt", sep = "\t",quote = F,col.names = NA)
write.table(pls.rotat, "ro.txt", sep = "\t",quote = F,col.names = NA)
write.table(pls.impo, "impo.txt", sep = "\t",quote = F,col.names = NA)

vip_otu <- vip(plsda.otu)
#write.table(vip_otu,\"__vip__\",sep=\"\t\")