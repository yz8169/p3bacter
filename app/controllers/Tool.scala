package controllers

import java.io.File
import java.nio.file.Files

import dao.{GroupDao, ModeDao, ProjectDao}
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats.doubleFormat
import play.api.mvc.{Request, RequestHeader}
import utils.Utils
import scala.collection.JavaConverters._
import models.Tables._


/**
  * Created by yz on 2018/5/10
  */
class Tool @Inject()(projectDao: ProjectDao, groupDao: GroupDao, modeDao: ModeDao) {

  def getProjectName(implicit request: RequestHeader) = {
    request.session.get("projectName").get
  }

  def getDealFile(implicit request: RequestHeader) = {
    val projectName = getProjectName
    val project = Utils.execFuture(projectDao.selectByProjectName(projectName)).get
    new File(Utils.proteinDir, project.id.toString + "/deal.txt")
  }

  def productGroupFile(groupName: String, tmpDir: File)(implicit request: RequestHeader) = {
    val projectName = getProjectName
    val group = Utils.execFuture(groupDao.selectByGroupName(projectName, groupName)).get
    Utils.productGroupFile(tmpDir, group.content)
  }

  case class TTestData(groupName: String, paired: String, pCutoff: String, qCutoff: String, method: String,
                       logCutoff: String, varEqual: String)

  val tTestForm = Form(
    mapping(
      "groupName" -> text,
      "paired" -> text,
      "pCutoff" -> text,
      "qCutoff" -> text,
      "method" -> text,
      "logCutoff" -> text,
      "varEqual" -> text
    )(TTestData.apply)(TTestData.unapply)
  )

  case class DiffData(groupName: String)

  val diffForm = Form(
    mapping(
      "groupName" -> text
    )(DiffData.apply)(DiffData.unapply)
  )

  case class GeneIdData(geneId: String)

  val geneIdForm = Form(
    mapping(
      "geneId" -> text
    )(GeneIdData.apply)(GeneIdData.unapply)
  )

  case class PcaData(method: String, sampleName: Seq[String], groupName: String, dtMethod: String, x: String, y: String)

  val pcaForm = Form(
    mapping(
      "method" -> text,
      "sampleName" -> seq(text),
      "groupName" -> text,
      "dtMethod" -> text,
      "x" -> text,
      "y" -> text
    )(PcaData.apply)(PcaData.unapply)
  )

  case class SampleNamesData(sampleNames: Seq[String])

  val sampleNamesForm = Form(
    mapping(
      "sampleNames" -> seq(text)
    )(SampleNamesData.apply)(SampleNamesData.unapply)
  )

  case class LocusNamesData(sampleName: String, locusName: String)

  val locusNameForm = Form(
    mapping(
      "sampleName" -> text,
      "locusName" -> text
    )(LocusNamesData.apply)(LocusNamesData.unapply)
  )

  case class SampleNameData(sampleName: String)

  val sampleNameForm = Form(
    mapping(
      "sampleName" -> text
    )(SampleNameData.apply)(SampleNameData.unapply)
  )

  case class DownloadData(sampleName: String, kind: String)

  val downloadForm = Form(
    mapping(
      "sampleName" -> text,
      "kind" -> text
    )(DownloadData.apply)(DownloadData.unapply)
  )

  case class HeatmapData(method: String, rowDist: String, colDist: String, rowCluster: String, colCluster: String)

  val heatmapForm = Form(
    mapping(
      "method" -> text,
      "rowDist" -> text,
      "colDist" -> text,
      "rowCluster" -> text,
      "colCluster" -> text
    )(HeatmapData.apply)(HeatmapData.unapply)
  )

  case class KeggEnrichData(geneId: String, method: String, fdr: String, cutoff: String, pValue: String)

  val keggEnrichForm = Form(
    mapping(
      "geneId" -> text,
      "method" -> text,
      "fdr" -> text,
      "cutoff" -> text,
      "pValue" -> text
    )(KeggEnrichData.apply)(KeggEnrichData.unapply)
  )

  case class GoEnrichData(geneId: String, twa: String, ewa: String)

  val goEnrichForm = Form(
    mapping(
      "geneId" -> text,
      "twa" -> text,
      "ewa" -> text
    )(GoEnrichData.apply)(GoEnrichData.unapply)
  )

