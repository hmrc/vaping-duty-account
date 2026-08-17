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

import play.api.libs.json.{Json, Writes}

case class Self(href: String, method: String)
case class ManageContactPreference(href: String, method: String)
case class MakePayment(href: String, method: String)
case class SetUpDirectDebit(href: String, method: String)

implicit val selfFormats: Writes[Self]                                       = Json.writes[Self]
implicit val manageContactPreferenceFormats: Writes[ManageContactPreference] = Json.writes[ManageContactPreference]
implicit val completeReturnFormats: Writes[CompleteReturn]                   = Json.writes[CompleteReturn]
implicit val viewReturnsFormats: Writes[ViewReturns]                         = Json.writes[ViewReturns]
implicit val makePaymentFormats: Writes[MakePayment]                         = Json.writes[MakePayment]
implicit val setUpDirectDebitFormats: Writes[SetUpDirectDebit]               = Json.writes[SetUpDirectDebit]

case class Links(
                  self: Self,
                  manageContactPreference: ManageContactPreference,
                  completeReturn: Option[CompleteReturn] = None,
                  viewReturns: Option[ViewReturns] = None,
                  makePayment: Option[MakePayment] = None,
                  setUpDirectDebit: Option[SetUpDirectDebit] = None
)

implicit val linksFormats: Writes[Links] = Json.writes[Links]
