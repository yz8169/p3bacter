package utils

import java.io.{File, FileInputStream, FileOutputStream}
import java.lang.reflect.Field

import org.apache.batik.transcoder.{TranscoderInput, TranscoderOutput}
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
//import org.apache.commons.math3.stat.StatUtils
//import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
//import org.saddle.io._
//import CsvImplicits._
//import javax.imageio.ImageIO
import org.apache.commons.codec.binary.Base64
//import org.apache.pdfbox.pdmodel.PDDocument
//import org.apache.pdfbox.rendering.PDFRenderer
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
//import scala.math.log10

object Utils {

  val dbName = "p3_database"
  val windowsPath = "D:\\databases\\p3_database"
  //  val linuxPath = "/mnt/sdb/yinzheng/projects/play/p3_database"
  //  val playPath = "/root/projects/play"
  val playPath = "/mnt/vdb/projects/play"
  val linuxPath = playPath + "/p3_database"
  val isWindows = {
    if (new File(windowsPath).exists()) true else false
  }

  val orthoMclStr = "orthoMcl"
  val mummerStr = "mummer"
  val ardbStr = "ardb"
  val cazyStr = "cazy"


  val path = {
    if (new File(windowsPath).exists()) windowsPath else linuxPath
  }
  val dataFile = new File(path, "data")
  val proteinDir = new File(dataFile, "protein")
  val exampleDir = new File(path, s"example")


  val binPath = new File(path, "bin")
  val wsl = if (isWindows) "wsl" else ""
  val anno = new File(binPath, "Anno")
  val orthoMcl = new File(binPath, "ORTHOMCLV1.4")
  val mummer = new File("/mnt/sdb/linait/tools/quickmerge/MUMmer3.23/")
  val blastFile = new File(binPath, "ncbi-blast-2.6.0+/bin/blastn")
  val blastpFile = new File(binPath, "ncbi-blast-2.6.0+/bin/blastp")
  val blastxFile = new File(binPath, "ncbi-blast-2.6.0+/bin/blastx")
  val blast2HtmlFile = new File(binPath, "blast2html-82b8c9722996/blast2html.py")
  val svBin = new File(binPath, "SV_finder_2.2.1/bin")
  val rPath = {
    val rPath = "D:\\workspaceForIDEA\\p3bacter\\rScripts"
    val linuxRPath = linuxPath + "/rScripts"
    if (new File(rPath).exists()) rPath else linuxRPath
  }
  val userDir = new File(path, "user")

  val windowsTestDir = new File("E:\\tmp")
  val linuxTestDir = new File(playPath, "workspace")
  val testDir = if (windowsTestDir.exists()) windowsTestDir else linuxTestDir
  val crisprDir = new File(binPath, s"CRISPRCasFinder")
  val vmatchDir = new File(binPath, s"vmatch-2.3.0-Linux_x86_64-64bit")

  val Rscript = {
    "Rscript"
  }

  val pyPath = {
    val path = "D:\\workspaceForIDEA\\p3bacter\\pyScripts"
    val linuxPyPath = linuxPath + "/pyScripts"
    if (isWindows) new File(path) else new File(linuxPyPath)
  }

  val goPy = {
    val path = "C:\\Python\\python.exe"
    if (new File(path).exists()) path else "python"
  }

  val pyScript =
    """
      |<script>
      |Plotly.Plots.resize(document.getElementById($('#charts').children().eq(0).attr("id")));
      |window.addEventListener("resize", function (ev) {
      |				Plotly.Plots.resize(document.getElementById($('#charts').children().eq(0).attr("id")));
      |					})
      |</script>
      |
    """.stripMargin

  val phylotreeCss =
    """
      |<style>
      |.tree-selection-brush .extent {
      |    fill-opacity: .05;
      |    stroke: #fff;
      |    shape-rendering: crispEdges;
      |}
      |
      |.tree-scale-bar text {
      |  font: sans-serif;
      |}
      |
      |.tree-scale-bar line,
      |.tree-scale-bar path {
      |  fill: none;
      |  stroke: #000;
      |  shape-rendering: crispEdges;
      |}
      |
      |.node circle, .node ellipse, .node rect {
      |fill: steelblue;
      |stroke: black;
      |stroke-width: 0.5px;
      |}
      |
      |.internal-node circle, .internal-node ellipse, .internal-node rect{
      |fill: #CCC;
      |stroke: black;
      |stroke-width: 0.5px;
      |}
      |
      |.node {
      |font: 10px sans-serif;
      |}
      |
      |.node-selected {
      |fill: #f00 !important;
      |}
      |
      |.node-collapsed circle, .node-collapsed ellipse, .node-collapsed rect{
      |fill: black !important;
      |}
      |
      |.node-tagged {
      |fill: #00f;
      |}
      |
      |.branch {
      |fill: none;
      |stroke: #999;
      |stroke-width: 2px;
      |}
      |
      |.clade {
      |fill: #1f77b4;
      |stroke: #444;
      |stroke-width: 2px;
      |opacity: 0.5;
      |}
      |
      |.branch-selected {
      |stroke: #f00 !important;
      |stroke-width: 3px;
      |}
      |
      |.branch-tagged {
      |stroke: #00f;
      |stroke-dasharray: 10,5;
      |stroke-width: 2px;
      |}
      |
      |.branch-tracer {
      |stroke: #bbb;
      |stroke-dasharray: 3,4;
      |stroke-width: 1px;
      |}
      |
      |
      |.branch-multiple {
      |stroke-dasharray: 5, 5, 1, 5;
      |stroke-width: 3px;
      |}
      |
      |.branch:hover {
      |stroke-width: 10px;
      |}
      |
      |.internal-node circle:hover, .internal-node ellipse:hover, .internal-node rect:hover {
      |fill: black;
      |stroke: #CCC;
      |}
      |
      |.tree-widget {
      |}
      |</style>
    """.stripMargin

