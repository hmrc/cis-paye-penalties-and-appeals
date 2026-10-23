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

package uk.gov.hmrc.cispayepenaltiesandappeals.services

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.connectors.RdsDataCacheConnector
import uk.gov.hmrc.cispayepenaltiesandappeals.models.{CreateCisAppealRequest, CreateCisAppealResponse, GetAppealablePenaltiesRequest, GetAppealablePenaltiesResponse, GetAppealablePenaltyResponse, GetPenaltyAllowedRequest, GetPenaltyAllowedResponse}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.Future

final class PayeServiceSpec extends SpecBase {

  "PayeService getPayeAppealablePenalties" - {

    def testRequest(hmrcOfficeNum: String = "123", schemeRefNum: String = "AB456") = GetAppealablePenaltiesRequest(
      hmrcOfficeNumber = hmrcOfficeNum,
      schemeReferenceNumber = schemeRefNum,
      startRow = 1L,
      endRow = 10L
    )

    val testResponse = GetAppealablePenaltiesResponse(
      total = 2,
      appealablePenalties = List(
        GetAppealablePenaltyResponse(
          uniqueId = Some(10001),
          issueDate = Some(LocalDate.of(2023, 1, 1)),
          taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
          penaltyType = Some("PAYE3"),
          penaltyAmount = Some(BigDecimal(999.23))
        ),
        GetAppealablePenaltyResponse(
          uniqueId = Some(10002),
          issueDate = Some(LocalDate.of(2023, 1, 1)),
          taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
          penaltyType = Some("PAYE03"),
          penaltyAmount = Some(BigDecimal(999.23))
        )
      )
    )

    "must delegate to connector (happy path)" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new PayeService(connector)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getPayeAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier]))
        .thenReturn(Future.successful(testResponse))

      val result: GetAppealablePenaltiesResponse = service.getPayeAppealablePenalties(testRequest()).futureValue
      result mustBe testResponse

      verify(connector).getPayeAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new PayeService(connector)
      val boom = UpstreamErrorResponse("Service unavailable", 503)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getPayeAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.getPayeAppealablePenalties(testRequest()).failed.futureValue
      ex mustBe boom

      verify(connector).getPayeAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new PayeService(connector)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getPayeAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier]))
        .thenReturn(Future.successful(testResponse))

      service.getPayeAppealablePenalties(testRequest()).futureValue

      verify(connector, times(1)).getPayeAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new PayeService(connector)
      val request1 = testRequest()
      val request2 = testRequest("456", "CD789")
      val request3 = testRequest("789", "EF123")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getPayeAppealablePenalties(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(testResponse))
      when(connector.getPayeAppealablePenalties(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(testResponse))
      when(connector.getPayeAppealablePenalties(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(testResponse))

      service.getPayeAppealablePenalties(request1).futureValue mustBe testResponse
      service.getPayeAppealablePenalties(request2).futureValue mustBe testResponse
      service.getPayeAppealablePenalties(request3).futureValue mustBe testResponse

      verify(connector, times(3)).getPayeAppealablePenalties(any[GetAppealablePenaltiesRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new PayeService(connector)
      val boom = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getPayeAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.getPayeAppealablePenalties(testRequest()).failed.futureValue
      ex mustBe boom

      verify(connector).getPayeAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "getPayePenaltyAllowed" - {

    def getPenaltyAllowedRequest(employerReference: String = "hdz") =
      GetPenaltyAllowedRequest(
        districtNumber = 1L,
        employerReference = employerReference,
        uniqueId = 1L
      )

    val getPenaltyAllowedResponse = GetPenaltyAllowedResponse(
      allowed = true
    )

    "must delegate to connector (happy path)" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new PayeService(connector)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getPayePenaltyAllowed(eqTo(getPenaltyAllowedRequest()))(any[HeaderCarrier]))
        .thenReturn(Future.successful(getPenaltyAllowedResponse))

      val result: GetPenaltyAllowedResponse = service.getPayePenaltyAllowed(getPenaltyAllowedRequest()).futureValue
      result mustBe getPenaltyAllowedResponse

      verify(connector).getPayePenaltyAllowed(eqTo(getPenaltyAllowedRequest()))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate failures from connector" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new PayeService(connector)
      val boom = UpstreamErrorResponse("CIS taxpayer not found", 404)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getPayePenaltyAllowed(eqTo(getPenaltyAllowedRequest()))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.getPayePenaltyAllowed(getPenaltyAllowedRequest()).failed.futureValue
      ex mustBe boom

      verify(connector).getPayePenaltyAllowed(eqTo(getPenaltyAllowedRequest()))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must call connector exactly once per request" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new PayeService(connector)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getPayePenaltyAllowed(eqTo(getPenaltyAllowedRequest()))(any[HeaderCarrier]))
        .thenReturn(Future.successful(getPenaltyAllowedResponse))

      service.getPayePenaltyAllowed(getPenaltyAllowedRequest()).futureValue

      verify(connector, times(1)).getPayePenaltyAllowed(eqTo(getPenaltyAllowedRequest()))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must handle consecutive requests independently" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new CisService(connector)
      val request1 = getPenaltyAllowedRequest()
      val request2 = getPenaltyAllowedRequest("CD789")
      val request3 = getPenaltyAllowedRequest("EF012")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getCisPenaltyAllowed(eqTo(request1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(getPenaltyAllowedResponse))
      when(connector.getCisPenaltyAllowed(eqTo(request2))(any[HeaderCarrier]))
        .thenReturn(Future.successful(getPenaltyAllowedResponse))
      when(connector.getCisPenaltyAllowed(eqTo(request3))(any[HeaderCarrier]))
        .thenReturn(Future.successful(getPenaltyAllowedResponse))

      service.getCisPenaltyAllowed(request1).futureValue mustBe getPenaltyAllowedResponse
      service.getCisPenaltyAllowed(request2).futureValue mustBe getPenaltyAllowedResponse
      service.getCisPenaltyAllowed(request3).futureValue mustBe getPenaltyAllowedResponse

      verify(connector, times(3)).getPayePenaltyAllowed(any[GetPenaltyAllowedRequest])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "must propagate RuntimeException from connector" in {
      val connector = mock[RdsDataCacheConnector]
      val service = new PayeService(connector)
      val boom = new RuntimeException("Connection failed")
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(connector.getCisPenaltyAllowed(eqTo(getPenaltyAllowedRequest()))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex: Throwable = service.getPayePenaltyAllowed(getPenaltyAllowedRequest()).failed.futureValue
      ex mustBe boom

      verify(connector).getCisPenaltyAllowed(eqTo(getPenaltyAllowedRequest()))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

}
