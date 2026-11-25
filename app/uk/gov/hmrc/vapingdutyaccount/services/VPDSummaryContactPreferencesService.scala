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

import com.google.inject.Inject
import play.api.http.HttpVerbs
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionDisplay
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{Access, AccessApprovalStatus, ContactMethod, ContactPreferenceStatus, ManageContactPreference}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.internal.ContactPreferencesAndLinks

class VPDSummaryContactPreferencesService @Inject(config: AppConfig) {

  def buildContactPreferencesAndLinks(contactPreferences: SubscriptionDisplay): ContactPreferencesAndLinks = {
    val approvalStatus = AccessApprovalStatus.fromSubscription(contactPreferences)
    val isInsolvent    = approvalStatus == AccessApprovalStatus.Insolvent
    val access         = Some(Access(approvalStatus = Some(approvalStatus)))
    val (contactMethod, contactPreferenceStatus) = buildContactPreferences(contactPreferences, isInsolvent)

    ContactPreferencesAndLinks(
      isInsolvent = isInsolvent,
      access = access,
      contactMethod = contactMethod,
      contactPreferenceStatus = contactPreferenceStatus,
      manageContactPreferencesLink = manageContactPreferencesLink(isInsolvent)
    )
  }

  private def buildContactPreferences(contactPreferences: SubscriptionDisplay, isInsolvent: Boolean) =
    if (isInsolvent)
      (None, None)
    else {
      val cm = resolveContactMethod(contactPreferences)
      (Some(cm), resolveContactPreferenceStatus(cm, contactPreferences))
    }

  private def resolveContactMethod(contactPreferences: SubscriptionDisplay): ContactMethod =
    if (contactPreferences.paperlessPreference) ContactMethod.Email else ContactMethod.Post

  private def resolveContactPreferenceStatus(contactMethod: ContactMethod,
                                             contactPreferences: SubscriptionDisplay
                                            ): Option[ContactPreferenceStatus] =
    if (contactMethod == ContactMethod.Email)
      Some(ContactPreferenceStatus(contactPreferences.bouncedEmail.getOrElse(false)))
    else
      None

  private def manageContactPreferencesLink(isInsolvent: Boolean) =
    if (isInsolvent)
      None
    else
      Some(ManageContactPreference(config.manageContactPreferenceUrl, HttpVerbs.GET))

}
