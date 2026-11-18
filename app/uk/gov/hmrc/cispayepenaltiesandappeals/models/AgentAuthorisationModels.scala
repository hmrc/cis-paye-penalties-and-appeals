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

package uk.gov.hmrc.cispayepenaltiesandappeals.models

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import uk.gov.hmrc.cispayepenaltiesandappeals.utils.EnrolmentHelper

/** Request body for `POST /cis/agent/authorise`: the CIS contractor (client) the agent wants to act for. */
case class AgentAuthoriseRequest(hmrcOfficeNumber: String, schemeReferenceNumber: String)

object AgentAuthoriseRequest {

  // Field rules live here so a bad request gets one 400 shape, whichever rule it breaks
  private val officeNumberReads: Reads[String] =
    Reads.StringReads.filter(JsonValidationError("hmrcOfficeNumber must be 3 digits"))(EnrolmentHelper.isValidTaxOfficeNumber)

  private val schemeReferenceReads: Reads[String] =
    Reads.StringReads.filter(JsonValidationError("schemeReferenceNumber must be provided"))(_.trim.nonEmpty)

  implicit val reads: Reads[AgentAuthoriseRequest] = (
    (__ \ "hmrcOfficeNumber").read(officeNumberReads) and
      (__ \ "schemeReferenceNumber").read(schemeReferenceReads)
  )(AgentAuthoriseRequest.apply)

  private val writes: OWrites[AgentAuthoriseRequest] = Json.writes[AgentAuthoriseRequest]

  implicit val format: OFormat[AgentAuthoriseRequest] = OFormat(reads, writes)
}

/** Response body: `authorised` plus, when false, a `reason` explaining why. */
case class AgentAuthoriseResponse(authorised: Boolean, reason: Option[String] = None)

object AgentAuthoriseResponse {
  implicit val format: OFormat[AgentAuthoriseResponse] = Json.format[AgentAuthoriseResponse]
}

/** Outcome of the agent authorisation guard, returned by the service and mapped to JSON by the controller. */
sealed trait AgentAuthorisationResult

object AgentAuthorisationResult {
  /** The client list is available and the agent has the client. */
  case object Authorised                      extends AgentAuthorisationResult
  /** `reason` is the client list status (`Failed`, `InProgress`, `InitiateDownload`) or [[NoClientRelationship]]. */
  case class NotAuthorised(reason: String)    extends AgentAuthorisationResult

  /** The client list was available but the agent is not linked to this client. */
  val NoClientRelationship: String = "NoClientRelationship"
}

/** How long to wait between status checks while a client list download completes.
  *
  * Comes from the Client Exchange Proxy when a download is initiated, otherwise from config defaults.
  * All values are milliseconds.
  *
  * @param browserIntervalMs   one final wait after the business intervals are used up
  * @param businessIntervalsMs waits before each of the main status checks, in order
  */
case class AsynchronousProcessWaitTime(browserIntervalMs: Long, businessIntervalsMs: List[Long])
