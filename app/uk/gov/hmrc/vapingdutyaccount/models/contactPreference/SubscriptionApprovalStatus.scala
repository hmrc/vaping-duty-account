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

package uk.gov.hmrc.vapingdutyaccount.models.contactPreference

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}

/** Raw approval status codes as returned by the VPD Subscription Display (ETMP) API. */
sealed trait SubscriptionApprovalStatus

object SubscriptionApprovalStatus {

  case object Approved     extends SubscriptionApprovalStatus
  case object DeRegistered extends SubscriptionApprovalStatus
  case object Revoked      extends SubscriptionApprovalStatus

  implicit val subscriptionApprovalStatusReads: Reads[SubscriptionApprovalStatus] = {
    case JsString("01") => JsSuccess(Approved)
    case JsString("04") => JsSuccess(DeRegistered)
    case JsString("05") => JsSuccess(Revoked)
    case s: JsString    => JsError(s"$s is not a valid SubscriptionApprovalStatus")
    case v              => JsError(s"got $v was expecting a string representing a SubscriptionApprovalStatus")
  }

  implicit val subscriptionApprovalStatusWrites: Writes[SubscriptionApprovalStatus] = {
    case Approved     => JsString("01")
    case DeRegistered => JsString("04")
    case Revoked      => JsString("05")
  }
}
