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

package uk.gov.hmrc.cispayepenaltiesandappeals.services

import uk.gov.hmrc.cispayepenaltiesandappeals.connectors.RdsDataCacheConnector
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CisService @Inject()(rdsDataCacheConnector: RdsDataCacheConnector) {

  def createCisAppeal(createCisAppealRequest: CreateCisAppealRequest)
                     (implicit hc: HeaderCarrier): Future[CreateCisAppealResponse] =
    rdsDataCacheConnector.createCisAppeal(createCisAppealRequest)

  def getCisAppealablePenalties(request: GetAppealablePenaltiesRequest)
                     (implicit hc: HeaderCarrier): Future[GetAppealablePenaltiesResponse] =
    rdsDataCacheConnector.getCisAppealablePenalties(request)

  def getCisAppealablePenalty(request: GetAppealablePenaltyRequest)
                             (implicit hc: HeaderCarrier): Future[GetAppealablePenaltyResponse] =
    rdsDataCacheConnector.getCisAppealablePenalty(request)

  def getCisTaxpayer(request: GetCisTaxpayerRequest)
                    (implicit hc: HeaderCarrier): Future[GetCisTaxpayerResponse] =
    rdsDataCacheConnector.getCisTaxpayer(request)

  def getCisPenaltyAllowed(request: GetPenaltyAllowedRequest)
                             (implicit hc: HeaderCarrier): Future[GetPenaltyAllowedResponse] =
    rdsDataCacheConnector.getCisPenaltyAllowed(request)
}
