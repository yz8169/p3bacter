# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/3/1

library(optparse)
option_list <- list(
make_option("--d", type = 'character', action = "store", default = "", help = "the database direcotry"),
make_option("--l", type = 'character', action = "store", default = "hsa", help = "input pathway library(eg.hsa)"),
make_option("--m", type = 'character', action = "store", default = "hyperg", help = "Please specify Over Representation Analysis algorithms"),
make_option("--t", type = 'character', action = "store", default = "rbc", help = "Please specify Pathway Topology Analysis algorithms"),
make_option("--r", type = 'character', action = "store", default = "all", help = "Please specify a reference metabolome")
)
opt <- parse_args(OptionParser(option_list = option_list))

GetFisherPvalue <- function(numSigMembers, numSigAll, numMembers, numAllMembers){
    z <- cbind(numSigMembers, numSigAll - numSigMembers, numMembers - numSigMembers, numAllMembers - numMembers - numSigAll + numSigMembers);
    z <- lapply(split(z, 1 : nrow(z)), matrix, ncol = 2);
    z <- lapply(z, fisher.test, alternative = 'greater');
    p.values <- as.numeric(unlist(lapply(z, "[[", "p.value")));
    return(p.values);
}

kegg.rda = opt$l
TorF = if (opt$r == "all")FALSE else TRUE
nameMapFile = "kegg.txt"
nodeImp = opt$t
method = opt$m
filePath = if (opt$r == "all")"F" else "list.txt"
analSet <- list()
name.map <- list()
direcotry = opt$d

path.dir <- paste (direcotry, "/libs/kegg/", kegg.rda, ".rda", sep = "")
load(path.dir, .GlobalEnv)
use.metabo.filter <- TorF
nm.map <- read.delim(nameMapFile, header = T, sep = "\t", quote = "", check.names = F)
nm.mat <- nm.map[c(1, 2, 5)]
colnames(nm.mat) <- c("query", "hmdb", "kegg");
nm.map <- as.data.frame(nm.mat)
nm.map = subset(nm.map, kegg != "")
valid.inx <- ! (is.na(nm.map$kegg) | duplicated(nm.map$kegg));
ora.vec <- nm.map$kegg[valid.inx];
q.size <- length(ora.vec);
if (is.na(ora.vec) || q.size == 0) {
    return(0);
}
library(KEGGgraph);
current.mset <- metpa$mset.list;
uniq.count <- metpa$uniq.count;
if (filePath != "F") {
    inFile <- file(filePath, "r");
    ref.vec <- try(scan(inFile, 'character', strip.white = T, sep = "\n")); # must be single column
    close(inFile);
    libCheck.msg <- NULL;
    if (class(ref.vec) == "try-error") {
        libCheck.msg <- c(libCheck.msg, "Data format error - fail to read in the data!");
        print(libCheck.msg);
        return(0);
    }
    cmpd.db <- readRDS(paste(direcotry, "/libs/compound_db.rds", sep = ""));
    hits <- tolower(ref.vec) %in% tolower(cmpd.db$kegg_id);
    metabo.filter.kegg <<- ref.vec[hits];
}
if (use.metabo.filter && exists('metabo.filter.kegg')) {
    current.mset <- lapply(current.mset, function(x){x[x %in% metabo.filter.kegg]});
    analSet$ora.filtered.mset <- current.mset;
    uniq.count <- length(unique(unlist(current.mset, use.names = FALSE)));
}
hits <- lapply(current.mset, function(x){x[x %in% ora.vec]});
hit.num <- unlist(lapply(hits, function(x){length(x)}), use.names = FALSE);
set.size <- length(current.mset);
set.num <- unlist(lapply(current.mset, length), use.names = FALSE);
res.mat <- matrix(0, nrow = set.size, ncol = 8);
rownames(res.mat) <- names(current.mset);
colnames(res.mat) <- c("Total", "Expected", "Hits", "Raw p", "-log(p)", "Holm adjust", "FDR", "Impact");
if (nodeImp == "rbc") {
    imp.list <- metpa$rbc;
}else {
    imp.list <- metpa$dgr;
}
res.mat[, 1] <- set.num;
res.mat[, 2] <- q.size * (set.num / uniq.count);
res.mat[, 3] <- hit.num;
if (method == "fisher") {
    res.mat[, 4] <- GetFisherPvalue(hit.num, q.size, set.num, uniq.count);
}else {
    res.mat[, 4] <- phyper(hit.num - 1, set.num, uniq.count - set.num, q.size, lower.tail = F);
}
res.mat[, 5] <- - log(res.mat[, 4]);
res.mat[, 6] <- p.adjust(res.mat[, 4], "holm");
res.mat[, 7] <- p.adjust(res.mat[, 4], "fdr");
res.mat[, 8] <- mapply(function(x, y){sum(x[y])}, imp.list, hits);
res.mat <- res.mat[hit.num > 0,];
ord.inx <- order(res.mat[, 4], res.mat[, 8]);
analSet$ora.mat <- signif(res.mat[ord.inx,], 5);
analSet$ora.hits <- hits;
analSet$node.imp <- nodeImp;
save.mat <- analSet$ora.mat;
hit.inx <- match(rownames(analSet$ora.mat), metpa$path.ids);
rownames(save.mat) <- names(metpa$path.ids)[hit.inx]
extraKegg <- c()
redNames <- c()
setName <- names(metpa$path.ids)[hit.inx]
for (msetNm in setName) {
    pathid <- metpa$path.ids[msetNm];
    mset <- metpa$mset.list[[pathid]];
    hits <- analSet$ora.hits;
    red.inx <- which(mset %in% hits[[pathid]]);
    cName = mset[red.inx]
    nms <- names(mset);
    eachRowkeggID <- c()
    for (value in nms[red.inx]) {
        keggId = cName[value]
        eachRowkeggID <- c(eachRowkeggID, keggId)
    }
    eachRowkegg <- paste("/", eachRowkeggID, "%09red", collapse = "", sep = "")
    extraKegg <- c(extraKegg, eachRowkegg)
    redName <- paste(nms[red.inx], collapse = "|")
    redNames <- c(redNames, redName)
}
save.mat <- cbind(save.mat, redNames)
kegg.vec <- rownames(analSet$ora.mat);
kegg.vec <- paste("http://www.genome.jp/kegg-bin/show_pathway?", kegg.vec, extraKegg, sep = "");
save.mat <- cbind(save.mat, kegg.vec)
colnames(save.mat) <- c("Total", "Expected", "Hits", "Raw p", "-log(p)", "Holm adjust", "FDR", "Impact", "EnrichCompound", "Hyperlink")
write.table(save.mat, file = "out.txt", row.names = T, sep = "\t", quote = F, col.names = NA);
