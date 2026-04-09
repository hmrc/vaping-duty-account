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
import play.api.libs.json.JsSuccess
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
            case error =>
              val formatted = formatJsonErrors(error.asInstanceOf[play.api.libs.json.JsError].errors)
              logger.warn(s"[SubscriptionConnector][getSubscription] Unable to parse JSON as SubscriptionContactPreferences: $formatted")
              Left(SubscriptionErrorResponse("Unable to parse JSON as SubscriptionContactPreferences", Some(formatted)))
          }
        case NOT_FOUND =>
          Left(SubscriptionNotFound())
        case BAD_REQUEST =>
          logger.error(s"[SubscriptionConnector][getSubscription] Bad request sent - check our request payload")
          response.json.validate[SubscriptionErrorResponse] match {
            case JsSuccess(value, _) =>
              Left(value)
            case _ =>
              Left(SubscriptionErrorResponse("Bad request", Some("400")))
          }
        case UNPROCESSABLE_ENTITY =>
          logger.error(s"[SubscriptionConnector][getSubscription] Unprocessable entity - check our JSON structure")
          response.json.validate[SubscriptionErrorResponse] match {
            case JsSuccess(value, _) =>
              Left(value)
            case _ =>
              Left(SubscriptionErrorResponse("Unprocessable entity", Some("422")))
          }
        case statusCode =>
          response.json.validate[SubscriptionErrorResponse] match {
            case JsSuccess(value, _) =>
              logger.warn(s"[SubscriptionConnector][getSubscription] Downstream error $statusCode: ${value.error}")
              Left(value)
            case _ =>
              val err = logBackendError("[SubscriptionConnector][getSubscription]", response)
              logger.warn(s"[SubscriptionConnector][getSubscription] Unable to parse Json as SubscriptionErrorResponse")
              Left(SubscriptionErrorResponse(err.message, Some(err.body)))
          }
      }
  }
}