  def file2Lines(file: File) = {
    FileUtils.readLines(file).asScala
  }

  def lines2File(file: File, lines: mutable.Buffer[String], append: Boolean = false) = {
    FileUtils.writeLines(file, lines.asJava, append)
  }

  def createDirectoryWhenNoExist(file: File): Unit = {
    if (!file.exists && !file.isDirectory) file.mkdir
  }

  def getPrefix(file: File) = {
    val fileName = file.getName
    val index = fileName.lastIndexOf(".")
    fileName.substring(0, index)
  }

  def svg2png(file: File) = {
    val input = new TranscoderInput(file.toURI.toString)
    val outFile = new File(file.getParent, s"${Utils.getPrefix(file)}.png")
    val outStream = new FileOutputStream(outFile)
    val output = new TranscoderOutput(outStream)
    val t = new PNGTranscoder()
    t.transcode(input, output)
    outStream.flush()
    outStream.close()
  }

  def callLinuxScript(tmpDir: File, shBuffer: ArrayBuffer[String]) = {
    val execCommand = new ExecCommand
    val runFile = new File(tmpDir, "run.sh")
    FileUtils.writeLines(runFile, shBuffer.asJava)
    val dos2Unix = s"${Utils.command2Wsl("dos2unix")} ${dosPath2Unix(runFile)} "
    val shCommand = s"${Utils.command2Wsl("sh")} ${dosPath2Unix(runFile)}"
    execCommand.exec(dos2Unix, shCommand, tmpDir)
    execCommand
  }

  def command2Wsl(command: String) = {
    if (isWindows) s"wsl ${command}" else command
  }

  def dosPath2Unix(file: File) = {
    val path = file.getAbsolutePath
    path.replace("\\", "/").replaceAll("D:", "/mnt/d").replaceAll("E:", "/mnt/e")
  }

  def callScript(tmpDir: File, shBuffer: ArrayBuffer[String]) = {
    val execCommand = new ExecCommand
    val runFile = if (Utils.isWindows) {
      new File(tmpDir, "run.bat")
    } else {
      new File(tmpDir, "run.sh")
    }
    FileUtils.writeLines(runFile, shBuffer.asJava)
    val shCommand = runFile.getAbsolutePath
    if (Utils.isWindows) {
      execCommand.exec(shCommand, tmpDir)
    } else {
      val useCommand = "chmod +x " + runFile.getAbsolutePath
      val dos2Unix = "dos2unix " + runFile.getAbsolutePath
      execCommand.exec(dos2Unix, useCommand, shCommand, tmpDir)
    }
    execCommand
  }


  def deleteDirectory(direcotry: File) = {
    try {
      FileUtils.deleteDirectory(direcotry)
    } catch {
      case _ =>
    }
  }

  def getTime(startTime: Long) = {
    val endTime = System.currentTimeMillis()
    (endTime - startTime) / 1000.0
  }

  def isDoubleP(value: String, p: Double => Boolean): Boolean = {
    try {
      val dbValue = value.toDouble
      p(dbValue)
    } catch {
      case _: Exception =>
        false
    }
  }

  def getGroupNum(content: String) = {
    content.split(";").size
  }

  def productGroupFile(tmpDir: File, content: String) = {
    val groupFile = new File(tmpDir, "group.txt")
    val groupLines = ArrayBuffer("Sample\tGroup")
    groupLines ++= content.split(";").flatMap { group =>
      val groupName = group.split(":")(0)
      val sampleNames = group.split(":")(1).split(",")
      sampleNames.map { sampleName =>
        sampleName + "\t" + groupName
      }
    }
    FileUtils.writeLines(groupFile, groupLines.asJava)
  }

