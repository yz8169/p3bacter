#!/usr/bin/env Rscript

library(batch) ## parseCommandArgs

if(!('parAsColC' %in% names(argVc)))
    argVc["parAsColC"] <- "none"
flgF("argVc['parAsColC'] == 'none' || argVc['parAsColC'] %in% colnames(samDF)", txtC = paste0("Sample color argument (", argVc['parAsColC'], ") must be either none or one of the column names (first row) of your sample metadata"))

if(!('parMahalC' %in% names(argVc)) || argVc["parMahalC"] == "NA") {
    if(!is.null(yMCN) && ncol(yMCN) == 1 && mode(yMCN) == "character")
        argVc["parMahalC"] <- argVc["respC"]
    else
        argVc["parMahalC"] <- "none"
}
flgF("argVc['parMahalC'] == 'none' || (argVc['parMahalC'] %in% colnames(samDF))",
     txtC = paste0("Mahalanobis argument (", argVc['parMahalC'], ") must be either 'NA', 'none' or one of the column names (first row) of your sample metadata"))
if(argVc["parMahalC"] == "none") {
    parEllipsesL <- FALSE
} else {
    if(is.null(yMCN)) { ## PCA case
        flgF("mode(samDF[, argVc['parMahalC']]) == 'character'",
             txtC = paste0("Mahalanobis argument (", argVc['parMahalC'], ") must correspond to a column of characters in your sampleMetadata"))
        parAsColFcVn <- factor(samDF[, argVc["parMahalC"]])
        parEllipsesL <- TRUE
    } else { ## (O)PLS-DA case
        flgF("identical(as.character(argVc['respC']), as.character(argVc['parMahalC']))",
             txtC = paste0("The Mahalanobis argument (", argVc['parMahalC'], ") must be identical to the Y response argument (", argVc['respC'], ")"))
        parEllipsesL <- TRUE
    }
}

if(!('parLabVc' %in% names(argVc)))
    argVc["parLabVc"] <- "none"
flgF("argVc['parLabVc'] == 'none' || (argVc['parLabVc'] %in% colnames(samDF))",
     txtC = paste0("Sample labels argument (", argVc['parLabVc'], ") must be either none or one of the column names (first row) of your sample metadata"))
if('parLabVc' %in% names(argVc))
    if(argVc["parLabVc"] != "none") {
        flgF("mode(samDF[, argVc['parLabVc']]) == 'character'",
             txtC = paste0("The sample label argument (", argVc['parLabVc'], ") must correspond to a sample metadata column of characters (not numerics)"))
        parLabVc <- samDF[, argVc['parLabVc']]
    } else
        parLabVc <- NA

if('parPc1I' %in% names(argVc)) {
    parCompVi <-  as.numeric(c(argVc["parPc1I"], argVc["parPc2I"]))
} else
    parCompVi <- c(1, 2)



modC <- ropLs@typeC
sumDF <- getSummaryDF(ropLs)
desMC <- ropLs@descriptionMC
scoreMN <- getScoreMN(ropLs)
loadingMN <- getLoadingMN(ropLs)


vipVn <- coeMN <- orthoScoreMN <- orthoLoadingMN <- orthoVipVn <- NULL

if(grepl("PLS", modC)) {

    vipVn <- getVipVn(ropLs)
    coeMN <- coef(ropLs)

    if(grepl("OPLS", modC)) {
        orthoScoreMN <- getScoreMN(ropLs, orthoL = TRUE)
        orthoLoadingMN <- getLoadingMN(ropLs, orthoL = TRUE)
        orthoVipVn <- getVipVn(ropLs, orthoL = TRUE)
    }

}

ploC <- ifelse('typeC' %in% names(argVc), argVc["typeC"], "summary")

if(sumDF[, "pre"] + sumDF[, "ort"] < 2) {
    if(!(ploC %in% c("permutation", "overview"))) {
        ploC <- "summary"
        plotWarnL <- TRUE
    }
} else
    plotWarnL <- FALSE



options(warn = optWrnN)


##------------------------------
## Print
##------------------------------


sink(argVc["information"], append = TRUE)

if(plotWarnL)
    cat("\nWarning: For single component models, only 'overview' (and 'permutation' in case of single response (O)PLS(-DA)) plot(s) are available\n", sep = "")


cat("\n", modC, "\n", sep = "")

cat("\n", desMC["samples", ],
    " samples x ",
    desMC["X_variables", ],
    " variables",
    ifelse(modC != "PCA",
           " and 1 response",
           ""),
    "\n", sep = "")

