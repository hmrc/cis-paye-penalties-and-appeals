/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cispayepenaltiesandappeals.models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}

import java.time.LocalDate

class CisPayeModelsSpec extends AnyWordSpec with Matchers {

  "CreateAppealRequest" should {

    "serialize to JSON correctly" in {
      val request = CreateAppealRequest(
        penaltyId = 1
      )

      val json = Json.toJson(request)

      (json \ "penaltyId").as[Long] mustBe 1
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "penaltyId" -> 1
      )

      val result = json.validate[CreateAppealRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.penaltyId mustBe 1
    }

    "fail to deserialize when penaltyId is missing" in {
      val json = Json.obj(
      )

      val result = json.validate[CreateAppealRequest]

      result.isError mustBe true
    }
  }

  "CreateAppealResponse" should {

    "serialize to JSON correctly when created is true" in {
      val response = CreateAppealResponse(created = true)

      val json = Json.toJson(response)

      (json \ "created").as[Boolean] mustBe true
    }

    "serialize to JSON correctly when created is false" in {
      val response = CreateAppealResponse(created = false)

      val json = Json.toJson(response)

      (json \ "created").as[Boolean] mustBe false
    }

    "deserialize from JSON correctly when created is true" in {
      val json = Json.obj("created" -> true)

      val result = json.validate[CreateAppealResponse]

      result mustBe a[JsSuccess[_]]
      result.get.created mustBe true
    }

    "deserialize from JSON correctly when created is false" in {
      val json = Json.obj("created" -> false)

      val result = json.validate[CreateAppealResponse]

      result mustBe a[JsSuccess[_]]
      result.get.created mustBe false
    }

    "fail to deserialize when created field is missing" in {
      val json = Json.obj()

      val result = json.validate[CreateAppealResponse]

      result.isError mustBe true
    }
  }

  "GetAppealablePenaltiesRequest" should {

    "serialize to JSON correctly" in {
      val request = GetAppealablePenaltiesRequest(
        hmrcOfficeNumber = "123",
        schemeReferenceNumber = "AB456",
        startRow = 1L,
        endRow = 10L
      )

      val json = Json.toJson(request)

      (json \ "hmrcOfficeNumber").as[String] mustBe "123"
      (json \ "schemeReferenceNumber").as[String] mustBe "AB456"
      (json \ "startRow").as[Long] mustBe 1L
      (json \ "endRow").as[Long] mustBe 10L
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "hmrcOfficeNumber" -> "123",
        "schemeReferenceNumber" -> "AB456",
        "startRow" -> 1L,
        "endRow" -> 10L
      )

      val result = json.validate[GetAppealablePenaltiesRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.hmrcOfficeNumber mustBe "123"
      request.schemeReferenceNumber mustBe "AB456"
      request.startRow mustBe 1L
      request.endRow mustBe 10L
    }

    "fail to deserialize when entire request is missing" in {
      val json = Json.obj()

      val result = json.validate[GetAppealablePenaltiesRequest]

      result.isError mustBe true
    }
  }

  "GetAppealablePenaltiesResponse" should {

    "serialize to JSON correctly" in {
      val penalty1 = GetAppealablePenaltyResponse(
        uniqueId = Some(10001),
        issueDate = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
        penaltyType = Some("CIS003"),
        penaltyAmount = Some(BigDecimal("999.23"))
      )

      val penalty2 = GetAppealablePenaltyResponse(
        uniqueId = Some(10002),
        issueDate = Some(LocalDate.of(2025, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2026, 4, 1)),
        penaltyType = Some("CIS005"),
        penaltyAmount = Some(BigDecimal("148.29"))
      )

      val response = GetAppealablePenaltiesResponse(
        total = 2,
        appealablePenalties = List(penalty1, penalty2)
      )

      val json = Json.toJson(response)

      (json \ "total").as[Int] mustBe 2

      val first = (json \ "appealablePenalties" \ 0).get
      (first \ "uniqueId").as[Long] mustBe 10001
      (first \ "issueDate").as[LocalDate] mustBe LocalDate.of(2023, 1, 1)
      (first \ "taxPeriodEndDate").as[LocalDate] mustBe LocalDate.of(2023, 4, 1)
      (first \ "penaltyType").as[String] mustBe "CIS003"
      (first \ "penaltyAmount").as[BigDecimal] mustBe BigDecimal("999.23")

      val second = (json \ "appealablePenalties" \ 1).get
      (second \ "uniqueId").as[Long] mustBe 10002
      (second \ "issueDate").as[LocalDate] mustBe LocalDate.of(2025, 1, 1)
      (second \ "taxPeriodEndDate").as[LocalDate] mustBe LocalDate.of(2026, 4, 1)
      (second \ "penaltyType").as[String] mustBe "CIS005"
      (second \ "penaltyAmount").as[BigDecimal] mustBe BigDecimal("148.29")
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "total" -> 2,
        "appealablePenalties" -> Json.arr(
          Json.obj(
            "uniqueId" -> 1001,
            "issueDate" -> "2026-01-15",
            "taxPeriodEndDate" -> "2025-12-05",
            "penaltyType" -> "CIS003",
            "penaltyAmount" -> 100
          ),
          Json.obj(
            "uniqueId" -> 1002,
            "issueDate" -> "2026-02-20",
            "taxPeriodEndDate" -> "2026-01-05",
            "penaltyType" -> "CIS005",
            "penaltyAmount" -> 200
          )
        )
      )

      val result = json.validate[GetAppealablePenaltiesResponse]

      result mustBe a[JsSuccess[_]]
      val response = result.get

      response.total mustBe 2
      response.appealablePenalties mustBe List(
        GetAppealablePenaltyResponse(
          uniqueId = Some(1001),
          issueDate = Some(LocalDate.of(2026, 1, 15)),
          taxPeriodEndDate = Some(LocalDate.of(2025, 12, 5)),
          penaltyType = Some("CIS003"),
          penaltyAmount = Some(BigDecimal(100))
        ),
        GetAppealablePenaltyResponse(
          uniqueId = Some(1002),
          issueDate = Some(LocalDate.of(2026, 2, 20)),
          taxPeriodEndDate = Some(LocalDate.of(2026, 1, 5)),
          penaltyType = Some("CIS005"),
          penaltyAmount = Some(BigDecimal(200))
        )
      )
    }

