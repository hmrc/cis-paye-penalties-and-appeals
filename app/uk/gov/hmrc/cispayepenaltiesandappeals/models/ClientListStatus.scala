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

sealed trait ClientListStatus {
  val asString: String
}

object ClientListStatus {
  case object InitiateDownload extends ClientListStatus { val asString = "InitiateDownload" }
  case object InProgress       extends ClientListStatus { val asString = "InProgress" }
  case object Succeeded        extends ClientListStatus { val asString = "Succeeded" }
  case object Failed           extends ClientListStatus { val asString = "Failed" }

  def fromString(value: String): Option[ClientListStatus] =
    value match {
      case InitiateDownload.asString => Some(InitiateDownload)
      case InProgress.asString       => Some(InProgress)
      case Succeeded.asString        => Some(Succeeded)
      case Failed.asString           => Some(Failed)
      case _                         => None
    }
}
