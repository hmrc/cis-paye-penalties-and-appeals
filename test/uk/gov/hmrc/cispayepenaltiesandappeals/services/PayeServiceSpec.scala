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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.connectors.RdsDataCacheConnector
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class PayeServiceSpec extends SpecBase {

  "PayeService" - {

    "createPayeAppeal" - {

      def createPayeAppealRequest(penaltyId: Long = 1L) = CreatePayeAppealRequest(penaltyId = penaltyId)

      val createPayeAppealResponse = CreatePayeAppealResponse(created = true)

      "must delegate to connector (happy path)" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new PayeService(connector)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.createPayeAppeal(eqTo(createPayeAppealRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.successful(createPayeAppealResponse))

        val result: CreatePayeAppealResponse = service.createPayeAppeal(createPayeAppealRequest()).futureValue
        result mustBe createPayeAppealResponse

        verify(connector).createPayeAppeal(eqTo(createPayeAppealRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate failures from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new PayeService(connector)
        val boom = UpstreamErrorResponse("Service unavailable", 503)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.createPayeAppeal(eqTo(createPayeAppealRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.createPayeAppeal(createPayeAppealRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).createPayeAppeal(eqTo(createPayeAppealRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }
    }
  }
}
