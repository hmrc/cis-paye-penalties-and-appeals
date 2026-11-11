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

import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase

class ClientListStatusSpec extends SpecBase {

  "ClientListStatus.fromString" - {
    "must map every known value" in {
      ClientListStatus.fromString("InitiateDownload") mustBe Some(ClientListStatus.InitiateDownload)
      ClientListStatus.fromString("InProgress") mustBe Some(ClientListStatus.InProgress)
      ClientListStatus.fromString("Succeeded") mustBe Some(ClientListStatus.Succeeded)
      ClientListStatus.fromString("Failed") mustBe Some(ClientListStatus.Failed)
    }
    "must return None for an unknown value" in {
      ClientListStatus.fromString("Unknown") mustBe None
    }
    "must round trip through asString" in {
      Seq(ClientListStatus.InitiateDownload, ClientListStatus.InProgress, ClientListStatus.Succeeded, ClientListStatus.Failed)
        .foreach(s => ClientListStatus.fromString(s.asString) mustBe Some(s))
    }
  }
}
