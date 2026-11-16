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
import play.api.mvc.*
import play.api.mvc.Results.Forbidden
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.cispayepenaltiesandappeals.models.auth.AgentRequest
import uk.gov.hmrc.cispayepenaltiesandappeals.utils.EnrolmentHelper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/** Action builder for endpoints that only agents may call. See [[AuthenticatedAgentAction]]. */
trait AgentAction extends ActionBuilder[AgentRequest, AnyContent] with ActionFunction[Request, AgentRequest]

/** Authenticates the caller as an agent and builds an [[uk.gov.hmrc.cispayepenaltiesandappeals.models.auth.AgentRequest]].
  *
  * Allows the request only when all of these hold, otherwise it returns 403 Forbidden:
  *  - the auth session is valid
  *  - the affinity group is `Agent`
  *  - the user has an activated `IR-PAYE-AGENT` enrolment with a valid `IRAgentReference`
  *  - credentials are present (the provider id becomes the `credentialId`)
  *
  * This is separate from `AuthenticatedIdentifierAction`, which only allows `Organisation` users and is left unchanged.
  */
class AuthenticatedAgentAction @Inject()(
                                          override val authConnector: AuthConnector,
                                          val parser: BodyParsers.Default
                                        )(implicit val executionContext: ExecutionContext)
  extends AgentAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A], block: AgentRequest[A] => Future[Result]): Future[Result] = {
    given hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised()
      .retrieve(Retrievals.affinityGroup and Retrievals.allEnrolments and Retrievals.credentials) {
        // Agent with credentials: the enrolment must also be present, activated and well formed
        case Some(AffinityGroup.Agent) ~ enrolments ~ Some(credentials) =>
          EnrolmentHelper.irAgentReference(enrolments) match {
            case Some(irAgentId) =>
              block(AgentRequest(request, irAgentId, credentials.providerId))
            case None            =>
              logger.warn("[AuthenticatedAgentAction][invokeBlock] - No activated IR-PAYE-AGENT enrolment with a valid IRAgentReference")
              Future.successful(Forbidden)
          }
        // Not an agent (for example an organisation or individual), or no credentials returned
        case _                                                          =>
          logger.warn("[AuthenticatedAgentAction][invokeBlock] - User is not an agent or credentials are missing")
          Future.successful(Forbidden)
      }
      // Any auth failure (no session, insufficient enrolments, auth service error) is returned as 403
      .recover {
        case ex =>
          logger.error(s"[AuthenticatedAgentAction][invokeBlock] - Authentication failed: ${ex.getMessage}")
          Forbidden
      }
  }
}
