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

package uk.gov.hmrc.cispayepenaltiesandappeals.controllers.actions

import play.api.Logging
import uk.gov.hmrc.auth.core.*
import play.api.mvc.*
import play.api.mvc.Results.Forbidden
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.cispayepenaltiesandappeals.models.auth.IdentifierRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedIdentifierAction @Inject()(
                                               override val authConnector: AuthConnector,
                                               val parser: BodyParsers.Default
                                             )
                                             (implicit val executionContext: ExecutionContext) extends IdentifierAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A],
                              block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    given hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised()
      .retrieve(
        Retrievals.affinityGroup
      ) {
        case Some(Organisation) =>
          block(IdentifierRequest(request, Organisation))
        case _ =>
          throw InternalError("Authentication error: expected affinity group not found")

      }.recoverWith {
        case ex =>
          logger.error(s"[AuthenticatedIdentifierAction][authorised] - Authentication failed: ${ex.getCause}-${ex.getMessage}")
          Future.successful(Forbidden)
      }
  }
}