  case class NormalData(normalMethod: Option[Seq[Int]], colName: String, coefficient: Double)

  val normalForm = Form(
    mapping(
      "normalMethod" -> optional(seq(number)),
      "colName" -> text,
      "coefficient" -> of(doubleFormat)
    )(NormalData.apply)(NormalData.unapply)
  )

  case class DataDealData(normal: Option[String], delete: Option[String], replace: Option[String])

  val dataDealForm = Form(
    mapping(
      "normal" -> optional(text),
      "delete" -> optional(text),
      "replace" -> optional(text)
    )(DataDealData.apply)(DataDealData.unapply)
  )

  case class DeleteData(iqr: Double)

  val deleteForm = Form(
    mapping(
      "iqr" -> of(doubleFormat)
    )(DeleteData.apply)(DeleteData.unapply)
  )

  case class ReplaceData(replaceMethod: String, rate: Double, assignValue: String, kValue: Int)

  val replaceForm = Form(
    mapping(
      "replaceMethod" -> text,
      "rate" -> of(doubleFormat),
      "assignValue" -> text,
      "kValue" -> number
    )(ReplaceData.apply)(ReplaceData.unapply)
  )

  case class GeneIdStrData(geneIdStr: String)

  val geneIdStrForm = Form(
    mapping(
      "geneIdStr" -> text
    )(GeneIdStrData.apply)(GeneIdStrData.unapply)
  )

  case class HClusterData(method: String, sampleDist: String, sampleCluster: String)

  val hClusterForm = Form(
    mapping(
      "method" -> text,
      "sampleDist" -> text,
      "sampleCluster" -> text
    )(HClusterData.apply)(HClusterData.unapply)
  )

  val groupingNameFom = Form(
    single(
      "groupingName" -> text
    )
  )

  case class RocData(groupingName: String, pGroup: String)

  val rocForm = Form(
    mapping(
      "groupingName" -> text,
      "pGroup" -> text
    )(RocData.apply)(RocData.unapply)
  )

  case class GroupMessage(groupingName: String, groupNames: Seq[String],
                          sampleNames: Seq[String])

  val groupForm = Form(
    mapping(
      "groupingName" -> text,
      "groupNames" -> seq(text),
      "sampleNames" -> seq(text)
    )(GroupMessage.apply)(GroupMessage.unapply)
  )

  case class RelationData(id: Int, group1SampleNames: Seq[String], group2SampleNames: Seq[String])

  val relationForm = Form(
    mapping(
      "id" -> number,
      "group1SampleNames" -> seq(text),
      "group2SampleNames" -> seq(text)
    )(RelationData.apply)(RelationData.unapply)
  )

  case class GroupData(groupName: String)

  val groupNameForm = Form(
    mapping(
      "groupName" -> text
    )(GroupData.apply)(GroupData.unapply)
  )


  case class GoBarData(terms: Seq[String], datas: Seq[String], kinds: Seq[String])

  val goBarForm = Form(
    mapping(
      "terms" -> seq(text),
      "datas" -> seq(text),
      "kinds" -> seq(text)
    )(GoBarData.apply)(GoBarData.unapply)
  )

  case class BarData(terms: Seq[String], datas: Seq[String])

  val barForm = Form(
    mapping(
      "terms" -> seq(text),
      "datas" -> seq(text)
    )(BarData.apply)(BarData.unapply)
  )

  case class ExpressPatternData(sampleNameStr: String, method: String, cMethod: String, pTree: String, kTree: String,
                                k: String, sampleDist: String, sampleCluster: String)

  val expressPatternForm = Form(
    mapping(
      "sampleNameStr" -> text,
      "method" -> text,
      "cMethod" -> text,
      "pTree" -> text,
      "kTree" -> text,
      "k" -> text,
      "sampleDist" -> text,
      "sampleCluster" -> text
    )(ExpressPatternData.apply)(ExpressPatternData.unapply)
  )

  def getPepFile(sampleName: String) = {
    new File(Utils.dataFile, s"${sampleName}.pep.fa")
  }

  def getCdsFile(sampleName: String) = {
    new File(Utils.dataFile, s"${sampleName}.cds.fa")
  }

