package controllers

import java.io.File
import java.nio.file.Files

import dao.MissionDao
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.libs.json.Json
import play.api.mvc._
import utils.{MissionExecutor, Utils}

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._
import models.Tables._
import org.joda.time.DateTime

/**
  * Created by yz on 2018/6/28
  */
class MultiOmicsController @Inject()(formTool: FormTool, tool: Tool, missionDao: MissionDao) extends Controller {

  def cAnaBefore = Action { implicit request =>
    Ok(views.html.user.multiOmics.cAna())
  }

  def cAna = Action(parse.multipartFormData) { implicit request =>
    val data = formTool.cAnaForm.bindFromRequest.get
    val tmpDir = tool.createTempDirectory("tmpDir")
    val dataFile = new File(tmpDir, "deal.txt")
    val file = request.body.file("file").get
    file.ref.moveTo(dataFile, replace = true)
    val rFile = new File(Utils.rPath, "cCoefficient.R")
    val rCommand = "Rscript  --restore --no-save " + rFile.getAbsolutePath + " --m " + data.method
    val pyFile = new File(Utils.pyPath, "heatmap.py")
    val pyCommand = "python " + pyFile.getAbsolutePath
    Utils.dealFileHeader(dataFile)
    val execCommand = Utils.callScript(tmpDir, shBuffer = ArrayBuffer(rCommand, pyCommand))
    if (!execCommand.isSuccess) {
      tool.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    } else {
      val divFile = new File(tmpDir, "div.txt")
      val divStr = FileUtils.readFileToString(divFile) + Utils.pyScript
      val dataFile = new File(tmpDir, "deal.txt")
      val array = Utils.getDataJson(dataFile)
      val json = Json.obj("div" -> divStr, "json" -> array)
      tool.deleteDirectory(tmpDir)
      Ok(json)
    }

  }

  def geneWiseBefore = Action { implicit request =>
    Ok(views.html.user.multiOmics.geneWise())
  }

  def muscleBefore = Action { implicit request =>
    Ok(views.html.user.multiOmics.muscle())
  }

  def geneWise = Action(parse.multipartFormData) { implicit request =>
    val data = formTool.geneWiseForm.bindFromRequest.get
    val tmpDir = tool.createTempDirectory("tmpDir")
    val proteinFile = new File(tmpDir, "protein.fa")
    data.proteinMethod match {
      case "text" =>
        FileUtils.writeStringToFile(proteinFile, data.proteinText)
      case "file" =>
        val file = request.body.file("proteinFile").get
        file.ref.moveTo(proteinFile, replace = true)
    }
    val dnaFile = new File(tmpDir, "dna.fa")
    data.dnaMethod match {
      case "text" =>
        FileUtils.writeStringToFile(dnaFile, data.dnaText)
      case "file" =>
        val file = request.body.file("dnaFile").get
        file.ref.moveTo(dnaFile, replace = true)
    }
    val outFile = new File(tmpDir, "out.txt")
    val commandBuffer = ArrayBuffer("genewise " + Utils.dosPath2Unix(proteinFile) + " " + Utils.dosPath2Unix(dnaFile) + " -kbyte 4000 " +
      " -init " + data.init + " -null  " + data.random + " -alg " + data.alg)
    if (data.para) commandBuffer += "-para"
    if (data.pretty) commandBuffer += "-pretty"
    if (data.genes) commandBuffer += "-genes"
    if (data.trans) commandBuffer += "-trans"
    if (data.cdna) commandBuffer += "-cdna"
    if (data.embl) commandBuffer += "-embl"
    if (data.ace) commandBuffer += "-ace"
    if (data.gff) commandBuffer += "-gff"
    if (data.diana) commandBuffer += "-diana"
    data.splice match {
      case "flat" =>
      case "model" => commandBuffer += "-nosplice_gtag"
    }
    val command1 = s"${commandBuffer.mkString(" ")} >${Utils.dosPath2Unix(outFile)}"
    val execCommand = Utils.callLinuxScript(tmpDir, shBuffer = ArrayBuffer(command1))
    if (execCommand.isSuccess) {
      val str = FileUtils.readFileToString(outFile)
      tool.deleteDirectory(tmpDir)
      Ok(Json.toJson(str))
    } else {
      tool.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def downloadTree = Action { implicit request =>
    val data = formTool.svgForm.bindFromRequest().get
    val dataFile = Files.createTempFile("tmp", ".svg").toFile
    val str = data.svgHtml.replaceAll("</svg>", "\n" + Utils.phylotreeCss + "</svg>")
    FileUtils.writeStringToFile(dataFile, str, "UTF-8")
    Ok.sendFile(dataFile, onClose = () => {
      Files.deleteIfExists(dataFile.toPath)
    }).withHeaders(
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> "attachment; filename=tree.svg",
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def muscle = Action(parse.multipartFormData) { implicit request =>
    val data = formTool.muscleForm.bindFromRequest.get
    val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val kind = "muscle"
    val treeStr = data.tree match {
      case "none" => "none"
      case "tree1" => "From first iteration"
      case "tree2" => "From second iteration"
    }
    val args = ArrayBuffer(
      s"Output tree :${treeStr}"
    )
    val argStr = args.mkString(";")
    val row = MissionRow(0, s"${missionName}", userId, kind, argStr, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val (tmpDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
    val seqFile = new File(tmpDir, "seq.txt")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(seqFile, data.text)
      case "file" =>
        val file = request.body.file("proteinFile").get
        file.ref.moveTo(seqFile, replace = true)
    }
    val size = FileUtils.sizeOf(seqFile);
    val mSize = size / (1024 * 1024);
    if (mSize >= 1) {
      Utils.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> "Sequences is too long(over 1MB)!"))
    } else {
      val outFile = new File(resultDir, "out.txt")
      val commandBuffer = ArrayBuffer[String]("muscle " + "-in " + Utils.dosPath2Unix(seqFile) + " -verbose " + " -log " +
        " -fasta " + " -out " + Utils.dosPath2Unix(outFile) + " -group")
      val treeFile = new File(resultDir, "tree.txt")
      data.tree match {
        case "none" =>
        case "tree1" => commandBuffer ++= ArrayBuffer(" -tree1 ", Utils.dosPath2Unix(treeFile))
        case "tree2" => commandBuffer ++= ArrayBuffer(" -tree2 ", Utils.dosPath2Unix(treeFile))
      }
      val command1 = commandBuffer.mkString(" ")
      val command =
        s"""
           |dos2unix *
           |${command1}
         """.stripMargin
      val shBuffer = ArrayBuffer(command)
      missionExecutor.execLinux(shBuffer)
      Redirect(routes.GenomeController.getAllMission() + s"?kind=${kind}")
    }
  }

  def muscleResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val treeFile = new File(resultDir, "tree.txt")
    val outFile = new File(resultDir, "out.txt")
    val str = FileUtils.readFileToString(outFile)
    val treeStr = if (treeFile.exists()) {
      FileUtils.readFileToString(treeFile)
    } else ""
    Ok(Json.obj("out" -> str, "tree" -> treeStr))
  }

}
