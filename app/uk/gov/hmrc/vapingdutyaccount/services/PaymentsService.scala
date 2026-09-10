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

import uk.gov.hmrc.vapingdutyaccount.models.payments.PaymentsResponse as UpstreamPaymentsResponse
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{PaymentBalance, Payments}

import javax.inject.Singleton

@Singleton
class PaymentsService {

  def toPayments(response: UpstreamPaymentsResponse): Payments = {
    val positiveOutstanding  = response.outstanding.filter(_.amountDue > 0)
    val distinctChargeRefs   = positiveOutstanding.flatMap(_.chargeReference).distinct
    val amount               = response.totalAccountBalance.getOrElse(BigDecimal(0))
    val isMultiplePaymentDue = distinctChargeRefs.size > 1

    val balance = PaymentBalance(
      amount               = amount,
      isMultiplePaymentDue = isMultiplePaymentDue,
      chargeReference      =
        if (amount > 0 && distinctChargeRefs.size == 1) distinctChargeRefs.headOption else None
    )

    Payments(hasPaymentsError = false, balance = Some(balance))
  }
}