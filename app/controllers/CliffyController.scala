package controllers

import play.api.mvc.{Action, Controller}


object CliffyController extends Controller {

  def start = Action {

    Ok(views.html.Application.start())
  }

}
