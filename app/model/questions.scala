package model

import play.api.Logger
import play.twirl.api.HtmlFormat

trait Question {
  def renderQuestion: HtmlFormat.Appendable

  def nextQuestion: Question
  def processAnswer(context: Context, submission: Map[String, Seq[String]])
}

class ChooseAccountQuestion() extends Question{
  override def renderQuestion = views.html.Application.Questions.chooseAccount(AWSAccounts.accounts)

  override def nextQuestion: Question = new DoneQuestion

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]) {
    val accountName = submission("account")
    Logger.info(s"using account ${accountName.head}")

    val account = AWSAccounts(accountName.head)
    context.account = account
    context.currentQuestion = nextQuestion
  }
}

class DoneQuestion extends Question {
  override def renderQuestion = views.html.Application.Questions.done()

  override def nextQuestion: Question = new DoneQuestion

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]) {
    println(context)
  }
}