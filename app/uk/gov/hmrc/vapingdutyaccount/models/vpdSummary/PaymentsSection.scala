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

/** The payments part of the VPD Summary, with the links that belong to it. Internal only, never serialised. */
final case class PaymentsSection(
  payments: Payments,
  makePayment: Option[MakePayment],
  claimRepayment: Option[ClaimRepayment],
  viewPayments: Option[ViewPayments]
)

object PaymentsSection {

  /** The payments value used when the payments could not be retrieved. */
  val errorPayments: Payments = Payments(hasPaymentsError = true, balance = None)
}
