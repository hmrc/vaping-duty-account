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
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionDisplay
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
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
                                   returnsService            : VPDSummaryReturnsService,
                                   paymentsService           : VPDSummaryPaymentsService
)(implicit ec: ExecutionContext) extends Logging {

  def getVPDSummary(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[VPDSummary] = {

    val subscriptionDisplayFuture = subscriptionConnector.getSubscriptionDisplay(vpdId)
      .andThen {
        case Failure(ex) =>
          logger.warn(s"Failed to retrieve subscription contact preferences ${ex.getMessage}")
      }

    val obligationsFuture = obligationsConnector.getObligations(vpdId)
    val paymentsFuture = paymentsConnector.getPayments()

    for {
      subscriptionDisplay        <- subscriptionDisplayFuture
      contactPreferencesAndLinks  = contactPreferencesService.buildContactPreferencesAndLinks(subscriptionDisplay)
      isInsolvent                 = contactPreferencesAndLinks.isInsolvent
      returnsAndLinks            <- obligationsFuture
                                      .map(returnsService.buildReturnsAndLinks(_, isInsolvent))
                                      .recover { case ex => errorReturnsAndLinks(ex)}
      paymentsAndLinks           <- paymentsFuture
                                      .map(paymentsService.buildPaymentsAndLinks(_, isInsolvent))
                                      .recover { case ex => errorPaymentsAndLinks(ex) }
    } yield {
      VPDSummary(
        service                 = ServiceInfo(config.serviceName, config.serviceId),
        identifiers             = Identifier(vpdId.toString),
        access                  = contactPreferencesAndLinks.access,
        contactPreference       = contactPreferencesAndLinks.contactMethod,
        contactPreferenceStatus = contactPreferencesAndLinks.contactPreferenceStatus,
        returns                 = returnsAndLinks.returns,
        payments                = paymentsAndLinks.payments,
        links                   = Links(
          self                    = selfLink(vpdId),
          manageContactPreference = contactPreferencesAndLinks.manageContactPreferencesLink,
          completeReturn          = returnsAndLinks.completeReturnLink,
          viewReturns             = returnsAndLinks.viewReturnsLink,
          viewPayments            = paymentsAndLinks.viewPaymentsLink,
          makePayment             = paymentsAndLinks.makePaymentLink,
          claimRepayment          = paymentsAndLinks.claimRepaymentLink,
          setUpDirectDebit        = setupDirectDebitLink(isInsolvent)
        )
      )
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
      completeReturnLink = None,
      viewReturnsLink = None
    )
  }

  private def errorPaymentsAndLinks(ex: Throwable) = {
    logger.warn(s"Failed to retrieve payments ${ex.getMessage}")
    PaymentsAndLinks(
      payments = Some(Payments(hasPaymentsError = true, balance = None)),
      makePaymentLink = Some(MakePayment(config.makePaymentUrl(None), HttpVerbs.GET)),
      claimRepaymentLink = None,
      viewPaymentsLink = None
    )
  }

  private def selfLink(vpdId: VpdId) =
    Self(config.selfHref(vpdId), HttpVerbs.GET)

  private def setupDirectDebitLink(isInsolvent: Boolean) = {
    if (isInsolvent)
      None
    else
      Some(SetUpDirectDebit(config.startDirectDebitUrl, HttpVerbs.GET))
  }

}
