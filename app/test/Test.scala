package test

import java.io.{File, FileOutputStream}

import org.apache.commons.io.FileUtils
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper
import org.biojava.nbio.core.util.SequenceTools

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import io.netty.buffer.AbstractByteBuf
import org.apache.batik.transcoder.{SVGAbstractTranscoder, TranscoderInput, TranscoderOutput}
import org.apache.batik.transcoder.image.PNGTranscoder
import utils.Utils

import scala.collection.JavaConverters._
import sys.process._

/**
  * Created by yz on 2018/4/17
  */
object Test {

  def main(args: Array[String]): Unit = {
    //    val parent=new File("E:\\tmp")
    //    val b=ArrayBuffer("SAMN02604095").map{sampleName=>
    //      val cdsFile=FileUtils.readLines(new File(parent,s"${sampleName}.cds.fa")).asScala
    //      val pepFile=FileUtils.readLines(new File(parent,s"${sampleName}.pep.fa")).asScala
    //      cdsFile.size==pepFile.size
    //    }
    //    println(b.reduce(_ && _))

    case class CDSInfo(proteinId: String, start: Int, end: Int)
    val parent = new File("D:\\databases\\p3_database\\user\\1\\mission\\152\\result")
    val file = new File(parent, "sv.txt")
    val dbFile = new File(Utils.dataFile, "SAMN02604315.cds.fa")
    val dbLines = Utils.file2Lines(dbFile)
    val map = dbLines.filter(_.startsWith(">")).map { line =>
      val columns = line.drop(1).split(" ")
      (columns(2), CDSInfo(columns(0), columns(3).toInt, columns(4).toInt))
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
    Utils.lines2File(new File(parent, "sv_anno.txt"), newLines)


  }

  def dosPath2Unix(file: File) = {
    val path = file.getAbsolutePath
    path.replace("\\", "/").replaceAll("D:", "/mnt/d")
  }


}
