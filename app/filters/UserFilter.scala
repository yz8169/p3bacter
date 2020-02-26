package filters

import javax.inject.Inject

import akka.stream.Materializer
import controllers.routes
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by yz on 2017/6/13.
  */
class UserFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (rh.session.get("user").isEmpty && rh.path.contains("/user") && !rh.path.contains("/assets/") &&
      !rh.path.contains("/login") && !rh.path.contains("/register") && !rh.path.contains("/accountCheck")) {
      Future.successful(Results.Redirect(routes.AppController.loginBefore()).flashing("info"->"请先登录!"))
    } else {
      f(rh)
    }
  }

}
