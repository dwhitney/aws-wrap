package aws.ses

import java.lang.{ Long => JLong, Double => JDouble }

import scala.xml.Node

import play.api.libs.ws.Response
import aws.core._
import aws.core.parsers._

object SESParsers {

  implicit def emailResultParser = Parser[EmailResult] { r =>
    Success(EmailResult((r.xml \\ "MessageId").text))
  }

  implicit def sesMetadataParser = Parser[SESMetadata] { r =>
    Success(SESMetadata((r.xml \\ "RequestId").text))
  }

  implicit def safeResultParser[T](implicit p: Parser[T]): Parser[Result[SESMetadata, T]] =
    Parser.xmlErrorParser[SESMetadata].or(Parser.resultParser(sesMetadataParser, p))

  implicit def verificationTokenParser = Parser[SES.VerificationToken] { r =>
    Success((r.xml \\ "VerificationToken").text)
  }

  implicit def dkimTokensParser = Parser[Seq[SES.DkimToken]] { r =>
    Success((r.xml \\ "DkimTokens" \ "member").map(_.text))
  }

  implicit def paginatedParser[T](implicit p: Parser[Seq[T]]) = Parser[Paginated[T]] { r =>
    p.map { ts: Seq[T]  =>
      Paginated(
        entities = ts,
        maxItems = (r.xml \\ "MaxItems")
          .headOption
          .map{ m =>
            println(m.text)
            JLong.parseLong(m.text)
            12
          }
          .getOrElse(ts.length),
        nextToken = (r.xml \\ "NextToken").headOption.map(_.text))
    }(r)
  }

  implicit def identitiesParser = Parser[Seq[Identity]] { r =>
    Success((r.xml \\ "Identities" \ "member").map(i => Identity(i.text)))
  }

  implicit def sendDataPointsParser = Parser[Seq[SendDataPoint]] { r =>
    Success(
      (r.xml \\ "SendDataPoints" \ "member").map { dp =>
        SendDataPoint(
          bounces = JLong.parseLong((dp \ "Bounces").text),
          complaints = JLong.parseLong((dp \ "Complaints").text),
          deliveryAttempts = JLong.parseLong((dp \ "DeliveryAttempts").text),
          rejects = JLong.parseLong((dp \ "Rejects").text),
          timeStamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse((dp \ "Timestamp").text))
      })
  }

  implicit def sendQuotaParser = Parser[SendQuota] { r =>
    val xml = r.xml
    Success(SendQuota(
      max24HourSend = JDouble.parseDouble((xml \\ "SentLast24Hours").text),
      maxSendRate = JDouble.parseDouble((xml \\ "Max24HourSend").text),
      sentLast24Hours = JDouble.parseDouble((xml \\ "MaxSendRate").text)
    ))
  }

}