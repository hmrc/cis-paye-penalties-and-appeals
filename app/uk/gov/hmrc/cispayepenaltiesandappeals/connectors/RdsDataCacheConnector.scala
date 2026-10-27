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
  private val rdsDatacacheCisTaxpayerPath = config.baseUrl("rds-datacache-proxy") + "/rds-datacache-proxy/cis-taxpayer"

  // CIS
  def createCisAppeal(createCisAppealRequest: CreateAppealRequest)(implicit hc: HeaderCarrier): Future[CreateAppealResponse] =
    val url: URL = url"$rdsDatacachePath/cis/appeal"
    http.post(url)
      .withBody(Json.toJson(createCisAppealRequest))
      .execute[CreateAppealResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][createCisAppeal]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][createCisAppeal]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getCisAppealablePenalties(request: GetAppealablePenaltiesRequest)(implicit hc: HeaderCarrier): Future[GetAppealablePenaltiesResponse] =
    val url: URL = url"$rdsDatacachePath/cis/penalties"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[GetAppealablePenaltiesResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][getCisAppealablePenalties]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][getCisAppealablePenalties]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getCisAppealablePenalty(request: GetAppealablePenaltyRequest)(implicit hc: HeaderCarrier): Future[GetAppealablePenaltyResponse] =
    val url: URL = url"$rdsDatacachePath/cis/penalty"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[GetAppealablePenaltyResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][getCisAppealablePenalty]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][getCisAppealablePenalty]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getCisTaxpayer(request: GetTaxpayerRequest)(implicit hc: HeaderCarrier): Future[GetCisTaxpayerResponse] =
    val url: URL = url"$rdsDatacacheCisTaxpayerPath"
    val proxyRequest = Json.obj(
      "taxOfficeNumber"    -> request.hmrcOfficeNumber,
      "taxOfficeReference" -> request.schemeReferenceNumber
    )
    http.post(url)
      .withBody(proxyRequest)
      .execute[GetCisTaxpayerResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][getCisTaxpayer]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][getCisTaxpayer]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getCisPenaltyAllowed(request: GetPenaltyAllowedRequest)(implicit hc: HeaderCarrier): Future[GetPenaltyAllowedResponse] =
    val url: URL = url"$rdsDatacachePath/cis/penalties/access-allowed"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[GetPenaltyAllowedResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][getCisPenaltyAllowed]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][getCisPenaltyAllowed]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  // PAYE
  def createPayeAppeal(createAppealRequest: CreateAppealRequest)(implicit hc: HeaderCarrier): Future[CreateAppealResponse] =
    val url: URL = url"$rdsDatacachePath/paye/appeal"
    http.post(url)
      .withBody(Json.toJson(createAppealRequest))
      .execute[CreateAppealResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][createPayeAppeal]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][createPayeAppeal]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getPayeAppealablePenalties(request: GetAppealablePenaltiesRequest)(implicit hc: HeaderCarrier): Future[GetAppealablePenaltiesResponse] =
    val url: URL = url"$rdsDatacachePath/paye/penalties"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[GetAppealablePenaltiesResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][getPayeAppealablePenalties]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][getPayeAppealablePenalties]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getPayeAppealablePenalty(request: GetAppealablePenaltyRequest)(implicit hc: HeaderCarrier): Future[GetAppealablePenaltyResponse] =
    val url: URL = url"$rdsDatacachePath/paye/penalty"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[GetAppealablePenaltyResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][getPayeAppealablePenalty]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][getPayeAppealablePenalty]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getPayeTaxpayer(request: GetTaxpayerRequest)(implicit hc: HeaderCarrier): Future[GetPayeTaxpayerResponse] =
    val url: URL = url"$rdsDatacachePath/paye/taxpayer"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[GetPayeTaxpayerResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][getPayeTaxpayer]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][getPayeTaxpayer]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  def getPayePenaltyAllowed(request: GetPenaltyAllowedRequest)(implicit hc: HeaderCarrier): Future[GetPenaltyAllowedResponse] =
    val url: URL = url"$rdsDatacachePath/paye/penalties/access-allowed"
    http.post(url)
      .withBody(Json.toJson(request))
      .execute[GetPenaltyAllowedResponse]
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[RdsDataCacheConnector][getPayePenaltyAllowed]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[RdsDataCacheConnector][getPayePenaltyAllowed]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }
}
