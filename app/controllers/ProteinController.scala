package controllers

import java.io.{File, FilenameFilter}
import java.nio.file.Files

import dao.{GroupDao, ProjectDao, ProteinDealDao}
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import utils.Utils
import play.api.data.format.Formats._

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import models.Tables._
import org.apache.commons.lang3.StringUtils

import scala.collection
import scala.collection.parallel.mutable
import scala.concurrent.Future
import scala.collection.JavaConverters._

/**
  * Created by yz on 2018/5/10
  */
class ProteinController @Inject()(proteinDealDao: ProteinDealDao, projectDao: ProjectDao, tool: Tool,
                                  groupDao: GroupDao) extends Controller {

  def toIndex(projectName: String) = Action { implicit request =>
    Redirect(routes.ProteinController.redirectIndex(projectName)).withSession(request.session+("projectName" -> projectName))
  }

  def redirectIndex(projectName: String) = Action.async { implicit request =>
    proteinDealDao.selectByProjectName(projectName).map { x =>
      Ok(views.html.user.protein.index(x))
    }
  }

  def getSampleNames = Action.async { implicit request =>
    val projectName = tool.getProjectName
    projectDao.selectByProjectName(projectName).map { x =>
      val project = x.get
      val file = new File(Utils.proteinDir, project.id.toString + "/data.txt")
      val lines = FileUtils.readLines(file).asScala
      val sampleNames = lines.head.split("\t").drop(1)
      Ok(Json.obj("sampleNames" -> sampleNames))
    }
  }

  def getData = Action.async { implicit request =>
    val projectName = tool.getProjectName
    projectDao.selectByProjectName(projectName).map { x =>
      val project = x.get
      val file = new File(Utils.proteinDir, project.id.toString + "/deal.txt")
      val json = Utils.getDataJson(file)
      Ok(json)
    }

  }

  def dataDeal = Action.async { implicit request =>
    var valid = true
    var message = ""
    val projectName = tool.getProjectName
    projectDao.selectByProjectName(projectName).flatMap { x =>
      val project = x.get
      var dbFile = new File(Utils.proteinDir, project.id.toString + "/data.txt")
      val tmpDir = Files.createTempDirectory("tmpDir").toFile
      //      val tmpDir = new File("E:\\tmp")
      val dataFile = new File(tmpDir, "data.txt")
      FileUtils.copyFileToDirectory(dbFile, tmpDir)
      val dataDeal = tool.dataDealForm.bindFromRequest.get
      val delete = tool.deleteForm.bindFromRequest().get
      if (dataDeal.delete.isDefined) {
        val rFile = new File(Utils.rPath, "evDelete.R")
        val command = Utils.Rscript + " --restore --no-save " + rFile.getAbsolutePath + " --c " + delete.iqr
        val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command))
        if (!execCommand.isSuccess) {
          valid = false
          message = execCommand.getErrStr
        }
      }

      val replace = tool.replaceForm.bindFromRequest().get
      if (dataDeal.replace.isDefined && valid) {
        if (replace.replaceMethod == "最小正数的倍数") {
          Utils.replaceByRate(dataFile, replace.rate)
        } else if (replace.replaceMethod == "指定某个值") {
          Utils.replaceByValue(dataFile, replace.assignValue)
        } else {
          Utils.relace0byNan(dataFile)
          val rFile = new File(Utils.rPath, "kNN.R")
          val command = Utils.Rscript + " --restore --no-save " + rFile.getAbsolutePath + " --k " + replace.kValue
          val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command))
          if (!execCommand.isSuccess) {
            valid = false
            message = execCommand.getErrStr
          }
        }
      }

      val normal =tool.normalForm.bindFromRequest().get
      if (dataDeal.normal.isDefined && valid) {
        val normalMethod = normal.normalMethod.get
        if (normalMethod.size == 2) {
          Utils.qcNormal(dataFile, normal.colName)
          Utils.peakAreaNormal(dataFile, normal.coefficient)
        } else if (normalMethod.sum == 1) {
          Utils.qcNormal(dataFile, normal.colName)
        } else {
          Utils.peakAreaNormal(dataFile, normal.coefficient)
        }
      }

      val dealFile = new File(Utils.proteinDir, project.id.toString + "/deal.txt")
      FileUtils.copyFile(dataFile, dealFile)
      Utils.deleteDirectory(tmpDir)
      if (valid) {
        val row = ProteindealRow(projectName, dataDeal.delete, delete.iqr, dataDeal.replace, replace.replaceMethod, replace.rate,
          replace.assignValue, replace.kValue, dataDeal.normal, normal.normalMethod.map(_.sum), normal.colName,
          normal.coefficient)
        proteinDealDao.insertOrUpdate(row).map { x =>
          val json = Utils.getDataJson(dealFile)
          Ok(json)
        }
      } else {
        Future.successful(Ok(Json.obj("valid" -> "false", "message" -> message)))
      }

    }

  }

  def groupBefore = Action.async { implicit request =>
    val projectName = tool.getProjectName
    projectDao.selectByProjectName(projectName).map { x =>
      val project = x.get
      val file = new File(Utils.proteinDir, project.id.toString + "/data.txt")
      val lines = FileUtils.readLines(file).asScala
      val sampleNames = lines.head.split("\t").drop(1)
      Ok(views.html.user.protein.group(sampleNames))
    }

  }

  def getAllGroup = Action.async { implicit request =>
    val projectName = tool.getProjectName
    groupDao.selectAll(projectName).map { x =>
      val array = getArrayByGroups(x)
      Ok(Json.toJson(array))
    }
  }

  def getArrayByGroups(x: Seq[GroupRow]) = {
    x.map { y =>
      val contentStr = y.content.split(";").map { x =>
        val sampleNameStr = x.split(":")(1).split(",").mkString("&nbsp;")
        x.split(":")(0) + (":&nbsp;") + sampleNameStr
      }.mkString("<br>")

      val groupNum = Utils.getGroupNum(y.content)
      val relationStr = if (groupNum == 2) {
        val isSameSampleNum = y.content.split(";").map(_.split(":")(1).split(",").size).toSet.size == 1
        if (!isSameSampleNum) {
          "无"
        } else if (y.relation.isDefined) {
          y.relation.get.split(";").map { x =>
            val sampleName1 = x.split("<->")(0)
            val sampleName2 = x.split("<->")(1)
            sampleName1 + "&nbsp;<i class='fa fa-arrows-h'></i>&nbsp;" + sampleName2
          }.mkString("<br>")
        } else {
          "<a title='构建配对样品关系' onclick=\"addRelation('" + y.id + "')\" style='cursor: pointer;'><span><em class='fa fa-plus'></em></span></a>"
        }
      } else {
        "无"
      }

      val deleteStr = "<a title='删除' onclick=\"deleteGrouping('" + y.id + "')\" style='cursor: pointer;'><span><em class='fa fa-close'></em></span></a>"
      Json.obj(
        "groupname" -> y.groupname, "content" -> contentStr, "operate" -> deleteStr, "relation" -> relationStr
      )
    }
  }

  val idForm = Form(
    single(
      "id" -> number
    )
  )

  def deleteGroupById = Action.async { implicit request =>
    val id = idForm.bindFromRequest().get
    val projectName = tool.getProjectName
    groupDao.deleteById(id).flatMap { x =>
      groupDao.selectAll(projectName)
    }.map { x =>
      val array = getArrayByGroups(x)
      Ok(Json.toJson(array))
    }
  }

  def selectGroupById = Action.async { implicit request =>
    val id = idForm.bindFromRequest().get
    val projectName = tool.getProjectName
    groupDao.selectById(id).map { x =>
      val group1Name = x.content.split(";")(0).split(":")(0)
      val group2Name = x.content.split(";")(1).split(":")(0)
      val group1SampleNames = x.content.split(";")(0).split(":")(1).split(",")
      val group2SampleNames = x.content.split(";")(1).split(":")(1).split(",")
      Ok(Json.obj("groupingName" -> x.groupname, "group1Name" -> group1Name, "group2Name" -> group2Name,
        "group1SampleNames" -> group1SampleNames, "group2SampleNames" -> group2SampleNames))
    }
  }

  def addGroup = Action.async { implicit request =>
    val data = tool.groupForm.bindFromRequest().get
    val groupNames = data.groupNames
    val disGroupNames = groupNames.distinct
    val errorGroupNames = groupNames.diff(disGroupNames).distinct
    var valid = "true"
    var message = ""
    if (errorGroupNames.nonEmpty) {
      valid = "false"
      message = "组名： " + errorGroupNames.mkString("、") + " 重复！"
    }
    val projectName = tool.getProjectName
    val group = Utils.execFuture(groupDao.selectByGroupName(projectName, data.groupingName))
    if (group.isDefined) {
      valid = "false"
      message = "分组名已存在！"
    }

    if (valid == "true") {
      val sampleNames = data.sampleNames
      val content = groupNames.zip(sampleNames).map { case (groupName, sampleNames) =>
        groupName + ":" + sampleNames
      }.mkString(";")
      val row = GroupRow(0, projectName, data.groupingName, content)
      groupDao.insert(row).flatMap(_ => groupDao.selectAll(projectName)).map { x =>
        val array = getArrayByGroups(x)
        Ok(Json.toJson(array))
      }
    } else {
      Future.successful(Ok(Json.obj("valid" -> valid, "message" -> message)))
    }
  }

  def updateRelation = Action.async { implicit request =>
    val data = tool.relationForm.bindFromRequest().get
    val group1SampleNames = data.group1SampleNames
    val group2SampleNames = data.group2SampleNames
    val projectName = tool.getProjectName
    val relation = group1SampleNames.zip(group2SampleNames).map { case (group1SampleName, group2SampleName) =>
      group1SampleName + "<->" + group2SampleName
    }.mkString(";")
    groupDao.update(data.id, Some(relation)).flatMap(_ => groupDao.selectAll(projectName)).map { x =>
      val array = getArrayByGroups(x)
      Ok(Json.toJson(array))
    }
  }

  def getAllGroupNum2Names = Action.async { implicit request =>
    val projectName = tool.getProjectName
    groupDao.selectAll(projectName).map { x =>
      val groupNames = x.filter { y =>
        Utils.getGroupNum(y.content) == 2
      }.map(_.groupname)
      Ok(Json.toJson(groupNames))
    }
  }

  def checkRelation = Action.async { implicit request =>
    val groupName = tool.groupNameForm.bindFromRequest().get.groupName
    val projectName = tool.getProjectName
    groupDao.selectByGroupName(projectName, groupName).map { x =>
      val relation = if (x.get.relation.isDefined) "True" else "False"
      Ok(relation)
    }
  }

  def differenceAna = Action.async { implicit request =>
    val projectName = tool.getProjectName
    val data = tool.tTestForm.bindFromRequest.get
    groupDao.selectByGroupName(projectName, data.groupName).map(_.get).
      map { group =>
        val dataFile = tool.getDealFile
        val tmpDir = tool.createTempDirectory("tmpDir")
        FileUtils.copyFileToDirectory(dataFile, tmpDir)
        val groupFile = new File(tmpDir, "group.txt")
        val sampleGroup = collection.mutable.LinkedHashMap[String, String]()
        group.content.split(";").foreach { x =>
          val lines = x.split(":")
          lines(1).split(",").foreach { sam =>
            sampleGroup += (sam -> lines(0))
          }
        }
        val groupBuffer = if (data.paired == "T") {
          group.relation.get.split(";").flatMap { x =>
            val columns = x.split("<->")
            ArrayBuffer(columns(0) + "\t" + sampleGroup(columns(0)), columns(1) + "\t" + sampleGroup(columns(1)))
          }.toBuffer
        } else {
          sampleGroup.map { case (sam, groupName) =>
            sam + "\t" + groupName
          }.toBuffer
        }
        FileUtils.writeLines(groupFile, groupBuffer.asJava)
        val rFile = new File(Utils.rPath, "proteinDifferenceAna.R")
        val command = Utils.Rscript + " --restore --no-save " + rFile.getAbsolutePath + " --paired " + data.paired +
          " --pCutoff " + data.pCutoff + " --qCutoff " + data.qCutoff + " --m " + data.method + " --l " + data.logCutoff +
          " --ve " + data.varEqual
        val pyFile = new File(Utils.pyPath, "volcano.py")
        val command1 = "python " + pyFile.getAbsolutePath +
          " --pCutoff " + data.pCutoff + " --qCutoff " + data.qCutoff + " --l " + data.logCutoff
        val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command, command1))
        if (execCommand.isSuccess) {
          val file = new File(tmpDir, "result.txt")
          val (columnNames, array) = Utils.getInfoByFile(file)
          val logFcs = FileUtils.readLines(file).asScala.drop(1).map(_.split("\t")(5))
          val keggStr = array.map(_ ("geneId")).zip(logFcs).map { case (geneId, log2Fc) =>
            s"${geneId}\t${log2Fc}"
          }.mkString("\n")
          val geneIdStr = Utils.lfJoin(array.map(_ ("geneId")))
          val divFile = new File(tmpDir, "div.txt")
          val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
          val json = Json.obj("columnNames" -> columnNames, "array" -> array, "div" -> divStr,
            "geneIdStr" -> geneIdStr, "keggStr" -> keggStr)
          tool.deleteDirectory(tmpDir)
          Ok(json)
        } else {
          tool.deleteDirectory(tmpDir)
          Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
        }
      }
  }

  def differenceAnaBefore = Action { implicit request =>
    Ok(views.html.user.protein.differenceAna())
  }

  def goBarPlot = Action { implicit request =>
    val data = tool.goBarForm.bindFromRequest().get
    val tmpDir = Files.createTempDirectory("tmpDir").toFile
    val buffer = ArrayBuffer(s"ID\tdata\tkind")
    buffer ++= data.terms.zipWithIndex.map { case (v, i) =>
      s"${v}\t${data.datas(i)}\t${data.kinds(i)}"
    }
    FileUtils.writeLines(new File(tmpDir, "deal.txt"), buffer.asJava)
    //    FileUtils.writeLines(new File("E:\\testData\\deal.txt"), buffer.asJava)
    val pyFile = new File(Utils.pyPath, "goBarChart.py")
    val command = "python " + pyFile.getAbsolutePath
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command))
    if (execCommand.isSuccess) {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
      val json = Json.obj("div" -> divStr)
      Utils.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def goPBarPlot = Action { implicit request =>
    val data =tool.goBarForm.bindFromRequest().get
    val tmpDir = Files.createTempDirectory("tmpDir").toFile
    val buffer = ArrayBuffer(s"ID\tdata\tkind")
    buffer ++= data.terms.zipWithIndex.map { case (v, i) =>
      s"${v}\t${data.datas(i)}\t${data.kinds(i)}"
    }
    FileUtils.writeLines(new File(tmpDir, "deal.txt"), buffer.asJava)
    //    FileUtils.writeLines(new File("E:\\testData\\deal.txt"), buffer.asJava)
    val pyFile = new File(Utils.pyPath, "goPBarChart.py")
    val command = "python " + pyFile.getAbsolutePath
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command))
    if (execCommand.isSuccess) {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
      val json = Json.obj("div" -> divStr)
      Utils.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def pie = Action { implicit request =>
    val data = tool.barForm.bindFromRequest().get
    val tmpDir = Files.createTempDirectory("tmpDir").toFile
    val buffer = ArrayBuffer(s"ID\t${data.terms.mkString("\t")}")
    buffer += s"data\t${data.datas.mkString("\t")}"
    FileUtils.writeLines(new File(tmpDir, "deal.txt"), buffer.asJava)
    val pyFile = new File(Utils.pyPath, "pie.py")
    val command = "python " + pyFile.getAbsolutePath
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command))
    if (execCommand.isSuccess) {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
      val json = Json.obj("div" -> divStr)
      Utils.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def getFirst10GeneIdByLf = Action.async { implicit request =>
    val projectName = tool.getProjectName
    projectDao.selectByProjectName(projectName).map(_.get).map { project =>
      val dataFile = new File(Utils.proteinDir, project.id.toString + "/deal.txt")
      val lines = FileUtils.readLines(dataFile).asScala.drop(1)
      val geneIds = lines.map(_.split("\t")(0)).take(10)
      val geneIdStr = geneIds.mkString("\n")
      Ok(geneIdStr)
    }
  }

  def getGeneIds = Action.async { implicit request =>
    val projectName = tool.getProjectName
    projectDao.selectByProjectName(projectName).map { x =>
      val project = x.get
      val file = new File(Utils.proteinDir, project.id.toString + "/data.txt")
      val lines = FileUtils.readLines(file).asScala
      val geneIds = lines.drop(1).map(_.split("\t")(0))
      Ok(Json.obj("geneIds" -> geneIds))
    }
  }

  def goEnrich = Action.async { implicit request =>
    val projectName = tool.getProjectName
    projectDao.selectByProjectName(projectName).map { x =>
      val project = x.get
      val userPath = new File(Utils.proteinDir, project.id.toString)
      val data = tool.goEnrichForm.bindFromRequest.get
      val tmpDir = Files.createTempDirectory("tmpDir").toFile
      val diffFile = new File(tmpDir, "diff.txt")
      val buffer = Utils.dealGeneIds(data.geneId)
      FileUtils.writeLines(diffFile, buffer.asJava)
      val outFile = new File(tmpDir, "out.txt")
      val enrichFile = new File(Utils.binPath, "goatools-0.5.7/scripts/find_enrichment.py")
      val geneListFile = new File(userPath, "list.txt")
      val annoFile = new File(userPath, "go.txt")
      val command1 = Utils.goPy + " " + enrichFile.getAbsolutePath + " --alpha " + data.twa + " --pval " + data.ewa +
        " --output " + outFile.getAbsolutePath + " " + diffFile.getAbsolutePath + " " + geneListFile.getAbsolutePath + " " +
        annoFile.getAbsolutePath
      val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command1))
      if (execCommand.isSuccess) {
        val buffer = FileUtils.readLines(outFile).asScala.drop(1)
        val jsons = buffer.filter(_.split("\t")(1) == "e").map { x =>
          val columns = x.split("\t")
          val pu = columns(5).toDouble.formatted("%.3f").toString
          Json.obj("id" -> columns(0), "enrichment" -> columns(1), "description" -> columns(2), "ratio_in_study" -> columns(3),
            "ratio_in_pop" -> columns(4), "p_uncorrected" -> pu, "p_bonferroni" -> columns(6), "p_holm" -> columns(7),
            "p_sidak" -> columns(8), "p_fdr" -> columns(9), "namespace" -> columns(10), "genes_in_study" -> columns(11))
        }
        Utils.deleteDirectory(tmpDir)
        Ok(Json.toJson(jsons))
      } else {
        Utils.deleteDirectory(tmpDir)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
      }
    }
  }

  def goEnrichBefore = Action { implicit request =>
    Ok(views.html.user.protein.goEnrich(None))
  }

  def plotExp = Action { implicit request =>
    val geneId = tool.geneIdForm.bindFromRequest().get.geneId
    val dataFile = tool.getDealFile
    val tmpDir = Files.createTempDirectory("tmpDir").toFile
    FileUtils.copyFileToDirectory(dataFile, tmpDir)
    val pyFile = new File(Utils.pyPath, "barChart.py")
    val command = "python " + pyFile.getAbsolutePath + " -g " + geneId.mkString("\"", "", "\"")
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command))
    if (execCommand.isSuccess) {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile)
      val json = Json.obj("div" -> divStr)
      Utils.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def barPlot = Action { implicit request =>
    val data = tool.barForm.bindFromRequest().get
    val tmpDir = Files.createTempDirectory("tmpDir").toFile
    val buffer = ArrayBuffer(s"ID\t${data.terms.mkString("\t")}")
    buffer += s"data\t${data.datas.mkString("\t")}"
    FileUtils.writeLines(new File(tmpDir, "deal.txt"), buffer.asJava)
    val pyFile = new File(Utils.pyPath, "myBarChart.py")
    val command = "python " + pyFile.getAbsolutePath
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command))
    if (execCommand.isSuccess) {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
      val json = Json.obj("div" -> divStr)
      Utils.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def pBarPlot = Action { implicit request =>
    val data = tool.barForm.bindFromRequest().get
    val tmpDir = Files.createTempDirectory("tmpDir").toFile
    val buffer = ArrayBuffer(s"ID\t${data.terms.mkString("\t")}")
    buffer += s"data\t${data.datas.mkString("\t")}"
    FileUtils.writeLines(new File(tmpDir, "deal.txt"), buffer.asJava)
    val pyFile = new File(Utils.pyPath, "pBarChart.py")
    val command = "python " + pyFile.getAbsolutePath
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command))
    if (execCommand.isSuccess) {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
      val json = Json.obj("div" -> divStr)
      Utils.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def geneIdCheck = new ActionBuilder[Request] with ActionFilter[Request] {
    override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful {
      val data = tool.keggEnrichForm.bindFromRequest()(request).get
      val lines = data.geneId.split("\n").map(_.trim).distinct.toBuffer
      val array = lines.map(_.split("\\s+"))
      val b = array match {
        case x if x.exists(_.size > 2) => false
        case x if x.exists(_.size == 2) && x.forall(_.size == 2) =>
          val values = array.map(_ (1))
          if (!values.forall(x => Utils.isDouble(x))) {
            false
          } else true
        case x if x.exists(_.size == 2) && !x.forall(_.size == 2) => false
        case _ => true
      }
      if (!b) {
        Some(Ok(Json.obj("valid" -> "false", "message" -> "蛋白ID格式不正确")))
      } else {
        None
      }
    }
  }

  def keggEnrich = geneIdCheck.async { implicit request =>
    val projectName = tool.getProjectName
    projectDao.selectByProjectName(projectName).map { x =>
      val project = x.get
      val userPath = new File(Utils.proteinDir, project.id.toString)
      val data = tool.keggEnrichForm.bindFromRequest.get
      val tmpDir = Files.createTempDirectory("tmpDir").toFile
      val diffFile = new File(tmpDir, "diff.txt")
      val lines = data.geneId.split("\n").map(_.trim).distinct.toBuffer
      val geneIdBuffer = lines.map(_.split("\\s+")(0))
      val geneIdlog2FcMap = if (lines(0).split("\\s+").size == 2) {
        lines.map { x =>
          (x.split("\\s+")(0), x.split("\\s+")(1).toDouble)
        }.toMap
      } else Map[String, Double]()
      FileUtils.writeLines(diffFile, geneIdBuffer.asJava)
      val outFile = new File(tmpDir, "out.txt")
      val identifyFile = new File(Utils.binPath, "identify.pl")
      val geneListFile = new File(userPath, "list.txt")
      val annoFile = new File(userPath, "kegg.txt")
      val geneIdKMap = FileUtils.readLines(annoFile).asScala.map { x =>
        (x.split("\t")(0), x.split("\t")(1))
      }.toMap
      val kGeneIdMap = geneIdKMap.filter { case (key, value) =>
        geneIdBuffer.contains(key)
      }.map { case (key, value) =>
        (value, key)
      }
      val command1 = "perl " + identifyFile.getAbsolutePath + " -study " + diffFile.getAbsolutePath + " -population " + geneListFile.getAbsolutePath +
        " -association " + annoFile.getAbsolutePath + " -m " + data.method + " -n " +
        data.fdr + " -c " + data.cutoff + " -o " + outFile.getAbsolutePath + " -maxp " + data.pValue
      val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command1))
      if (execCommand.isSuccess) {
        val buffer = FileUtils.readLines(outFile).asScala.drop(1).filter(x => StringUtils.isNoneBlank(x))
        Utils.deleteDirectory(tmpDir)
        val jsons = buffer.map { x =>
          val columns = x.split("\t")
          val pValue = columns(5).toDouble.formatted("%.3f").toString
          val cPValue = columns(6).toDouble.formatted("%.3f").toString
          val heads = columns(8).split("\\?")
          val koHeads = heads(1).split("\\/")
          val ks = koHeads.tail
          val kStr = ks.map { k =>
            val buffer = k.split("%09")
            val kNumber = buffer(0)
            val geneId = kGeneIdMap(kNumber)
            val log2Fc = geneIdlog2FcMap.getOrElse(geneId, 2.0)
            val color = if (log2Fc > 0) {
              "red"
            } else {
              "blue"
            }
            s"${buffer(0)}%09${color}"
          }.mkString("/")
          val href = s"${heads(0)}?${koHeads(0)}/${kStr}"
          val link = s"<a href='${href}' target='_blank'>link</a>"
          Json.obj("term" -> columns(0), "database" -> columns(1), "id" -> columns(2), "inputNumber" -> columns(3),
            "backgroundNumber" -> columns(4), "pValue" -> pValue, "cPValue" -> cPValue, "input" -> columns(7),
            "hyperlink" -> link)
        }
        Ok(Json.toJson(jsons))
      } else {
        Utils.deleteDirectory(tmpDir)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
      }
    }
  }

  def keggEnrichBefore = Action { implicit request =>
    Ok(views.html.user.protein.keggEnrich(None))
  }


  def keggEnrichBefore1 = Action { implicit request =>
    val data = tool.geneIdStrForm.bindFromRequest().get
    Ok(views.html.user.protein.keggEnrich(Some(data.geneIdStr)))
  }

  def goEnrichBefore1 = Action { implicit request =>
    val data = tool.geneIdStrForm.bindFromRequest().get
    Ok(views.html.user.protein.goEnrich(Some(data.geneIdStr)))
  }

  def plotDiffExp = Action { implicit request =>
    val geneId = tool.geneIdForm.bindFromRequest().get.geneId
    val diffData = tool.diffForm.bindFromRequest().get
    val dataFile = tool.getDealFile
    val tmpDir = Files.createTempDirectory("tmpDir").toFile
    FileUtils.copyFileToDirectory(dataFile, tmpDir)
    tool.productGroupFile(diffData.groupName, tmpDir)
    val pyFile = new File(Utils.pyPath, "diffBarChart.py")
    val command = "python " + pyFile.getAbsolutePath + " -g " + geneId.mkString("\"", "", "\"")
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command))
    if (execCommand.isSuccess) {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile)
      val json = Json.obj("div" -> divStr)
      Utils.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def heatmap = Action { implicit request =>
    val sampleNames = tool.sampleNamesForm.bindFromRequest.get.sampleNames
    val geneIdStr = tool.geneIdStrForm.bindFromRequest.get.geneIdStr
    val heatmapData = tool.heatmapForm.bindFromRequest.get
    val dataFile = tool.getDealFile
    val tmpDir = Files.createTempDirectory("tmpDir").toFile
    FileUtils.copyFileToDirectory(dataFile, tmpDir)
    val rFile = new File(Utils.rPath, "dataTransform.R")
    val rCommand = Utils.Rscript + " --restore --no-save " + rFile.getAbsolutePath + " --m " + heatmapData.method
    val pyFile = new File(Utils.pyPath, "dendrogramWithHeatmap.py")
    //        val pyFile = new File(Utils.pyPath, "heatmap.py")
    val geneIds = Utils.dealGeneIds(geneIdStr)
    FileUtils.writeLines(new File(tmpDir, "gene.txt"), geneIds.asJava)
    val command = "python " + pyFile.getAbsolutePath + " -s " + sampleNames.mkString(" ") +
      " -rd " + heatmapData.rowDist + " -cd " + heatmapData.colDist + " -rl " + heatmapData.rowCluster + " -cl " +
      heatmapData.colCluster
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(rCommand, command))
    if (execCommand.isSuccess) {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
      val dataFile = new File(tmpDir, "deal.txt")
      val array = Utils.getDataJson(dataFile)
      val json = Json.obj("div" -> divStr, "json" -> array)
      Utils.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def heatmapBefore = Action { implicit request =>
    Ok(views.html.user.protein.heatmap(None))
  }

  def getAllGroupNames = Action.async { implicit request =>
    val projectName = tool.getProjectName
    groupDao.selectAll(projectName).map { x =>
      val groupNames = x.map(_.groupname)
      Ok(Json.toJson(groupNames))
    }
  }

  def pca = Action.async { implicit request =>
    val projectName = tool.getProjectName
    val data = tool.pcaForm.bindFromRequest.get
    (projectDao.selectByProjectName(projectName).map(_.get)).
      map { project =>
        val dataFile = new File(Utils.proteinDir, project.id.toString + "/deal.txt")
        val tmpDir = Files.createTempDirectory("tmpDir").toFile
        FileUtils.copyFileToDirectory(dataFile, tmpDir)
        val rFile = new File(Utils.rPath, "pca.R")
        val group = Utils.execFuture(groupDao.selectByGroupName(projectName, data.groupName)).get
        val sampleNames = if (data.method == "hand") {
          data.sampleName.toBuffer
        } else {
          group.content.split(";").flatMap(_.split(":")(1).split(",")).toBuffer
        }
        Utils.productGroupFile(tmpDir, group.content)
        val rFile1 = new File(Utils.rPath, "dataTransform.R")
        val rCommand = Utils.Rscript + " --restore --no-save " + rFile1.getAbsolutePath + " --m " + data.dtMethod
        val command = Utils.Rscript + " --restore --no-save " + rFile.getAbsolutePath + " --s " + sampleNames.mkString(",")
        val pyFile = new File(Utils.pyPath, "pca.py")
        val command1 = "python " + pyFile.getAbsolutePath + " -m " + data.method + " -x " + data.x + " -y " + data.y
        //        val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(rCommand,command, command1))
        val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(rCommand, command, command1))
        if (execCommand.isSuccess) {
          val divFile = new File(tmpDir, "div.txt")
          val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
          val json = Json.obj("div" -> divStr)
          Utils.deleteDirectory(tmpDir)
          Ok(json)
        } else {
          Utils.deleteDirectory(tmpDir)
          Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
        }
      }
  }

  def pcaBefore = Action { implicit request =>
    Ok(views.html.user.protein.pca())
  }

  def hCluster = Action.async { implicit request =>
    val projectName = tool.getProjectName
    val data = tool.sampleNamesForm.bindFromRequest.get
    val hClusterData =tool.hClusterForm.bindFromRequest.get
    (projectDao.selectByProjectName(projectName).map(_.get)).
      map { project =>
        val dataFile = new File(Utils.proteinDir, project.id.toString + "/deal.txt")
        val tmpDir = Files.createTempDirectory("tmpDir").toFile
        FileUtils.copyFileToDirectory(dataFile, tmpDir)
        val rFile = new File(Utils.rPath, "dataTransform.R")
        val rCommand = Utils.Rscript + " --restore --no-save " + rFile.getAbsolutePath + " --m " + hClusterData.method
        val pyFile = new File(Utils.pyPath, "dendrogram.py")
        val command = "python " + pyFile.getAbsolutePath + " -s " + data.sampleNames.mkString(" ") + " -pm " +
          hClusterData.sampleDist + " -lm " + hClusterData.sampleCluster
        val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(rCommand, command))
        if (execCommand.isSuccess) {
          val divFile = new File(tmpDir, "div.txt")
          val divStr = FileUtils.readFileToString(divFile)
          val json = Json.obj("div" -> divStr)
          Utils.deleteDirectory(tmpDir)
          Ok(json)
        } else {
          Utils.deleteDirectory(tmpDir)
          Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
        }
      }
  }

  def hClusterBefore = Action { implicit request =>
    Ok(views.html.user.protein.hCluster())
  }

  def rocBefore = Action { implicit request =>
    Ok(views.html.user.protein.roc(None))
  }

  def getGroupNames = Action.async { implicit request =>
    val projectName = tool.getProjectName
    val groupingName = tool.groupingNameFom.bindFromRequest().get
    groupDao.selectByGroupName(projectName, groupingName).map { x =>
      val group = x.get
      val groupNames = Utils.getGroupNames(group.content)
      Ok(Json.toJson(groupNames))
    }
  }

  def roc = Action { implicit request =>
    val projectName = tool.getProjectName
    val data = tool.rocForm.bindFromRequest.get
    val geneIdStr = tool.geneIdStrForm.bindFromRequest().get.geneIdStr
    val dataFile = tool.getDealFile
    val tmpDir = Files.createTempDirectory("tmpDir").toFile
    FileUtils.copyFileToDirectory(dataFile, tmpDir)
    val group = Utils.execFuture(groupDao.selectByGroupName(projectName, data.groupingName)).get
    val groupSample = Utils.getMap(group.content)
    val pSamples = groupSample(data.pGroup)
    val buffer = FileUtils.readLines(dataFile).asScala
    val flag = ("FLAG") +: buffer.head.split("\t").drop(1).map { x =>
      if (pSamples.contains(x)) "1" else "0"
    }.toBuffer
    buffer += flag.mkString("\t")
    FileUtils.writeLines(new File(tmpDir, "deal.txt"), buffer.asJava)
    val pyFile = new File(Utils.pyPath, "roc.py")
    val geneIds = Utils.dealGeneIds(geneIdStr)
    FileUtils.writeLines(new File(tmpDir, "gene.txt"), geneIds.asJava)
    val pyCommand = "python " + pyFile.getAbsolutePath
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(pyCommand))
    if (execCommand.isSuccess) {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
      val json = Json.obj("div" -> divStr)
      Utils.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def expressPatternBefore = Action { implicit request =>
    Ok(views.html.user.protein.expressPattern(None))
  }

  def heatmapBefore1 = Action { implicit request =>
    val data = tool.geneIdStrForm.bindFromRequest().get
    Ok(views.html.user.protein.heatmap(Some(data.geneIdStr)))
  }

  def rocBefore1 = Action { implicit request =>
    val data = tool.geneIdStrForm.bindFromRequest().get
    Ok(views.html.user.protein.roc(Some(data.geneIdStr)))
  }

  def expressPattern = Action { implicit request =>
    val data = tool.expressPatternForm.bindFromRequest.get
    val geneIdStr = tool.geneIdStrForm.bindFromRequest.get.geneIdStr
    val dataFile = tool.getDealFile
    val tmpDir = tool.createTempDirectory("tmpDir")
    FileUtils.copyFileToDirectory(dataFile, tmpDir)
    val rFile = new File(Utils.rPath, "expressPattern.R")
    val geneIds=Utils.dealGeneIds(geneIdStr)
    FileUtils.writeLines(new File(tmpDir,"gene.txt"),geneIds.asJava)
    val rCommandBuffer = ArrayBuffer(Utils.Rscript + " --restore --no-save " + rFile.getAbsolutePath + " --s " + data.sampleNameStr+
      " --m " + data.method + " --dm " + data.sampleDist + " --cm " + data.sampleCluster)
    data.cMethod match {
      case "1" => rCommandBuffer += ("--k " + data.k)
      case "2" => rCommandBuffer += ("--kt " + data.kTree)
      case _ => rCommandBuffer += ("--pt " + data.pTree)
    }
    val rCommand = rCommandBuffer.mkString(" ")
    val pyFile = new File(Utils.pyPath, "expressPattern.py")
    val pyCommand = "python " + pyFile.getAbsolutePath
    val shBuffer = ArrayBuffer(rCommand, pyCommand)
    val execCommand = Utils.callScript(tmpDir, shBuffer)
    if (execCommand.isSuccess) {
      val filter = new FilenameFilter {
        override def accept(dir: File, name: String): Boolean = name.endsWith(".matrix.txt")
      }
      val files = tmpDir.listFiles(filter).sortBy(file => file.getName.split("\\.")(0))
      val subNames = files.map(_.getName.split("\\.")(0))
      val data = files.map { file =>
        val (columnNames, array) = Utils.getInfoByFile(file)
        val divFile = new File(file.getParent, file.getName.split("\\.")(0) + ".div.txt")
        val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
        Json.obj("columnNames" -> columnNames, "array" -> array, "div" -> divStr)
      }
      val json = Json.obj("data" -> data, "subNames" -> subNames)
      tool.deleteDirectory(tmpDir)
      Ok(json)
    } else {
      tool.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def expressPatternBefore1 = Action { implicit request =>
    val data = tool.geneIdStrForm.bindFromRequest().get
    Ok(views.html.user.protein.expressPattern(Some(data.geneIdStr)))
  }

}
