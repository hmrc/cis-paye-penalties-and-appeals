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
import org.mockito.Mockito.{never, verify, when}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.cispayepenaltiesandappeals.services.ClientListDownloadService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ClientListDownloadControllerSpec extends SpecBase {

  private val testRequest  = InitiateClientListDownloadRequest("CIS", "cred-1", "agent-1")
  private val testResponse = InitiateClientListDownloadResponse(8000, Seq(1000, 2000))

  "ClientListDownloadController initiateClientListDownload" - {

    "return OK with the intervals when the service succeeds" in new BaseSetup {
      when(mockService.initiateClientListDownload(eqTo(testRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(testResponse))

      val result: Future[Result] =
        controller.initiateClientListDownload()(fakeRequest.withBody(Json.toJson(testRequest)))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("browserInterval" -> 8000, "businessServiceIntervals" -> Json.arr(1000, 2000))
    }

    "accept PAYE as the service" in new BaseSetup {
      val payeRequest: InitiateClientListDownloadRequest = testRequest.copy(serviceId = "PAYE")
      when(mockService.initiateClientListDownload(eqTo(payeRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(testResponse))

      val result: Future[Result] =
        controller.initiateClientListDownload()(fakeRequest.withBody(Json.toJson(payeRequest)))

      status(result) mustBe OK
    }

    "return BAD_REQUEST when the JSON is invalid" in new BaseSetup {
      val result: Future[Result] =
        controller.initiateClientListDownload()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      (contentAsJson(result) \ "errors").isDefined mustBe true
    }

    "return BAD_REQUEST when the serviceId is not CIS or PAYE" in new BaseSetup {
      val result: Future[Result] =
        controller.initiateClientListDownload()(fakeRequest.withBody(Json.toJson(testRequest.copy(serviceId = "VAT"))))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      verify(mockService, never()).initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier])
    }

    "return BAD_REQUEST when the credentialId is blank" in new BaseSetup {
      val result: Future[Result] =
        controller.initiateClientListDownload()(fakeRequest.withBody(Json.toJson(testRequest.copy(credentialId = " "))))

      status(result) mustBe BAD_REQUEST
    }

    "return BAD_REQUEST when the agentId is blank" in new BaseSetup {
      val result: Future[Result] =
        controller.initiateClientListDownload()(fakeRequest.withBody(Json.toJson(testRequest.copy(agentId = ""))))

      status(result) mustBe BAD_REQUEST
    }


    "return 500 Unexpected error when the service fails" in new BaseSetup {
      when(mockService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("unexpected")))

      val result: Future[Result] =
        controller.initiateClientListDownload()(fakeRequest.withBody(Json.toJson(testRequest)))

      status(result) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
    }
  }

  private trait BaseSetup {
    val mockService: ClientListDownloadService = mock[ClientListDownloadService]
    val controller = new ClientListDownloadController(cc, fakeIdentifierAction, mockService)
  }
}
