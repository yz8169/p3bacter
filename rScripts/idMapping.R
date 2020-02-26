# Title     : TODO
# Objective : TODO
# Created by: yz
# Created on: 2018/6/8
library(optparse)
option_list <- list(
make_option("--t", type = 'character', action = "store", default = "name", help = "input metabolite ID type"),
make_option("--d", type = 'character', action = "store", default = "", help = "the database direcotry")
)
opt <- parse_args(OptionParser(option_list = option_list))

dataSet <- list();
dataSet$type <- "conc";
dataSet$design.type <- "regular"; # one factor to two factor
dataSet$cls.type <- "disc"; # default until specified otherwise
dataSet$format <- "rowu";
dataSet$paired <- FALSE;
analSet <- list();
analSet$type <- "pathora";
imgSet <- list();
msg.vec <- vector(mode = "character");

current.msetlib <- NULL;
cachexia.set.used <- FALSE;
conc.db <- NULL;

# record the current name(s) to be transferred to client
library('Cairo'); # plotting required by all

# fix Mac font issue
CairoFonts("Arial:style=Regular", "Arial:style=Bold", "Arial:style=Italic", "Helvetica", "Symbol")

print("R objects intialized ...");

# record all the data
if (! exists("name.map")) {
    name.map <- list();
}
data <- read.table("input.txt", header = F, sep = "\t", quote = "", check.names = F)
vc = as.vector(data$V1)
dataSet$cmpd <- vc
qvec <- dataSet$cmpd;
direcotry = opt$d

# variables to record results
hit.inx = vector(mode = 'numeric', length = length(qvec)); # record hit index, initial 0
match.values = vector(mode = 'character', length = length(qvec)); # the best matched values (hit names), initial ""
match.state = vector(mode = 'numeric', length = length(qvec));  # match status - 0, no match; 1, exact match; initial 0

cmpd.db <- readRDS(paste(direcotry, "/libs/compound_db.rds", sep = ""));

metaboliteIDType <- opt$t
q.type <- metaboliteIDType
dataSet$q.type <- metaboliteIDType

if (q.type == "hmdb") {
    hit.inx <- match(tolower(qvec), tolower(cmpd.db$hmdb));
    match.values <- cmpd.db$name[hit.inx];
    match.state[! is.na(hit.inx)] <- 1;
}else if (q.type == "pubchem") {
    hit.inx <- match(tolower(qvec), tolower(cmpd.db$pubchem));
    match.values <- cmpd.db$name[hit.inx];
    match.state[! is.na(hit.inx)] <- 1;
}else if (q.type == "chebi") {
    hit.inx <- match(tolower(qvec), tolower(cmpd.db$chebi));
    match.values <- cmpd.db$name[hit.inx];
    match.state[! is.na(hit.inx)] <- 1;
}else if (q.type == "metlin") {
    hit.inx <- match(tolower(qvec), tolower(cmpd.db$metlin));
    match.values <- cmpd.db$name[hit.inx];
    match.state[! is.na(hit.inx)] <- 1;
}else if (q.type == "kegg") {
    hit.inx <- match(tolower(qvec), tolower(cmpd.db$kegg));
    hit.inx2 <- match(tolower(qvec), rev(tolower(cmpd.db$kegg)));

    # unique hits
    nonuniq.hits <- hit.inx + hit.inx2 != nrow(cmpd.db) + 1;
    hit.inx[nonuniq.hits] <- NA;
    match.values <- cmpd.db$name[hit.inx];
    match.state[! is.na(hit.inx)] <- 1;
}else if (q.type == "name") {
    # first find exact match to the common compound names
    hit.inx <- match(tolower(qvec), tolower(cmpd.db$name));
    match.values <- cmpd.db$name[hit.inx];
    match.state[! is.na(hit.inx)] <- 1;

    # then try to find exact match to synanyms for the remaining unmatched query names one by one
    syn.db <- readRDS(paste(direcotry, "/libs/syn_nms.rds", sep = ""))
    syns.list <- syn.db$syns.list;
    todo.inx <- which(is.na(hit.inx));
    if (length(todo.inx) > 0) {
        for (i in 1 : length(syns.list)) {
            syns <- syns.list[[i]];
            hitInx <- match(tolower(qvec[todo.inx]), tolower(syns));

            hitPos <- which(! is.na(hitInx));
            if (length(hitPos) > 0) {
                # record matched ones
                orig.inx <- todo.inx[hitPos];
                hit.inx[orig.inx] <- i;
                # match.values[orig.inx] <- syns[hitInx[hitPos]];  # show matched synnames
                match.values[orig.inx] <- cmpd.db$name[i];    # show common name
                match.state[orig.inx] <- 1;

                # update unmatched list
                todo.inx <- todo.inx[is.na(hitInx)];
            }
            if (length(todo.inx) == 0)break;
        }
    }
}else {
    print(paste("Unknown compound ID type:", q.type));
    # guess a mix of kegg and hmdb ids
    hit.inx <- match(tolower(qvec), tolower(cmpd.db$hmdb));
    hit.inx2 <- match(tolower(qvec), tolower(cmpd.db$kegg));
    nohmdbInx <- is.na(hit.inx);
    hit.inx[nohmdbInx] <- hit.inx2[nohmdbInx]
}
# empty memory
gc();

