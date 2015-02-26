package model

import play.api.Logger
import play.twirl.api.HtmlFormat

trait Question {
  def renderQuestion: HtmlFormat.Appendable

  def processAnswer(context: Context, submission: Map[String, Seq[String]])
}

class ChooseAccountQuestion() extends Question {
  override def renderQuestion = views.html.Application.Questions.chooseAccount(AWSAccounts.accounts)

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]) {
    val accountName = submission("account")
    Logger.info(s"using account ${accountName.head}")

    val account = AWSAccounts(accountName.head)
    context.account = account
    context.currentQuestion = new AddSomethingQuestion
  }
}

class AddSomethingQuestion extends Question {

  override def renderQuestion: HtmlFormat.Appendable = views.html.Application.Questions.currentlyEmpty()

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]): Unit = {
    val thing = submission("thing")
    Logger.info(s"configuring ${thing.head}")

    context.currentQuestion = thing.head match {
      case "instance" => new InstanceRecipeQuestion
      case "resources" => new DoneQuestion
      case _ => new DoneQuestion
    }
  }
}

class InstanceRecipeQuestion extends Question {

  override def renderQuestion: HtmlFormat.Appendable = views.html.Application.Questions.instanceRecipe()

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]): Unit = {
    val recipe = submission("recipe")
    Logger.info(s"configuring ${recipe.head}")

    context.currentQuestion = recipe.head match {
      case "play" => new InstanceAppName
      case _ => new DoneQuestion
    }
  }
}

class InstanceAppName extends Question {

  override def renderQuestion: HtmlFormat.Appendable = views.html.Application.Questions.instanceAppName()

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]): Unit = {
    val name = submission("name").head
    Logger.info(s"app name = $name")

    context.currentQuestion = new DisplayValueQuestion(name)
  }
}

class DisplayValueQuestion(value: String) extends Question {

  override def renderQuestion: HtmlFormat.Appendable = views.html.Application.Questions.displayValue(value)

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]): Unit = {

  }
}



class DoneQuestion extends Question {
  override def renderQuestion =views.html.Application.Questions.done()

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]) {
    println(context)
  }
}

