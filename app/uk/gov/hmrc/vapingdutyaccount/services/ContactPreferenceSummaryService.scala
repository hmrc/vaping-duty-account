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

package uk.gov.hmrc.vapingdutyaccount.services

import com.google.inject.{Inject, Singleton}
import play.api.http.HttpVerbs
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionSummary
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{ContactMethod, ContactPreferenceSection, ContactPreferenceStatus, ManageContactPreference}

/** Builds the contact preference section of the VPD Summary, and its link, from the Subscription Summary. */
@Singleton
class ContactPreferenceSummaryService @Inject()(config: AppConfig) {

  def build(subscriptionSummary: SubscriptionSummary): ContactPreferenceSection = {
    val contactMethod = resolveContactMethod(subscriptionSummary)

    ContactPreferenceSection(
      contactMethod           = contactMethod,
      contactPreferenceStatus = resolveContactPreferenceStatus(contactMethod, subscriptionSummary),
      manageContactPreference = ManageContactPreference(config.manageContactPreferenceUrl, HttpVerbs.GET)
    )
  }

  private def resolveContactMethod(subscriptionSummary: SubscriptionSummary): ContactMethod =
    if (subscriptionSummary.paperlessPreference) ContactMethod.Email else ContactMethod.Post

  private def resolveContactPreferenceStatus(
    contactMethod: ContactMethod,
    subscriptionSummary: SubscriptionSummary
  ): Option[ContactPreferenceStatus] =
    Option.when(contactMethod == ContactMethod.Email)(
      ContactPreferenceStatus(subscriptionSummary.bouncedEmail.getOrElse(false))
    )
}
