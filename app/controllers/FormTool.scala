package controllers

import play.api.data._
import play.api.data.Forms._

/**
  * Created by yz on 2018/6/12
  */

case class UserData(account: String, password: String)

class FormTool {

  case class DeleteMisiionData(missionId: Int, kind: String)

  val deleteMissionForm = Form(
    mapping(
      "missionId" -> number,
      "kind" -> text
    )(DeleteMisiionData.apply)(DeleteMisiionData.unapply)
  )

  case class MissionIdData(missionId: Int)

  val missionIdForm = Form(
    mapping(
      "missionId" -> number
    )(MissionIdData.apply)(MissionIdData.unapply)
  )

  case class MissionData(missionName: String, kind: String)

  val missionForm = Form(
    mapping(
      "missionName" -> text,
      "kind" -> text
    )(MissionData.apply)(MissionData.unapply)
  )

  case class MissionFileData(missionId: Int, fileName: String)

  val missionFileForm = Form(
    mapping(
      "missionId" -> number,
      "fileName" -> text
    )(MissionFileData.apply)(MissionFileData.unapply)
  )

  case class IframeData(html: String)

  val iframeForm = Form(
    mapping(
      "html" -> text
    )(IframeData.apply)(IframeData.unapply)
  )

  case class KindData(kind: String)

  val kindForm = Form(
    mapping(
      "kind" -> text
    )(KindData.apply)(KindData.unapply)
  )

  case class CrisprData(method: String, queryText: String, minDR: String, maxDR: String, percSPmin: String, percSPmax: String,
                        spSim: String, mismDRs: String, truncDR: String, flank: String, arm: Option[String], dt: Option[String],
                        cas: Option[String], defValue: String, meta: Option[String])

  val crisprForm = Form(
    mapping(
      "method" -> text,
      "queryText" -> text,
      "minDR" -> text,
      "maxDR" -> text,
      "percSPmin" -> text,
      "percSPmax" -> text,
      "spSim" -> text,
      "mismDRs" -> text,
      "truncDR" -> text,
      "flank" -> text,
      "arm" -> optional(text),
      "dt" -> optional(text),
      "cas" -> optional(text),
      "defValue" -> text,
      "meta" -> optional(text)
    )(CrisprData.apply)(CrisprData.unapply)
  )

  case class CAnaData(method: String)

  val cAnaForm = Form(
    mapping(
      "method" -> text
    )(CAnaData.apply)(CAnaData.unapply)
  )

  case class PhyTreeData(refSampleName: String, sampleNames: Seq[String])

  val phyTreeForm = Form(
    mapping(
      "refSampleName" -> text,
      "sampleNames" -> seq(text)
    )(PhyTreeData.apply)(PhyTreeData.unapply)
  )

  case class SvgData(svgHtml: String)

  val svgForm = Form(
    mapping(
      "svgHtml" -> text
    )(SvgData.apply)(SvgData.unapply)
  )

  case class AnnoData(method: String, queryText: String, annoMethods: Seq[String],
                      eValue: String, maxTargetSeqs: String
                     )

  val annoForm = Form(
    mapping(
      "method" -> text,
      "queryText" -> text,
      "annoMethods" -> seq(text),
      "eValue" -> text,
      "maxTargetSeqs" -> text
    )(AnnoData.apply)(AnnoData.unapply)
  )

  case class FileNameData(fileName: String)

  val fileNameForm = Form(
    mapping(
      "fileName" -> text
    )(FileNameData.apply)(FileNameData.unapply)
  )

  val userForm = Form(
    mapping(
      "account" -> text,
      "password" -> text
    )(UserData.apply)(UserData.unapply)
  )

  case class AdminData(account: String, password: String)

  val adminForm = Form(
    mapping(
      "account" -> text,
      "password" -> text
    )(AdminData.apply)(AdminData.unapply)
  )

  case class ArdbAnnoData(method: String, queryText: String, evalue: String)

  val ardbForm = Form(
    mapping(
      "method" -> text,
      "queryText" -> text,
      "evalue" -> text
    )(ArdbAnnoData.apply)(ArdbAnnoData.unapply)
  )

  case class GeneWiseData(proteinMethod: String, proteinText: String, dnaMethod: String, dnaText: String, para: Boolean,
                          pretty: Boolean, genes: Boolean, trans: Boolean, cdna: Boolean, embl: Boolean, ace: Boolean,
                          gff: Boolean, diana: Boolean, init: String, splice: String, random: String, alg: String)

  val geneWiseForm = Form(
    mapping(
      "proteinMethod" -> text,
      "proteinText" -> text,
      "dnaMethod" -> text,
      "dnaText" -> text,
      "para" -> boolean,
      "pretty" -> boolean,
      "genes" -> boolean,
      "trans" -> boolean,
      "cdna" -> boolean,
      "embl" -> boolean,
      "ace" -> boolean,
      "gff" -> boolean,
      "diana" -> boolean,
      "init" -> text,
      "splice" -> text,
      "random" -> text,
      "alg" -> text
    )(GeneWiseData.apply)(GeneWiseData.unapply)
  )

  case class MuscleData(method: String, text: String, tree: String)

  val muscleForm = Form(
    mapping(
      "method" -> text,
      "text" -> text,
      "tree" -> text
    )(MuscleData.apply)(MuscleData.unapply)
  )

  case class MissionNameData(missionName: String)

  val missionNameForm = Form(
    mapping(
      "missionName" -> text
    )(MissionNameData.apply)(MissionNameData.unapply)
  )


}
