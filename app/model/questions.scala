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

case class InstanceDetails(
  val appName: Option[String],
  val instanceType: Option[String] = None,
  val image: Option[String] = None,
  val number: Option[Int] = None,
  val lbType: Option[String] = None
) {

  def resourceAppName = {
    val parts = appName.get.split("-")
    parts.map(capitaliseFirstChar _).mkString("")
  }

  def capitaliseFirstChar(s: String) = {
    s.head.toUpper + s.tail
  }
}

class InstanceAppName extends Question {

  override def renderQuestion: HtmlFormat.Appendable = views.html.Application.Questions.instanceAppName()

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]): Unit = {
    val name = submission("name").head
    Logger.info(s"app name = $name")

    context.currentQuestion = new InstanceTypeDetails(new InstanceDetails(appName = Some(name)))
  }
}

class InstanceTypeDetails(instanceDetails: InstanceDetails) extends Question {
  override def renderQuestion: HtmlFormat.Appendable = views.html.Application.Questions.instanceType(InstanceTypes.list)

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]) {
    val instanceType = submission("instanceType").headOption
    val ami = submission("ami").headOption

    context.currentQuestion = new ScaleGroupCountQuestion(
      instanceDetails.copy(instanceType = instanceType, image = ami)
    )
  }
}

class ScaleGroupCountQuestion(instanceDetails: InstanceDetails) extends Question {
  override def renderQuestion: HtmlFormat.Appendable = views.html.Application.Questions.scaleGroup(InstanceTypes.list)

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]) {
    val c = submission("howMany").headOption.map(_.toInt)

    context.currentQuestion = new AddLoadBalencerQuestion(
      instanceDetails.copy(number = c)
    )
  }
}

class AddLoadBalencerQuestion(instanceDetails: InstanceDetails) extends Question {
  override def renderQuestion: HtmlFormat.Appendable = views.html.Application.Questions.loadBalancer()

  override def processAnswer(context: Context, submission: Map[String, Seq[String]]) {
    val c = submission("lb").headOption


    context.currentQuestion = c match {
      case Some("no") => new DisplayValueQuestion(instanceDetails.toString)
      case s => {
        val id = instanceDetails.copy(lbType = s)

        val completedResources = List(
          new Role(id),
          new InstanceProfile(id),
          new SSHSecurityGroup(),
          new AppServerSecurityGroup(id),
          new LoadBalancerSecurityGroup(id),
          new LoadBalancer(id),
          new AutoScalingGroup(id, context.account.defaults),
          new LaunchConfig(id, context.account.defaults)
        )

        context.resources = context.resources ++ completedResources
        new DisplayValueQuestion(context.resources.map(_.asCfn).mkString(",\n"))
      }
    }
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

