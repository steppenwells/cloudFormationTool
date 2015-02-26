package controllers

import model.Contexts
import play.api.mvc.{Action, Controller}


object CliffyController extends Controller {

  def start = Action {

    Ok(views.html.Application.start())
  }

  def begin = Action {
    val id = Contexts.startSession

    Redirect(s"/configure/$id")
  }

  def renderQuestion(contextId: Long) = Action {
    val context = Contexts(contextId)

    Ok(views.html.Application.renderContext(context))
  }

  def processQuestion(contextId: Long) = Action { req =>
    val context = Contexts(contextId)

    context.currentQuestion.processAnswer(context, req.body.asFormUrlEncoded.get)

    Redirect(s"/configure/$contextId")
  }

}
