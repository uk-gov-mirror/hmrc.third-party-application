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

import play.api.mvc.PathBindable
import uk.gov.hmrc.thirdpartyapplication.models.{DeveloperIdentifier, EmailIdentifier, UserId, ApplicationId}
import play.api.mvc.QueryStringBindable
import java.{util => ju}
import scala.util.Try
import play.api.Logger

package object binders {
  private def applicationIdFromString(text: String): Either[String, ApplicationId] = {
    Try(ju.UUID.fromString(text))
    .toOption
    .toRight(s"Cannot accept $text as ApplicationId")
    .map(ApplicationId(_))
  }

  private def userIdFromString(text: String): Either[String, UserId] = {
    Try(ju.UUID.fromString(text))
    .toOption
    .toRight(s"Cannot accept $text as UserId")
    .map(UserId(_))
  }

  implicit def applicationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApplicationId] = new PathBindable[ApplicationId] {
    override def bind(key: String, value: String): Either[String, ApplicationId] = {
      textBinder.bind(key, value).flatMap(applicationIdFromString)
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      applicationId.value.toString()
    }
  }

  implicit def applicationIdQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApplicationId] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApplicationId]] = {
      textBinder.bind(key, params).map(_.flatMap(applicationIdFromString))
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      textBinder.unbind(key, applicationId.value.toString())
    }
  }

  implicit def userIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[UserId] = new PathBindable[UserId] {
    override def bind(key: String, value: String): Either[String, UserId] = {
      textBinder.bind(key, value).flatMap(userIdFromString)
    }

    override def unbind(key: String, userId: UserId): String = {
      userId.value.toString()
    }
  }

    private def warnOnEmailId(id: DeveloperIdentifier): DeveloperIdentifier = id match {
    case EmailIdentifier(_) => Logger.warn("Still using emails as identifier"); id
    case _ => id
  }

  implicit def developerIdentifierBinder(implicit textBinder: PathBindable[String]): PathBindable[DeveloperIdentifier] = new PathBindable[DeveloperIdentifier] {
    override def bind(key: String, value: String): Either[String, DeveloperIdentifier] = {
      for {
        text <- textBinder.bind(key, value)
        id <- DeveloperIdentifier(value).toRight(s"Cannot accept $text as a developer identifier")
        _ = warnOnEmailId(id)
      } yield id
    }

    override def unbind(key: String, developerId: DeveloperIdentifier): String = {
      DeveloperIdentifier.asText(warnOnEmailId(developerId))
    }
  }

  implicit def queryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[DeveloperIdentifier] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DeveloperIdentifier]] = {
      for {
        textOrBindError <- textBinder.bind("developerId", params).orElse(textBinder.bind("email", params))
      } yield textOrBindError match {
        case Right(idText) =>
          for {
            id <- DeveloperIdentifier(idText).toRight(s"Cannot accept $idText as a developer identifier")
            _ = warnOnEmailId(id)
          } yield id
        case _ => Left("Unable to bind a developer identifier")
      }
    }

    override def unbind(key: String, developerId: DeveloperIdentifier): String = {
      textBinder.unbind("developerId", DeveloperIdentifier.asText(warnOnEmailId(developerId)))
    }
  }
}
