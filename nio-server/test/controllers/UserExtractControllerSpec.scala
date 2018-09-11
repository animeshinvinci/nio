package controllers

import models.{Organisation, Permission, PermissionGroup, UserExtract}
import play.api.libs.json.{JsArray, JsValue}
import play.api.test.Helpers._
import utils.TestUtils

class UserExtractControllerSpec extends TestUtils {

  val orgKey: String = "org1"
  val org2Key: String = "org2"
  val userId: String = "user1"
  val userIdXml: String = "user1Xml"

  val org1 = Organisation(
    key = orgKey,
    label = "lbl",
    groups = Seq(
      PermissionGroup(key = "group1",
        label = "blalba",
        permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  val org2 = Organisation(
    key = org2Key,
    label = "lbl",
    groups = Seq(
      PermissionGroup(key = "group1",
        label = "blalba",
        permissions = Seq(Permission("sms", "Please accept sms")))
    )
  )

  val userExtract = UserExtract("user@nio.fr")

  "UserExtractController JSON" should {

    "ask an extraction" in {
      val path: String = s"/$tenant/organisations"
      val createResponse = postJson(path, org1.asJson)
      createResponse.status mustBe CREATED

      val userExtractStatus =
        postJson(s"/$tenant/organisations/$orgKey/users/$userId/_extract",
          userExtract.asJson())
      userExtractStatus.status mustBe OK

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "UserExtractTaskAsked"
      (msgAsJson \ "payload" \ "tenant").as[String] mustBe tenant
      (msgAsJson \ "payload" \ "orgKey").as[String] mustBe orgKey
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userId
    }

    "ask an extraction with an existing orgKey/userId" in {
      val userExtractStatusConflict =
        postJson(s"/$tenant/organisations/$orgKey/users/$userId/_extract",
          userExtract.asJson())
      userExtractStatusConflict.status mustBe CONFLICT
    }

    "ask an extraction for an unknow organisation" in {
      val userExtractStatus =
        postJson(s"/$tenant/organisations/org2/users/$userId/_extract",
          userExtract.asJson())
      userExtractStatus.status mustBe NOT_FOUND
    }

    "list all extracted data for an organisation" in {

      val path: String = s"/$tenant/organisations"
      val createResponse = postJson(path, org2.asJson)
      createResponse.status mustBe CREATED

      postJson(s"/$tenant/organisations/$org2Key/users/$userId/_extract",
        userExtract.asJson()).status mustBe OK

      val userExtractStatus =
        postJson(s"/$tenant/organisations/$orgKey/users/userId2/_extract",
          userExtract.asJson())
      userExtractStatus.status mustBe OK

      val userExtractStatus1 =
        postJson(s"/$tenant/organisations/$orgKey/users/userId3/_extract",
          userExtract.asJson())
      userExtractStatus1.status mustBe OK

      val userExtracted = getJson(s"/$tenant/organisations/$orgKey/_extracted")
      userExtracted.status mustBe OK

      val extracted: JsArray = userExtracted.json.as[JsArray]

      println(extracted)

      extracted.value.size mustBe (3)
      (extracted \ 0 \ "userId").as[String] mustBe ("user1")
      (extracted \ 1 \ "userId").as[String] mustBe ("userId2")
      (extracted \ 2 \ "userId").as[String] mustBe ("userId3")
    }

    "list all extracted data for an unknow organisation" in {
      val userExtracted = getJson(s"/$tenant/organisations/orgUnknow/_extracted")
      userExtracted.status mustBe NOT_FOUND
    }

  }

  "UserExtractController XML" should {

    "ask an extraction" in {
      val userExtractStatus =
        postXml(s"/$tenant/organisations/$orgKey/users/$userIdXml/_extract",
          userExtract.asXml())
      userExtractStatus.status mustBe OK

      val msgAsJson = readLastKafkaEvent()
      (msgAsJson \ "type").as[String] mustBe "UserExtractTaskAsked"
      (msgAsJson \ "payload" \ "tenant").as[String] mustBe tenant
      (msgAsJson \ "payload" \ "orgKey").as[String] mustBe orgKey
      (msgAsJson \ "payload" \ "userId").as[String] mustBe userIdXml
    }

    "ask an extraction with an existing orgKey/userId" in {
      val userExtractStatusConflict =
        postXml(s"/$tenant/organisations/$orgKey/users/$userIdXml/_extract",
          userExtract.asXml())
      userExtractStatusConflict.status mustBe CONFLICT
    }

    "ask an extraction for an unknow organisation" in {
      val userExtractStatus =
        postXml(s"/$tenant/organisations/orgUnknow/users/$userIdXml/_extract",
          userExtract.asXml())
      userExtractStatus.status mustBe NOT_FOUND
    }
  }

}
