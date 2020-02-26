package controllers

import java.io.File
import java.nio.file.Files

import akka.actor._
import akka.stream.Materializer
import dao.MissionDao
import javax.inject.Inject
import models.Tables.MissionRow
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import org.zeroturnaround.zip.ZipUtil
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import utils.{ExecCommand, MissionExecutor, Utils}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by yz on 2018/5/11
  */
class GenomeController @Inject()(tool: Tool, missionDao: MissionDao,
                                 formTool: FormTool)(implicit val system: ActorSystem, implicit val materializer: Materializer) extends Controller {

  def blastGeneBefore = Action { implicit request =>
    Ok(views.html.user.genome.blastGene())
  }

  case class QueryData(method: String, queryText: String, db: String, evalue: String, wordSize: String, maxTargetSeqs: String)

  val queryForm = Form(
    mapping(
      "method" -> text,
      "queryText" -> text,
      "db" -> text,
      "evalue" -> text,
      "wordSize" -> text,
      "maxTargetSeqs" -> text
    )(QueryData.apply)(QueryData.unapply)
  )

  def blastGene = Action(parse.multipartFormData) { implicit request =>
    val data = queryForm.bindFromRequest.get
    val tmpDir = tool.createTempDirectory("tmpDir")
    val seqFile = new File(tmpDir, "seq.fasta")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(seqFile, data.queryText)
      case "file" =>
        val file = request.body.file("file").get
        file.ref.moveTo(seqFile, replace = true)
    }
    val outXmlFile = new File(tmpDir, "data.xml")
    val outHtmlFile = new File(tmpDir, "data.html")
    val cdsFile = new File(Utils.dataFile, s"${data.db}.cds.fa")
    val command =
      s"""
         |${Utils.dosPath2Unix(Utils.blastFile)} -query ${Utils.dosPath2Unix(seqFile)} -subject ${Utils.dosPath2Unix(cdsFile)} -outfmt 5 -evalue ${data.evalue} -max_target_seqs ${data.maxTargetSeqs} -word_size ${data.wordSize} -out ${Utils.dosPath2Unix(outXmlFile)}
         |python ${Utils.dosPath2Unix(Utils.blast2HtmlFile)} -i ${Utils.dosPath2Unix(outXmlFile)} -o ${Utils.dosPath2Unix(outHtmlFile)}
         |perl ${Utils.dosPath2Unix(Utils.anno)}/anno_st/Blast2table -format 10 -xml data.xml -header -top >data.table.xls
       """.stripMargin
    val execCommand = Utils.callLinuxScript(tmpDir, shBuffer = ArrayBuffer(command))
    if (execCommand.isSuccess) {
      val html = FileUtils.readFileToString(outHtmlFile)
      val file1 = new File(tmpDir, "data.table.xls")
      val base64 = FileUtils.readFileToString(file1)
      tool.deleteDirectory(tmpDir)
      Ok(Json.obj("html" -> html, "table" -> base64))
    } else {
      tool.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def blastGenomeBefore = Action { implicit request =>
    Ok(views.html.user.genome.blastGenome())
  }

  def blastGenome = Action(parse.multipartFormData) { implicit request =>
    val data = queryForm.bindFromRequest.get
    val tmpDir = tool.createTempDirectory("tmpDir")
    val seqFile = new File(tmpDir, "seq.fasta")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(seqFile, data.queryText)
      case "file" =>
        val file = request.body.file("file").get
        file.ref.moveTo(seqFile, replace = true)
    }
    val outXmlFile = new File(tmpDir, "data.xml")
    val outHtmlFile = new File(tmpDir, "data.html")
    val faFile = new File(Utils.dataFile, s"${data.db}.genome.fa")
    val command =
      s"""
         |${Utils.dosPath2Unix(Utils.blastFile)} -query ${Utils.dosPath2Unix(seqFile)} -subject ${Utils.dosPath2Unix(faFile)} -outfmt 5 -evalue ${data.evalue} -max_target_seqs ${data.maxTargetSeqs} -word_size ${data.wordSize} -out ${Utils.dosPath2Unix(outXmlFile)}
         |python ${Utils.dosPath2Unix(Utils.blast2HtmlFile)} -i ${Utils.dosPath2Unix(outXmlFile)} -o ${Utils.dosPath2Unix(outHtmlFile)}
         |perl ${Utils.dosPath2Unix(Utils.anno)}/anno_st/Blast2table -format 10 -xml data.xml -header -top >data.table.xls
       """.stripMargin
    val execCommand = Utils.callLinuxScript(tmpDir, shBuffer = ArrayBuffer(command))
    if (execCommand.isSuccess) {
      val html = FileUtils.readFileToString(outHtmlFile)
      val file1 = new File(tmpDir, "data.table.xls")
      val base64 = FileUtils.readFileToString(file1)
      tool.deleteDirectory(tmpDir)
      Ok(Json.obj("html" -> html, "table" -> base64))
    } else {
      tool.deleteDirectory(tmpDir)
      Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
    }
  }

  def phyTreeBefore = Action { implicit request =>
    Ok(views.html.user.genome.phyTree())
  }

  def phyTree = Action { implicit request =>
    val data = formTool.phyTreeForm.bindFromRequest.get
    if (data.sampleNames.contains(data.refSampleName)) {
      Ok(Json.obj("valid" -> "false", "message" -> "样品不能包含参考样品!"))
    } else {
          val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
      val userId = tool.getUserId
      val kind = "phyTree"
      val args = ArrayBuffer(
        s"Reference sample:${data.refSampleName}",
        s"Sample:${data.sampleNames.mkString("<br>")}"
      )
      val refSampleName = tool.getSampleName(data.refSampleName)
      val sampleNames = data.sampleNames.map(x => tool.getSampleName(x))
      val argStr = args.mkString(";")
      val row = MissionRow(0, s"${missionName}", userId, kind, argStr, new DateTime(), None, "running")
      val missionExecutor = new MissionExecutor(missionDao, tool, row)
      val (tmpDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
      val file = tool.getGenomeFile(refSampleName)
      FileUtils.copyFileToDirectory(file, tmpDir)
      val lines = ArrayBuffer(s"ref=${file.getName}")
      sampleNames.foreach { sampleName =>
        val file = tool.getGenomeFile(sampleName)
        FileUtils.copyFileToDirectory(file, tmpDir)
        lines += s"${sampleName}=${file.getName}"
      }
      val faListFile = new File(tmpDir, "seq.list")
      FileUtils.writeLines(faListFile, lines.asJava)
      val command =
        s"""
           |dos2unix *
           |perl ${Utils.dosPath2Unix(Utils.binPath)}/09.Snp/src/find_repeat_for_bac_snp.pl  --ref ${file.getName} -out dup.out
           |perl ${Utils.dosPath2Unix(Utils.binPath)}/09.Snp/src/MUMmerSnpPipline.V2.3.pl -lib seq.list -dup dup.out -dir out -readlist reads_info.list -pileup_q 20 -pileup_e 5 -pileup_n 10
           |perl ${Utils.dosPath2Unix(Utils.binPath)}/13.Phylogenetic_tree/src/snp2fa_v2.pl out/clean.snp.pileup2 all.mfa ${refSampleName}
           |perl ${Utils.dosPath2Unix(Utils.binPath)}/13.Phylogenetic_tree/src/02.phylogeny/bin/phylo_tree.pl all.mfa -format mfa -type ml -b '-4' -d nt -outdir treeOut
           |cp treeOut/tree.png ${Utils.dosPath2Unix(resultDir)}/
           |cp treeOut/tree.newick ${Utils.dosPath2Unix(resultDir)}/
           |cp treeOut/tree.svg ${Utils.dosPath2Unix(resultDir)}/
           |cp treeOut/tree.root.svg ${Utils.dosPath2Unix(resultDir)}/
               """.stripMargin
      val shBuffer = ArrayBuffer(command)
      missionExecutor.execLinux(shBuffer)
      Redirect(routes.GenomeController.getAllMission() + s"?kind=${kind}")
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

  def phyTreeResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val treeFile = new File(resultDir, "tree.newick")
    val treeStr = FileUtils.readFileToString(treeFile)
    Ok(Json.obj("tree" -> treeStr))
  }


  def cazyAnnoBefore = Action { implicit request =>
    Ok(views.html.user.genome.cazyAnno())
  }

  def vfdbAnnoBefore = Action { implicit request =>
    Ok(views.html.user.genome.vfdbAnno())
  }

  def phiAnnoBefore = Action { implicit request =>
    Ok(views.html.user.genome.phiAnno())
  }

  def tcdbAnnoBefore = Action { implicit request =>
    Ok(views.html.user.genome.tcdbAnno())
  }

  def ardbAnnoBefore = Action { implicit request =>
    Ok(views.html.user.genome.ardbAnno())
  }

  def orthoMclBefore = Action { implicit request =>
    Ok(views.html.user.genome.orthoMcl())
  }

  def mummerBefore = Action { implicit request =>
    Ok(views.html.user.genome.mummer())
  }

  def annoBefore = Action { implicit request =>
    Ok(views.html.user.genome.anno())
  }

  def anno = Action(parse.multipartFormData) { implicit request =>
    val data = formTool.annoForm.bindFromRequest().get
        val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val kind = "anno"
    val args = if (data.annoMethods.contains("kegg")) {
      ArrayBuffer(
        s"Evalue:${data.eValue}",
        s"Max target seqs:${data.maxTargetSeqs}"
      ).mkString(";")
    } else {
      ""
    }
    val row = MissionRow(0, s"${missionName}", userId, kind, args, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val (tmpDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
    val dataFile = new File(tmpDir, "seq.fasta")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(dataFile, data.queryText)
      case "file" =>
        val file = request.body.file("file").get
        file.ref.moveTo(dataFile, replace = true)
    }
    val commands = ArrayBuffer[String]()
    val mergeCommands = ArrayBuffer(s"perl ${Utils.anno}/anno_st/mergeAnnotation.pl -fa ${dataFile}")
    val keggDir = new File(resultDir, "KEGG")
    if (data.annoMethods.contains("kegg")) {
      Utils.createDirectoryWhenNoExist(keggDir)
      val diamondFile = new File(Utils.binPath, "diamond/diamond")
      val keggCommand =
        s"""
           |${diamondFile} blastp -d ${Utils.binPath}/kobas-2.1.1/seq_pep/ko  -q ${dataFile} -o old.m8 -f 6 --evalue ${data.eValue} -k ${data.maxTargetSeqs} -b 8 -t ${tmpDir}
           |perl ${Utils.anno}/diamond.m8.2.blast.m8.pl ${Utils.binPath}/kobas-2.1.1/seq_pep/ko.map.txt old.m8 >vs.kobas.m8
           |python ${Utils.binPath}/kobas-2.1.1/scripts/annotate.py -i vs.kobas.m8 -t blastout:tab -s ko -o ${new File(keggDir, "pathway.txt")}
               """.stripMargin
      commands += keggCommand
      mergeCommands += s"-kobas ${new File(keggDir, "pathway.txt")}"
    }

    if (data.annoMethods.contains("go")) {
      val goDir = new File(resultDir, "GO")
      Utils.createDirectoryWhenNoExist(goDir)
      val goTxtFile = new File(goDir, "GO.list.txt")
      val goCommand =
        s"""
           |python ${Utils.binPath}/eggnog-mapper/emapper.py -i ${dataFile} --output eggNOG.m8.xls -m diamond --cpu 40
           |grep -v \\# eggNOG.m8.xls.emapper.annotations |cut -f 1,6 |grep \\GO |sed -e 's/,/;/g' >${goTxtFile}
           |python ${Utils.binPath}/goatools-0.5.7/scripts/gene_ontology.py --level 1 --input ${goTxtFile} --output ${new File(goDir, "level2.go.txt")}
           |perl   ${Utils.anno}/anno_st/go-bar.pl -i ${new File(goDir, "level2.go.txt")} -o ${new File(goDir, "level2.go.txt.pdf")}
               """.stripMargin
      commands += goCommand
      mergeCommands += s"-go ${goTxtFile}"
    }
    mergeCommands += s">${new File(resultDir, "annotation.table.xls")}"
    commands += mergeCommands.mkString(" ")
    missionExecutor.exec(commands).map { x =>
      if (data.annoMethods.contains("kegg")) {
        val keggFile = new File(keggDir, "pathway.txt")
        val cleanFile = new File(keggDir, "pathway.clean.txt")
        val lines = FileUtils.readLines(keggFile).asScala
        val newLines = lines.filter(_.split("\t").size == 2).filter(_.contains("http")).map { line =>
          val lines = line.split("\t")
          lines(1) = lines(1).takeWhile(_ != '|')
          lines.mkString("\t")
        }
        FileUtils.writeLines(cleanFile, newLines.asJava)
      }
    }

    Redirect(routes.GenomeController.getAllMission() + s"?kind=${kind}")
  }

  def annoResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val file = new File(resultDir, "annotation.table.xls")
    val (columnNames, array) = Utils.getInfoByFile(file)
    val json = Json.obj("columnNames" -> columnNames, "array" -> array)
    Ok(json)
  }

  def ardbAnno = Action(parse.multipartFormData) { implicit request =>
    val data = formTool.ardbForm.bindFromRequest.get
        val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val kind = Utils.ardbStr
    val args = ArrayBuffer(
      s"Evalue:${data.evalue}"
    ).mkString(";")
    val row = MissionRow(0, s"${missionName}", userId, kind, args, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val (tmpDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
    val seqFile = new File(tmpDir, "seq.fasta")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(seqFile, data.queryText)
      case "file" =>
        val file = request.body.file("file").get
        file.ref.moveTo(seqFile, replace = true)
    }
    val command1 =
      s"""
         |perl ${Utils.dosPath2Unix(Utils.anno)}/4-13.ardb_anno.pl -e ${data.evalue}  ${Utils.dosPath2Unix(new File(tmpDir, "seq.fasta"))} ${Utils.dosPath2Unix(Utils.anno)}/ardbAnno1.0/ ./
         |cp ardb.anno.xls ${Utils.dosPath2Unix(resultDir)}
       """.stripMargin
    val shBuffer = ArrayBuffer(command1)
    missionExecutor.execLinux(shBuffer)
    Redirect(routes.GenomeController.getAllMission() + s"?kind=${kind}")
  }

  def ardbResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val file = new File(resultDir, "ardb.anno.xls")
    val (columnNames, array) = Utils.getInfoByFile(file)
    val json = Json.obj("columnNames" -> columnNames, "array" -> array)
    Ok(json)
  }

  def crisprBefore = Action { implicit request =>
    Ok(views.html.user.genome.crispr())
  }

  def crispr = Action(parse.multipartFormData) { implicit request =>
    val data = formTool.crisprForm.bindFromRequest.get
        val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val kind = "crispr"
    val args = ArrayBuffer(
      s"Minimal Repeat Length:${data.minDR}",
      s"Maximal Repeat Length:${data.maxDR}",
      s"Allow Repeat Mismatch:${if (data.arm.isDefined) "yes" else "no"}",
      s"Minimal Spacers size in function of Repeat size:${data.percSPmin}",
      s"Maximal Spacers size in function of Repeat size:${data.percSPmax}",
      s"Maximal allowed percentage of similarity between Spacers:${data.spSim}",
      s"Percentage mismatchs allowed between Repeats:${data.mismDRs}",
      s"Percentage mismatchs allowed for truncated Repeat:${data.truncDR}",
      s"Size of Flanking regions in base pairs (bp) for each analyzed CRISPR array:${data.flank}",
      s"Alternative detection of truncated repeat:${if (data.dt.isDefined) "yes" else "no"}",
      s"Perform CAS gene detection:${if (data.cas.isDefined) "yes" else "no"}"
    )
    val defValue = data.defValue match {
      case "S" => "SubTyping"
      case "T" => "Typing"
      case "G" => "General (permissive)"
    }
    if (data.cas.isDefined) {
      args ++= ArrayBuffer(
        s"Clustering model:${defValue}",
        s"Unordered:${if (data.meta.isDefined) "yes" else "no"}"
      )
    }
    val argStr = args.mkString(";")
    val row = MissionRow(0, s"${missionName}", userId, kind, argStr, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val (tmpDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
    val seqFile = new File(tmpDir, "seq.fa")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(seqFile, data.queryText)
      case "file" =>
        val file = request.body.file("file").get
        file.ref.moveTo(seqFile, replace = true)
    }
    val commandBuffer = ArrayBuffer(s"export PATH=$$PATH:/${Utils.dosPath2Unix(Utils.vmatchDir)}\n", s"perl ${Utils.dosPath2Unix(Utils.crisprDir)}/CRISPRCasFinder.pl -in ${Utils.dosPath2Unix(seqFile)} -out ${Utils.dosPath2Unix(resultDir)}/out -so ${Utils.dosPath2Unix(Utils.vmatchDir)}/SELECT/sel392v2.so " +
      s"-drpt ${Utils.dosPath2Unix(Utils.crisprDir)}/supplementary_files/repeatDirection.tsv -rpts ${Utils.dosPath2Unix(Utils.crisprDir)}/supplementary_files/Repeat_List.csv " +
      s"-minDR ${data.minDR} -maxDR ${data.maxDR} -percSPmin ${data.percSPmin} -percSPmax ${data.percSPmax} " +
      s"-spSim ${data.spSim} -mismDRs ${data.mismDRs}  -truncDR ${data.truncDR} -flank ${data.flank} " +
      s"-cf ${Utils.dosPath2Unix(Utils.crisprDir)}/CasFinder-2.0.2 ")
    if (data.arm.isEmpty) commandBuffer += "-noMism"
    if (data.dt.isDefined) commandBuffer += "-betterDetectTrunc"
    if (data.cas.isDefined) {
      commandBuffer += "-cas -rcfowce "
      commandBuffer += s"-def ${data.defValue}"
      if (data.meta.isDefined) commandBuffer += "-meta"
    }
    val shBuffer = ArrayBuffer(commandBuffer.mkString(" "))
    missionExecutor.execLinux(shBuffer)
    Redirect(routes.GenomeController.getAllMission() + s"?kind=${kind}")
  }

  def crisprResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val file = new File(resultDir, "out/result.json")
    val jsonStr = FileUtils.readFileToString(file)
    val json = Json.parse(jsonStr)
    Ok(json)
  }

  case class CazyAnnoData(method: String, queryText: String)

  val cazyForm = Form(
    mapping(
      "method" -> text,
      "queryText" -> text
    )(CazyAnnoData.apply)(CazyAnnoData.unapply)
  )

  def cazyAnno = Action(parse.multipartFormData) { implicit request =>
    val data = cazyForm.bindFromRequest.get
        val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val kind = Utils.cazyStr
    val args = ArrayBuffer(
      "无"
    ).mkString(";")
    val row = MissionRow(0, s"${missionName}", userId, kind, args, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val (tmpDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
    val seqFile = new File(tmpDir, "seq.fasta")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(seqFile, data.queryText)
      case "file" =>
        val file = request.body.file("file").get
        file.ref.moveTo(seqFile, replace = true)
    }
    val command1 =
      s"""
         |perl ${Utils.dosPath2Unix(Utils.anno)}/4-10.cazy_anno.pl seq.fasta data ${Utils.dosPath2Unix(Utils.anno)}/cazy/v4
         |cp data.anno.xls ${Utils.dosPath2Unix(resultDir)}/cazy.anno.xls
       """.stripMargin
    val shBuffer = ArrayBuffer(command1)
    missionExecutor.execLinux(shBuffer).map { x =>
      val file = new File(resultDir, "cazy.anno.xls")
      val lines = Utils.file2Lines(file)
      val headers = lines(0).split("\t")
      headers(1) = "Q-length"
      headers(3) = "S-length"
      lines(0) = headers.mkString("\t")
      Utils.lines2File(file, lines)
    }
    Redirect(routes.GenomeController.getAllMission() + s"?kind=${kind}")
  }

  def cazyResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val file = new File(resultDir, "cazy.anno.xls")
    val (columnNames, array) = Utils.getInfoByFile(file)
    val json = Json.obj("columnNames" -> columnNames, "array" -> array)
    Ok(json)
  }

  case class VfdbAnnoData(method: String, queryText: String, seqType: String, evalue: String, maxTargetSeqs: String)

  val vfdbForm = Form(
    mapping(
      "method" -> text,
      "queryText" -> text,
      "seqType" -> text,
      "evalue" -> text,
      "maxTargetSeqs" -> text
    )(VfdbAnnoData.apply)(VfdbAnnoData.unapply)
  )

  def vfdbAnno = Action(parse.multipartFormData) { implicit request =>
    val data = vfdbForm.bindFromRequest.get
        val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val kind = "vfdb"
    val seqType = if (data.seqType == "p") "protein" else "nucleic acid"
    val args = ArrayBuffer(
      s"Evalue:${data.evalue}",
      s"Max target seqs:${data.maxTargetSeqs}"
    ).mkString(";")
    val row = MissionRow(0, s"${missionName}", userId, kind, args, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val (tmpDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
    val seqFile = new File(tmpDir, "seq.fa")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(seqFile, data.queryText)
      case "file" =>
        val file = request.body.file("file").get
        file.ref.moveTo(seqFile, replace = true)
    }
    val blast = if (data.seqType == "n") Utils.blastxFile else Utils.blastpFile
    val task = if (data.seqType == "p") "blastp-fast" else "blastx-fast"
    val command1 =
      s"""
         |${Utils.dosPath2Unix(blast)} -db ${Utils.dosPath2Unix(Utils.anno)}/VFDB/VFDB_setB_pro.fas -query seq.fa -out data.xml -task ${task} -evalue ${data.evalue} -outfmt 5 -num_threads 4 -max_target_seqs ${data.maxTargetSeqs}
         |perl ${Utils.dosPath2Unix(Utils.anno)}/anno_st/Blast2table -format 10 -xml data.xml -header -top >data.table.xls
         |cp data.table.xls ${Utils.dosPath2Unix(resultDir)}/vfdb.anno.xls
       """.stripMargin
    val shBuffer = ArrayBuffer(command1)
    missionExecutor.execLinux(shBuffer).map { x =>
      val file = new File(resultDir, "vfdb.anno.xls")
      val lines = Utils.file2Lines(file)
      val newLines = lines.map { line =>
        val columns = line.split("\t").toBuffer
        val newColumns = ArrayBuffer(columns(5), columns(11)) ++= (columns -- ArrayBuffer(columns(5), columns(11)))
        newColumns.mkString("\t")
      }
      Utils.lines2File(file, newLines)
    }
    Redirect(routes.GenomeController.getAllMission() + s"?kind=${kind}")
  }

  def vfdbResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val file = new File(resultDir, "vfdb.anno.xls")
    val (columnNames, array) = Utils.getInfoByFile(file)
    val json = Json.obj("columnNames" -> columnNames, "array" -> array)
    Ok(json)
  }

  def phiAnno = Action(parse.multipartFormData) { implicit request =>
    val data = vfdbForm.bindFromRequest.get
        val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val kind = "phi"
    val seqType = if (data.seqType == "p") "protein" else "nucleic acid"
    val args = ArrayBuffer(
      //      s"Sequence type:${seqType}",
      s"Evalue:${data.evalue}",
      s"Max target seqs:${data.maxTargetSeqs}"
    ).mkString(";")
    val row = MissionRow(0, s"${missionName}", userId, kind, args, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val (tmpDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
    val seqFile = new File(tmpDir, "seq.fa")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(seqFile, data.queryText)
      case "file" =>
        val file = request.body.file("file").get
        file.ref.moveTo(seqFile, replace = true)
    }
    val blast = if (data.seqType == "n") Utils.blastxFile else Utils.blastpFile
    val task = if (data.seqType == "p") "blastp-fast" else "blastx-fast"
    val command1 =
      s"""
         |${Utils.dosPath2Unix(blast)} -db ${Utils.dosPath2Unix(Utils.anno)}/PHI/PHI.fasta -query seq.fa -out data.xml -task ${task} -evalue ${data.evalue} -outfmt 5 -num_threads 4 -max_target_seqs ${data.maxTargetSeqs}
         |perl ${Utils.dosPath2Unix(Utils.anno)}/anno_st/Blast2table -format 10 -xml data.xml -header -top >data.table.xls
         |cp data.table.xls ${Utils.dosPath2Unix(resultDir)}/phi.anno.xls
       """.stripMargin
    val shBuffer = ArrayBuffer(command1)
    missionExecutor.execLinux(shBuffer).map { x =>
      val file = new File(resultDir, "phi.anno.xls")
      val lines = Utils.file2Lines(file)
      val newLines = lines.map { line =>
        val columns = line.split("\t").toBuffer
        val newColumns = ArrayBuffer(columns(5), columns(11)) ++= (columns -- ArrayBuffer(columns(5), columns(11)))
        newColumns.mkString("\t")
      }
      Utils.lines2File(file, newLines)
    }
    Redirect(routes.GenomeController.getAllMission() + s"?kind=${kind}")
  }

  def phiResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val file = new File(resultDir, "phi.anno.xls")
    val (columnNames, array) = Utils.getInfoByFile(file)
    val json = Json.obj("columnNames" -> columnNames, "array" -> array)
    Ok(json)
  }

  def getExampleContent = Action { implicit request =>
    val data = formTool.fileNameForm.bindFromRequest().get
    val file = new File(Utils.exampleDir, s"${data.fileName}")
    val str = FileUtils.readFileToString(file, "UTF-8")
    Ok(Json.toJson(str))
  }

  def tcdbAnno = Action(parse.multipartFormData) { implicit request =>
    val data = vfdbForm.bindFromRequest.get
        val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val kind = "tcdb"
    val seqType = if (data.seqType == "p") "protein" else "nucleic acid"
    val args = ArrayBuffer(
      //      s"Sequence type:${seqType}",
      s"Evalue:${data.evalue}",
      s"Max target seqs:${data.maxTargetSeqs}"
    ).mkString(";")
    val row = MissionRow(0, s"${missionName}", userId, kind, args, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val (tmpDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
    val seqFile = new File(tmpDir, "seq.fa")
    data.method match {
      case "text" =>
        FileUtils.writeStringToFile(seqFile, data.queryText)
      case "file" =>
        val file = request.body.file("file").get
        file.ref.moveTo(seqFile, replace = true)
    }
    val blast = if (data.seqType == "n") Utils.blastxFile else Utils.blastpFile
    val task = if (data.seqType == "p") "blastp-fast" else "blastx-fast"
    val command1 =
      s"""
         |${Utils.dosPath2Unix(blast)} -db ${Utils.dosPath2Unix(Utils.anno)}/TCDB/tcdb.fasta -query seq.fa -out data.xml -task ${task} -evalue ${data.evalue} -outfmt 5 -num_threads 4 -max_target_seqs ${data.maxTargetSeqs}
         |perl ${Utils.dosPath2Unix(Utils.anno)}/anno_st/Blast2table -format 10 -xml data.xml -header -top >data.table.xls
         |cp data.table.xls ${Utils.dosPath2Unix(resultDir)}/tcdb.anno.xls
       """.stripMargin
    val shBuffer = ArrayBuffer(command1)
    missionExecutor.execLinux(shBuffer).map { x =>
      val file = new File(resultDir, "tcdb.anno.xls")
      val lines = Utils.file2Lines(file)
      val newLines = lines.map { line =>
        val columns = line.split("\t").toBuffer
        val newColumns = ArrayBuffer(columns(5), columns(11)) ++= (columns -- ArrayBuffer(columns(5), columns(11)))
        newColumns.mkString("\t")
      }
      Utils.lines2File(file, newLines)
    }
    Redirect(routes.GenomeController.getAllMission() + s"?kind=${kind}")
  }

  def tcdbResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val file = new File(resultDir, "tcdb.anno.xls")
    val (columnNames, array) = Utils.getInfoByFile(file)
    val json = Json.obj("columnNames" -> columnNames, "array" -> array)
    Ok(json)
  }

  case class OrthoMclData(sampleNames: Seq[String], pvCutoff: String, piCutoff: String, pmatchCutoff: String)

  val orthoMclForm = Form(
    mapping(
      "sampleNames" -> seq(text),
      "pvCutoff" -> text,
      "piCutoff" -> text,
      "pmatchCutoff" -> text
    )(OrthoMclData.apply)(OrthoMclData.unapply)
  )

  def orthoMcl = Action { implicit request =>
    val data = orthoMclForm.bindFromRequest.get
    val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val args = ArrayBuffer(
      s"样品(多选):${data.sampleNames.mkString("<br>")}",
      s"Pv Cutoff:${data.pvCutoff}",
      s"Pi Cutoff:${data.piCutoff}",
      s"Pmatch Cutoff:${data.pmatchCutoff}"
    ).mkString(";")
    val sampleNames = data.sampleNames.map { tmpSampleName =>
      tool.getSampleName(tmpSampleName)
    }
    val row = MissionRow(0, s"${missionName}", userId, "orthoMcl", args, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val workspaceDir = missionExecutor.workspaceDir
    val resultDir = missionExecutor.resultDir
    sampleNames.foreach { sampleName =>
      val file = tool.getPepFile(sampleName)
      val destFile = new File(workspaceDir, s"${sampleName}.fa")
      FileUtils.copyFile(file, destFile)
    }
    val fileStr = sampleNames.map { sampleName =>
      s"${sampleName}.fa"
    }.mkString(",")

    val listFile = new File(workspaceDir, "fa.list")
    val listBuffer = sampleNames.map { sampleName =>
      s"${sampleName}\t${sampleName}.fa\t${sampleName}"
    }
    FileUtils.writeLines(listFile, listBuffer.asJava)
    val command =
      s"""
         |perl  ${Utils.orthoMcl}/orthomcl.pl --mode 1 --fa ${fileStr} --pi_cutoff ${data.piCutoff} --pv_cutoff ${data.pvCutoff} --pmatch_cutoff ${data.pmatchCutoff}
         |perl ${Utils.binPath}/Compare-rna/select_ortholog_gene_pro.pl fa.list all_orthomcl.out
         |cp all_orthomcl.out ${new File(resultDir, "all_orthomcl.out")}
         |cp gene.family.info.xls ${new File(resultDir, "gene.family.info.xls")}
         |cp genefamily.strain.mat.xls ${new File(resultDir, "genefamily.strain.mat.xls")}
       """.stripMargin
    val shBuffer = ArrayBuffer(command)
    missionExecutor.exec(shBuffer)
    Redirect(routes.GenomeController.getAllMission() + s"?kind=${Utils.orthoMclStr}")
  }

  def orthoMclResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val file = new File(resultDir, "all_orthomcl.out")
    val lines = FileUtils.readLines(file).asScala
    val allData = lines.map { line =>
      val columns = line.split("\t")
      val orthoMcls = columns(0)
      val orthoMcl = columns(0).replaceAll("\\(.*\\):", "")
      val genesR = ".*\\((\\d+) genes.*".r
      val genesR(genes) = orthoMcls
      val taxaR = ".*,(\\d+) taxa.*".r
      val taxaR(taxa) = orthoMcls
      Json.obj("orthomcl" -> orthoMcl, "genes" -> genes, "taxa" -> taxa, "detail" -> columns(1))
    }
    val infoFile = new File(resultDir, "gene.family.info.xls")
    val infoLines = FileUtils.readLines(infoFile).asScala.drop(1)
    val infoData = infoLines.map { line =>
      val columns = line.split("\t")
      Json.obj("Sam" -> columns(0), "homology.gene.family" -> columns(1), "special_gene" -> columns(2), "all" -> columns(3))
    }
    val strainFile = new File(resultDir, "genefamily.strain.mat.xls")
    val (columnNames, array) = Utils.getInfoByFile(strainFile)
    val json = Json.obj("allData" -> allData, "infoData" -> infoData, "columnNames" -> columnNames, "array" -> array)
    Ok(json)
  }

  case class MummerData(refSampleName: String, querySampleName: String)

  val mummerForm = Form(
    mapping(
      "refSampleName" -> text,
      "querySampleName" -> text
    )(MummerData.apply)(MummerData.unapply)
  )

  def mummer = Action { implicit request =>
    val data = mummerForm.bindFromRequest.get
    val missionName = formTool.missionNameForm.bindFromRequest().get.missionName
    val userId = tool.getUserId
    val args = ArrayBuffer(
      s"Query(查询样本):${data.querySampleName}",
      s"Target(目标样本):${data.refSampleName}"
    ).mkString(";")
    val refSampleName = tool.getSampleName(data.refSampleName)
    val querySampleName = tool.getSampleName(data.querySampleName)
    val row = MissionRow(0, s"${missionName}", userId, "mummer", args, new DateTime(), None, "running")
    val missionExecutor = new MissionExecutor(missionDao, tool, row)
    val (workspaceDir, resultDir) = (missionExecutor.workspaceDir, missionExecutor.resultDir)
    ArrayBuffer(refSampleName, querySampleName).foreach { sampleName =>
      val file = tool.getGenomeFile(sampleName)
      FileUtils.copyFileToDirectory(file, workspaceDir)
      val gffFile = tool.getGffFile(sampleName)
      FileUtils.copyFileToDirectory(gffFile, workspaceDir)
    }
    val fileStr = ArrayBuffer(refSampleName, querySampleName).map { sampleName =>
      val file = tool.getGenomeFile(sampleName)
      file.getName
    }.mkString(" ")
    val command =
      s"""
         |perl ${Utils.dosPath2Unix(Utils.svBin)}/SV_finder.pl ${fileStr} --prefix data --tfix ${refSampleName} --qfix ${querySampleName} --nocomp --outdir output --locate
         |cp output/Result/01.Synteny/Target-Query.parallel.png  ${Utils.dosPath2Unix(resultDir)}/Target-Query.parallel.png
         |cp output/Result/01.Synteny/Target-Query.parallel.svg ${Utils.dosPath2Unix(resultDir)}/Target-Query.parallel.svg
         |cp output/Result/01.Synteny/Target-Query.xoy.png ${Utils.dosPath2Unix(resultDir)}/Target-Query.xoy.png
         |cp output/Result/01.Synteny/Target-Query.xoy.svg ${Utils.dosPath2Unix(resultDir)}/Target-Query.xoy.svg
         |cp output/Result/01.Synteny/data.axt  ${Utils.dosPath2Unix(resultDir)}/axt.txt
         |cp output/Result/01.Synteny/data.alg  ${Utils.dosPath2Unix(resultDir)}/alg.txt
         |cp output/Result/01.Synteny/data.que.miss ${Utils.dosPath2Unix(resultDir)}/query.miss.txt
         |cp output/Result/01.Synteny/data.tar.miss ${Utils.dosPath2Unix(resultDir)}/target.miss.txt
         |cp output/Result/01.Synteny/data.cover ${Utils.dosPath2Unix(resultDir)}/cover.txt
         |cp output/Result/01.Synteny/data.identity.list ${Utils.dosPath2Unix(resultDir)}/identity_list.txt
         |cp output/Result/02.Variation/data.indel ${Utils.dosPath2Unix(resultDir)}/indel.txt
         |cp output/Result/02.Variation/data.snp ${Utils.dosPath2Unix(resultDir)}/snp.txt
         |cp output/Result/02.Variation/data.sv ${Utils.dosPath2Unix(resultDir)}/sv.txt
         |cp output/Result/02.Variation/SV_cycle.svg ${Utils.dosPath2Unix(resultDir)}/SV_cycle.svg
         |cp output/Result/02.Variation/SV_cycle.png ${Utils.dosPath2Unix(resultDir)}/SV_cycle.png
         |cp output/Result/02.Variation/final.snp_indel.dis.png ${Utils.dosPath2Unix(resultDir)}/final.snp_indel.dis.png
         |cp output/Result/02.Variation/final.snp_indel.dis.svg ${Utils.dosPath2Unix(resultDir)}/final.snp_indel.dis.svg
         |cp output/Result/02.Variation/data.snp ${Utils.dosPath2Unix(workspaceDir)}/snp.txt
         |python ${Utils.dosPath2Unix(Utils.pyPath)}/dealSnp.py -q ${querySampleName}
         |perl  ${Utils.dosPath2Unix(Utils.binPath)}/09.Snp/src/snp_annotation_v2.2.pl snp_out.txt ${refSampleName}.genome.fa ${refSampleName}.gff --prefix data --outdir snp_anno --code 11
         |cp  snp_anno/data.cds.axt.txt ${Utils.dosPath2Unix(resultDir)}/snp.cds.axt.txt
         |cp  snp_anno/data.cds.info.xls ${Utils.dosPath2Unix(resultDir)}/snp.cds.info.xls
         |cp  snp_anno/data.cds.stat.xls ${Utils.dosPath2Unix(resultDir)}/snp.cds.stat.xls
         |cp  snp_anno/data.chr.stat.xls ${Utils.dosPath2Unix(resultDir)}/snp.chr.stat.xls
         |cp  snp_anno/data.intergenic.info.xls ${Utils.dosPath2Unix(resultDir)}/snp.intergenic.info.xls
         |cp output/Result/02.Variation/data.indel ${Utils.dosPath2Unix(workspaceDir)}/indel.txt
         |python ${Utils.dosPath2Unix(Utils.pyPath)}/dealIndel.py
         |perl  ${Utils.dosPath2Unix(Utils.binPath)}/10.InDel/src/indel_annotation_v2.pl indel_out.txt ${refSampleName}.genome.fa ${refSampleName}.gff --prefix data --outdir indel_anno --sis --code 11
         |cp  indel_anno/data.len.svg ${Utils.dosPath2Unix(resultDir)}/indel.len.svg
         |cp  indel_anno/data.cds.axt ${Utils.dosPath2Unix(resultDir)}/indel.cds.axt.txt
         |cp  indel_anno/data.cdsindel.stat ${Utils.dosPath2Unix(resultDir)}/indel.cdsindel.stat.xls
         |cp  indel_anno/data.cds.stat ${Utils.dosPath2Unix(resultDir)}/indel.cds.stat.xls
         |cp  indel_anno/data.chr.stat ${Utils.dosPath2Unix(resultDir)}/indel.chr.stat.xls
         |cp  indel_anno/data.info ${Utils.dosPath2Unix(resultDir)}/indel.info.xls
         |cp  indel_anno/data.intergenic.info ${Utils.dosPath2Unix(resultDir)}/indel.intergenic.info.xls
         |cp  indel_anno/data.len.dis ${Utils.dosPath2Unix(resultDir)}/indel.len.dis.xls
       """.stripMargin
    val shBuffer = ArrayBuffer(command)
    missionExecutor.execLinux(shBuffer).onComplete {
      case Failure(exception) =>
      case Success(x) =>
        tool.dealMissFile(new File(resultDir, "query.miss.txt"))
        tool.dealMissFile(new File(resultDir, "target.miss.txt"))
        val file = new File(resultDir, "sv.txt")
        val lines = FileUtils.readLines(file).asScala
        val newLines = lines.map { line =>
          val columns = line.split("\t")
          columns.init.mkString("\t")
        }
        FileUtils.writeLines(file, newLines.asJava)
        Utils.svg2png(new File(resultDir, "indel.len.svg"))
        tool.svAnno(new File(resultDir, "sv.txt"), tool.getCdsFile(refSampleName))
    }
    Redirect(routes.GenomeController.getAllMission() + s"?kind=${Utils.mummerStr}"
    )
  }

  def toMummerHelp = Action { implicit request =>
    Ok(views.html.user.genome.mummerHelp())
  }

  def toCrisprHelp = Action { implicit request =>
    Ok(views.html.user.genome.crisprHelp())
  }

  def mummerResult = Action { implicit request =>
    val data = formTool.missionIdForm.bindFromRequest().get
    val resultDir = tool.getResultDirById(data.missionId)
    val file1 = new File(resultDir, "Target-Query.parallel.png")
    val base641 = Utils.getBase64Str(file1)
    val file2 = new File(resultDir, "Target-Query.xoy.png")
    val base642 = Utils.getBase64Str(file2)
    val coverFile = new File(resultDir, "cover.txt")
    val (coverColumnNames, coverArray) = Utils.getInfoByFile(coverFile)
    val json = Json.obj("base641" -> base641, "base642" -> base642,
      "cover" -> Json.obj("columnNames" -> coverColumnNames, "array" -> coverArray))
    Ok(json)
  }

  def getAllMission = Action.async {
    implicit request =>
      val data = formTool.kindForm.bindFromRequest().get
      val kind = data.kind
      val userId = tool.getUserId
      missionDao.selectAll(userId, kind).map {
        x =>
          val array = Utils.getArrayByTs(x)
          Ok(Json.toJson(array))
      }
  }

  def deleteMissionById = Action.async {
    implicit request =>
      val data = formTool.deleteMissionForm.bindFromRequest().get
      missionDao.deleteById(data.missionId).map {
        x =>
          val missionIdDir = tool.getMissionIdDirById(data.missionId)
          Utils.deleteDirectory(missionIdDir)
          Redirect(routes.GenomeController.getAllMission() + "?kind=" + data.kind)
      }
  }

  def downloadResult = Action.async {
    implicit request =>
      val userId = tool.getUserId
      val data = formTool.missionIdForm.bindFromRequest().get
      val missionId = data.missionId
      missionDao.selectByMissionId(userId, missionId).map {
        mission =>
          val missionIdDir = tool.getMissionIdDirById(missionId)
          val resultDir = new File(missionIdDir, "result")
          val resultFile = new File(missionIdDir, s"result.zip")
          if (!resultFile.exists()) ZipUtil.pack(resultDir, resultFile)
          Ok.sendFile(resultFile).withHeaders(
            CACHE_CONTROL -> "max-age=3600",
            CONTENT_DISPOSITION -> s"attachment; filename=${
              mission.missionname
            }.zip",
            CONTENT_TYPE -> "application/x-download"
          )
      }
  }

  def downloadResultFile = Action.async {
    implicit request =>
      val userId = tool.getUserId
      val data = formTool.missionFileForm.bindFromRequest().get
      val missionId = data.missionId
      missionDao.selectByMissionId(userId, missionId).map {
        mission =>
          val missionIdDir = tool.getMissionIdDirById(missionId)
          val resultDir = new File(missionIdDir, "result")
          val file = new File(resultDir, data.fileName)
          Ok.sendFile(file).withHeaders(
            //            CACHE_CONTROL -> "max-age=3600",
            CONTENT_DISPOSITION -> s"attachment; filename=${
              file.getName
            }",
            CONTENT_TYPE -> "application/x-download"
          )
      }
  }

  def getImage = Action.async { implicit request =>
    val ifModifiedSinceStr = request.headers.get(IF_MODIFIED_SINCE)
    val userId = tool.getUserId
    val data = formTool.missionFileForm.bindFromRequest().get
    val missionId = data.missionId
    missionDao.selectByMissionId(userId, missionId).map { mission =>
      val missionIdDir = tool.getMissionIdDirById(missionId)
      val resultDir = new File(missionIdDir, "result")
      val file = new File(resultDir, data.fileName)
      val lastModifiedStr = file.lastModified().toString
      val MimeType = "image/png"
      val byteArray = Files.readAllBytes(file.toPath)
      if (ifModifiedSinceStr.isDefined && ifModifiedSinceStr.get == lastModifiedStr) {
        NotModified
      }
      else {
        Ok(byteArray).as(MimeType).withHeaders(LAST_MODIFIED -> file.lastModified().toString)
      }
    }
  }

  def getFileContent = Action.async { implicit request =>
    val userId = tool.getUserId
    val data = formTool.missionFileForm.bindFromRequest().get
    val missionId = data.missionId
    missionDao.selectByMissionId(userId, missionId).map { mission =>
      val missionIdDir = tool.getMissionIdDirById(missionId)
      val resultDir = new File(missionIdDir, "result")
      val file = new File(resultDir, data.fileName)
      val byteArray = FileUtils.readFileToString(file)
      //      val byteArray = Files.readAllBytes(file.toPath)
      Ok(Json.toJson(byteArray))
    }
  }

  def getLogContent = Action.async {
    implicit request =>
      val userId = tool.getUserId
      val data = formTool.missionIdForm.bindFromRequest().get
      missionDao.selectByMissionId(userId, data.missionId).map {
        mission =>
          val missionIdDir = tool.getMissionIdDirById(data.missionId)
          val logFile = new File(missionIdDir, s"log.txt")
          val logStr = FileUtils.readFileToString(logFile, "UTF-8")
          Ok(Json.toJson(logStr))
      }
  }


  def updateMissionSocket(kind: String) = WebSocket.accept[JsValue, JsValue] {
    implicit request =>
      val userId = tool.getUserId
      var beforeMissions = Utils.execFuture(missionDao.selectAll(userId, kind))
      var currentMissions = beforeMissions
      ActorFlow.actorRef(out => Props(new Actor {
        override def receive: Receive = {
          case msg: JsValue if (msg \ "info").as[String] == "start" =>
            out ! Utils.getJsonByTs(beforeMissions)
            system.scheduler.scheduleOnce(3 seconds, self, Json.obj("info" -> "update"))
          case msg: JsValue if (msg \ "info").as[String] == "update" =>
            missionDao.selectAll(userId, kind).map {
              missions =>
                currentMissions = missions
                if (currentMissions.size != beforeMissions.size) {
                  out ! Utils.getJsonByTs(currentMissions)
                } else {
                  val b = currentMissions.zip(beforeMissions).forall {
                    case (currentMission, beforeMission) =>
                      currentMission.id == beforeMission.id && currentMission.state == beforeMission.state
                  }
                  if (!b) {
                    out ! Utils.getJsonByTs(currentMissions)
                  }
                }
                beforeMissions = currentMissions
                system.scheduler.scheduleOnce(3 seconds, self, Json.obj("info" -> "update"))
            }
          case _ =>
            self ! PoisonPill
        }

        override def postStop(): Unit = {
          self ! PoisonPill
        }

      }))

  }

  def missionNameCheck = Action.async { implicit request =>
    val data = formTool.missionForm.bindFromRequest.get
    val userId = tool.getUserId
    missionDao.selectOptionByMissionAttr(userId, data.missionName, data.kind).map { mission =>
      mission match {
        case Some(y) => Ok(Json.obj("valid" -> false))
        case None =>
          Ok(Json.obj("valid" -> true))
      }
    }
  }


}
