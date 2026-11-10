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

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.cispayepenaltiesandappeals.controllers.actions.IdentifierAction
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.cispayepenaltiesandappeals.services.ClientListDownloadService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientListDownloadController @Inject()(
                                              cc: ControllerComponents,
                                              auth: IdentifierAction,
                                              service: ClientListDownloadService
                                            )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  private val supportedServices = Set("CIS", "PAYE")

  def initiateClientListDownload(): Action[JsValue] = auth.async(parse.json) { implicit request =>
    request.body
      .validate[InitiateClientListDownloadRequest]
      .fold(
        errs =>
          Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs)))),
        body =>
          validationError(body) match
            case Some(reason) =>
              Future.successful(BadRequest(Json.obj("message" -> "Invalid payload", "reason" -> reason)))
            case None =>
              service
                .initiateClientListDownload(body)
                .map { result =>
                  Ok(Json.toJson(result))
                }
                .recover { case t =>
                  logger.error("[ClientListDownloadController][initiateClientListDownload] failed", t)
                  InternalServerError(Json.obj("message" -> "Unexpected error"))
                }
      )
  }

  private def validationError(request: InitiateClientListDownloadRequest): Option[String] =
    if !supportedServices.contains(request.serviceId) then
      Some("serviceId must be CIS or PAYE")
    else if request.credentialId.trim.isEmpty || request.agentId.trim.isEmpty then
      Some("credentialId and agentId must be provided")
    else
      None
}
