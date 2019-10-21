/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.uk.gov.hmrc.thirdpartyapplication.metrics

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.thirdpartyapplication.metrics.MissingMongoFields
import uk.gov.hmrc.thirdpartyapplication.repository.ApplicationRepository
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MissingMongoFieldsSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val mockApplicationRepository: ApplicationRepository = mock[ApplicationRepository]
    val metricUnderTest: MissingMongoFields = new MissingMongoFields(mockApplicationRepository)
  }

  "refresh metrics" should {
    "update values for number of applications with missing fields" in new Setup {
      private val numberOfMissingRateLimits = 10
      private val numberOfMissingLastAccessedDate = 3

      when(mockApplicationRepository.documentsWithFieldMissing("rateLimitTier")).thenReturn(Future.successful(numberOfMissingRateLimits))
      when(mockApplicationRepository.documentsWithFieldMissing("lastAccess")).thenReturn(Future.successful(numberOfMissingLastAccessedDate))

      private val result = await(metricUnderTest.metrics)

      result("applicationsMissingRateLimitField") shouldBe numberOfMissingRateLimits
      result("applicationsMissingLastAccessDateField") shouldBe numberOfMissingLastAccessedDate
    }
  }
}
