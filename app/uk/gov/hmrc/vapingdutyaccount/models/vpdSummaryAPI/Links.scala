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

import play.api.libs.json.{Json, Writes}

case class Self(href: String, method: String)
case class ManageContactPreference(href: String, method: String)

implicit val selfFormats: Writes[Self]                                       = Json.writes[Self]
implicit val manageContactPreferenceFormats: Writes[ManageContactPreference] = Json.writes[ManageContactPreference]
implicit val completeReturnFormats: Writes[CompleteReturn]                   = Json.writes[CompleteReturn]
implicit val viewReturnsFormats: Writes[ViewReturns]                         = Json.writes[ViewReturns]

case class Links(
  self: Self,
  manageContactPreference: ManageContactPreference,
  completeReturn: Option[CompleteReturn] = None,
  viewReturns: Option[ViewReturns] = None
)

implicit val linksFormats: Writes[Links] = Json.writes[Links]
