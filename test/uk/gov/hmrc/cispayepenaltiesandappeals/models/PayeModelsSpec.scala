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

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase

class PayeModelsSpec extends SpecBase {

  "CreatePayeAppealRequest" - {

    "must serialize to JSON correctly" in {
      val json = Json.toJson(CreatePayeAppealRequest(penaltyId = 1L))

      (json \ "penaltyId").as[Long] mustBe 1L
    }

    "must deserialize from JSON correctly" in {
      val result = Json.obj("penaltyId" -> 1L).validate[CreatePayeAppealRequest]

      result mustBe a[JsSuccess[_]]
      result.get.penaltyId mustBe 1L
    }

    "must fail to deserialize when penaltyId is missing" in {
      Json.obj().validate[CreatePayeAppealRequest].isError mustBe true
    }
  }

  "CreatePayeAppealResponse" - {

    "must serialize to JSON correctly" in {
      (Json.toJson(CreatePayeAppealResponse(created = true)) \ "created").as[Boolean] mustBe true
      (Json.toJson(CreatePayeAppealResponse(created = false)) \ "created").as[Boolean] mustBe false
    }

    "must deserialize from JSON correctly" in {
      val result = Json.obj("created" -> true).validate[CreatePayeAppealResponse]

      result mustBe a[JsSuccess[_]]
      result.get.created mustBe true
    }

    "must fail to deserialize when created is missing" in {
      Json.obj().validate[CreatePayeAppealResponse].isError mustBe true
    }
  }
}
