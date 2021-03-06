package com.pellucid.wrap.dynamodb

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import org.scalatest.{FlatSpec, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers

import com.amazonaws.AmazonClientException
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model._


class ReadsOnHashKeyTableSpec
  extends FlatSpec
     with ShouldMatchers
     with DynamoDBClient
{
  import SampleData.sampleForums

  override val tableNames = Seq(Forum.tableName)

  val mapper = AmazonDynamoDBScalaMapper(client)

  override def beforeAll() {
    super.beforeAll()

    tryCreateTable(Forum.tableRequest)
    awaitTableCreation(Forum.tableName)

    await(30.seconds) {
      mapper.batchDump(sampleForums)
    }
  }

  "DynamoDB" should s"contain the '${Forum.tableName}' table" in {
    val result = await(1.minutes) {
      client.listTables()
    }

    result.getTableNames().asScala should contain (Forum.tableName)
  }

  it should "contain the first sample Forum" in {
    import org.scalatest.OptionValues._
    await {
      mapper.loadByKey[Forum](sampleForums.head.name)
    } .value should be (sampleForums.head)

    val result = await {
      mapper.batchLoadByKeys[Forum](Seq(sampleForums.head.name))
    }
    result should have size (1)
    result.head should be (sampleForums.head)
  }

  it should s"contain ${sampleForums.size} forum items" in {
    await {
      mapper.countScan[Forum]()
    } should equal (sampleForums.size)
  }

  it should s"contain the sample forum items" in {
    val forumScan = await {
      mapper.scan[Forum]()
    }
    val forumScanOnce = await {
      mapper.scanOnce[Forum]()
    }
    val forumScanOnceLimit = await {
      mapper.scanOnce[Forum](limit = sampleForums.size)
    }
    val forumBatch = await {
      mapper.batchLoadByKeys[Forum](sampleForums map (_.name))
    }

    forumScan          should have size (sampleForums.size)
    forumScanOnce      should have size (sampleForums.size)
    forumScanOnceLimit should have size (sampleForums.size)
    forumBatch         should have size (sampleForums.size)

    for (forum <- sampleForums) {
      forumScan     should contain (forum)
      forumScanOnce should contain (forum)
      forumBatch    should contain (forum)
    }
  }

}
