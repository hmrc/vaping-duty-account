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
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{SubscriptionContactPreferences, SubscriptionError, SubscriptionErrorResponse, SubscriptionNotFound}
import uk.gov.hmrc.vapingdutyaccount.utils.DownstreamLogging

object SubscriptionParser {
  type SubscriptionDetailsType = Either[SubscriptionError, SubscriptionContactPreferences]

  implicit object GetSubscriptionHttpReads
      extends HttpReads[SubscriptionDetailsType]
      with DownstreamLogging {

    override def read(method: String, url: String, response: HttpResponse): SubscriptionDetailsType =
      response.status match {
        case OK =>
          response.json.validate[SubscriptionContactPreferences] match {
            case JsSuccess(value, _) =>
              Right(value)
            case JsError(errors) =>
              val formatted = formatJsonErrors(errors)
              logger.warn(s"[SubscriptionConnector][getSubscription] Unable to parse JSON as SubscriptionContactPreferences: $formatted")
              Left(SubscriptionErrorResponse("Unable to parse JSON as SubscriptionContactPreferences", Some(formatted)))
          }
        case NOT_FOUND =>
          Left(SubscriptionNotFound())
        case BAD_REQUEST =>
          logger.error("[SubscriptionConnector][getSubscription] Bad request sent - check our request payload")
          response.json.validate[SubscriptionErrorResponse] match {
            case JsSuccess(value, _) =>
              Left(value.copy(statusCode = Some(BAD_REQUEST)))
            case JsError(_) =>
              Left(SubscriptionErrorResponse("Bad request", None, Some(BAD_REQUEST)))
          }
        case UNPROCESSABLE_ENTITY =>
          logger.error("[SubscriptionConnector][getSubscription] Unprocessable entity - check our JSON structure")
          response.json.validate[SubscriptionErrorResponse] match {
            case JsSuccess(value, _) =>
              Left(value.copy(statusCode = Some(UNPROCESSABLE_ENTITY)))
            case JsError(_) =>
              Left(SubscriptionErrorResponse("Unprocessable entity", None, Some(UNPROCESSABLE_ENTITY)))
          }
        case statusCode =>
          response.json.validate[SubscriptionErrorResponse] match {
            case JsSuccess(value, _) =>
              logger.warn(s"[SubscriptionConnector][getSubscription] Downstream error $statusCode: ${value.error}")
              Left(value.copy(statusCode = Some(statusCode)))
            case JsError(errors) =>
              val formatted = formatJsonErrors(errors)
              val err = logBackendError("[SubscriptionConnector][getSubscription]", response)
              logger.warn(s"[SubscriptionConnector][getSubscription] Unable to parse Json as SubscriptionErrorResponse: $formatted")
              Left(SubscriptionErrorResponse(err.message, Some(err.body), Some(statusCode)))
          }
      }
  }
}
