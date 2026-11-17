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

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.cispayepenaltiesandappeals.controllers.actions.AgentAction
import uk.gov.hmrc.cispayepenaltiesandappeals.models.AgentAuthorisationResult.*
import uk.gov.hmrc.cispayepenaltiesandappeals.models.{AgentAuthoriseRequest, AgentAuthoriseResponse}
import uk.gov.hmrc.cispayepenaltiesandappeals.services.AgentAuthorisationService
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/** `POST /cis/agent/authorise` (F4).
  *
  * Body: `{ "hmrcOfficeNumber": "123", "schemeReferenceNumber": "AB456" }`
  *
  * Responses: 200 `{"authorised": true}`; 200 `{"authorised": false, "reason": ...}`; 400 invalid payload (JSON shape,
  * 3 digit office number or blank scheme reference, all reported in `errors`);
  * 403 not an agent or no activated IR-PAYE-AGENT (from [[AgentAction]]); 500 upstream or unexpected error.
  *
  * The agent's identity is taken from the auth token, not the body.
  */
@Singleton
class AgentAuthorisationController @Inject()(
                                              cc: ControllerComponents,
                                              auth: AgentAction,
                                              service: AgentAuthorisationService
                                            )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def authorise(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[AgentAuthoriseRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .authoriseCisAgent(request.irAgentId, request.credentialId, body)(using HeaderCarrierConverter.fromRequest(request))
            // Not being authorised is a normal outcome, so it is a 200 with authorised = false
            .map {
              case Authorised            => Ok(Json.toJson(AgentAuthoriseResponse(authorised = true)))
              case NotAuthorised(reason) => Ok(Json.toJson(AgentAuthoriseResponse(authorised = false, reason = Some(reason))))
            }
            .recover { case t =>
              logger.error("[AgentAuthorisationController][authorise] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }
}
