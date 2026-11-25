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
import play.api.Logging
import play.api.http.HttpVerbs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.connectors.obligations.ObligationsConnector
import uk.gov.hmrc.vapingdutyaccount.connectors.payments.PaymentsConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.obligations.Obligation
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.internal.{PaymentsAndLinks, ReturnsAndLinks}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class VPDSummaryService @Inject()(
                                   config                    : AppConfig,
                                   subscriptionConnector     : SubscriptionConnector,
                                   paymentsConnector         : PaymentsConnector,
                                   obligationsConnector      : ObligationsConnector,
                                   contactPreferencesService : VPDSummaryContactPreferencesService,
                                   vpdSummaryReturnsService  : VPDSummaryReturnsService,
                                   vpdSummaryPaymentsService : VPDSummaryPaymentsService
)(implicit ec: ExecutionContext) extends Logging {

  def getVPDSummary(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[VPDSummary] = {

    val contactPreferencesFuture: Future[SubscriptionContactPreferences] =
      subscriptionConnector.getSubscriptionContactPreferences(vpdId).andThen {
        case Failure(ex) =>
          logger.warn(s"Failed to retrieve subscription contact preferences ${ex.getMessage}")
      }

    val obligationsFuture = obligationsConnector.getObligations(vpdId)

    val returnsAndLinksFuture: Future[ReturnsAndLinks] =
      obligationsFuture
        .map(vpdSummaryReturnsService.buildReturnsAndLinks)
        .recover { case ex => errorReturnsAndLinks(ex)}

    val paymentsFuture = paymentsConnector.getPayments()

    val paymentsAndLinksFuture: Future[PaymentsAndLinks] =
      paymentsFuture
        .map(vpdSummaryPaymentsService.buildPaymentsAndLinks)
        .recover { case ex => errorPaymentsAndLinks(ex) }

    for {
      contactPreferences <- contactPreferencesFuture
      contactPreferencesAndLinks = contactPreferencesService.buildContactPreferencesAndLinks(contactPreferences)
      returnsAndLinks    <- returnsAndLinksFuture
      paymentsAndLinks   <- paymentsAndLinksFuture
    } yield {
      {
        val links = {
          val self = selfLink(vpdId)

          if (contactPreferencesAndLinks.isInsolvent) {
            Links(self = self)
          } else {
            {
              Links(
                self                    = self,
                manageContactPreference = contactPreferencesAndLinks.manageContactPreferencesLink,
                completeReturn          = returnsAndLinks.completeReturn,
                viewReturns             = returnsAndLinks.viewReturns,
                viewPayments            = paymentsAndLinks.viewPaymentsLink,
                makePayment             = paymentsAndLinks.makePaymentLink,
                claimRepayment          = paymentsAndLinks.claimRepaymentLink,
                setUpDirectDebit        = setupDirectDebitLink
              )
            }
          }
        }

        VPDSummary(
          service                 = ServiceInfo(config.serviceName, config.serviceId),
          identifiers             = Identifier(vpdId.toString),
          access                  = contactPreferencesAndLinks.access,
          contactPreference       = contactPreferencesAndLinks.contactMethod,
          contactPreferenceStatus = contactPreferencesAndLinks.contactPreferenceStatus,
          returns                 = if (contactPreferencesAndLinks.isInsolvent) None else returnsAndLinks.returns,
          payments                = if (contactPreferencesAndLinks.isInsolvent) None else Some(paymentsAndLinks.payments),
          links                   = links
        )
      }
    }
  }

  private def errorReturnsAndLinks(ex: Throwable) = {
    logger.warn(s"Failed to retrieve obligations ${ex.getMessage}")
    ReturnsAndLinks(
      returns = Some(Returns(
        hasReturnsError = true,
        currentReturn = None,
        dueReturnsCount = None,
        overdueReturnsCount = None,
        completedReturnsCount = None
      )),
      completeReturn = None,
      viewReturns = None
    )
  }

  private def errorPaymentsAndLinks(ex: Throwable) = {
    logger.warn(s"Failed to retrieve payments ${ex.getMessage}")
    PaymentsAndLinks(
      payments = Payments(hasPaymentsError = true, balance = None),
      financialDataStatus = FinancialDataStatus.NoFinancialData,
      makePaymentLink = Some(MakePayment(config.makePaymentUrl(None), HttpVerbs.GET)),
      claimRepaymentLink = None,
      viewPaymentsLink = None
    )
  }

  private def selfLink(vpdId: VpdId) =
    Self(config.selfHref(vpdId), HttpVerbs.GET)

  private def setupDirectDebitLink =
    Some(SetUpDirectDebit(config.startDirectDebitUrl, HttpVerbs.GET))

}
