/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.thirdpartyapplication.component

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import uk.gov.hmrc.thirdpartyapplication.component.stubs._
import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import scala.concurrent.duration._
import scala.language.postfixOps

abstract class BaseFeatureSpec extends FeatureSpec with GivenWhenThen with Matchers
  with BeforeAndAfterEach with BeforeAndAfterAll with GuiceOneServerPerSuite {

  override lazy val port = 19111
  val serviceUrl = s"http://localhost:$port"
  val timeout = 10 seconds

  var thirdPartyDeveloperStub = ThirdPartyDeveloperStub
  val apiSubscriptionFieldsStub = ApiSubscriptionFieldsStub
  val thirdPartyDelegatedAuthorityStub = ThirdPartyDelegatedAuthorityStub
  val authStub = AuthStub
  val totpStub = TOTPStub
  val awsApiGatewayStub = AwsApiGatewayStub
  val emailStub = EmailStub
  val apiPlatformEventsStub = ApiPlatformEventsStub
  val mocks = {
    Seq(thirdPartyDeveloperStub, apiSubscriptionFieldsStub, authStub, totpStub,
      thirdPartyDelegatedAuthorityStub, awsApiGatewayStub, emailStub, apiPlatformEventsStub)
  }

  override protected def beforeAll(): Unit = {
    mocks.foreach(m => if (!m.stub.server.isRunning) m.stub.server.start())
  }

  override protected def afterEach(): Unit = {
    mocks.foreach(_.stub.mock.resetMappings())
  }

  override protected def afterAll(): Unit = {
    mocks.foreach(_.stub.server.stop())
  }
}

case class MockHost(port: Int) {
  val server = new WireMockServer(WireMockConfiguration
    .wireMockConfig()
    .port(port))

  val mock = new WireMock("localhost", port)
}

trait Stub {
  val stub: MockHost
}
