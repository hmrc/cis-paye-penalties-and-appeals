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
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.cispayepenaltiesandappeals.models.AgentAuthorisationResult.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AgentAuthorisationServiceSpec extends SpecBase {

  private val credId = "cred-1"
  private val agentId = "123456"
  private val request = AgentAuthoriseRequest("123", "AB456")
  private def statusReq(service: String) = ClientListStatusRequest(credId, service.toUpperCase)
  private val hasCisClientReq =
    HasCisClientRequest(AgentDetails(credId, agentId), ContractorDetails("123", "AB456"))
  private val hasPayeClientReq =
    HasPayeClientRequest(AgentDetails(credId, agentId), EmployerDetails("123", "AB456"))

  private trait Setup {
    val statusService: ClientListService = mock[ClientListService]
    val downloadService: ClientListDownloadService = mock[ClientListDownloadService]
    val appConfig: AppConfig = mock[AppConfig]
    val sleeps = scala.collection.mutable.ListBuffer.empty[Long]

    when(appConfig.defaultBrowserIntervalMs).thenReturn(1000L)
    when(appConfig.defaultBusinessIntervalsMs).thenReturn(Seq(1000L, 5000L))

    val service: AgentAuthorisationService =
      new AgentAuthorisationService(statusService, downloadService, appConfig, mock[ActorSystem]) {
        override protected def sleep(ms: Long): Future[Unit] = { sleeps += ms; Future.unit }
      }

    def statuses(service: String, first: String, rest: String*): Unit = {
      val responses = (first +: rest).map(v => Future.successful(ClientListStatusResponse(v)))
      when(statusService.getClientListDownloadStatus(eqTo(statusReq(service)))(any[HeaderCarrier]))
        .thenReturn(responses.head, responses.tail*)
    }

    def hasCisClient(value: Boolean): Unit =
      when(statusService.hasCisClient(eqTo(hasCisClientReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(HasClientResponse(value)))

    def hasPayeClient(value: Boolean): Unit =
      when(statusService.hasPayeClient(eqTo(hasPayeClientReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(HasClientResponse(value)))
  }

  "AgentAuthorisationService.authoriseCisAgent" - {

    "must authorise when the status is Succeeded and the agent has the client" in new Setup {
      statuses("CIS", "Succeeded"); hasCisClient(true)
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps mustBe empty
    }

    "must not authorise when the status is Succeeded but the agent has no client" in new Setup {
      statuses("CIS", "Succeeded"); hasCisClient(false)
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised(NoClientRelationship)
    }

    "must not authorise and skip has-client when the status is Failed" in new Setup {
      statuses("CIS", "Failed")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("Failed")
      verify(statusService, never()).hasCisClient(any[HasCisClientRequest])(any[HeaderCarrier])
    }

    "must poll with the default wait plan when InProgress and then authorise on Succeeded" in new Setup {
      statuses("CIS", "InProgress", "InProgress", "Succeeded"); hasCisClient(true)
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps.toList mustBe List(1000L, 5000L)
    }

    "must run the browser phase and not authorise when still InProgress" in new Setup {
      statuses("CIS", "InProgress", "InProgress", "InProgress", "InProgress")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("InProgress")
      sleeps.toList mustBe List(1000L, 5000L, 1000L)
      verify(statusService, never()).hasCisClient(any[HasCisClientRequest])(any[HeaderCarrier])
    }

    "must stop polling on Failed during the business phase" in new Setup {
      statuses("CIS", "InProgress", "Failed")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("Failed")
      sleeps.toList mustBe List(1000L)
    }

    "must initiate the download, use the proxy wait plan and authorise on Succeeded" in new Setup {
      statuses("CIS", "InitiateDownload", "Succeeded"); hasCisClient(true)
      when(downloadService.initiateClientListDownload(eqTo(InitiateClientListDownloadRequest("CIS", credId, agentId)))(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(2000, Seq(3000, 4000))))
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps.toList mustBe List(3000L)
    }

    "must continue past InitiateDownload on a non-final business interval" in new Setup {
      statuses("CIS", "InitiateDownload", "InitiateDownload", "Succeeded"); hasCisClient(true)
      when(downloadService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(1000, Seq(10, 20))))
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps.toList mustBe List(10L, 20L)
    }

    "must not authorise when InitiateDownload is returned on the final business interval" in new Setup {
      statuses("CIS", "InitiateDownload", "InitiateDownload")
      when(downloadService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(1000, Seq(10))))
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("InitiateDownload")
    }

    "must not authorise when InitiateDownload is returned after the browser interval" in new Setup {
      statuses("CIS","InProgress", "InProgress", "InProgress", "InitiateDownload")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("InitiateDownload")
    }

    "must not authorise when Failed is returned after the browser interval" in new Setup {
      statuses("CIS", "InProgress", "InProgress", "InProgress", "Failed")
      service.authoriseCisAgent(agentId, credId, request).futureValue mustBe NotAuthorised("Failed")
    }

    "must fail when the proxy returns no business intervals" in new Setup {
      statuses("CIS", "InitiateDownload")
      when(downloadService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(1000, Seq.empty)))
      service.authoriseCisAgent(agentId, credId, request).failed.futureValue mustBe a[NoBusinessIntervalsException]
    }

    "must fail when the status is not recognised" in new Setup {
      statuses("CIS", "Banana")
      service.authoriseCisAgent(agentId, credId, request).failed.futureValue mustBe a[IllegalStateException]
    }

    "must propagate a failure from the status call" in new Setup {
      val boom = new RuntimeException("boom")
      when(statusService.getClientListDownloadStatus(eqTo(statusReq("CIS")))(any[HeaderCarrier])).thenReturn(Future.failed(boom))
      service.authoriseCisAgent(agentId, credId, request).failed.futureValue mustBe boom
    }

    "must propagate a failure from has-client" in new Setup {
      statuses("CIS", "Succeeded")
      val boom = new RuntimeException("rds down")
      when(statusService.hasCisClient(any[HasCisClientRequest])(any[HeaderCarrier])).thenReturn(Future.failed(boom))
      service.authoriseCisAgent(agentId, credId, request).failed.futureValue mustBe boom
    }
  }

  "AgentAuthorisationService.authorisePayeAgent" - {

    "must authorise when the status is Succeeded and the agent has the client" in new Setup {
      statuses("PAYE", "Succeeded"); hasPayeClient(true)
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps mustBe empty
    }

    "must not authorise when the status is Succeeded but the agent has no client" in new Setup {
      statuses("PAYE", "Succeeded"); hasPayeClient(false)
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe NotAuthorised(NoClientRelationship)
    }

    "must not authorise and skip has-client when the status is Failed" in new Setup {
      statuses("PAYE", "Failed")
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe NotAuthorised("Failed")
      verify(statusService, never()).hasPayeClient(any[HasPayeClientRequest])(any[HeaderCarrier])
    }

    "must poll with the default wait plan when InProgress and then authorise on Succeeded" in new Setup {
      statuses("PAYE", "InProgress", "InProgress", "Succeeded"); hasPayeClient(true)
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps.toList mustBe List(1000L, 5000L)
    }

    "must run the browser phase and not authorise when still InProgress" in new Setup {
      statuses("PAYE", "InProgress", "InProgress", "InProgress", "InProgress")
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe NotAuthorised("InProgress")
      sleeps.toList mustBe List(1000L, 5000L, 1000L)
      verify(statusService, never()).hasPayeClient(any[HasPayeClientRequest])(any[HeaderCarrier])
    }

    "must stop polling on Failed during the business phase" in new Setup {
      statuses("PAYE", "InProgress", "Failed")
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe NotAuthorised("Failed")
      sleeps.toList mustBe List(1000L)
    }

    "must initiate the download, use the proxy wait plan and authorise on Succeeded" in new Setup {
      statuses("PAYE", "InitiateDownload", "Succeeded"); hasPayeClient(true)
      when(downloadService.initiateClientListDownload(eqTo(InitiateClientListDownloadRequest("PAYE", credId, agentId)))(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(2000, Seq(3000, 4000))))
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps.toList mustBe List(3000L)
    }

    "must continue past InitiateDownload on a non-final business interval" in new Setup {
      statuses("PAYE", "InitiateDownload", "InitiateDownload", "Succeeded"); hasPayeClient(true)
      when(downloadService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(1000, Seq(10, 20))))
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe Authorised
      sleeps.toList mustBe List(10L, 20L)
    }

    "must not authorise when InitiateDownload is returned on the final business interval" in new Setup {
      statuses("PAYE", "InitiateDownload", "InitiateDownload")
      when(downloadService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(1000, Seq(10))))
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe NotAuthorised("InitiateDownload")
    }

    "must not authorise when InitiateDownload is returned after the browser interval" in new Setup {
      statuses("PAYE", "InProgress", "InProgress", "InProgress", "InitiateDownload")
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe NotAuthorised("InitiateDownload")
    }

    "must not authorise when Failed is returned after the browser interval" in new Setup {
      statuses("PAYE", "InProgress", "InProgress", "InProgress", "Failed")
      service.authorisePayeAgent(agentId, credId, request).futureValue mustBe NotAuthorised("Failed")
    }

    "must fail when the proxy returns no business intervals" in new Setup {
      statuses("PAYE", "InitiateDownload")
      when(downloadService.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateClientListDownloadResponse(1000, Seq.empty)))
      service.authorisePayeAgent(agentId, credId, request).failed.futureValue mustBe a[NoBusinessIntervalsException]
    }

    "must fail when the status is not recognised" in new Setup {
      statuses("PAYE", "Banana")
      service.authorisePayeAgent(agentId, credId, request).failed.futureValue mustBe a[IllegalStateException]
    }

    "must propagate a failure from the status call" in new Setup {
      val boom = new RuntimeException("boom")
      when(statusService.getClientListDownloadStatus(eqTo(statusReq("PAYE")))(any[HeaderCarrier])).thenReturn(Future.failed(boom))
      service.authorisePayeAgent(agentId, credId, request).failed.futureValue mustBe boom
    }

    "must propagate a failure from has-client" in new Setup {
      statuses("PAYE", "Succeeded")
      val boom = new RuntimeException("rds down")
      when(statusService.hasPayeClient(any[HasPayeClientRequest])(any[HeaderCarrier])).thenReturn(Future.failed(boom))
      service.authorisePayeAgent(agentId, credId, request).failed.futureValue mustBe boom
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
    extends AgentAuthorisationService(mock[ClientListService], mock[ClientListDownloadService], mock[AppConfig], system) {
    def callSleep(ms: Long): Future[Unit] = sleep(ms)
  }
}
