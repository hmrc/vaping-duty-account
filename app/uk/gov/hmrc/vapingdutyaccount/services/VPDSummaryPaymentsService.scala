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
import uk.gov.hmrc.vapingdutyaccount.models.payments.PaymentsResponse as UpstreamPaymentsResponse
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.FinancialDataStatus.{HasFinancialData, NoFinancialData}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.internal.PaymentsAndLinks
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{ClaimRepayment, FinancialDataStatus, MakePayment, PaymentBalance, Payments, ViewPayments}

import javax.inject.Singleton

@Singleton
class VPDSummaryPaymentsService @Inject(config: AppConfig) {

  def buildPaymentsAndLinks(response: UpstreamPaymentsResponse,
                            isInsolvent: Boolean): PaymentsAndLinks = {

    val (payments, financialDataStatus) = toPayments(response)
    val (makePayment, claimRepayment) = buildPaymentLinks(payments)
    val viewPaymentsLink = buildViewPaymentsLink(financialDataStatus)

    PaymentsAndLinks(
      payments = if (isInsolvent) None else Some(payments),
      financialDataStatus = financialDataStatus,
      makePaymentLink = makePayment,
      claimRepaymentLink = claimRepayment,
      viewPaymentsLink = viewPaymentsLink
    )
  }

  def toPayments(response: UpstreamPaymentsResponse): (Payments, FinancialDataStatus) = {
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

    val financialDataStatus = 
      if (response.outstanding.isEmpty && response.paymentOnAccount.isEmpty && response.cleared.isEmpty)
        FinancialDataStatus.NoFinancialData
      else
        FinancialDataStatus.HasFinancialData

    val payments = Payments(hasPaymentsError = false, balance = Some(balance))
    
    (payments, financialDataStatus)
  }

  private def buildPaymentLinks(payments: Payments): (Option[MakePayment], Option[ClaimRepayment]) =
    if (payments.hasPaymentsError)
      (Some(MakePayment(config.makePaymentUrl(None), HttpVerbs.GET)), None)
    else if (payments.balance.exists(_.amount > 0)) {
      val singleChargeRef = payments.balance.filterNot(_.isMultiplePaymentDue).flatMap(_.chargeReference)
      (Some(MakePayment(config.makePaymentUrl(singleChargeRef), HttpVerbs.GET)), None)
    }
    else if (payments.balance.exists(_.amount < 0))
      (None, Some(ClaimRepayment(config.claimRepaymentUrl, HttpVerbs.GET)))
    else
      (None, None)

  private def buildViewPaymentsLink(financialDataStatus: FinancialDataStatus): Option[ViewPayments] =
    financialDataStatus match {
      case HasFinancialData => Some(ViewPayments(config.viewPaymentsUrl, HttpVerbs.GET))
      case NoFinancialData => None
    }

}