  def getGenomeFile(sampleName: String) = {
    new File(Utils.dataFile, s"${sampleName}.genome.fa")
  }

  def getGffFile(sampleName: String) = {
    new File(Utils.dataFile, s"${sampleName}.gff")
  }

  def isTestMode = {
    val mode = Utils.execFuture(modeDao.select)
    if (mode.test == "t") true else false
  }

  def getUserId(implicit request: RequestHeader) = {
    request.session.get("id").get.toInt
  }

  def getUserMissionDir(implicit request: RequestHeader) = {
    val userIdDir = getUserIdDir
    new File(userIdDir, "mission")
  }

  def getUserIdDir(implicit request: RequestHeader) = {
    val userId = getUserId
    new File(Utils.userDir, userId.toString)
  }

  def getMissionIdDirById(missionId: Int)(implicit request: RequestHeader) = {
    val userMissionFile = getUserMissionDir
    new File(userMissionFile, missionId.toString)
  }

  def getResultDirById(missionId: Int)(implicit request: RequestHeader) = {
    val missionIdDir = getMissionIdDirById(missionId)
    new File(missionIdDir, "result")
  }

  def generateMissionName = {
    (new DateTime).toString("yyyy_MM_dd_HH_mm_ss")
  }

  def deleteDirectory(direcotry: File) = {
    if (!isTestMode) Utils.deleteDirectory(direcotry)
  }

  def createTempDirectory(prefix: String) = {
    if (isTestMode) Utils.testDir else Files.createTempDirectory(prefix).toFile
  }

  def getSampleName(str: String) = {
    val r = ".*\\((.*)\\).*".r
    val r(sampleName) = str
    sampleName
  }

  def dealMissFile(file: File) = {
    val lines = FileUtils.readLines(file).asScala
    val newLines = lines.map { line =>
      val columns = line.split("\t")
      s"${columns(0)}\t${columns(2)}"
    }
    FileUtils.writeLines(file, newLines.asJava)
  }

  def svAnno(file: File, dbFile: File) = {
    case class CDSInfo(proteinId: String, start: Int, end: Int)
    val dbLines = Utils.file2Lines(dbFile)
    val map = dbLines.filter(_.startsWith(">")).map { line =>
      val columns=line.drop(1).split("\\s+")
      val infos = columns(1).replaceAll("^locus=","").split(":")
      (infos(0), CDSInfo(columns(0), infos(1).toInt, infos(2).toInt))
    }.groupBy(_._1).mapValues(x => x.map(_._2).sortBy(_.start))
    val lines = Utils.file2Lines(file)
    val newLines = lines.map { line =>
      val columns = line.split("\t")
      val seqName = columns(0)
      val infos = map(seqName)
      val start = columns(1).toInt
      val end = columns(2).toInt
      val proteinIds = infos.filterNot { info =>
        info.end < start || info.start > end
      }.map(_.proteinId)
      val proteinIdStr = if (proteinIds.isEmpty) "NA" else proteinIds.mkString(",")
      s"${line}\t${proteinIds.size}\t${proteinIdStr}"
    }
    Utils.lines2File(new File(file.getParent, "sv.anno.txt"), newLines)
  }

  def getSeqMap(file: File) = {
    val lines = Utils.file2Lines(file)
    lines.zipWithIndex.filter { case (line, i) =>
      line.startsWith(">")
    }.map { case (line, i) =>
      val id = line.drop(1).split("\\s+")(0)
      val seq = lines.drop(i + 1).takeWhile(!_.startsWith(">")).mkString
      (id, seq)
    }.toMap

  }

  def getGeneInfos(cdsFile: File, pepFile: File, gffFile: File, sampleId: String) = {
    val cdsMap = getSeqMap(cdsFile)
    val pepMap = getSeqMap(pepFile)
    Utils.file2Lines(gffFile).filter(!_.startsWith("#")).filter { line =>
      val columns = line.split("\t")
      columns(2) == "gene"
    }.map { line =>
      val columns = line.split("\t")
      val id = columns(8).replaceAll(";.*$", "").replaceAll("^ID=", "")
      GeneinfoRow(sampleId, id, columns(0), columns(3).toInt, columns(4).toInt, columns(6),
        "NA", cdsMap(id), pepMap(id))
    }
  }


}
