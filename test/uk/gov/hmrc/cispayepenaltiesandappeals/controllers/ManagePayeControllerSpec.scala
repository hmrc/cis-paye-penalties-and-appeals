/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.cispayepenaltiesandappeals.services.PayeService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.Future

class ManagePayeControllerSpec extends SpecBase {

  "PayeController" - {

    "POST /paye/penalties (getPayeAppealablePenalties)" - {

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
            penaltyType = Some("PAYE3"),
            penaltyAmount = Some(BigDecimal(999.23))
          ),
          GetAppealablePenaltyResponse(
            uniqueId = Some(10002),
            issueDate = Some(LocalDate.of(2026, 2, 20)),
            taxPeriodEndDate = Some(LocalDate.of(2026, 1, 5)),
            penaltyType = Some("PAYE3"),
            penaltyAmount = Some(BigDecimal(928.23))
          )
        )
      )

      "return OK with response when service returns successfully" in new BaseSetup {
        when(mockPayeService.getPayeAppealablePenalties(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.getPayeAppealablePenalties()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockPayeService).getPayeAppealablePenalties(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.getPayeAppealablePenalties()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.getPayeAppealablePenalties()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPayeService.getPayeAppealablePenalties(any[GetAppealablePenaltiesRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.getPayeAppealablePenalties()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "POST /paye/penalty (getPayeAppealablePenalty)" - {

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
        when(mockPayeService.getPayeAppealablePenalty(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.getPayeAppealablePenalty()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockPayeService).getPayeAppealablePenalty(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.getPayeAppealablePenalty()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.getPayeAppealablePenalty()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPayeService.getPayeAppealablePenalty(any[GetAppealablePenaltyRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.getPayeAppealablePenalty()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "POST /paye/appeal (createPayeAppeal)" - {

      val testRequest: CreateAppealRequest = CreateAppealRequest(penaltyId = 1L)
      val testResponse: CreateAppealResponse = CreateAppealResponse(created = true)

      "return CREATED with response when service returns successfully" in new BaseSetup {
        when(mockPayeService.createPayeAppeal(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.createPayeAppeal()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockPayeService).createPayeAppeal(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createPayeAppeal()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when penaltyId is the wrong type" in new BaseSetup {
        val result: Future[Result] = controller.createPayeAppeal()(fakeRequest.withBody(Json.obj("penaltyId" -> "abc")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPayeService.createPayeAppeal(any[CreateAppealRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createPayeAppeal()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "POST /paye/taxpayer (getPayeTaxpayer)" - {

      val testRequest = GetTaxpayerRequest(
        hmrcOfficeNumber = "123",
        schemeReferenceNumber = "AB456"
      )

      val testResponse = GetPayeTaxpayerResponse(
        uniqueId = 1000123L,
        instanceId = 2000123L,
        districtNumber = "123",
        employerRef = "AB456",
        employerName1 = Some("ABC"),
        employerName2 = None,
        agentOwnRef = None
      )

      "return OK with response when service returns successfully" in new BaseSetup {
        when(mockPayeService.getPayeTaxpayer(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.getPayeTaxpayer()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockPayeService).getPayeTaxpayer(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.getPayeTaxpayer()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.getPayeTaxpayer()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPayeService.getPayeTaxpayer(any[GetTaxpayerRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.getPayeTaxpayer()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return NOT_FOUND when the taxpayer does not exist" in new BaseSetup {
        when(mockPayeService.getPayeTaxpayer(any[GetTaxpayerRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("PAYE taxpayer not found", NOT_FOUND)))

        val result: Future[Result] = controller.getPayeTaxpayer()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe NOT_FOUND
        (contentAsJson(result) \ "message").as[String] mustBe "PAYE taxpayer not found"
      }

      "return 500 Unexpected error on other upstream errors" in new BaseSetup {
        when(mockPayeService.getPayeTaxpayer(any[GetTaxpayerRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("Service unavailable", SERVICE_UNAVAILABLE)))

        val result: Future[Result] = controller.getPayeTaxpayer()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }
  }

  private trait BaseSetup {
    val mockPayeService: PayeService = mock[PayeService]
    val controller = new ManagePayeController(cc, fakeIdentifierAction, mockPayeService)
  }
}