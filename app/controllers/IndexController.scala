package controllers

import java.io.File

import dao._
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import models.Tables._
import org.apache.commons.io.FileUtils

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by yz on 2018/4/17
  */
class IndexController @Inject()(projectDao: ProjectDao, sampleDao: SampleDao) extends Controller {

  def toIndex = Action.async { implicit request =>
    sampleDao.selectAll.map { x =>
      Ok(views.html.index(x.size))
    }
  }

  def proteinListBefore = Action { implicit request =>
    Ok(views.html.proteinList())
  }

  def getAllProteinProject = Action.async { implicit request =>
    projectDao.selectAllProtein.map { x =>
      val array = getArrayByProjects(x)
      Ok(Json.toJson(array))
    }
  }

  def getArrayByProjects(x: Seq[ProjectRow]) = {
    x.map { y =>
      val deleteStr = s"<a href=${routes.ProteinController.toIndex(y.projectname)} style='cursor: pointer;' target='_blank'>立即进入</a>"
      Json.obj(
        "projectname" -> y.projectname, "kind" -> y.kind,
        "uploadtime" -> y.uploadtime.toString("yyyy-MM-dd HH:mm:ss"),
        "describe" -> y.describe,
        "operate" -> deleteStr
      )
    }
  }

}
