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

import play.api.libs.json.{Json, OFormat}

/**
 * Request to start a client list download for an agent.
 *
 * @param serviceId    the service the client list is for, either CIS or PAYE
 * @param credentialId the government gateway credential id of the agent user
 * @param agentId    the inland revenue id of the agent
 */
case class InitiateClientListDownloadRequest(
                                              serviceId: String,
                                              credentialId: String,
                                              agentId: String
                                            )

object InitiateClientListDownloadRequest {
  implicit val format: OFormat[InitiateClientListDownloadRequest] = Json.format[InitiateClientListDownloadRequest]
}

/**
 * The wait times returned by the Client Exchange Proxy once a download has been started.
 *
 * @param browserInterval          milliseconds to wait, after all the business service intervals are used up,
 *                                 before polling the download status one last time
 * @param businessServiceIntervals milliseconds to wait, in order, before each poll of the download status
 */
case class InitiateClientListDownloadResponse(
                                               browserInterval: Int,
                                               businessServiceIntervals: Seq[Int]
                                             )

object InitiateClientListDownloadResponse {
  implicit val format: OFormat[InitiateClientListDownloadResponse] = Json.format[InitiateClientListDownloadResponse]
}
