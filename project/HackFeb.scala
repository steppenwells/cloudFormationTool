import com.gu.deploy.PlayArtifact._
import sbt._
import sbt.Keys._
import play.Play.autoImport._
import PlayKeys._
import com.typesafe.sbt.web._

object HackFebBuild extends Build {

  val commonSettings =
    Seq(
      scalaVersion := "2.11.2",
      scalaVersion in ThisBuild := "2.11.2",
      organization := "com.gu",
      version      := "0.1",
      fork in Test := false,
      resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
      scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
      doc in Compile <<= target.map(_ / "none"),
      incOptions := incOptions.value.withNameHashing(nameHashing = true)
    )

  val awsDependencies = Seq("com.amazonaws" % "aws-java-sdk" % "1.9.4")

  val root = Project("hack-feb", file(".")).enablePlugins(play.PlayScala).enablePlugins(SbtWeb)
    .settings(libraryDependencies += ws)
    .settings(commonSettings ++ playArtifactDistSettings ++ playArtifactSettings: _*)
    .settings(magentaPackageName := "rights-feeder")
    .settings(libraryDependencies ++= awsDependencies)

  def playArtifactSettings = Seq(
    ivyXML :=
      <dependencies>
        <exclude org="commons-logging"/>
        <exclude org="org.springframework"/>
        <exclude org="org.scala-tools.sbt"/>
      </dependencies>
  )
}
