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
import play.api.Logging
import uk.gov.hmrc.cispayepenaltiesandappeals.config.AppConfig
import uk.gov.hmrc.cispayepenaltiesandappeals.models.AgentAuthorisationResult.*
import uk.gov.hmrc.cispayepenaltiesandappeals.models.ClientListStatus.*
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future, Promise}

final case class NoBusinessIntervalsException(msg: String) extends RuntimeException(msg)

@Singleton
class AgentAuthorisationService @Inject()(
                                           clientListService: ClientListService,
                                           clientListDownloadService: ClientListDownloadService,
                                           cisService: CisService,
                                           appConfig: AppConfig,
                                           actorSystem: ActorSystem
                                         )(implicit ec: ExecutionContext) extends Logging {

  private val CisServiceId = "CIS"

  private val waitTimeCache: TrieMap[(String, String), AsynchronousProcessWaitTime] = TrieMap.empty

  private def defaultWaitPlan: AsynchronousProcessWaitTime =
    AsynchronousProcessWaitTime(
      browserIntervalMs = appConfig.defaultBrowserIntervalMs,
      businessIntervalsMs = appConfig.defaultBusinessIntervalsMs.toList
    )

  protected def sleep(ms: Long): Future[Unit] = {
    val promise = Promise[Unit]()
    actorSystem.scheduler.scheduleOnce(ms.millis)(promise.success(()))
    promise.future
  }

  def authoriseCisAgent(irAgentId: String, credentialId: String, request: AgentAuthoriseRequest)
                       (implicit hc: HeaderCarrier): Future[AgentAuthorisationResult] =
    process(CisServiceId, credentialId, irAgentId).flatMap {
      case Succeeded =>
        cisService
          .hasCisClient(
            HasCisClientRequest(
              agentDetails = AgentDetails(credentialId = credentialId, irAgentId = irAgentId),
              contractorDetails = ContractorDetails(request.hmrcOfficeNumber, request.schemeReferenceNumber)
            )
          )
          .map { response =>
            if response.hasClient then Authorised else NotAuthorised(NoClientRelationship)
          }
      case other     =>
        logger.warn(s"[AgentAuthorisationService][authoriseCisAgent] - client list not available: ${other.asString}")
        Future.successful(NotAuthorised(other.asString))
    }

  def process(serviceId: String, credentialId: String, agentId: String)
             (implicit hc: HeaderCarrier): Future[ClientListStatus] = {
    val key = (serviceId, credentialId)
    getStatus(serviceId, credentialId)
      .flatMap {
        case Succeeded        => Future.successful(Succeeded)
        case Failed           => Future.successful(Failed)
        case InProgress       => poll(serviceId, credentialId, waitTimeCache.getOrElse(key, defaultWaitPlan))
        case InitiateDownload =>
          initiate(serviceId, credentialId, agentId).flatMap { waitPlan =>
            waitTimeCache.update(key, waitPlan)
            poll(serviceId, credentialId, waitPlan)
          }
      }
      .map { status =>
        waitTimeCache.remove(key)
        status
      }
  }

  private def getStatus(serviceId: String, credentialId: String)
                       (implicit hc: HeaderCarrier): Future[ClientListStatus] =
    clientListService
      .getClientListDownloadStatus(ClientListStatusRequest(credentialId, serviceId))
      .flatMap { response =>
        ClientListStatus.fromString(response.status) match {
          case Some(status) => Future.successful(status)
          case None         => Future.failed(IllegalStateException(s"Unrecognised client list status: ${response.status}"))
        }
      }

  private def initiate(serviceId: String, credentialId: String, agentId: String)
                      (implicit hc: HeaderCarrier): Future[AsynchronousProcessWaitTime] =
    clientListDownloadService
      .initiateClientListDownload(InitiateClientListDownloadRequest(serviceId, credentialId, agentId))
      .map { response =>
        AsynchronousProcessWaitTime(
          browserIntervalMs = response.browserInterval.toLong,
          businessIntervalsMs = response.businessServiceIntervals.map(_.toLong).toList
        )
      }

  private def poll(serviceId: String, credentialId: String, waitPlan: AsynchronousProcessWaitTime)
                  (implicit hc: HeaderCarrier): Future[ClientListStatus] = {
    val business = waitPlan.businessIntervalsMs

    def browserPhase(): Future[ClientListStatus] =
      sleep(waitPlan.browserIntervalMs).flatMap(_ => getStatus(serviceId, credentialId))

    def businessPhase(index: Int): Future[ClientListStatus] =
      if index >= business.length then browserPhase()
      else
        sleep(business(index))
          .flatMap(_ => getStatus(serviceId, credentialId))
          .flatMap {
            case Succeeded        => Future.successful(Succeeded)
            case Failed           => Future.successful(Failed)
            case InProgress       => businessPhase(index + 1)
            case InitiateDownload =>
              if index < business.length - 1 then businessPhase(index + 1)
              else Future.successful(InitiateDownload)
          }

    if business.isEmpty then {
      waitTimeCache.remove((serviceId, credentialId))
      Future.failed(NoBusinessIntervalsException("No business intervals"))
    } else businessPhase(0)
  }
}
