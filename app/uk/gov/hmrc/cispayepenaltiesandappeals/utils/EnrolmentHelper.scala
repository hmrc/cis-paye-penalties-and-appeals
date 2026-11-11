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

object EnrolmentHelper {

  val IrPayeAgentKey: String         = "IR-PAYE-AGENT"
  val IrAgentReferenceKey: String    = "IRAgentReference"

  def irAgentReference(enrolments: Enrolments): Option[String] =
    enrolments
      .getEnrolment(IrPayeAgentKey)
      .filter(_.isActivated)
      .flatMap(_.getIdentifier(IrAgentReferenceKey))
      .map(_.value)
      .filter(isValidAgentReference)

  def isValidTaxOfficeNumber(taxOfficeNumber: String): Boolean =
    taxOfficeNumber != null && taxOfficeNumber.matches("[0-9]{3}")

  def isValidAgentReference(agentReference: String): Boolean =
    agentReference != null && agentReference.matches("[A-Za-z0-9]{6}")
}
