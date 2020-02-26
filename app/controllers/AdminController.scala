package controllers

import java.io.{File, FileFilter, FilenameFilter}
import java.nio.file.Files

import dao._
import javax.inject.Inject
import models.Tables.{AdminRow, GeneinfoRow, SampleRow}
import org.apache.commons.io.FileUtils
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import utils.Utils

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.collection.JavaConverters._
import models.Tables._
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits._


/**
  * Created by yz on 2018/5/9
  */
class AdminController @Inject()(adminDao: AdminDao, sampleDao: SampleDao, geneInfoDao: GeneInfoDao,
                                projectDao: ProjectDao, groupDao: GroupDao, dealDao: DealDao,
                                classifyDao: ClassifyDao, tool: Tool, virusSampleDao: VirusSampleDao,
                                virusGeneInfoDao: VirusGeneInfoDao, formTool: FormTool) extends Controller {

  def sampleManageBefore = Action { implicit request =>
    Ok(views.html.admin.sampleManage())
  }

  def logout = Action {
    Redirect(routes.AppController.loginBefore()).flashing("info" -> "退出登录成功!").withNewSession
  }

  def changePasswordBefore = Action { implicit request =>
    Ok(views.html.admin.changePassword())
  }

  case class ChangePasswordData(account: String, password: String, newPassword: String)

  val changePasswordForm = Form(
    mapping(
      "account" -> text,
      "password" -> text,
      "newPassword" -> text
    )(ChangePasswordData.apply)(ChangePasswordData.unapply)
  )

  def changePassword = Action.async { implicit request =>
    val data = changePasswordForm.bindFromRequest().get
    adminDao.selectById1.flatMap { x =>
      if (data.account == x.account && data.password == x.password) {
        val row = AdminRow(x.id, x.account, data.newPassword)
        adminDao.update(row).map { y =>
          Redirect(routes.AppController.loginBefore()).flashing("info" -> "密码修改成功!").withNewSession
        }
      } else {
        Future.successful(Redirect(routes.AdminController.changePasswordBefore()).flashing("info" -> "账号或密码错误!"))
      }
    }
  }

  def addSampleDataBefore = Action { implicit request =>
    Ok(views.html.admin.addSampleData())
  }

  def addVirusSampleBefore = Action { implicit request =>
    Ok(views.html.admin.addVirusSample())
  }

  def addSampleData = Action(parse.multipartFormData) { implicit request =>
    var b = true
    var message = ""
    request.body.files.find { file =>
      try {
        val tmpDir = tool.createTempDirectory("tmpDir")
        val gbffFile = new File(tmpDir, "data.gbff")
        file.ref.moveTo(gbffFile, true)
        val pyFile = new File(Utils.pyPath, "readGenBank.py")
        val command1 = "python " + pyFile.getAbsolutePath
        val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command1))
        if (!execCommand.isSuccess) {
          tool.deleteDirectory(tmpDir)
          message = execCommand.getErrStr
          b = false
        } else {
          val infoFile = new File(tmpDir, "info.txt")
          val infoLines = FileUtils.readLines(infoFile).asScala.drop(1)
          val infos = infoLines.map { x =>
            val columns = x.split("\t")
            SampleRow(columns(0), columns(1).toInt, columns(4), columns(6), columns(7))
          }
          val clssifys = infos.map { x =>
            val classify = x.taxonomy
            val columns = classify.split(",")
            ClassifyRow(x.samplename, columns(1), columns(2), columns(3), columns(4), columns(5), columns(6))
          }
          val geneInfoFile = new File(tmpDir, "geneInfo.txt")
          val geneInfoLines = FileUtils.readLines(geneInfoFile).asScala.drop(1)
          val geneInfos = geneInfoLines.map { x =>
            val columns = x.split("\t")
            GeneinfoRow(columns(0), columns(7), columns(1), columns(2).toInt, columns(3).toInt, columns(4),
              columns(6), columns(8), columns(9))
          }
          val columns = infoLines.head.split("\t")
          val sampleId = columns(0)
          val kind = columns(8)
          var isExec = true
          if (geneInfoLines.isEmpty) {
            val command =
              s"""
                 |perl ${Utils.dosPath2Unix(Utils.binPath)}/01.Gene-prediction/bin/gene-predict_v2.pl --SpeType B --SampleId ${sampleId} --genemark --verbose -shape ${kind} --cpu 10  --run multi -outdir output ${Utils.dosPath2Unix(new File(tmpDir, sampleId + ".genome.fa"))}
                 |cp ${Utils.dosPath2Unix(tmpDir)}/output/final/${sampleId}.cds ${Utils.dosPath2Unix(tmpDir)}/${sampleId}.cds.fa
                 |cp ${Utils.dosPath2Unix(tmpDir)}/output/final/${sampleId}.pep ${Utils.dosPath2Unix(tmpDir)}/${sampleId}.pep.fa
                 |cp ${Utils.dosPath2Unix(tmpDir)}/output/final/${sampleId}.gff ${Utils.dosPath2Unix(tmpDir)}/
             """.stripMargin
            val execCommand = Utils.callLinuxScript(tmpDir, ArrayBuffer(command))
            if (!execCommand.isSuccess) {
              tool.deleteDirectory(tmpDir)
              message = execCommand.getErrStr
              b = false
              isExec = false
            } else {
              val parent = new File(tmpDir, "output/final")
              val gffFile = new File(parent, s"${sampleId}.gff")
              val cdsFile = new File(parent, s"${sampleId}.cds")
              val pepFile = new File(parent, s"${sampleId}.pep")
              geneInfos ++= tool.getGeneInfos(cdsFile, pepFile, gffFile, sampleId)
            }
          }
          if (isExec) {
            val filter = new FileFilter {
              override def accept(file: File): Boolean = file.getName.endsWith(".fa") || file.getName.endsWith(".gff") ||
                file.getName.endsWith(".gbff") && file.getName != "data.gbff"
            }
            FileUtils.copyDirectory(tmpDir, Utils.dataFile, filter)
            val f = sampleDao.updateAll(infos).zip(geneInfoDao.updateAll(geneInfos)).zip(classifyDao.updateAll(clssifys)).map { x =>
              tool.deleteDirectory(tmpDir)
            }
            Utils.execFuture(f)
          }
        }
      } catch {
        case x: Exception => b = false
          x.printStackTrace
          message = x.toString
      }
      b == false
    }
    if (b) {
      Ok(Json.toJson("success"))
    } else {
      Ok(Json.obj("valid" -> "false", "message" -> message))
    }

  }

  def addVirusSample = Action.async(parse.multipartFormData) { implicit request =>
    val file = request.body.file("file").get
    val tmpDir = tool.createTempDirectory("tmpDir")
    val gbffFile = new File(tmpDir, "data.gbff")
    file.ref.moveTo(gbffFile, true)
    val pyFile = new File(Utils.pyPath, "readVirusGenBank.py")
    val command1 = "python " + pyFile.getAbsolutePath
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(command1))
    if (!execCommand.isSuccess) {
      tool.deleteDirectory(tmpDir)
      Future.successful(Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr)))
    } else {
      val infoFile = new File(tmpDir, "info.txt")
      val infoLines = FileUtils.readLines(infoFile).asScala.drop(1)
      val infos = infoLines.map { x =>
        val columns = x.split("\t")
        VirusSampleRow(columns(0), columns(1).toInt, columns(4), columns(6), columns(7))
      }
      val geneInfoFile = new File(tmpDir, "geneInfo.txt")
      val geneInfoLines = FileUtils.readLines(geneInfoFile).asScala.drop(1)
      val geneInfos = geneInfoLines.map { x =>
        val columns = x.split("\t")
        VirusGeneInfoRow(columns(0), columns(9), columns(1), columns(2), columns(3), columns(4).toInt, columns(5).toInt, columns(6),
          columns(8), columns(10), columns(11))
      }
      virusSampleDao.updateAll(infos).zip(virusGeneInfoDao.updateAll(geneInfos)).map { x =>
        val filter = new FileFilter {
          override def accept(file: File): Boolean = file.getName.endsWith(".fa")
        }
        FileUtils.copyDirectory(tmpDir, Utils.dataFile, filter)
        tool.deleteDirectory(tmpDir)
        Ok(Json.toJson("success"))
      }
    }
  }

  def getArrayBySamples(x: Seq[SampleRow]) = {
    x.map { y =>
      val deleteStr = "<a title='删除' onclick=\"deleteProject('" + y.samplename + "')\" style='cursor: pointer;'><span><em class='fa fa-close'></em></span></a>"
      Json.obj(
        "samplename" -> y.samplename, "length" -> y.length, "definition" -> y.definition,
        "organism" -> y.organism, "operate" -> deleteStr, "taxonomy" -> y.taxonomy
      )
    }
  }

  def getAllSample = Action.async { implicit request =>
    sampleDao.selectAll.map { x =>
      val array = getArrayBySamples(x)
      Ok(Json.toJson(array))
    }
  }

  def deleteSampleBySampleName(sampleName: String) = Action.async { implicit request =>
    sampleDao.deleteBySampleName(sampleName).zip(geneInfoDao.deleteBySampleName(sampleName)).zip(classifyDao.deleteById(sampleName)).map { x =>
      val filter = new FilenameFilter {
        override def accept(dir: File, name: String): Boolean = name.startsWith(sampleName)
      }
      Utils.dataFile.listFiles(filter).foreach { file =>
        file.delete()
      }
      Redirect(routes.AdminController.getAllSample())
    }
  }

  def proteinManageBefore = Action { implicit request =>
    Ok(views.html.admin.proteinManage())
  }

  def toHome = Action { implicit request =>
    Ok(views.html.admin.home())
  }

  case class ProteinProjectData(projectName: String, describe: String)

  val proteinProjectForm = Form(
    mapping(
      "projectName" -> text,
      "describe" -> text
    )(ProteinProjectData.apply)(ProteinProjectData.unapply)
  )

  def addProteinProject = Action.async(parse.multipartFormData) { implicit request =>
    val data = proteinProjectForm.bindFromRequest().get
    val row = ProjectRow(0, data.projectName, data.describe, "蛋白组", new DateTime())
    val tmpDataFile = request.body.file("dataFile").get
    val tmpFile = Files.createTempFile("data", ".txt").toFile
    tmpDataFile.ref.moveTo(tmpFile, true)
    val (b, message) = Utils.checkFile(tmpFile)
    if (b) {
      projectDao.insert(row).flatMap { x =>
        projectDao.selectByProjectName(data.projectName)
      }.map { x =>
        val id = x.get.id
        val file = new File(Utils.proteinDir, id.toString)
        Utils.createDirectoryWhenNoExist(file)
        val dataFile = new File(file, "data.txt")
        val dealFile = new File(file, "deal.txt")
        FileUtils.copyFile(tmpFile, dataFile)
        FileUtils.copyFile(dataFile, dealFile)
        tmpFile.delete()
        val tmpListFile = request.body.file("listFile").get
        val listFile = new File(file, "list.txt")
        tmpListFile.ref.moveTo(listFile, true)
        val tmpKeggFile = request.body.file("keggFile").get
        val keggFile = new File(file, "kegg.txt")
        tmpKeggFile.ref.moveTo(keggFile, true)
        val tmpGoFile = request.body.file("goFile").get
        val goFile = new File(file, "go.txt")
        tmpGoFile.ref.moveTo(goFile, true)
        Redirect(routes.ToolController.getAllProteinProject())
      }
    } else {
      tmpFile.delete()
      Future.successful(Ok(Json.obj("valid" -> "false", "message" -> message)))
    }
  }

  case class ProjectNameData(projectName: String)

  val projectNameForm = Form(
    mapping(
      "projectName" -> text
    )(ProjectNameData.apply)(ProjectNameData.unapply)
  )

  def projectNameCheck = Action.async { implicit request =>
    val data = projectNameForm.bindFromRequest.get
    projectDao.selectByProjectName(data.projectName).map {
      case Some(y) => Ok(Json.obj("valid" -> false))
      case None => Ok(Json.obj("valid" -> true))
    }
  }

  def deleteProteinProjectById(id: Int) = Action.async { implicit request =>
    projectDao.selectById(id).flatMap { x =>
      groupDao.deleteByProjectName(x.projectname).zip(projectDao.deleteById(id)).zip(dealDao.deleteByProjectName(x.projectname))
    }.map { x =>
      val file = new File(Utils.proteinDir, id.toString)
      Utils.deleteDirectory(file)
      Redirect(routes.ToolController.getAllProteinProject())
    }
  }


}
