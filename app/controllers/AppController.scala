package controllers

import dao.{AdminDao, UserDao}
import javax.inject.Inject
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits._

/**
  * Created by yz on 2018/11/26
  */
class AppController @Inject()(formTool: FormTool, userDao: UserDao, adminDao: AdminDao) extends Controller {

  def loginBefore = Action { implicit request =>
    Ok(views.html.login())
  }

  def login = Action.async { implicit request =>
    val data = formTool.userForm.bindFromRequest().get
    adminDao.selectById1.zip(userDao.selectByUserData(data)).map { case (account, optionUser) =>
      if (data.account == account.account && data.password == account.password) {
        Redirect(routes.AdminController.toHome()).withSession(request.session + ("admin" -> data.account))
      } else if (optionUser.isDefined) {
        val user = optionUser.get
        val rSession = request.session + ("user" -> data.account) + ("id" -> user.id.toString)
        Redirect(routes.IndexController.toIndex()).withSession(rSession)
      } else {
        Redirect(routes.AppController.loginBefore()).flashing("info" -> "账号或密码错误!")
      }
    }
  }

  def toAbout = Action { implicit request =>
    Ok(views.html.about())
  }




}
