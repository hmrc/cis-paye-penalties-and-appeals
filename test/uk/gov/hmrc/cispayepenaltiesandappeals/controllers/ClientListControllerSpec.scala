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
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.cispayepenaltiesandappeals.services.ClientListService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

class ClientListControllerSpec extends SpecBase {

  "ClientListController" - {

    "POST /agent/clientlist/status (getClientListDownloadStatus)" - {

      val testRequest: ClientListStatusRequest   = ClientListStatusRequest(credentialId = "123", serviceId = "CIS")
      val testResponse: ClientListStatusResponse = ClientListStatusResponse(status = "Succeeded")

      "return OK with response when service returns successfully" in new BaseSetup {
        when(mockClientListService.getClientListDownloadStatus(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.getClientListDownloadStatus()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockClientListService).getClientListDownloadStatus(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.getClientListDownloadStatus()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.getClientListDownloadStatus()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockClientListService.getClientListDownloadStatus(any[ClientListStatusRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.getClientListDownloadStatus()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "POST /cis/agent/clientlist/has-client (hasCisClient)" - {

      val testRequest = HasCisClientRequest(
        agentDetails = AgentDetails(credentialId = "CRED01", irAgentId = "123456"),
        contractorDetails = ContractorDetails(hmrcOfficeNumber = "754", schemeReferenceNumber = "EZ00100")
      )

      "return OK with hasClient true when service returns true" in new BaseSetup {
        val testResponse = HasClientResponse(hasClient = true)
        when(mockClientListService.hasCisClient(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.hasCisClient()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockClientListService).hasCisClient(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return OK with hasClient false when service returns false" in new BaseSetup {
        val testResponse = HasClientResponse(hasClient = false)
        when(mockClientListService.hasCisClient(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.hasCisClient()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.hasCisClient()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.hasCisClient()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockClientListService.hasCisClient(any[HasCisClientRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.hasCisClient()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 Unexpected error on upstream errors" in new BaseSetup {
        when(mockClientListService.hasCisClient(any[HasCisClientRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("Service unavailable", SERVICE_UNAVAILABLE)))

        val result: Future[Result] = controller.hasCisClient()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "POST /paye/agent/clientlist/has-client (hasPayeClient)" - {

      val testRequest = HasPayeClientRequest(
        agentDetails = AgentDetails(credentialId = "CRED01", irAgentId = "123456"),
        employerDetails = EmployerDetails(hmrcOfficeNumber = "840", schemeReferenceNumber = "MZ00064")
      )

      "return OK with hasClient true when service returns true" in new BaseSetup {
        val testResponse = HasClientResponse(hasClient = true)
        when(mockClientListService.hasPayeClient(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.hasPayeClient()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
        verify(mockClientListService).hasPayeClient(eqTo(testRequest))(any[HeaderCarrier])
      }

      "return OK with hasClient false when service returns false" in new BaseSetup {
        val testResponse = HasClientResponse(hasClient = false)
        when(mockClientListService.hasPayeClient(eqTo(testRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: Future[Result] = controller.hasPayeClient()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testResponse)
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.hasPayeClient()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.hasPayeClient()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockClientListService.hasPayeClient(any[HasPayeClientRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.hasPayeClient()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }

      "return 500 Unexpected error on upstream errors" in new BaseSetup {
        when(mockClientListService.hasPayeClient(any[HasPayeClientRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("Service unavailable", SERVICE_UNAVAILABLE)))

        val result: Future[Result] = controller.hasPayeClient()(fakeRequest.withBody(Json.toJson(testRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }
  }

  private trait BaseSetup {
    val mockClientListService: ClientListService = mock[ClientListService]
    val controller = new ClientListController(cc, fakeIdentifierAction, mockClientListService)
  }
}
