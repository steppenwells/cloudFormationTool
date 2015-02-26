package model

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.sqs.AmazonSQSClient
import play.api.Play.current


class AWSAccount(val name: String, key: String, secret: String) {
  lazy val region = Region getRegion Regions.EU_WEST_1

  lazy val credentialsProvider = new StaticCredentialsProvider(new BasicAWSCredentials(key, secret))

  lazy val SQS = region.createClient(classOf[AmazonSQSClient], credentialsProvider, null)

  lazy val DynamoDB = region.createClient(classOf[AmazonDynamoDBClient], credentialsProvider, null)

  lazy val CloudWatch = region.createClient(classOf[AmazonCloudWatchClient], credentialsProvider, null)
}

object AWSAccounts {

  val config = play.api.Play.configuration

  val accounts = List(
    new AWSAccount("composer", config.getString("composer.key").get, config.getString("composer.secret").get),
    new AWSAccount("workflow", config.getString("workflow.key").get, config.getString("workflow.secret").get)
  )

  def apply(accountName: String): AWSAccount = {
    accounts.find(_.name == accountName).get
  }
}