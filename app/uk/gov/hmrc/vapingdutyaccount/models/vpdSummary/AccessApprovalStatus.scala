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

package uk.gov.hmrc.vapingdutyaccount.models.vpdSummary

import play.api.libs.json.{JsString, Writes}
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{SubscriptionApprovalStatus, SubscriptionContactPreferences}

/** Access status exposed in the VPD Summary API `access` block, derived from the upstream
  * subscription's approval status and insolvency flag.
  */
enum AccessApprovalStatus {
  case Approved, Deregistered, Revoked, Insolvent
}

object AccessApprovalStatus {

  given Writes[AccessApprovalStatus] = Writes {
    case Approved     => JsString("APPROVED")
    case Deregistered => JsString("DEREGISTERED")
    case Revoked      => JsString("REVOKED")
    case Insolvent    => JsString("INSOLVENT")
  }

  def fromSubscription(contactPreferences: SubscriptionContactPreferences): AccessApprovalStatus =
    contactPreferences.approvalStatus match {
      case SubscriptionApprovalStatus.DeRegistered                               => Deregistered
      case SubscriptionApprovalStatus.Revoked                                    => Revoked
      case SubscriptionApprovalStatus.Approved if contactPreferences.isInsolvent => Insolvent
      case SubscriptionApprovalStatus.Approved                                   => Approved
    }
}