cat("\n", ropLs@suppLs[["scaleC"]], " scaling of dataMatrix",
            ifelse(modC == "PCA",
                   "",
                   paste0(" and ",
                          ifelse(mode(ropLs@suppLs[["yMCN"]]) == "character" && ropLs@suppLs[["scaleC"]] != "standard",
                                 "standard scaling of ",
                                 ""),
                          "response\n")), sep = "")

if(substr(desMC["missing_values", ], 1, 1) != "0")
    cat("\n", desMC["missing_values", ], " NAs\n", sep = "")

if(substr(desMC["near_zero_excluded_X_variables", ], 1, 1) != "0")
    cat("\n", desMC["near_zero_excluded_X_variables", ],
        " excluded variables during model building (because of near zero variance)\n", sep = "")

cat("\n")

optDigN <- options()[["digits"]]
options(digits = 3)
print(ropLs@modelDF)
options(digits = optDigN)


rspModC <- gsub("-", "", modC)
if(rspModC != "PCA")
    rspModC <- paste0(make.names(argVc['respC']), "_", rspModC)

if(sumDF[, "pre"] + sumDF[, "ort"] < 2) {

    tCompMN <- scoreMN
    pCompMN <- loadingMN

} else {

    if(sumDF[, "ort"] > 0) {
        if(parCompVi[2] > sumDF[, "ort"] + 1)
            stop("Selected orthogonal component for plotting (ordinate) exceeds the total number of orthogonal components of the model", call. = FALSE)
        tCompMN <- cbind(scoreMN[, 1], orthoScoreMN[, parCompVi[2] - 1])
        pCompMN <- cbind(loadingMN[, 1], orthoLoadingMN[, parCompVi[2] - 1])
        colnames(pCompMN) <- colnames(tCompMN) <- c("h1", paste("o", parCompVi[2] - 1, sep = ""))
    } else {
        if(max(parCompVi) > sumDF[, "pre"])
            stop("Selected component for plotting as ordinate exceeds the total number of predictive components of the model", call. = FALSE)
        tCompMN <- scoreMN[, parCompVi, drop = FALSE]
        pCompMN <- loadingMN[, parCompVi, drop = FALSE]
    }

}

## x-scores and prediction






## x-loadings and VIP

colnames(pCompMN) <- paste0(rspModC, "_XLOAD-", colnames(pCompMN))
if(!is.null(vipVn)) {
    pCompMN <- cbind(pCompMN, vipVn)
    colnames(pCompMN)[ncol(pCompMN)] <- paste0(rspModC,
                                               "_VIP",
                                               ifelse(!is.null(orthoVipVn),
                                                      "_pred",
                                                      ""))
    if(!is.null(orthoVipVn)) {
        pCompMN <- cbind(pCompMN, orthoVipVn)
        colnames(pCompMN)[ncol(pCompMN)] <- paste0(rspModC,
                                                   "_VIP_ortho")
    }
}
if(!is.null(coeMN)) {
    pCompMN <- cbind(pCompMN, coeMN)
    if(ncol(coeMN) == 1)
        colnames(pCompMN)[ncol(pCompMN)] <- paste0(rspModC, "_COEFF")
    else
        colnames(pCompMN)[(ncol(pCompMN) - ncol(coeMN) + 1):ncol(pCompMN)] <- paste0(rspModC, "_", colnames(coeMN), "-COEFF")
}
pCompDF <- as.data.frame(pCompMN)[rownames(varDF), , drop = FALSE]
varDF <- cbind.data.frame(varDF, pCompDF)

## sampleMetadata




## variableMetadata

varDF <- cbind.data.frame(variableMetadata = rownames(varDF),
                          varDF)


# Output ropLs
if (!is.null(argVc['ropls_out']) && !is.na(argVc['ropls_out']))
    save(ropLs, file = argVc['ropls_out'])

## Closing
##--------

cat("\nEnd of '", modNamC, "' Galaxy module call: ",
    as.character(Sys.time()), "\n", sep = "")

cat("\n\n\n============================================================================")
cat("\nAdditional information about the call:\n")
cat("\n1) Parameters:\n")
print(cbind(value = argVc))

cat("\n2) Session Info:\n")
sessioninfo <- sessionInfo()
cat(sessioninfo$R.version$version.string,"\n")
cat("Main packages:\n")
for (pkg in names(sessioninfo$otherPkgs)) { cat(paste(pkg,packageVersion(pkg)),"\t") }; cat("\n")
cat("Other loaded packages:\n")
for (pkg in names(sessioninfo$loadedOnly)) { cat(paste(pkg,packageVersion(pkg)),"\t") }; cat("\n")

cat("============================================================================\n")

sink()

options(stringsAsFactors = strAsFacL)

rm(list = ls())
