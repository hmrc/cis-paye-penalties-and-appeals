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

package uk.gov.hmrc.cispayepenaltiesandappeals.base

import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, PlayBodyParsers}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.cispayepenaltiesandappeals.controllers.actions.FakeIdentifierAction

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

trait   SpecBase
  extends AnyFreeSpec
    with Matchers
    with DefaultAwaitTimeout
    with ScalaFutures
    with FakeApplicationFactory
    with BaseOneAppPerSuite
    with IntegrationPatience
    with MockitoSugar
    with OptionValues
    with BeforeAndAfterEach {
  
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 2.seconds, interval = 25.millis)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host" -> "localhost",
        "microservice.services.auth.port" -> 11111,
        "microservice.services.rds-datacache-proxy.host" -> "localhost",
        "microservice.services.rds-datacache-proxy.port" -> 11111
      )
      .build()

  protected def applicationBuilder(): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
  }

  val cc: ControllerComponents = stubControllerComponents()
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val bodyParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  val fakeIdentifierAction: FakeIdentifierAction = app.injector.instanceOf[FakeIdentifierAction]

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = cc.executionContext

}