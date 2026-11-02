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

package uk.gov.hmrc.cispayepenaltiesandappeals.connectors

import play.api.Logging
import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.{Elem, XML}

@Singleton
class ClientExchangeProxyConnector @Inject()(http: HttpClientV2,
                                             config: ServicesConfig)
                                            (implicit ec: ExecutionContext) extends Logging {

  private val clientExchangeProxyPath = config.baseUrl("client-exchange-proxy") + "/client-exchange-proxy"

  def initiateClientListDownload(request: InitiateClientListDownloadRequest)
                                (implicit hc: HeaderCarrier): Future[InitiateClientListDownloadResponse] =
    val service      = request.serviceId
    val credentialId = request.credentialId
    val agentId    = request.agentId
    val url: URL = url"$clientExchangeProxyPath/$service/$credentialId/$agentId/clientlist"
    http.get(url)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(response) => Future.fromTry(Try(parseResponse(response.body)))
        case Left(error)     => Future.failed(error)
      }
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[ClientExchangeProxyConnector][initiateClientListDownload]: Upstream error - ${e.getMessage}")
          throw e
        case e: Throwable =>
          logger.error(s"[ClientExchangeProxyConnector][initiateClientListDownload]: ${e.getMessage}")
          throw new RuntimeException(e.getMessage)
      }

  /**
   * Reads the IASS response, for example:
   * {{{
   * <gwe:AsynchronousProcessWaitTime browserInterval="8000" xmlns:gwe="gwe">
   *   <BusinessServiceInterval>1000</BusinessServiceInterval>
   *   <BusinessServiceInterval>2000</BusinessServiceInterval>
   * </gwe:AsynchronousProcessWaitTime>
   * }}}
   * Anything that does not match this shape is reported as a bad gateway error.
   */
  private[connectors] def parseResponse(body: String): InitiateClientListDownloadResponse =
    if body.contains("<!DOCTYPE") || body.contains("<!ENTITY") then
      throw invalidResponse("document type declarations are not allowed")

    val xml: Elem =
      try XML.loadString(body)
      catch case NonFatal(_) => throw invalidResponse("not well-formed XML")

    if xml.label != "AsynchronousProcessWaitTime" then
      throw invalidResponse(s"unexpected root element '${xml.label}'")

    val browserInterval = toMilliseconds((xml \ "@browserInterval").text, "browserInterval")
    val businessServiceIntervals =
      (xml \ "BusinessServiceInterval").map(node => toMilliseconds(node.text, "BusinessServiceInterval")).toList

    InitiateClientListDownloadResponse(browserInterval, businessServiceIntervals)

  private def toMilliseconds(raw: String, field: String): Int =
    raw.trim.toIntOption.filter(_ >= 0).getOrElse(throw invalidResponse(s"$field is not a valid number of milliseconds"))

  private def invalidResponse(reason: String): UpstreamErrorResponse =
    UpstreamErrorResponse(s"Unexpected response from Client Exchange Proxy: $reason", BAD_GATEWAY)
}
