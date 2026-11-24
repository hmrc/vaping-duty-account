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
import uk.gov.hmrc.vapingdutyaccount.connectors.payments.PaymentsConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.obligations.Obligation
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.FinancialDataStatus.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class VPDSummaryService @Inject()(
                                   config                : AppConfig,
                                   subscriptionConnector : SubscriptionConnector,
                                   paymentsConnector     : PaymentsConnector,
                                   getObligationsService : GetObligationsService,
                                   obligationService     : ObligationService,
                                   paymentsService       : PaymentsService
)(implicit ec: ExecutionContext) extends Logging {

  def getVPDSummary(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[VPDSummary] = {

    val contactPreferencesFuture: Future[SubscriptionContactPreferences] =
      subscriptionConnector.getSubscriptionContactPreferences(vpdId).andThen {
        case Failure(ex) =>
          logger.warn(s"Failed to retrieve subscription contact preferences ${ex.getMessage}")
      }

    val returnsFuture: Future[Option[Returns]] =
      getObligationsService.getObligationDetails(vpdId)
        .map(obligationService.processObligations)
        .recover {
          case ex =>
            logger.warn(s"Failed to retrieve obligations ${ex.getMessage}")
            Some(Returns(
              hasReturnsError = true,
              currentReturn = None,
              dueReturnsCount = None,
              overdueReturnsCount = None,
              completedReturnsCount = None
            ))
        }

    val paymentsFuture: Future[(Payments, FinancialDataStatus)] =
      paymentsConnector.getPayments()
        .map(paymentsService.toPayments)
        .recover {
          case ex =>
            logger.warn(s"Failed to retrieve payments ${ex.getMessage}")
            (Payments(hasPaymentsError = true, balance = None), FinancialDataStatus.NoFinancialData)
        }

    for {
      contactPreferences              <- contactPreferencesFuture
      returns                         <- returnsFuture
      (completeReturn, viewReturns)    = obligationService.buildReturnLinks(returns)
      (payments, financialDataStatus) <- paymentsFuture
      (makePayment, claimRepayment)    = buildPaymentLinks(payments)
    } yield {
      {
        val approvalStatus = AccessApprovalStatus.fromSubscription(contactPreferences)
        val isInsolvent = approvalStatus == AccessApprovalStatus.Insolvent

        val links = {
          val self = selfLink(vpdId)

          if (isInsolvent) {
            Links(self = self)
          } else {
            {
              Links(
                self                    = self,
                manageContactPreference = manageContactPreferencesLink,
                completeReturn          = completeReturn,
                viewReturns             = viewReturns,
                viewPayments            = buildViewPaymentsLink(financialDataStatus),
                makePayment             = makePayment,
                claimRepayment          = claimRepayment,
                setUpDirectDebit        = setupDirectDebitLink
              )
            }
          }
        }

        val (contactMethod, contactPreferenceStatus) =
          if (isInsolvent) (None, None)
          else {
            val cm = resolveContactMethod(contactPreferences)
            (Some(cm), resolveContactPreferenceStatus(cm, contactPreferences))
          }

        VPDSummary(
          service                 = ServiceInfo(config.serviceName, config.serviceId),
          identifiers             = Identifier(vpdId.toString),
          access                  = Some(Access(approvalStatus = Some(approvalStatus))),
          contactPreference       = contactMethod,
          contactPreferenceStatus = contactPreferenceStatus,
          returns                 = if (isInsolvent) None else returns,
          payments                = if (isInsolvent) None else Some(payments),
          links                   = links
        )
      }
    }
  }

  private def resolveContactMethod(contactPreferences: SubscriptionContactPreferences): ContactMethod =
    if (contactPreferences.paperlessPreference) ContactMethod.Email else ContactMethod.Post

  private def resolveContactPreferenceStatus(
                                              contactMethod: ContactMethod,
                                              contactPreferences: SubscriptionContactPreferences
  ): Option[ContactPreferenceStatus] =
    if (contactMethod == ContactMethod.Email)
      Some(ContactPreferenceStatus(contactPreferences.bouncedEmail.getOrElse(false)))
    else
      None

  private def selfLink(vpdId: VpdId) =
    Self(config.selfHref(vpdId), HttpVerbs.GET)

  private def manageContactPreferencesLink =
    Some(ManageContactPreference(config.manageContactPreferenceUrl, HttpVerbs.GET))

  private def setupDirectDebitLink =
    Some(SetUpDirectDebit(config.startDirectDebitUrl, HttpVerbs.GET))

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
      case NoFinancialData  => None
    }
}