    "fail to deserialize when request is missing" in {
      val json = Json.obj()

      val result = json.validate[GetAppealablePenaltiesResponse]

      result.isError mustBe true
    }
  }

  "GetAppealablePenaltyRequest" should {

    "serialize to JSON correctly" in {
      val request = GetAppealablePenaltyRequest(
        penaltyId = 1L,
        hmrcOfficeNumber = "840123",
        schemeReferenceNumber = "098345"
      )

      val json = Json.toJson(request)

      (json \ "penaltyId").as[Long] mustBe 1
      (json \ "hmrcOfficeNumber").as[String] mustBe "840123"
      (json \ "schemeReferenceNumber").as[String] mustBe "098345"
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "penaltyId" -> 1,
        "hmrcOfficeNumber" -> "840123",
        "schemeReferenceNumber" -> "098345"
      )

      val result = json.validate[GetAppealablePenaltyRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.penaltyId mustBe 1
      request.hmrcOfficeNumber mustBe "840123"
      request.schemeReferenceNumber mustBe "098345"
    }

    "fail to deserialize when data is missing" in {
      val json = Json.obj(
        "hmrcOfficeNumber" -> "840123",
        "schemeReferenceNumber" -> "098345"
      )

      val result = json.validate[GetAppealablePenaltyRequest]

      result.isError mustBe true
    }
  }

  "GetAppealablePenaltyResponse" should {

    "serialize to JSON correctly" in {
      val response = GetAppealablePenaltyResponse(
        uniqueId = Some(10001),
        issueDate = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
        penaltyType = Some("CIS003"),
        penaltyAmount = Some(BigDecimal(999.23))
      )

      val json = Json.toJson(response)

      (json \ "uniqueId").as[Long] mustBe 10001
      (json \ "issueDate").as[String] mustBe "2023-01-01"
      (json \ "taxPeriodEndDate").as[String] mustBe "2023-04-01"
      (json \ "penaltyType").as[String] mustBe "CIS003"
      (json \ "penaltyAmount").as[Double] mustBe 999.23
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "uniqueId" -> 10001,
        "issueDate" -> "2023-01-01",
        "taxPeriodEndDate" -> "2023-04-01",
        "penaltyType" -> "CIS003",
        "penaltyAmount" -> 999.23
      )

      val result = json.validate[GetAppealablePenaltyResponse]

      result mustBe a[JsSuccess[_]]
      result.get.uniqueId mustBe Some(10001)
      result.get.issueDate mustBe Some(LocalDate.of(2023, 1, 1))
      result.get.taxPeriodEndDate mustBe Some(LocalDate.of(2023, 4, 1))
      result.get.penaltyType mustBe Some("CIS003")
      result.get.penaltyAmount mustBe Some(999.23)
    }

    "fail to deserialize when type is incorrect" in {
      val json = Json.obj(
        "uniqueId" -> "10001"
      )

      val result = json.validate[GetAppealablePenaltyResponse]

      result.isError mustBe true
    }
  }

  "GetCisTaxpayerRequest" should {

    "serialize to JSON correctly" in {
      val request = GetCisTaxpayerRequest(
        hmrcOfficeNumber = "123",
        schemeReferenceNumber = "AB456"
      )

      val json = Json.toJson(request)

      (json \ "hmrcOfficeNumber").as[String] mustBe "123"
      (json \ "schemeReferenceNumber").as[String] mustBe "AB456"
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "hmrcOfficeNumber" -> "123",
        "schemeReferenceNumber" -> "AB456"
      )

      val result = json.validate[GetCisTaxpayerRequest]

      result mustBe a[JsSuccess[_]]
      val request = result.get

      request.hmrcOfficeNumber mustBe "123"
      request.schemeReferenceNumber mustBe "AB456"
    }

    "fail to deserialize when data is missing" in {
      val json = Json.obj(
        "hmrcOfficeNumber" -> "123"
      )

      val result = json.validate[GetCisTaxpayerRequest]

      result.isError mustBe true
    }

    "fail to deserialize when proxy field names are used" in {
      val json = Json.obj(
        "taxOfficeNumber" -> "123",
        "taxOfficeReference" -> "AB456"
      )

      val result = json.validate[GetCisTaxpayerRequest]

      result.isError mustBe true
    }
  }

  "GetCisTaxpayerResponse" should {

    "serialize to JSON correctly" in {
      val response = GetCisTaxpayerResponse(
        uniqueId = "1000123",
        taxOfficeNumber = "123",
        taxOfficeRef = "AB456",
        aoDistrict = Some("123"),
        aoPayType = Some("P"),
        aoCheckCode = Some("X"),
        aoReference = Some("00123456"),
        validBusinessAddr = Some("Y"),
        correlation = Some("corr-1"),
        ggAgentId = Some("GG123"),
        employerName1 = Some("ABC"),
        employerName2 = Some("Builders"),
        agentOwnRef = Some("REF1"),
        schemeName = Some("ABC Builders Ltd"),
        utr = Some("1234567890"),
        enrolledSig = Some("Y")
      )

      val json = Json.toJson(response)

      (json \ "uniqueId").as[String] mustBe "1000123"
      (json \ "taxOfficeNumber").as[String] mustBe "123"
      (json \ "taxOfficeRef").as[String] mustBe "AB456"
      (json \ "aoDistrict").as[String] mustBe "123"
      (json \ "aoPayType").as[String] mustBe "P"
      (json \ "aoCheckCode").as[String] mustBe "X"
      (json \ "aoReference").as[String] mustBe "00123456"
      (json \ "validBusinessAddr").as[String] mustBe "Y"
      (json \ "correlation").as[String] mustBe "corr-1"
      (json \ "ggAgentId").as[String] mustBe "GG123"
      (json \ "employerName1").as[String] mustBe "ABC"
      (json \ "employerName2").as[String] mustBe "Builders"
      (json \ "agentOwnRef").as[String] mustBe "REF1"
      (json \ "schemeName").as[String] mustBe "ABC Builders Ltd"
      (json \ "utr").as[String] mustBe "1234567890"
      (json \ "enrolledSig").as[String] mustBe "Y"
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "uniqueId" -> "1000123",
        "taxOfficeNumber" -> "123",
        "taxOfficeRef" -> "AB456",
        "aoDistrict" -> "123",
        "aoPayType" -> "P",
        "aoCheckCode" -> "X",
        "aoReference" -> "00123456",
        "validBusinessAddr" -> "Y",
        "correlation" -> "corr-1",
        "ggAgentId" -> "GG123",
        "employerName1" -> "ABC",
        "employerName2" -> "Builders",
        "agentOwnRef" -> "REF1",
        "schemeName" -> "ABC Builders Ltd",
        "utr" -> "1234567890",
        "enrolledSig" -> "Y"
      )

      val result = json.validate[GetCisTaxpayerResponse]

      result mustBe a[JsSuccess[_]]
      result.get.uniqueId mustBe "1000123"
      result.get.taxOfficeNumber mustBe "123"
      result.get.taxOfficeRef mustBe "AB456"
      result.get.aoDistrict mustBe Some("123")
      result.get.aoPayType mustBe Some("P")
      result.get.aoCheckCode mustBe Some("X")
      result.get.aoReference mustBe Some("00123456")
      result.get.validBusinessAddr mustBe Some("Y")
      result.get.correlation mustBe Some("corr-1")
      result.get.ggAgentId mustBe Some("GG123")
      result.get.employerName1 mustBe Some("ABC")
      result.get.employerName2 mustBe Some("Builders")
      result.get.agentOwnRef mustBe Some("REF1")
      result.get.schemeName mustBe Some("ABC Builders Ltd")
      result.get.utr mustBe Some("1234567890")
      result.get.enrolledSig mustBe Some("Y")
    }

    "fail to deserialize when type is incorrect" in {
      val json = Json.obj(
        "uniqueId" -> 1000123,
        "taxOfficeNumber" -> "123",
        "taxOfficeRef" -> "AB456"
      )

      val result = json.validate[GetCisTaxpayerResponse]

      result.isError mustBe true
    }

    "deserialize when only mandatory fields are present" in {
      val json = Json.obj(
        "uniqueId" -> "1000123",
        "taxOfficeNumber" -> "123",
        "taxOfficeRef" -> "AB456"
      )

      val result = json.validate[GetCisTaxpayerResponse]

      result mustBe a[JsSuccess[_]]
      result.get.schemeName mustBe None
      result.get.utr mustBe None
    }

    "fail to deserialize when a mandatory field is missing" in {
      val json = Json.obj(
        "taxOfficeNumber" -> "123",
        "taxOfficeRef" -> "AB456"
      )

      val result = json.validate[GetCisTaxpayerResponse]

      result.isError mustBe true
    }
  }
}
