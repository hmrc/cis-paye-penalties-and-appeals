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

package uk.gov.hmrc.cispayepenaltiesandappeals.models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}

class ClientListDownloadModelsSpec extends AnyWordSpec with Matchers {

  "InitiateClientListDownloadRequest" should {

    "deserialize from JSON" in {
      val json = Json.obj("serviceId" -> "CIS", "credentialId" -> "cred-1", "agentId" -> "agent-1")

      json.validate[InitiateClientListDownloadRequest] mustBe
        JsSuccess(InitiateClientListDownloadRequest("CIS", "cred-1", "agent-1"))
    }

    "fail to deserialize when a field is missing" in {
      Json.obj("serviceId" -> "CIS").validate[InitiateClientListDownloadRequest].isError mustBe true
    }
  }

  "InitiateClientListDownloadResponse" should {

    "serialize to JSON" in {
      val json = Json.toJson(InitiateClientListDownloadResponse(8000, Seq(1000, 2000)))

      (json \ "browserInterval").as[Int] mustBe 8000
      (json \ "businessServiceIntervals").as[Seq[Int]] mustBe Seq(1000, 2000)
    }

    "round trip through JSON" in {
      val model = InitiateClientListDownloadResponse(500, Seq.empty)
      Json.toJson(model).validate[InitiateClientListDownloadResponse] mustBe JsSuccess(model)
    }
  }
}
