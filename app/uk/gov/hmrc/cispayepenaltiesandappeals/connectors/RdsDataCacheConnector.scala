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

package uk.gov.hmrc.cispayepenaltiesandappeals.connectors


import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RdsDataCacheConnector @Inject()(http: HttpClientV2,
                                      config: ServicesConfig)
                                     (implicit ec: ExecutionContext) extends Logging {

  private val rdsDatacachePath = config.baseUrl("rds-datacache-proxy") + "/rds-datacache-proxy/pas"

  def createCisAppeal(createCisAppealRequest: CreateCisAppealRequest)(implicit hc: HeaderCarrier): Future[CreateCisAppealResponse] =
    val url: URL = url"$rdsDatacachePath/cis/appeal"
    http.post(url)
      .withBody(Json.toJson(createCisAppealRequest))
      .execute[CreateCisAppealResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][createCisAppeal]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][createCisAppeal]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getCisAppealablePenalty(request: GetCisAppealablePenaltyRequest)(implicit hc: HeaderCarrier): Future[GetCisAppealablePenaltyResponse] =
    val url: URL = url"$rdsDatacachePath/cis/penalty"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[GetCisAppealablePenaltyResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][getCisAppealablePenalty]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][getCisAppealablePenalty]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }
}
