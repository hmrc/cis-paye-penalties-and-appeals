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
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.cispayepenaltiesandappeals.services.PayeService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ManagePayeControllerSpec extends SpecBase {

  "ManagePayeController" - {

    "POST /paye/appeal (createPayeAppeal)" - {

      val testCreatePayeAppealRequest: CreatePayeAppealRequest   = CreatePayeAppealRequest(penaltyId = 1L)
      val testCreatePayeAppealResponse: CreatePayeAppealResponse = CreatePayeAppealResponse(created = true)

      "return CREATED with response when service returns successfully" in new BaseSetup {
        when(mockPayeService.createPayeAppeal(eqTo(testCreatePayeAppealRequest))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testCreatePayeAppealResponse))

        val result: Future[Result] = controller.createPayeAppeal()(fakeRequest.withBody(Json.toJson(testCreatePayeAppealRequest)))

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(testCreatePayeAppealResponse)
        verify(mockPayeService).createPayeAppeal(eqTo(testCreatePayeAppealRequest))(any[HeaderCarrier])
      }

      "return BAD_REQUEST with message when given an invalid json body" in new BaseSetup {
        val result: Future[Result] = controller.createPayeAppeal()(fakeRequest.withBody(Json.obj("invalid" -> "data")))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
        (contentAsJson(result) \ "errors").isDefined mustBe true
      }

      "return BAD_REQUEST when required fields are missing" in new BaseSetup {
        val result: Future[Result] = controller.createPayeAppeal()(fakeRequest.withBody(Json.obj()))

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
      }

      "return 500 Unexpected error on unknown exception" in new BaseSetup {
        when(mockPayeService.createPayeAppeal(any[CreatePayeAppealRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected")))

        val result: Future[Result] = controller.createPayeAppeal()(fakeRequest.withBody(Json.toJson(testCreatePayeAppealRequest)))

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
