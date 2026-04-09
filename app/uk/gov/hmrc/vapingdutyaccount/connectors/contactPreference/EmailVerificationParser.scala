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
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{EmailVerificationError, EmailVerificationErrorResponse, GetVerificationStatusResponse}
import uk.gov.hmrc.vapingdutyaccount.utils.DownstreamLogging

object EmailVerificationParser {
  type EmailVerificationDetailsType = Either[EmailVerificationError, GetVerificationStatusResponse]

  implicit object GetEmailVerificationHttpReads
      extends HttpReads[EmailVerificationDetailsType]
      with DownstreamLogging {

    override def read(method: String, url: String, response: HttpResponse): EmailVerificationDetailsType =
      response.status match {
        case OK =>
          response.json.validate[GetVerificationStatusResponse] match {
            case JsSuccess(value, _) =>
              Right(value)
            case JsError(errors) =>
              val formatted = formatJsonErrors(errors)
              logger.warn(s"[EmailVerificationConnector][getEmailVerification] Unable to parse JSON as GetVerificationStatusResponse: $formatted")
              Left(EmailVerificationErrorResponse("Unable to parse JSON as GetVerificationStatusResponse", Some(formatted)))
          }
        case NOT_FOUND =>
          logger.info("[EmailVerificationConnector][getEmailVerification] No verified emails found")
          Right(GetVerificationStatusResponse(emails = List.empty))
        case BAD_REQUEST =>
          logger.error("[EmailVerificationConnector][getEmailVerification] Bad request sent - check our request")
          response.json.validate[EmailVerificationErrorResponse] match {
            case JsSuccess(value, _) =>
              Left(value.copy(statusCode = Some(BAD_REQUEST)))
            case JsError(_) =>
              Left(EmailVerificationErrorResponse("Bad request", None, Some(BAD_REQUEST)))
          }
        case statusCode =>
          response.json.validate[EmailVerificationErrorResponse] match {
            case JsSuccess(value, _) =>
              logger.warn(s"[EmailVerificationConnector][getEmailVerification] Downstream error $statusCode: ${value.error}")
              Left(value.copy(statusCode = Some(statusCode)))
            case JsError(errors) =>
              val formatted = formatJsonErrors(errors)
              val err = logBackendError("[EmailVerificationConnector][getEmailVerification]", response)
              logger.warn(s"[EmailVerificationConnector][getEmailVerification] Unable to parse Json as EmailVerificationErrorResponse: $formatted")
              Left(EmailVerificationErrorResponse(err.message, Some(err.body), Some(statusCode)))
          }
      }
  }
}