  def getMap(content: String) = {
    val map = mutable.LinkedHashMap[String, mutable.Buffer[String]]()
    content.split(";").foreach { x =>
      val columns = x.split(":")
      map += (columns(0) -> columns(1).split(",").toBuffer)
    }
    map
  }

  def getGroupNames(content: String) = {
    val map = getMap(content)
    map.keys.toBuffer

  }

  def replaceByRate(dataFile: File, rate: Double) = {
    val buffer = FileUtils.readLines(dataFile).asScala
    val array = buffer.map(_.split("\t"))
    val minValue = array.drop(1).flatMap(_.tail).filter(Utils.isDoubleP(_, _ > 0)).map(_.toDouble).min
    val rateMinValue = minValue * rate
    for (i <- 1 until array.length; j <- 1 until array(i).length) {
      if (Utils.isDoubleP(array(i)(j), _ == 0)) array(i)(j) = rateMinValue.toString
      if (StringUtils.isBlank(array(i)(j))) array(i)(j) = rateMinValue.toString
    }
    FileUtils.writeLines(dataFile, array.map(_.mkString("\t")).asJava)
  }

  def replaceByValue(dataFile: File, assignValue: String) = {
    val buffer = FileUtils.readLines(dataFile).asScala
    val array = buffer.map(_.split("\t"))
    for (i <- 1 until array.length; j <- 1 until array(i).length) {
      if (Utils.isDoubleP(array(i)(j), _ == 0)) array(i)(j) = assignValue
      if (StringUtils.isBlank(array(i)(j))) array(i)(j) = assignValue
    }
    FileUtils.writeLines(dataFile, array.map(_.mkString("\t")).asJava)
  }

  def relace0byNan(dataFile: File) = {
    val buffer = FileUtils.readLines(dataFile).asScala
    val array = buffer.map(_.split("\t"))
    for (i <- 1 until array.length; j <- 1 until array(i).length) {
      if (Utils.isDoubleP(array(i)(j), _ == 0)) array(i)(j) = "NA"
    }
    FileUtils.writeLines(dataFile, array.map(_.mkString("\t")).asJava)
  }

  def innerNormal(dataFile: File, colName: String, rowName: String) = {
    val buffer = FileUtils.readLines(dataFile).asScala
    val array = buffer.map(_.split("\t"))
    val colNum = array.take(1).flatten.indexOf(colName)
    val rowNum = array.map(_ (0)).indexOf(rowName)
    val div = array(rowNum)(colNum).toDouble
    val divArray = array(rowNum).drop(1).map(div / _.toDouble)
    for (i <- 1 until array.length; j <- 1 until array(i).length) {
      array(i)(j) = (array(i)(j).toDouble * divArray(j - 1)).toString
    }
    FileUtils.writeLines(dataFile, array.map(_.mkString("\t")).asJava)
  }

  def qcNormal(dataFile: File, colName: String) = {
    val buffer = FileUtils.readLines(dataFile).asScala
    val array = buffer.map(_.split("\t"))
    val colNum = array.head.indexOf(colName)
    for (i <- 1 until array.length) {
      val div = array(i)(colNum).toDouble
      for (j <- 1 until array(i).length) {
        array(i)(j) = (array(i)(j).toDouble / div).toString
      }
    }
    FileUtils.writeLines(dataFile, array.map(_.mkString("\t")).asJava)
  }

  def lfJoin(seq: Seq[String]) = {
    seq.mkString("\n")
  }

  def execFuture[T](f: Future[T]): T = {
    Await.result(f, Duration.Inf)
  }

  def getValue[T](kind: T): String = {
    kind match {
      case x if x.isInstanceOf[DateTime] => val time = x.asInstanceOf[DateTime]
        time.toString("yyyy-MM-dd HH:mm:ss")
      case x if x.isInstanceOf[Option[T]] => val option = x.asInstanceOf[Option[T]]
        if (option.isDefined) getValue(option.get) else "暂无"
      case _ => kind.toString
    }
  }

  def getArrayByTs[T](x: Seq[T]) = {
    x.map { y =>
      y.getClass.getDeclaredFields.toBuffer.map { x: Field =>
        x.setAccessible(true)
        val kind = x.get(y)
        val value = getValue(kind)
        (x.getName, value)
      }.init.toMap
    }
  }

  def getJsonByTs[T](x: Seq[T]) = {
    val array = getArrayByTs(x)
    Json.toJson(array)
  }

  def peakAreaNormal(dataFile: File, coefficient: Double) = {
    val buffer = FileUtils.readLines(dataFile).asScala
    val array = buffer.map(_.split("\t"))
    val sumArray = new Array[Double](array(0).length)
    for (i <- 1 until array.length; j <- 1 until array(i).length) {
      sumArray(j) += array(i)(j).toDouble
    }
    for (i <- 1 until array.length; j <- 1 until array(i).length) {
      array(i)(j) = (coefficient * array(i)(j).toDouble / sumArray(j)).toString
    }
    FileUtils.writeLines(dataFile, array.map(_.mkString("\t")).asJava)
  }

