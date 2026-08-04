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

import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.models.payments.{OutstandingPayment, PaymentsResponse as UpstreamPaymentsResponse}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{PaymentBalance, Payments}

class PaymentsServiceSpec extends SpecBase {

  val service: PaymentsService = new PaymentsService()

  "PaymentsService" - {

    "toPayments must" - {

      "return a single charge reference when exactly one charge is outstanding" in {
        val response = UpstreamPaymentsResponse(
          outstanding         = Seq(OutstandingPayment(Some("XVP123456789"), BigDecimal(4574.84), None, "Due")),
          totalAccountBalance = Some(BigDecimal(4574.84))
        )

        service.toPayments(response) mustBe Payments(
          hasPaymentsError = false,
          balance = Some(PaymentBalance(BigDecimal(4574.84), isMultiplePaymentDue = false, Some("XVP123456789")))
        )
      }

      "return isMultiplePaymentDue=true and no charge reference when multiple charges are outstanding" in {
        val response = UpstreamPaymentsResponse(
          outstanding = Seq(
            OutstandingPayment(Some("XVP111111111"), BigDecimal(5000), None, "Due"),
            OutstandingPayment(Some("XVP222222222"), BigDecimal(3250), None, "Overdue")
          ),
          totalAccountBalance = Some(BigDecimal(8250))
        )

        service.toPayments(response).balance mustBe Some(PaymentBalance(BigDecimal(8250), isMultiplePaymentDue = true, None))
      }

      "return a negative balance and no charge reference when the account is in credit" in {
        val response = UpstreamPaymentsResponse(outstanding = Seq.empty, totalAccountBalance = Some(BigDecimal(-325.50)))

        service.toPayments(response).balance mustBe Some(PaymentBalance(BigDecimal(-325.50), isMultiplePaymentDue = false, None))
      }

      "return a zero balance when nothing is owed" in {
        val response = UpstreamPaymentsResponse(outstanding = Seq.empty, totalAccountBalance = Some(BigDecimal(0)))

        service.toPayments(response).balance mustBe Some(PaymentBalance(BigDecimal(0), isMultiplePaymentDue = false, None))
      }

      "default to a zero balance when totalAccountBalance is absent" in {
        val response = UpstreamPaymentsResponse(outstanding = Seq.empty, totalAccountBalance = None)

        service.toPayments(response).balance mustBe Some(PaymentBalance(BigDecimal(0), isMultiplePaymentDue = false, None))
      }

      "ignore non-positive outstanding entries when counting distinct charge references" in {
        val response = UpstreamPaymentsResponse(
          outstanding = Seq(
            OutstandingPayment(Some("XVP111111111"), BigDecimal(5000), None, "Due"),
            OutstandingPayment(Some("XVP222222222"), BigDecimal(0), None, "Due")
          ),
          totalAccountBalance = Some(BigDecimal(5000))
        )

        service.toPayments(response).balance mustBe Some(PaymentBalance(BigDecimal(5000), isMultiplePaymentDue = false, Some("XVP111111111")))
      }

      "always report hasPaymentsError=false" in {
        service.toPayments(UpstreamPaymentsResponse(Seq.empty, None)).hasPaymentsError mustBe false
      }
    }
  }
}
