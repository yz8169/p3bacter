package controllers

import dao.UserDao
import javax.inject.Inject
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import models.Tables._
import org.joda.time.DateTime
import play.api.libs.json.Json

/**
  * Created by yz on 2018/6/5
  */

class UserController @Inject()(userDao: UserDao) extends Controller {

  val userForm = Form(
    mapping(
      "account" -> text,
      "password" -> text
    )(UserData.apply)(UserData.unapply)
  )

  def logout = Action {
    Redirect(routes.AppController.loginBefore()).flashing("info" -> "退出登录成功!").withNewSession
  }

  def changePasswordBefore = Action { implicit request =>
    Ok(views.html.user.changePassword())
  }

  case class ChangePasswordData(password: String, newPassword: String)

  val changePasswordForm = Form(
    mapping(
      "password" -> text,
      "newPassword" -> text
    )(ChangePasswordData.apply)(ChangePasswordData.unapply)
  )

  def changePassword = Action.async { implicit request =>
    val data = changePasswordForm.bindFromRequest().get
    val account = request.session.get("user").get
    userDao.selectByAccount(account).map(_.get).flatMap { x =>
      if (data.password == x.password) {
        val row = x.copy(password = data.newPassword)
        userDao.update(row).map { y =>
          Redirect(routes.AppController.loginBefore()).flashing("info" -> "密码修改成功!").withNewSession
        }
      } else {
        Future.successful(Redirect(routes.UserController.changePasswordBefore()).flashing("info" -> "原始密码错误!"))
      }
    }
  }

  def registerBefore = Action { implicit request =>
    Ok(views.html.user.register())
  }

  def register = Action.async { implicit request =>
    val data = userForm.bindFromRequest.get
    val row = UserRow(0, data.account, data.password, new DateTime())
    userDao.insert(row).map { x =>
      Redirect(routes.UserController.registerBefore()).flashing("info" -> "success!")
    }
  }

  case class AccountData(account: String)

  val accountForm = Form(
    mapping(
      "account" -> text
    )(AccountData.apply)(AccountData.unapply)
  )

  def accountCheck = Action.async { implicit request =>
    val data = accountForm.bindFromRequest().get
    println(data)
    userDao.selectByAccount(data.account).map {
      case Some(y) => Ok(Json.obj("valid" -> false))
      case None => Ok(Json.obj("valid" -> true))
    }
  }


}
