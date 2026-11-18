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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.invocation.InvocationOnMock
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.payments.PaymentsConnector
import uk.gov.hmrc.vapingdutyaccount.models.payments.PaymentsResponse
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*

import scala.concurrent.Future

class PaymentsSummaryServiceSpec extends SpecBase {

  private val mockConfig: AppConfig                    = mock[AppConfig]
  private val mockPaymentsConnector: PaymentsConnector = mock[PaymentsConnector]

  when(mockConfig.makePaymentUrl(any())).thenAnswer((invocation: InvocationOnMock) =>
    makePaymentHref(invocation.getArgument[Option[String]](0))
  )
  when(mockConfig.claimRepaymentUrl).thenReturn(testClaimRepaymentUrl)
  when(mockConfig.viewPaymentsUrl).thenReturn(testViewPaymentsUrl)

  private val service = new PaymentsSummaryService(mockConfig, mockPaymentsConnector, new PaymentsService())

  private val viewPaymentsLink   = ViewPayments(testViewPaymentsUrl, httpGet)
  private val claimRepaymentLink = ClaimRepayment(testClaimRepaymentUrl, httpGet)

  private def givenUpstreamPayments(response: PaymentsResponse): Unit =
    when(mockPaymentsConnector.getPayments()(using any())).thenReturn(Future.successful(response))

  override def beforeEach(): Unit = reset(mockPaymentsConnector)

  "PaymentsSummaryService" - {
    "getPaymentsSection must" - {

      "return the balance, a makePayment link with the charge reference and a viewPayments link when one charge is outstanding" in {
        givenUpstreamPayments(upstreamPaymentsSingleCharge)

        service.getPaymentsSection().futureValue mustBe PaymentsSection(
          payments       = Payments(
            hasPaymentsError = false,
            balance          = Some(PaymentBalance(singleChargeAmount, isMultiplePaymentDue = false, Some(singleChargeReference)))
          ),
          makePayment    = Some(MakePayment(makePaymentHref(Some(singleChargeReference)), httpGet)),
          claimRepayment = None,
          viewPayments   = Some(viewPaymentsLink)
        )
      }

      "return isMultiplePaymentDue and a makePayment link with no charge reference when multiple charges are outstanding" in {
        givenUpstreamPayments(upstreamPaymentsMultipleCharges)

        service.getPaymentsSection().futureValue mustBe PaymentsSection(
          payments       = Payments(
            hasPaymentsError = false,
            balance          = Some(PaymentBalance(multipleChargesTotalAmount, isMultiplePaymentDue = true, None))
          ),
          makePayment    = Some(MakePayment(makePaymentHref(None), httpGet)),
          claimRepayment = None,
          viewPayments   = Some(viewPaymentsLink)
        )
      }

      "return a negative balance and a claimRepayment link, but no makePayment link, when the account is in credit" in {
        givenUpstreamPayments(upstreamPaymentsInCredit)

        service.getPaymentsSection().futureValue mustBe PaymentsSection(
          payments       = Payments(
            hasPaymentsError = false,
            balance          = Some(PaymentBalance(creditBalanceAmount, isMultiplePaymentDue = false, None))
          ),
          makePayment    = None,
          claimRepayment = Some(claimRepaymentLink),
          viewPayments   = Some(viewPaymentsLink)
        )
      }

      "return a zero balance and only a viewPayments link when nothing is owed but there is financial history" in {
        givenUpstreamPayments(upstreamPaymentsNothingOwed)

        service.getPaymentsSection().futureValue mustBe PaymentsSection(
          payments       = Payments(
            hasPaymentsError = false,
            balance          = Some(PaymentBalance(zeroBalanceAmount, isMultiplePaymentDue = false, None))
          ),
          makePayment    = None,
          claimRepayment = None,
          viewPayments   = Some(viewPaymentsLink)
        )
      }

      "return a viewPayments link when there is financial data but no total account balance" in {
        givenUpstreamPayments(upstreamPaymentsNoBalance)

        val result = service.getPaymentsSection().futureValue

        result.viewPayments mustBe Some(viewPaymentsLink)
        result.makePayment mustBe None
        result.claimRepayment mustBe None
      }

      "return a zero balance and no links when there is no financial data yet" in {
        givenUpstreamPayments(upstreamPaymentsNoFinancialData)

        service.getPaymentsSection().futureValue mustBe PaymentsSection(
          payments       = Payments(
            hasPaymentsError = false,
            balance          = Some(PaymentBalance(zeroBalanceAmount, isMultiplePaymentDue = false, None))
          ),
          makePayment    = None,
          claimRepayment = None,
          viewPayments   = None
        )
      }

      "return hasPaymentsError, a generic makePayment link and no viewPayments link when the payments call fails" in {
        when(mockPaymentsConnector.getPayments()(using any()))
          .thenReturn(Future.failed(new InternalServerException("Failed to retrieve payments")))

        service.getPaymentsSection().futureValue mustBe PaymentsSection(
          payments       = Payments(hasPaymentsError = true, balance = None),
          makePayment    = Some(MakePayment(makePaymentHref(None), httpGet)),
          claimRepayment = None,
          viewPayments   = None
        )
      }
    }
  }
}
