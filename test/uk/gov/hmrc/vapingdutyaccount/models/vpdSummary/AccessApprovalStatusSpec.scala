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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{SubscriptionApprovalStatus, SubscriptionContactPreferences}

class AccessApprovalStatusSpec extends AnyFreeSpec with Matchers {

  private def contactPreferences(approvalStatus: SubscriptionApprovalStatus, insolvencyStatus: Option[String]) =
    SubscriptionContactPreferences(
      paperlessPreference = true,
      emailAddress = None,
      verifiedEmail = None,
      bouncedEmail = None,
      addressLine1 = None,
      addressLine2 = None,
      addressLine3 = None,
      postCode = None,
      approvalStatus = approvalStatus,
      insolvencyStatus = insolvencyStatus
    )

  "AccessApprovalStatus.fromSubscription" - {
    "must return Approved when the subscription is approved and not insolvent" in {
      AccessApprovalStatus.fromSubscription(
        contactPreferences(SubscriptionApprovalStatus.Approved, Some("N"))
      ) shouldBe AccessApprovalStatus.Approved
    }

    "must return Approved when the subscription is approved and insolvencyStatus is absent" in {
      AccessApprovalStatus.fromSubscription(
        contactPreferences(SubscriptionApprovalStatus.Approved, None)
      ) shouldBe AccessApprovalStatus.Approved
    }

    "must return Insolvent when the subscription is approved and insolvent" in {
      AccessApprovalStatus.fromSubscription(
        contactPreferences(SubscriptionApprovalStatus.Approved, Some("Y"))
      ) shouldBe AccessApprovalStatus.Insolvent
    }

    "must return Deregistered when the subscription is deregistered and not insolvent" in {
      AccessApprovalStatus.fromSubscription(
        contactPreferences(SubscriptionApprovalStatus.DeRegistered, Some("N"))
      ) shouldBe AccessApprovalStatus.Deregistered
    }

    "must return Insolvent when the subscription is deregistered and insolvent" in {
      AccessApprovalStatus.fromSubscription(
        contactPreferences(SubscriptionApprovalStatus.DeRegistered, Some("Y"))
      ) shouldBe AccessApprovalStatus.Insolvent
    }

    "must return Revoked when the subscription is revoked and not insolvent" in {
      AccessApprovalStatus.fromSubscription(
        contactPreferences(SubscriptionApprovalStatus.Revoked, Some("N"))
      ) shouldBe AccessApprovalStatus.Revoked
    }

    "must return Insolvent when the subscription is revoked and insolvent" in {
      AccessApprovalStatus.fromSubscription(
        contactPreferences(SubscriptionApprovalStatus.Revoked, Some("Y"))
      ) shouldBe AccessApprovalStatus.Insolvent
    }
  }
}
