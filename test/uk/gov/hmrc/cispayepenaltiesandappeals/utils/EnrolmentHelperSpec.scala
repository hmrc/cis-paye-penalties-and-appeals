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

import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase

class EnrolmentHelperSpec extends SpecBase {

  private def agentEnrolment(ref: String, state: String = "Activated") =
    Enrolment("IR-PAYE-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", ref)), state)

  "EnrolmentHelper.irAgentReference" - {
    "must return the reference for an activated IR-PAYE-AGENT enrolment" in {
      EnrolmentHelper.irAgentReference(Enrolments(Set(agentEnrolment("123456")))) mustBe Some("123456")
    }
    "must return None when the enrolment is not activated" in {
      EnrolmentHelper.irAgentReference(Enrolments(Set(agentEnrolment("123456", "Inactive")))) mustBe None
    }
    "must return None when the reference is invalid" in {
      EnrolmentHelper.irAgentReference(Enrolments(Set(agentEnrolment("12")))) mustBe None
    }
    "must return None when the identifier is missing" in {
      EnrolmentHelper.irAgentReference(
        Enrolments(Set(Enrolment("IR-PAYE-AGENT", Seq(EnrolmentIdentifier("Other", "123456")), "Activated")))
      ) mustBe None
    }
    "must return None when there is no IR-PAYE-AGENT enrolment" in {
      EnrolmentHelper.irAgentReference(Enrolments(Set.empty)) mustBe None
    }
  }

  "EnrolmentHelper.isValidTaxOfficeNumber" - {
    "must accept exactly three digits" in {
      EnrolmentHelper.isValidTaxOfficeNumber("123") mustBe true
    }
    "must reject other values" in {
      Seq("12", "1234", "abc", "", null).foreach(v => EnrolmentHelper.isValidTaxOfficeNumber(v) mustBe false)
    }
  }

  "EnrolmentHelper.isValidAgentReference" - {
    "must accept six alphanumeric characters" in {
      EnrolmentHelper.isValidAgentReference("AA1216") mustBe true
    }
    "must reject other values" in {
      Seq("AA121", "AA12167", "AA-216", "", null).foreach(v => EnrolmentHelper.isValidAgentReference(v) mustBe false)
    }
  }
}
