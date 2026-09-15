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
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.obligations.ObligationDetails
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.FinancialDataStatus.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class VPDSummaryService @Inject()(
                                   config                : AppConfig,
                                   subscriptionConnector : SubscriptionConnector,
                                   getObligationsService : GetObligationsService,
                                   obligationService     : ObligationService,
                                   getPaymentsService    : GetPaymentsService
)(implicit ec: ExecutionContext) extends Logging {

  def getVPDSummary(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[VPDSummary] = {

    val contactPreferencesFuture: Future[SubscriptionContactPreferences] =
      subscriptionConnector.getSubscriptionContactPreferences(vpdId).andThen {
        case Failure(ex) =>
          logger.warn(s"Failed to retrieve subscription contact preferences ${ex.getMessage}")
      }

    if (!config.phase2Enabled)
      contactPreferencesFuture.map(contactPreferences => {
        val (contactMethod, manageContactPreferenceLink) =
          if (contactPreferences.isInsolvent)
            (None, None)
          else
            (Some(resolveContactMethod(contactPreferences)), manageContactPreferencesLink)

        VPDSummary(
          service           = ServiceInfo(config.serviceName, config.serviceId),
          identifiers       = Identifier(vpdId.toString),
          contactPreference = contactMethod,
          links             = Links(self = selfLink(vpdId), manageContactPreference = manageContactPreferenceLink)
        )
      })
    else {

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

      val paymentsFuture = getPaymentsService.getPayments()

      for {
        contactPreferences <- contactPreferencesFuture
        returns            <- returnsFuture
        paymentsWithFlag   <- paymentsFuture
      } yield {
        val (payments, financialDataStatus) = paymentsWithFlag match {
          case Some((p, status)) => (Some(p), status)
          case None              => (None, NoFinancialData)
        }
        createVPDSummary(vpdId, contactPreferences, returns, payments, financialDataStatus)
      }
    }
  }

  private def createVPDSummary(
                                vpdId: VpdId,
                                contactPreferences: SubscriptionContactPreferences,
                                returns: Option[Returns],
                                payments: Option[Payments],
                                financialDataStatus: FinancialDataStatus
  ): VPDSummary = {
    val approvalStatus  = AccessApprovalStatus.fromSubscription(contactPreferences)
    val isNoAccess      = approvalStatus == AccessApprovalStatus.Insolvent

    val links = buildLinks(vpdId, isNoAccess, returns, payments, financialDataStatus)

    val (contactMethod, contactPreferenceStatus) =
      if (isNoAccess) (None, None)
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
      returns                 = if (isNoAccess) None else returns,
      payments                = if (isNoAccess) None else payments,
      links                   = links
    )
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

  private def buildLinks(
                          vpdId: VpdId,
                          isNoAccess: Boolean,
                          returns: Option[Returns],
                          payments: Option[Payments],
                          financialDataStatus: FinancialDataStatus
  ): Links = {
    val self = selfLink(vpdId)

    if (isNoAccess) {
      Links(self = self)
    } else {
      buildFullAccessLinks(self, returns, payments, financialDataStatus)
    }
  }

  private def buildFullAccessLinks(
                                    self: Self,
                                    returns: Option[Returns],
                                    payments: Option[Payments],
                                    financialDataStatus: FinancialDataStatus
  ): Links = {
    val (completeReturn, viewReturns) = buildReturnLinks(returns)
    val (makePayment, claimRepayment) = buildPaymentLinks(payments)

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

  private def selfLink(vpdId: VpdId) =
    Self(config.selfHref(vpdId), HttpVerbs.GET)

  private def manageContactPreferencesLink =
    Some(ManageContactPreference(config.manageContactPreferenceUrl, HttpVerbs.GET))

  private def setupDirectDebitLink =
    Some(SetUpDirectDebit(config.startDirectDebitUrl, HttpVerbs.GET))

  private def buildReturnLinks(returns: Option[Returns]) =
    returns match {
      case Some(r) =>
        (completeReturnLink(r), viewReturnsLink(r))
      case None =>
        (None, None)
    }

  private def completeReturnLink(returns: Returns) =
    returns.currentReturn.map(current => completeReturnForPeriod(current.periodKey))

  private def completeReturnForPeriod(periodKey: String): CompleteReturn =
    CompleteReturn(
      s"${config.completeReturnUrlPrefix}?period=$periodKey",
      HttpVerbs.GET
    )

  private def viewReturnsLink(r: Returns): Option[ViewReturns] = {
    val due           = r.dueReturnsCount.getOrElse(0)
    val overdue       = r.overdueReturnsCount.getOrElse(0)
    val completed     = r.completedReturnsCount.getOrElse(0)

    val openReturns  = due + overdue

    if (openReturns > 1 || completed > 0)
      Some(ViewReturns(config.viewReturnsUrl, HttpVerbs.GET))
    else
      None
  }

  private def buildPaymentLinks(payments: Option[Payments]): (Option[MakePayment], Option[ClaimRepayment]) =
    payments match {
      case Some(p) if p.hasPaymentsError =>
        (Some(MakePayment(config.makePaymentUrl(None), HttpVerbs.GET)), None)
      case Some(p) if p.balance.exists(_.amount > 0) =>
        val singleChargeRef = p.balance.filterNot(_.isMultiplePaymentDue).flatMap(_.chargeReference)
        (Some(MakePayment(config.makePaymentUrl(singleChargeRef), HttpVerbs.GET)), None)
      case Some(p) if p.balance.exists(_.amount < 0) =>
        (None, Some(ClaimRepayment(config.claimRepaymentUrl, HttpVerbs.GET)))
      case _ =>
        (None, None)
    }

  private def buildViewPaymentsLink(financialDataStatus: FinancialDataStatus): Option[ViewPayments] =
    financialDataStatus match {
      case HasFinancialData => Some(ViewPayments(config.viewPaymentsUrl, HttpVerbs.GET))
      case NoFinancialData  => None
    }
}
