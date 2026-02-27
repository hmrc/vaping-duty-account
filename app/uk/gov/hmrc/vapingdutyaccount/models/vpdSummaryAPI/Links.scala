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

package uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI

import play.api.libs.json.{Json, OFormat}

case class Self(href: String, method: String)
case class ManageContactPreference(href: String, method: String)

implicit val selfFormats: OFormat[Self]                                       = Json.format[Self]
implicit val manageContactPreferenceFormats: OFormat[ManageContactPreference] = Json.format[ManageContactPreference]

/** The links that will be appended to the VPDSummary.
  *
  * @param self
  *   The link that will be sent along under the "self" field.
  * @param manageContactPreferences
  *   The link that will be sent along under the "manage-contact-preference" field.
  */
case class Links(self: Self, `manage-contact-preference`: ManageContactPreference)

implicit val linksFormats: OFormat[Links] = Json.format[Links]
