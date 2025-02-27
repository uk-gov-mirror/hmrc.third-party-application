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
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.thirdpartyapplication.connector.{AuthConfig, AuthConnector}
import uk.gov.hmrc.thirdpartyapplication.controllers.ErrorCode._
import uk.gov.hmrc.thirdpartyapplication.models.JsonFormatters._
import uk.gov.hmrc.thirdpartyapplication.models.{Blocked, InvalidStateTransition, Unblocked}
import uk.gov.hmrc.thirdpartyapplication.services.{ApplicationService, GatekeeperService}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.thirdpartyapplication.services.SubscriptionService
import uk.gov.hmrc.thirdpartyapplication.models.ApiIdentifier
import uk.gov.hmrc.thirdpartyapplication.models.SubscriptionAlreadyExistsException
import uk.gov.hmrc.thirdpartyapplication.models.ApplicationId

@Singleton
class GatekeeperController @Inject()(
                                      val authConnector: AuthConnector,
                                      val applicationService: ApplicationService,
                                      gatekeeperService: GatekeeperService,
                                      subscriptionService: SubscriptionService,
                                      val authConfig: AuthConfig,
                                      cc: ControllerComponents)(
                                      implicit val ec: ExecutionContext)  
                                      extends BackendController(cc) with JsonUtils with AuthorisationWrapper {

  private lazy val badStateResponse = PreconditionFailed(
    JsErrorResponse(INVALID_STATE_TRANSITION, "Application is not in state 'PENDING_GATEKEEPER_APPROVAL'"))

  private lazy val badResendResponse = PreconditionFailed(
    JsErrorResponse(INVALID_STATE_TRANSITION, "Application is not in state 'PENDING_REQUESTER_VERIFICATION'"))


  def approveUplift(id: ApplicationId) = requiresAuthentication().async(parse.json) {
    implicit request =>
      withJsonBody[ApproveUpliftRequest] { approveUpliftPayload =>
        gatekeeperService.approveUplift(id, approveUpliftPayload.gatekeeperUserId)
          .map(_ => NoContent)
      } recover {
        case _: InvalidStateTransition => badStateResponse
      } recover recovery
  }

  def rejectUplift(id: ApplicationId) = requiresAuthentication().async(parse.json) {
    implicit request =>
      withJsonBody[RejectUpliftRequest] {
        gatekeeperService.rejectUplift(id, _).map(_ => NoContent)
      } recover {
        case _: InvalidStateTransition => badStateResponse
      } recover recovery
  }

  def resendVerification(id: ApplicationId) = requiresAuthentication().async(parse.json) {
    implicit request =>
      withJsonBody[ResendVerificationRequest] { resendVerificationPayload =>
        gatekeeperService.resendVerification(id, resendVerificationPayload.gatekeeperUserId).map(_ => NoContent)
      } recover {
        case _: InvalidStateTransition => badResendResponse
      } recover recovery
  }

  def blockApplication(id: ApplicationId) = requiresAuthentication().async { _ =>
    gatekeeperService.blockApplication(id) map {
      case Blocked => Ok
    } recover recovery
  }

  def unblockApplication(id: ApplicationId) = requiresAuthentication().async { _ =>
    gatekeeperService.unblockApplication(id) map {
      case Unblocked => Ok
    } recover recovery
  }

  def fetchAppsForGatekeeper = requiresAuthentication().async {
    gatekeeperService.fetchNonTestingAppsWithSubmittedDate() map {
      apps => Ok(Json.toJson(apps))
    } recover recovery
  }

  def fetchAppById(id: ApplicationId) = requiresAuthentication().async {
    gatekeeperService.fetchAppWithHistory(id) map (app => Ok(Json.toJson(app))) recover recovery
  }

  def fetchAppStateHistoryById(id: ApplicationId) = requiresAuthentication().async {
    gatekeeperService.fetchAppStateHistoryById(id) map (app => Ok(Json.toJson(app))) recover recovery
  }

  def createSubscriptionForApplication(applicationId: ApplicationId) =
    requiresAuthenticationForPrivilegedOrRopcApplications(applicationId).async(parse.json) {
      implicit request =>
        withJsonBody[ApiIdentifier] { api =>
          subscriptionService.createSubscriptionForApplicationMinusChecks(applicationId, api).map(_ => NoContent) recover {
            case e: SubscriptionAlreadyExistsException => Conflict(JsErrorResponse(SUBSCRIPTION_ALREADY_EXISTS, e.getMessage))
          } recover recovery
        }
    }
}
