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

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.HttpVerbs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.payments.PaymentsConnector
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.FinancialDataStatus.*

import scala.concurrent.{ExecutionContext, Future}

/** Builds the payments section of the VPD Summary, and its links, from the manufacturer's financial data. */
@Singleton
class PaymentsSummaryService @Inject()(
  config: AppConfig,
  paymentsConnector: PaymentsConnector,
  paymentsService: PaymentsService
)(implicit ec: ExecutionContext) extends Logging {

  private val ZERO_BALANCE = BigDecimal(0)

  def getPaymentsSection()(using HeaderCarrier): Future[PaymentsSection] =
    paymentsConnector.getPayments()
      .map(paymentsService.toPayments)
      .map((payments, financialDataStatus) => buildSection(payments, financialDataStatus))
      .recover { case ex =>
        logger.warn(s"[PaymentsSummaryService][getPaymentsSection] Failed to retrieve payments. Message: ${ex.getMessage}")
        buildSection(PaymentsSection.errorPayments, NoFinancialData)
      }

  private def buildSection(payments: Payments, financialDataStatus: FinancialDataStatus): PaymentsSection = {
    val (makePayment, claimRepayment) = paymentLinks(payments)

    PaymentsSection(
      payments       = payments,
      makePayment    = makePayment,
      claimRepayment = claimRepayment,
      viewPayments   = viewPaymentsLink(financialDataStatus)
    )
  }

  private def paymentLinks(payments: Payments): (Option[MakePayment], Option[ClaimRepayment]) =
    payments match {
      case p if p.hasPaymentsError =>
        (Some(MakePayment(config.makePaymentUrl(None), HttpVerbs.GET)), None)
      case p if p.balance.exists(_.amount > ZERO_BALANCE) =>
        val singleChargeRef = p.balance.filterNot(_.isMultiplePaymentDue).flatMap(_.chargeReference)
        (Some(MakePayment(config.makePaymentUrl(singleChargeRef), HttpVerbs.GET)), None)
      case p if p.balance.exists(_.amount < ZERO_BALANCE) =>
        (None, Some(ClaimRepayment(config.claimRepaymentUrl, HttpVerbs.GET)))
      case _ =>
        (None, None)
    }

  private def viewPaymentsLink(financialDataStatus: FinancialDataStatus): Option[ViewPayments] =
    financialDataStatus match {
      case HasFinancialData => Some(ViewPayments(config.viewPaymentsUrl, HttpVerbs.GET))
      case NoFinancialData  => None
    }
}
