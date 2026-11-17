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

package uk.gov.hmrc.cispayepenaltiesandappeals.utils

import uk.gov.hmrc.auth.core.Enrolments

/** Enrolment lookups and format checks shared by the authorisation guards (F4 now; F5-F7 reuse it). */
object EnrolmentHelper {

  val IrPayeAgentKey: String         = "IR-PAYE-AGENT"
  val IrAgentReferenceKey: String    = "IRAgentReference"

  /** Returns the agent reference only when the user holds an *activated* `IR-PAYE-AGENT` enrolment whose
    * `IRAgentReference` is a valid 6 character alphanumeric value. Anything else returns `None` (treated as not authorised).
    */
  def irAgentReference(enrolments: Enrolments): Option[String] =
    enrolments
      .getEnrolment(IrPayeAgentKey)
      .filter(_.isActivated)
      .flatMap(_.getIdentifier(IrAgentReferenceKey))
      .map(_.value)
      .filter(isValidAgentReference)

  /** A tax office number (HMRC office number) is exactly three digits. */
  def isValidTaxOfficeNumber(taxOfficeNumber: String): Boolean =
    taxOfficeNumber != null && taxOfficeNumber.matches("[0-9]{3}")

  /** An IR agent reference is exactly six letters or digits. */
  def isValidAgentReference(agentReference: String): Boolean =
    agentReference != null && agentReference.matches("[A-Za-z0-9]{6}")
}