  //
  //  def log2(x: Double) = log10(x) / log10(2.0)
  //
  //  def getStdErr(values: Array[Double]) = {
  //    val standardDeviation = new StandardDeviation
  //    val stderr = standardDeviation.evaluate(values) / Math.sqrt(values.length)
  //    stderr
  //  }

  def dealGeneIds(geneId: String) = {
    geneId.split("\n").map(_.trim).distinct.toBuffer
  }

  def getDataJson(file: File) = {
    val lines = FileUtils.readLines(file).asScala
    val sampleNames = lines.head.split("\t").drop(1)
    val array = lines.drop(1).map { line =>
      val columns = line.split("\t")
      val map = mutable.Map[String, String]()
      map += ("geneId" -> columns(0))
      sampleNames.zip(columns.drop(1)).foreach { case (sampleName, data) =>
        map += (sampleName -> data)
      }
      map
    }
    Json.obj("array" -> array, "sampleNames" -> sampleNames)
  }

  def dealInputFile(file: File) = {
    val lines = FileUtils.readLines(file).asScala
    val buffer = lines.map(_.trim)
    FileUtils.writeLines(file, buffer.asJava)
  }

  def dealFileHeader(file: File) = {
    val lines = FileUtils.readLines(file).asScala
    val headers = lines(0).split("\t")
    println(headers(0))
    headers(0) = ""
    lines(0) = headers.mkString("\t")
    FileUtils.writeLines(file, lines.asJava)
  }


  //  def pdf2png(tmpDir: File, fileName: String) = {
  //    val pdfFile = new File(tmpDir, fileName)
  //    val outFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".png"
  //    val outFile = new File(tmpDir, outFileName)
  //    val document = PDDocument.load(pdfFile)
  //    val renderer = new PDFRenderer(document)
  //    ImageIO.write(renderer.renderImage(0, 3), "png", outFile)
  //    document.close()
  //  }
  //
  def getInfoByFile(file: File) = {
    val lines = FileUtils.readLines(file).asScala
    val columnNames = lines.head.split("\t").drop(1)
    val array = lines.drop(1).map { line =>
      val columns = line.split("\t")
      val map = mutable.Map[String, String]()
      map += ("geneId" -> columns(0))
      columnNames.zip(columns.drop(1)).foreach { case (columnName, data) =>
        map += (columnName -> data)
      }
      map
    }
    (columnNames, array)
  }

  def checkFile(file: File): (Boolean, String) = {
    val buffer = FileUtils.readLines(file).asScala
    val headers = buffer.head.split("\t")
    var error = ""
    if (headers.size < 2) {
      error = "错误：文件列数小于2！"
      return (false, error)
    }
    val headersNoHead = headers.drop(1)
    val repeatElement = headersNoHead.diff(headersNoHead.distinct).distinct.headOption
    repeatElement match {
      case Some(x) => val nums = headers.zipWithIndex.filter(_._1 == x).map(_._2 + 1).mkString("(", "、", ")")
        error = "错误：样品名" + x + "在第" + nums + "列重复出现！"
        return (false, error)
      case None =>
    }

    val ids = buffer.drop(1).map(_.split("\t")(0))
    val repeatid = ids.diff(ids.distinct).distinct.headOption
    repeatid match {
      case Some(x) => val nums = ids.zipWithIndex.filter(_._1 == x).map(_._2 + 2).mkString("(", "、", ")")
        error = "错误：第一列:" + x + "在第" + nums + "行重复出现！"
        return (false, error)
      case None =>
    }

    val headerSize = headers.size
    for (i <- 1 until buffer.size) {
      val columns = buffer(i).split("\t")
      if (columns.size != headerSize) {
        error = "错误：数据文件第" + (i + 1) + "行列数不对！"
        return (false, error)
      }

    }

    for (i <- 1 until buffer.size) {
      val columns = buffer(i).split("\t")
      for (j <- 1 until columns.size) {
        val value = columns(j)
        if (!isDouble(value)) {
          error = "错误：数据文件第" + (i + 1) + "行第" + (j + 1) + "列不为数字！"
          return (false, error)
        }
      }
    }
    (true, error)
  }

  def isDouble(value: String): Boolean = {
    try {
      value.toDouble
    } catch {
      case _: Exception =>
        return false
    }
    true
  }

  def getBase64Str(imageFile: File): String = {
    val inputStream = new FileInputStream(imageFile)
    val bytes = IOUtils.toByteArray(inputStream)
    val bytes64 = Base64.encodeBase64(bytes)
    inputStream.close()
    new String(bytes64)
  }

}
