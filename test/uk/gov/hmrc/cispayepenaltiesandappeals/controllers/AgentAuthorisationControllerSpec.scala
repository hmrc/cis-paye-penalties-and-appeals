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
import org.mockito.Mockito.when
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.controllers.actions.FakeAgentAction
import uk.gov.hmrc.cispayepenaltiesandappeals.models.AgentAuthorisationResult.*
import uk.gov.hmrc.cispayepenaltiesandappeals.models.AgentAuthoriseRequest
import uk.gov.hmrc.cispayepenaltiesandappeals.services.AgentAuthorisationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AgentAuthorisationControllerSpec extends SpecBase {

  private val body = AgentAuthoriseRequest("123", "AB456")

  private trait Setup {
    val mockService: AgentAuthorisationService = mock[AgentAuthorisationService]
    val controller = new AgentAuthorisationController(cc, new FakeAgentAction(bodyParsers), mockService)
  }

  "AgentAuthorisationController.authorise" - {

    "must return 200 authorised true when the service authorises" in new Setup {
      when(mockService.authoriseCisAgent(eqTo("123456"), eqTo("cred-1"), eqTo(body))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Authorised))
      val result: Future[Result] = controller.authorise()(fakeRequest.withBody(Json.toJson(body)))
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("authorised" -> true)
    }

    "must return 200 authorised false with the reason when not authorised" in new Setup {
      when(mockService.authoriseCisAgent(any[String], any[String], any[AgentAuthoriseRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(NotAuthorised("Failed")))
      val result: Future[Result] = controller.authorise()(fakeRequest.withBody(Json.toJson(body)))
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("authorised" -> false, "reason" -> "Failed")
    }

    "must return 400 for a malformed payload" in new Setup {
      val result: Future[Result] = controller.authorise()(fakeRequest.withBody(Json.obj("x" -> "y")))
      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
    }

    "must return 400 when the office number is not three digits" in new Setup {
      val result: Future[Result] =
        controller.authorise()(fakeRequest.withBody(Json.toJson(AgentAuthoriseRequest("12", "AB456"))))
      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid payload"
    }

    "must return 400 when the scheme reference is blank" in new Setup {
      val result: Future[Result] =
        controller.authorise()(fakeRequest.withBody(Json.toJson(AgentAuthoriseRequest("123", " "))))
      status(result) mustBe BAD_REQUEST
    }

    "must return 500 when the service fails" in new Setup {
      when(mockService.authoriseCisAgent(any[String], any[String], any[AgentAuthoriseRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))
      val result: Future[Result] = controller.authorise()(fakeRequest.withBody(Json.toJson(body)))
      status(result) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
    }
  }
}
