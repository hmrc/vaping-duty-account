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
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{PaperlessPreferenceSubmittedResponse, PaperlessPreferenceSubmittedSuccess, SubmitPreferencesError, SubmitPreferencesErrorResponse}
import uk.gov.hmrc.vapingdutyaccount.utils.DownstreamLogging

object SubmitPreferencesParser {
  type SubmitPreferencesDetailsType = Either[SubmitPreferencesError, PaperlessPreferenceSubmittedResponse]

  implicit object SubmitPreferencesHttpReads
      extends HttpReads[SubmitPreferencesDetailsType]
      with DownstreamLogging {

    override def read(method: String, url: String, response: HttpResponse): SubmitPreferencesDetailsType =
      response.status match {
        case OK =>
          response.json.validate[PaperlessPreferenceSubmittedSuccess] match {
            case JsSuccess(value, _) =>
              Right(value.success)
            case JsError(errors) =>
              val formatted = formatJsonErrors(errors)
              logger.warn(s"[SubmitPreferencesConnector][submitContactPreferences] Unable to parse JSON as PaperlessPreferenceSubmittedSuccess: $formatted")
              Left(SubmitPreferencesErrorResponse("Unable to parse JSON as PaperlessPreferenceSubmittedSuccess", Some(formatted)))
          }
        case NOT_FOUND =>
          logger.error("[SubmitPreferencesConnector][submitContactPreferences] Not found - check vpdId is valid")
          Left(SubmitPreferencesErrorResponse("Entity not found", None, Some(NOT_FOUND)))
        case BAD_REQUEST =>
          logger.error("[SubmitPreferencesConnector][submitContactPreferences] Bad request sent - check our request payload")
          response.json.validate[SubmitPreferencesErrorResponse] match {
            case JsSuccess(value, _) =>
              Left(value.copy(statusCode = Some(BAD_REQUEST)))
            case JsError(_) =>
              Left(SubmitPreferencesErrorResponse("Bad request", None, Some(BAD_REQUEST)))
          }
        case UNPROCESSABLE_ENTITY =>
          logger.error("[SubmitPreferencesConnector][submitContactPreferences] Unprocessable entity - check our JSON structure")
          response.json.validate[SubmitPreferencesErrorResponse] match {
            case JsSuccess(value, _) =>
              Left(value.copy(statusCode = Some(UNPROCESSABLE_ENTITY)))
            case JsError(_) =>
              Left(SubmitPreferencesErrorResponse("Unprocessable entity", None, Some(UNPROCESSABLE_ENTITY)))
          }
        case statusCode =>
          response.json.validate[SubmitPreferencesErrorResponse] match {
            case JsSuccess(value, _) =>
              logger.warn(s"[SubmitPreferencesConnector][submitContactPreferences] Downstream error $statusCode: ${value.error}")
              Left(value.copy(statusCode = Some(statusCode)))
            case JsError(errors) =>
              val formatted = formatJsonErrors(errors)
              val err = logBackendError("[SubmitPreferencesConnector][submitContactPreferences]", response)
              logger.warn(s"[SubmitPreferencesConnector][submitContactPreferences] Unable to parse Json as SubmitPreferencesErrorResponse: $formatted")
              Left(SubmitPreferencesErrorResponse(err.message, Some(err.body), Some(statusCode)))
          }
      }
  }
}