name.map$hit.inx <- hit.inx;
name.map$hit.values <- match.values;
name.map$match.state <- match.state;

return.cols <- c(T, T, F, T, F);
qvec <- dataSet$cmpd;
if (is.null(qvec)) {
    return();
}
# style for highlighted background for unmatched names
pre.style <- NULL;
post.style <- NULL;

# style for no matches
if (dataSet$q.type == "name") {
    no.prestyle <- "<strong style=\"background-color:yellow; font-size=125%; color=\"black\">";
    no.poststyle <- "</strong>";
}else {
    no.prestyle <- "<strong style=\"background-color:red; font-size=125%; color=\"black\">";
    no.poststyle <- "</strong>";
}

hit.inx <- name.map$hit.inx;
hit.values <- name.map$hit.values;
match.state <- name.map$match.state;

# contruct the result table with cells wrapped in html tags
# the unmatched will be highlighted in different background
html.res <- matrix("", nrow = length(qvec), ncol = 8);
csv.res <- matrix("", nrow = length(qvec), ncol = 8);
colnames(csv.res) <- c("Query", "Match", "HMDB", "PubChem", "ChEBI", "KEGG", "METLIN", "Comment");
cmpd.db <- readRDS(paste(direcotry, "/libs/compound_db.rds", sep = ""));
for (i in 1 : length(qvec)) {
    if (match.state[i] == 1) {
        pre.style <- "";
        post.style = "";
    }else { # no matches
        pre.style <- no.prestyle;
        post.style <- no.poststyle;
    }
    hit <- cmpd.db[hit.inx[i], , drop = F];
    html.res[i,] <- c(paste(pre.style, qvec[i], post.style, sep = ""),
    paste(ifelse(match.state[i] == 0, "", hit.values[i]), sep = ""),
    paste(ifelse(match.state[i] == 0 ||
        is.na(hit$hmdb_id) ||
        hit$hmdb_id == "" ||
        hit$hmdb_id == "NA", "-", paste("<a href=http://www.hmdb.ca/metabolites/", hit$hmdb_id, " target='_blank'>", hit$hmdb_id, "</a>", sep = "")), sep = ""),
    paste(ifelse(match.state[i] == 0 ||
        is.na(hit$pubchem_id) ||
        hit$pubchem_id == "" ||
        hit$pubchem_id == "NA", "-", paste("<a href=http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid=", hit$pubchem_id, " target='_blank'>", hit$pubchem_id, "</a>", sep = "")), sep = ""),
    paste(ifelse(match.state[i] == 0 ||
        is.na(hit$chebi_id) ||
        hit$chebi_id == "" ||
        hit$chebi_id == "NA", "-", paste("<a href=http://www.ebi.ac.uk/chebi/searchId.do?chebiId=", hit$chebi_id, " target='_blank'>", hit$chebi_id, "</a>", sep = "")), sep = ""),
    paste(ifelse(match.state[i] == 0 ||
        is.na(hit$kegg_id) ||
        hit$kegg_id == "" ||
        hit$kegg_id == "NA", "-", paste("<a href=http://www.genome.jp/dbget-bin/www_bget?", hit$kegg_id, " target='_blank'>", hit$kegg_id, "</a>", sep = "")), sep = ""),
    paste(ifelse(match.state[i] == 0 ||
        is.na(hit$metlin_id) ||
        hit$metlin_id == "" ||
        hit$metlin_id == "NA", "-", paste("<a href=http://metlin.scripps.edu/metabo_info.php?molid=", hit$metlin_id, " target='_blank'>", hit$metlin_id, "</a>", sep = "")), sep = ""),
    ifelse(match.state[i] != 1, "View", ""));
    csv.res[i,] <- c(qvec[i],
    ifelse(match.state[i] == 0, "NA", hit.values[i]),
    ifelse(match.state[i] == 0, "NA", hit$hmdb_id),
    ifelse(match.state[i] == 0, "NA", hit$pubchem_id),
    ifelse(match.state[i] == 0, "NA", hit$chebi_id),
    ifelse(match.state[i] == 0, "NA", hit$kegg_id),
    ifelse(match.state[i] == 0, "NA", hit$metlin_id),
    match.state[i]);
}
# return only columns user selected

# add query and match columns at the the beginning, and 'Detail' at the end
return.cols <- c(TRUE, TRUE, return.cols, TRUE);
html.res <- html.res[, return.cols, drop = F];
csv.res <- csv.res[, return.cols, drop = F];

# store the value for report
dataSet$map.table <- csv.res;
write.table(csv.res, file = "kegg.txt", row.names = F, sep = "\t", quote = F);