package controllers

import java.io.File
import java.nio.file.Files

import dao._
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import models.Tables._
import org.apache.commons.io.FileUtils
import utils.Utils

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

/**
  * Created by yz on 2018/5/10
  */
class ToolController @Inject()(projectDao: ProjectDao, formTool: FormTool,tool:Tool) extends Controller {

  def getAllProteinProject = Action.async { implicit request =>
    projectDao.selectAllProtein.map { x =>
      val array = getArrayByProjects(x)
      Ok(Json.toJson(array))
    }
  }

  def getArrayByProjects(x: Seq[ProjectRow]) = {
    x.map { y =>
      val deleteStr = "<a title='删除' onclick=\"deleteProject('" + y.id + "')\" style='cursor: pointer;'><span><em class='fa fa-close'></em></span></a>"
      Json.obj(
        "projectname" -> y.projectname, "kind" -> y.kind,
        "uploadtime" -> y.uploadtime.toString("yyyy-MM-dd HH:mm:ss"),
        "describe" -> y.describe,
        "operate" -> deleteStr
      )
    }
  }

  def getExampleFile = Action { implicit request =>
    val data = formTool.fileNameForm.bindFromRequest().get
    val file = new File(Utils.path, s"example/${data.fileName}")
    Ok.sendFile(file).as(TEXT)
  }

  def downloadExampleFile = Action { implicit request =>
    val data = formTool.fileNameForm.bindFromRequest().get
    val file = new File(Utils.path, s"example/${data.fileName}")
    Ok.sendFile(file).withHeaders(
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> s"attachment; filename=${
        file.getName
      }",
      CONTENT_TYPE -> "application/x-download"
    )
}

  def iframe=Action{implicit request=>
    Ok(views.html.user.iframe())
  }



}
