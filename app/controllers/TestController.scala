package controllers

import play.api.libs.json.Json
import play.api.mvc._

/**
  * Created by yz on 2018/6/4
  */
class TestController extends Controller{

  def cors=Action{implicit request=>
    Ok(Json.toJson("success!"))
  }

}
