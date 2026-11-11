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

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.config.AppConfig
import uk.gov.hmrc.cispayepenaltiesandappeals.models.AgentAuthorisationResult.*
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AgentAuthorisationServiceSpec extends SpecBase {

  private val credId = "cred-1"
  private val agentId = "123456"
  private val request = AgentAuthoriseRequest("123", "AB456")
  private val statusReq = ClientListStatusRequest(credId, "CIS")
  private val hasClientReq =
    HasCisClientRequest(AgentDetails(credId, agentId), ContractorDetails("123", "AB456"))

  private trait Setup {
    val statusService: ClientListService = mock[ClientListService]
    val downloadService: ClientListDownloadService = mock[ClientListDownloadService]
    val cisService: CisService = mock[CisService]
    val appConfig: AppConfig = mock[AppConfig]
    val sleeps = scala.collection.mutable.ListBuffer.empty[Long]

    when(appConfig.defaultBrowserIntervalMs).thenReturn(1000L)
    when(appConfig.defaultBusinessIntervalsMs).thenReturn(Seq(1000L, 5000L))

    val service: AgentAuthorisationService =
      new AgentAuthorisationService(statusService, downloadService, cisService, appConfig, mock[ActorSystem]) {
        override protected def sleep(ms: Long): Future[Unit] = { sleeps += ms; Future.unit }
      }

    def statuses(values: String*): Unit = {
      val responses = values.map(v => Future.successful(ClientListStatusResponse(v)))
      when(statusService.getClientListDownloadStatus(eqTo(statusReq))(any[HeaderCarrier]))
        .thenReturn(responses.head, responses.tail*)
    }

    def hasClient(value: Boolean): Unit =
      when(cisService.hasCisClient(eqTo(hasClientReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(HasClientResponse(value)))
  }

  "AgentAuthorisationService.authoriseCisAgent" - {

    "must authorise when the status is Succeeded and the agent has the client" in new Setup {
      statuses("Succeeded"); hasClient(true)
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps mustBe empty
    }

    "must not authorise when the status is Succeeded but the agent has no client" in new Setup {
      statuses("Succeeded"); hasClient(false)
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised(NoClientRelationship)
    }

    "must not authorise and skip has-client when the status is Failed" in new Setup {
      statuses("Failed")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("Failed")
      verifyNoInteractions(cisService)
    }

    "must poll with the default wait plan when InProgress and then authorise on Succeeded" in new Setup {
      statuses("InProgress", "InProgress", "Succeeded"); hasClient(true)
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps.toList mustBe List(1000L, 5000L)
    }

    "must run the browser phase and not authorise when still InProgress" in new Setup {
      statuses("InProgress", "InProgress", "InProgress", "InProgress")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("InProgress")
      sleeps.toList mustBe List(1000L, 5000L, 1000L)
      verifyNoInteractions(cisService)
    }

    "must stop polling on Failed during the business phase" in new Setup {
      statuses("InProgress", "Failed")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("Failed")
      sleeps.toList mustBe List(1000L)
    }

    "must initiate the download, use the proxy wait plan and authorise on Succeeded" in new Setup {
      statuses("InitiateDownload", "Succeeded"); hasClient(true)
      when(downloadService.initiateClientListDownload(eqTo(InitiateClientListDownloadRequest("CIS", credId, agentId)))(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(2000, Seq(3000, 4000))))
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps.toList mustBe List(3000L)
    }

    "must continue past InitiateDownload on a non-final business interval" in new Setup {
      statuses("InitiateDownload", "InitiateDownload", "Succeeded"); hasClient(true)
      when(downloadService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(1000, Seq(10, 20))))
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps.toList mustBe List(10L, 20L)
    }

    "must not authorise when InitiateDownload is returned on the final business interval" in new Setup {
      statuses("InitiateDownload", "InitiateDownload")
      when(downloadService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(1000, Seq(10))))
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("InitiateDownload")
    }

    "must not authorise when InitiateDownload is returned after the browser interval" in new Setup {
      statuses("InProgress", "InProgress", "InProgress", "InitiateDownload")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("InitiateDownload")
    }

    "must not authorise when Failed is returned after the browser interval" in new Setup {
      statuses("InProgress", "InProgress", "InProgress", "Failed")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("Failed")
    }

    "must fail when the proxy returns no business intervals" in new Setup {
      statuses("InitiateDownload")
      when(downloadService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(1000, Seq.empty)))
      service.authoriseCisAgent(agentId, credId, request).failed.futureValue mustBe a[NoBusinessIntervalsException]
    }

    "must fail when the status is not recognised" in new Setup {
      statuses("Banana")
      service.authoriseCisAgent(agentId, credId, request).failed.futureValue mustBe a[IllegalStateException]
    }

    "must propagate a failure from the status call" in new Setup {
      val boom = new RuntimeException("boom")
      when(statusService.getClientListDownloadStatus(eqTo(statusReq))(any[HeaderCarrier])).thenReturn(Future.failed(boom))
      service.authoriseCisAgent(agentId, credId, request).failed.futureValue mustBe boom
    }

    "must propagate a failure from has-client" in new Setup {
      statuses("Succeeded")
      val boom = new RuntimeException("rds down")
      when(cisService.hasCisClient(any[HasCisClientRequest])(any[HeaderCarrier])).thenReturn(Future.failed(boom))
      service.authoriseCisAgent(agentId, credId, request).failed.futureValue mustBe boom
    }
  }

  "AgentAuthorisationService.sleep" - {
    "must complete after the delay using the scheduler" in {
      val system = ActorSystem("sleep-test")
      try {
        val svc = new SleepProbe(system)
        svc.callSleep(10).futureValue mustBe (())
      } finally system.terminate()
    }
  }

  private class SleepProbe(system: ActorSystem)
    extends AgentAuthorisationService(mock[ClientListService], mock[ClientListDownloadService], mock[CisService], mock[AppConfig], system) {
    def callSleep(ms: Long): Future[Unit] = sleep(ms)
  }
}
