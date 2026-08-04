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

final case class PaymentBalance(
  amount: BigDecimal,
  isMultiplePaymentDue: Boolean,
  chargeReference: Option[String]
)

object PaymentBalance {
  given Writes[PaymentBalance] = Json.writes[PaymentBalance]
}

final case class Payments(
  hasPaymentsError: Boolean,
  balance: Option[PaymentBalance]
)

object Payments {
  given Writes[Payments] = Json.writes[Payments]
}
