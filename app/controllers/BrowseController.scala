package controllers

import java.io.File

import dao._
import javax.inject.Inject
import play.api.libs.json.{JsString, Json}
import play.api.mvc._
import models.Tables._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import java.lang.reflect.Field

import utils.Utils

/**
  * Created by yz on 2018/5/11
  */

case class ClassifyData(phylum: String, outline: String, order: String, family: String, genus: String, species: String)

class BrowseController @Inject()(sampleDao: SampleDao, geneInfoDao: GeneInfoDao, tool: Tool, classifyDao: ClassifyDao) extends Controller {

  def bacteriaBrowseBefore = Action { implicit request =>
    Ok(views.html.user.browse.bacteriaBrowse())
  }

  def bacteriaBrowseByTaxonomy = Action { implicit request =>
    Ok(views.html.user.browse.bacteriaBrowseByTaxonomy())
  }

  def getAllSample = Action.async { implicit request =>
    sampleDao.selectAll.map { x =>
      val array = Utils.getArrayByTs(x)
      Ok(Json.toJson(array))
    }
  }

  def getGeneInfoBefore = Action { implicit request =>
    val data = tool.sampleNameForm.bindFromRequest().get
    Ok(views.html.user.browse.geneInfo(data.sampleName))
  }

  def getGeneDetailInfo = Action.async { implicit request =>
    val data = tool.locusNameForm.bindFromRequest().get
    geneInfoDao.selectByName(data.sampleName, data.locusName).map { x =>
      Ok(views.html.user.browse.geneDetailInfo(x))
    }

  }

  def getAllGeneInfo = Action.async { implicit request =>
    val data = tool.sampleNameForm.bindFromRequest().get
    geneInfoDao.selectAll(data.sampleName).map { x =>
      val array = getArrayByGeneInfos(x)
      Ok(Json.toJson(array))
    }
  }

  def getArrayByGeneInfos(x: Seq[GeneinfoRow]) = {
    x.map { y =>
      Json.obj(
        "name" -> y.proteinid, "start" -> y.start, "end" -> y.end, "strand" -> y.strand, "product" -> y.product
      )
    }
  }


  def downloadDataFile = Action { implicit request =>
    val data = tool.downloadForm.bindFromRequest().get
    val dataFile = new File(Utils.dataFile, s"${data.sampleName}.${data.kind}.fa")
    val fileName = s"${data.sampleName}.${data.kind}.fa"
    Ok.sendFile(dataFile).withHeaders(
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> s"attachment; filename=${fileName}",
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def getPhylumInfo = Action.async {
    classifyDao.selectAll.map { x =>
      val phylums = x.map(_.phylum).distinct
      val json = Json.obj("num" -> phylums.size, "info" -> phylums)
      Ok(json)
    }
  }

  val classifyForm = Form(
    mapping(
      "phylum" -> text,
      "outline" -> text,
      "order" -> text,
      "family" -> text,
      "genus" -> text,
      "species" -> text
    )(ClassifyData.apply)(ClassifyData.unapply)
  )

  def getOutlineInfo = Action.async { implicit request =>
    val data = classifyForm.bindFromRequest.get
    classifyDao.selectByPhylum(data.phylum).map { x =>
      val infos = x.map(_.outline).distinct
      val json = Json.obj("num" -> infos.size, "info" -> infos)
      Ok(json)
    }
  }

  def getOrderInfo = Action.async { implicit request =>
    val data = classifyForm.bindFromRequest.get
    classifyDao.selectGetOrderInfo(data).map { x =>
      val infos = x.map(_.order).distinct
      val json = Json.obj("num" -> infos.size, "info" -> infos)
      Ok(json)
    }
  }

  def getFamilyInfo = Action.async { implicit request =>
    val data = classifyForm.bindFromRequest.get
    classifyDao.selectGetFamilyInfo(data).map { x =>
      val infos = x.map(_.family).distinct
      val json = Json.obj("num" -> infos.size, "info" -> infos)
      Ok(json)
    }
  }

  def getGenusInfo = Action.async { implicit request =>
    val data = classifyForm.bindFromRequest.get
    classifyDao.selectGetGenusInfo(data).map { x =>
      val infos = x.map(_.genus).distinct
      val json = Json.obj("num" -> infos.size, "info" -> infos)
      Ok(json)
    }
  }

  def getSpeciesInfo = Action.async { implicit request =>
    val data = classifyForm.bindFromRequest.get
    classifyDao.selectGetSpeciesInfo(data).map { x =>
      val infos = x.map(_.species).distinct
      val json = Json.obj("num" -> infos.size, "info" -> infos)
      Ok(json)
    }
  }

  def getIdInfo = Action.async { implicit request =>
    val data = classifyForm.bindFromRequest.get
    classifyDao.selectGetIdInfo(data).map { x =>
      val infos = x.map(_.id).distinct
      val json = Json.obj("num" -> infos.size, "info" -> infos)
      Ok(json)
    }
  }


}
