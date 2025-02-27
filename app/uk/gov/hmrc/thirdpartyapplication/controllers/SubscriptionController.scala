/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.thirdpartyapplication.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.thirdpartyapplication.models.JsonFormatters._
import uk.gov.hmrc.thirdpartyapplication.models._
import uk.gov.hmrc.thirdpartyapplication.models.DeveloperIdentifier
import uk.gov.hmrc.thirdpartyapplication.repository.SubscriptionRepository
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class SubscriptionController @Inject()(subscriptionRepository: SubscriptionRepository, cc: ControllerComponents)
                                     (implicit val ec: ExecutionContext)
                                     extends BackendController(cc) with JsonUtils {

  def getSubscribers(context: String, version: String): Action[AnyContent] = Action.async {_ =>
    subscriptionRepository.getSubscribers(ApiIdentifier(context, version)).map(subscribers => Ok(toJson(SubscribersResponse(subscribers)))) recover recovery
  }

  def getSubscriptionsForDeveloper(developerId: DeveloperIdentifier): Action[AnyContent] = Action.async {_ =>
    (developerId match {
      case EmailIdentifier(email) => 
        subscriptionRepository.getSubscriptionsForDeveloper(email)
      case UuidIdentifier(userId) =>
        subscriptionRepository.getSubscriptionsForDeveloper(userId)
    }).map(subscriptions => Ok(toJson(subscriptions))) recover recovery
  }
}

case class SubscribersResponse(subscribers: Set[ApplicationId])
