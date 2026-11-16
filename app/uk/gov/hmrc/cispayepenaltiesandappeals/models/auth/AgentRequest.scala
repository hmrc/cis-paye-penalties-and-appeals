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

package uk.gov.hmrc.cispayepenaltiesandappeals.models.auth

import play.api.mvc.{Request, WrappedRequest}

/** Request wrapper produced by [[uk.gov.hmrc.cispayepenaltiesandappeals.controllers.actions.AgentAction]].
  *
  * Both values come from the auth token, never from the request body, so a caller cannot act as another agent.
  *
  * @param irAgentId    the `IRAgentReference` of the activated `IR-PAYE-AGENT` enrolment
  * @param credentialId the Government Gateway credential (provider) id of the signed-in user
  */
case class AgentRequest[A](request: Request[A], irAgentId: String, credentialId: String) extends WrappedRequest[A](request)
