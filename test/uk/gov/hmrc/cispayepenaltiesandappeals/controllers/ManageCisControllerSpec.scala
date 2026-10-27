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

package uk.gov.hmrc.cispayepenaltiesandappeals.controllers

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.cispayepenaltiesandappeals.services.CisService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.Future

class ManageCisControllerSpec extends SpecBase {

  "ManageCisController" - {

    "POST /cis/appeal (createCisAppeal)" - {

      val testCreateAppealRequest: CreateAppealRequest   = CreateAppealRequest(penaltyId = 1L)
      val testCreateAppealResponse: CreateAppealResponse = CreateAppealResponse(created = true)

      "return CREATED with response when service returns successfully" in new BaseSetup {
        when(mockCisService.createCisAppeal(eqTo(testCreateAppealRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreateAppealResponse))

        val result: Future[Result] = controller.createCisAppeal()(fakeRequest.withBody(Json.toJson(testCreateAppealRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreateAppealResponse)
        verify(mockCisService).createCisAppeal(eqTo(testCreateAppealRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createCisAppeal()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.createCisAppeal()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockCisService.createCisAppeal(any[CreateAppealRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createCisAppeal()(fakeRequest.withBody(Json.toJson(testCreateAppealRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "POST /cis/penalties (getCisAppealablePenalties)" - {

      val testRequest: GetAppealablePenaltiesRequest = GetAppealablePenaltiesRequest(
        hmrcOfficeNumber = "123",
        schemeReferenceNumber = "AB456",
        startRow = 1L,
        endRow = 10L
      )
      val testResponse: GetAppealablePenaltiesResponse = GetAppealablePenaltiesResponse(
        total = 2,
        appealablePenalties = List(
          GetAppealablePenaltyResponse(
            uniqueId = Some(10001),
            issueDate = Some(LocalDate.of(2023, 1, 1)),
            taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
            penaltyType = Some("CIS003"),
            penaltyAmount = Some(BigDecimal(999.23))
          ),
          GetAppealablePenaltyResponse(
            uniqueId = Some(10002),
            issueDate = Some(LocalDate.of(2026, 2, 20)),
            taxPeriodEndDate = Some(LocalDate.of(2026, 1, 5)),
            penaltyType = Some("CIS005"),
            penaltyAmount = Some(BigDecimal(928.23))
          )
        )
      )

      "return OK with response when service returns successfully" in new BaseSetup {
        when(mockCisService.getCisAppealablePenalties(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.getCisAppealablePenalties()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockCisService).getCisAppealablePenalties(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.getCisAppealablePenalties()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.getCisAppealablePenalties()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockCisService.getCisAppealablePenalties(any[GetAppealablePenaltiesRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.getCisAppealablePenalties()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }
    "POST /cis/penalty (getCisAppealablePenalty)" - {

      val testRequest = GetAppealablePenaltyRequest(
        penaltyId = 1L,
        hmrcOfficeNumber = "840123",
        schemeReferenceNumber = "098345"
      )

      val testResponse = GetAppealablePenaltyResponse(
        uniqueId = None,
        issueDate = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
        penaltyType = Some("CIS003"),
        penaltyAmount = Some(BigDecimal(999.23))
      )

      "return OK with response when service returns successfully" in new BaseSetup {
        when(mockCisService.getCisAppealablePenalty(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.getCisAppealablePenalty()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockCisService).getCisAppealablePenalty(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.getCisAppealablePenalty()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.getCisAppealablePenalty()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockCisService.getCisAppealablePenalty(any[GetAppealablePenaltyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.getCisAppealablePenalty()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "POST /cis/taxpayer (getCisTaxpayer)" - {

      val testRequest = GetTaxpayerRequest(
        hmrcOfficeNumber = "123",
        schemeReferenceNumber = "AB456"
      )

      val testResponse = GetCisTaxpayerResponse(
        uniqueId = "1000123",
        taxOfficeNumber = "123",
        taxOfficeRef = "AB456",
        aoDistrict = Some("123"),
        aoPayType = Some("P"),
        aoCheckCode = Some("X"),
        aoReference = Some("00123456"),
        validBusinessAddr = None,
        correlation = None,
        ggAgentId = None,
        employerName1 = Some("ABC"),
        employerName2 = None,
        agentOwnRef = None,
        schemeName = Some("ABC Builders Ltd"),
        utr = Some("1234567890"),
        enrolledSig = None
      )

      "return OK with response when service returns successfully" in new BaseSetup {
        when(mockCisService.getCisTaxpayer(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.getCisTaxpayer()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockCisService).getCisTaxpayer(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.getCisTaxpayer()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.getCisTaxpayer()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockCisService.getCisTaxpayer(any[GetTaxpayerRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.getCisTaxpayer()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return NOT_FOUND when the taxpayer does not exist" in new BaseSetup {
        when(mockCisService.getCisTaxpayer(any[GetTaxpayerRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("CIS taxpayer not found", NOT_FOUND)))

        val result: Future[Result] = controller.getCisTaxpayer()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe NOT_FOUND
        (contentAsJson(result) \ "message").as[String] mustBe "CIS taxpayer not found"
      }

      "return 500 Unexpected error on other upstream errors" in new BaseSetup {
        when(mockCisService.getCisTaxpayer(any[GetTaxpayerRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("Service unavailable", SERVICE_UNAVAILABLE)))

        val result: Future[Result] = controller.getCisTaxpayer()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }
  }

  private trait BaseSetup {
    val mockCisService: CisService = mock[CisService]
    val controller = new ManageCisController(cc, fakeIdentifierAction, mockCisService)
  }
}
