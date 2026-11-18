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

import org.mockito.Mockito.when
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{ContactMethod, ContactPreferenceSection, ContactPreferenceStatus, ManageContactPreference}

class ContactPreferenceSummaryServiceSpec extends SpecBase {

  private val mockConfig: AppConfig = mock[AppConfig]
  when(mockConfig.manageContactPreferenceUrl).thenReturn(testManageContactPreferenceUrl)

  private val service = new ContactPreferenceSummaryService(mockConfig)

  private val manageContactPreferenceLink = ManageContactPreference(testManageContactPreferenceUrl, httpGet)

  "ContactPreferenceSummaryService" - {
    "build must" - {

      "return Email with a not-bounced status and the manage link when paperless is selected" in {
        service.build(subscriptionSummaryEmailSelected) mustBe ContactPreferenceSection(
          contactMethod           = ContactMethod.Email,
          contactPreferenceStatus = Some(ContactPreferenceStatus(bouncedEmail = false)),
          manageContactPreference = manageContactPreferenceLink
        )
      }

      "return Email with a bounced status when the email has bounced" in {
        service.build(subscriptionSummaryEmailSelected.copy(bouncedEmail = Some(true))) mustBe ContactPreferenceSection(
          contactMethod           = ContactMethod.Email,
          contactPreferenceStatus = Some(ContactPreferenceStatus(bouncedEmail = true)),
          manageContactPreference = manageContactPreferenceLink
        )
      }

      "treat a missing bounced flag as not bounced" in {
        service.build(subscriptionSummaryEmailSelected.copy(bouncedEmail = None)).contactPreferenceStatus mustBe
          Some(ContactPreferenceStatus(bouncedEmail = false))
      }

      "return Post with no status and the manage link when paperless is not selected" in {
        service.build(subscriptionSummaryPostNoEmail) mustBe ContactPreferenceSection(
          contactMethod           = ContactMethod.Post,
          contactPreferenceStatus = None,
          manageContactPreference = manageContactPreferenceLink
        )
      }
    }
  }
}
