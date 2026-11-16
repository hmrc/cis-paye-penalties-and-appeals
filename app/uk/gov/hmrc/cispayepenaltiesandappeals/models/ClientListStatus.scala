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

/** Client list download status held in RDS for an agent credential.
  *
  * `asString` must match the values RDS returns exactly (`ClientListStatusResponse.status`).
  */
sealed trait ClientListStatus {
  val asString: String
}

object ClientListStatus {
  /** No usable download exists: it must be initiated (or re-initiated once the grace period has passed). */
  case object InitiateDownload extends ClientListStatus { val asString = "InitiateDownload" }
  /** A download has been requested and is still being prepared. Poll again. */
  case object InProgress       extends ClientListStatus { val asString = "InProgress" }
  /** The agent's client list is ready. */
  case object Succeeded        extends ClientListStatus { val asString = "Succeeded" }
  /** The download failed. The agent cannot be authorised. */
  case object Failed           extends ClientListStatus { val asString = "Failed" }

  /** Maps the RDS status text to a status, or `None` if the value is not recognised. */
  def fromString(value: String): Option[ClientListStatus] =
    value match {
      case InitiateDownload.asString => Some(InitiateDownload)
      case InProgress.asString       => Some(InProgress)
      case Succeeded.asString        => Some(Succeeded)
      case Failed.asString           => Some(Failed)
      case _                         => None
    }
}
