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

package uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference

import play.api.http.Status.*
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionParser.GetSubscriptionHttpReads.logBackendError
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.GetVerificationStatusResponse
import uk.gov.hmrc.vapingdutyaccount.utils.DownstreamLogging

import scala.util.{Failure, Success, Try}

object EmailVerificationParser {
  type EmailVerificationDetailsType = Either[Exception, GetVerificationStatusResponse]

  implicit object GetEmailVerificationHttpReads
      extends HttpReads[EmailVerificationDetailsType]
      with DownstreamLogging {

    private val LOG_PREFIX = "[EmailVerificationConnector][getEmailVerification]"

    override def read(method: String, url: String, response: HttpResponse): EmailVerificationDetailsType =
      response.status match {
        case OK =>
          Try(response.json.as[GetVerificationStatusResponse]) match {
            case Success(value) => Right(value)
            case Failure(ex) =>
              val msg = logJsonParseError(LOG_PREFIX, ex)
              Left(new Exception(msg))
          }

        case NOT_FOUND =>
          Right(GetVerificationStatusResponse(emails = List.empty))

        case BAD_REQUEST =>
          val err = logBackendError(LOG_PREFIX, response)
          Left(new Exception(s"4XX error occurred: ${err.body}"))

        case _ =>
          val err = logBackendError(LOG_PREFIX, response)
          Left(new Exception(s"Downstream error ${response.status}: ${err.message}"))
      }
  }
}
