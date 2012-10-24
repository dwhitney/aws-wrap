package aws.sns

import java.util.Date

import scala.annotation.implicitNotFound
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws._

import aws.core._
import aws.core.Types._
import aws.core.parsers._
import aws.core.signature.V2

import aws.sns.SNSParsers._

case class SNSMeta(requestId: String) extends Metadata

object SNSRegion {

  val NAME = "sns"

  val US_EAST_1 = AWSRegion.US_EAST_1(NAME)
  val US_WEST_1 = AWSRegion.US_WEST_1(NAME)
  val US_WEST_2 = AWSRegion.US_WEST_2(NAME)
  val EU_WEST_1 = AWSRegion.EU_WEST_1(NAME)
  val ASIA_SOUTHEAST_1 = AWSRegion.ASIA_SOUTHEAST_1(NAME)
  val ASIA_NORTHEAST_1 = AWSRegion.ASIA_NORTHEAST_1(NAME)
  val SA_EAST_1 = AWSRegion.SA_EAST_1(NAME)

  implicit val DEFAULT = US_EAST_1
}

object SNS extends V2[SNSMeta] {

  override val VERSION = "2010-03-31"

  object Parameters {
    def NextToken(nextToken: Option[String]):Seq[(String, String)] = nextToken.toSeq.map("NextToken" -> _)
    def Name(name: String) = ("Name" -> name)
    def TopicArn(arn: String) = ("TopicArn" -> arn)
    def Label(name: String) = ("Label" -> name)
    def EndpointProtocol(endpoint: Endpoint) = Seq(
      "Endpoint" -> endpoint.value,
      "Protocol" -> endpoint.protocol
    )
    def SubscriptionArn(arn: String) = ("SubscriptionArn" -> arn)
    def AuthenticateOnUnsubscribe(auth: Boolean) = ("AuthenticateOnUnsubscribe" -> (if (auth) "true" else "false"))
    def AWSAccounts(accounts: Seq[String]):Seq[(String, String)] = (for ((account, i) <- accounts.zipWithIndex) yield {
      (("AWSAccountId.member." + i) -> account)
    })
    def ActionList(actions: Seq[Action]):Seq[(String, String)] = (for ((action, i) <- actions.zipWithIndex) yield {
      (("ActionName.member." + i) -> action.toString)
    })

  }

  import AWS.Parameters._
  import Parameters._

  def addPermission(topicArn: String, label: String, awsAccounts: Seq[String], actions: Seq[Action])(implicit region: AWSRegion): Future[EmptyResult[SNSMeta]] = {
    val params = Seq(
      Action("AddPermission"),
      TopicArn(topicArn),
      Label(label)
    ) ++ AWSAccounts(awsAccounts) ++ ActionList(actions)
    get[Unit](params:_*)
  }

  def confirmSubscription(topicArn: String, token: String, authenticateOnUnsubscribe: Boolean = false)(implicit region: AWSRegion): Future[Result[SNSMeta, SubscriptionResult]] = {
    get[SubscriptionResult](
      Action("ConfirmSubscription"),
      TopicArn(topicArn),
      AuthenticateOnUnsubscribe(authenticateOnUnsubscribe)
    )
  }

  def createTopic(name: String)(implicit region: AWSRegion): Future[Result[SNSMeta, CreateTopicResult]] = {
    get[CreateTopicResult](Action("CreateTopic"), Name(name))
  }

  def deleteTopic(topicArn: String)(implicit region: AWSRegion): Future[EmptyResult[SNSMeta]] = {
    get[Unit](
      Action("DeleteTopic"),
      TopicArn(topicArn)
    )
  }

  // GetSubscriptionAttributes

  // GetTopicAttributes

  def listSubscriptions(nextToken: Option[String] = None)(implicit region: AWSRegion): Future[Result[SNSMeta, SubscriptionListResult]] = {
    val params = Seq(Action("ListSubscriptions")) ++ NextToken(nextToken)
    get[SubscriptionListResult](params:_*)
  }

  def listSubscriptionsByTopic(topicArn: String, nextToken: Option[String] = None)(implicit region: AWSRegion): Future[Result[SNSMeta, SubscriptionListResult]] = {
    val params = Seq(
      Action("ListSubscriptionsByTopic"),
      TopicArn(topicArn)
    ) ++ NextToken(nextToken)
    get[SubscriptionListResult](params:_*)
  }

  def listTopics(nextToken: Option[String] = None)(implicit region: AWSRegion): Future[Result[SNSMeta, ListTopicsResult]] = {
    val params = Seq(Action("ListTopics")) ++ NextToken(nextToken)
    get[ListTopicsResult](params:_*)
  }

  // Publish

  // RemovePermission

  // SetSubscriptionAttributes

  // SetTopicAttributes

  def subscribe(endpoint: Endpoint, topicArn: String)(implicit region: AWSRegion): Future[Result[SNSMeta, SubscriptionResult]] = {
    val params = Seq(Action("Subscribe"), TopicArn(topicArn)) ++ EndpointProtocol(endpoint)
    get[SubscriptionResult](params:_*)
  }

  def unsubscribe(subscriptionArn: String)(implicit region: AWSRegion): Future[EmptyResult[SNSMeta]] = {
    get[Unit](Action("Unsubscribe"), SubscriptionArn(subscriptionArn))
  }

}

