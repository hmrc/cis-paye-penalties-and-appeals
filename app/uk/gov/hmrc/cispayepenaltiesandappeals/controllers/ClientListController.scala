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
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.cispayepenaltiesandappeals.services.{AgentAuthorisationService, ClientListService}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/** Agent-only endpoints: client list status, has-client checks and the F4 agent authorisation guard.
  *
  * All of them use [[AgentAction]], so a caller who is not an agent with an activated IR-PAYE-AGENT enrolment gets 403.
  */
@Singleton
class ClientListController @Inject()(
                                     cc: ControllerComponents,
                                     auth: AgentAction,
                                     service: ClientListService,
                                     agentAuthorisationService: AgentAuthorisationService
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  /** `POST /cis/agent/authorise` (F4).
    *
    * Body: `{ "hmrcOfficeNumber": "123", "schemeReferenceNumber": "AB456" }`
    *
    * Responses: 200 `{"authorised": true}`; 200 `{"authorised": false, "reason": ...}`; 400 invalid payload (JSON shape,
    * 3 digit office number or blank scheme reference, all reported in `errors`); 403 not an agent or no activated
    * IR-PAYE-AGENT (from [[AgentAction]]); 500 upstream or unexpected error.
    *
    * The agent's identity is taken from the auth token, not the body.
    */
  def authorise(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[AgentAuthoriseRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          agentAuthorisationService
            .authoriseCisAgent(request.irAgentId, request.credentialId, body)(using HeaderCarrierConverter.fromRequest(request))
            // Not being authorised is a normal outcome, so it is a 200 with authorised = false
            .map {
              case AgentAuthorisationResult.Authorised            =>
                Ok(Json.toJson(AgentAuthoriseResponse(authorised = true)))
              case AgentAuthorisationResult.NotAuthorised(reason) =>
                Ok(Json.toJson(AgentAuthoriseResponse(authorised = false, reason = Some(reason))))
            }
            .recover { case t =>
              logger.error("[ClientListController][authorise] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }


  def getClientListDownloadStatus(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[ClientListStatusRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .getClientListDownloadStatus(body)
            .map { result =>
              Ok(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[ClientListController][getClientListDownloadStatus] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def hasCisClient(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[HasCisClientRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .hasCisClient(body)
            .map { result =>
              Ok(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[ClientListController][hasCisClient] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }

  def hasPayeClient(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[HasPayeClientRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          service
            .hasPayeClient(body)
            .map { result =>
              Ok(Json.toJson(result))
            }
            .recover { case t =>
              logger.error("[ClientListController][hasPayeClient] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      )
  }
}
