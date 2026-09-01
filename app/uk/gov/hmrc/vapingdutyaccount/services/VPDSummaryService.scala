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

import java.time.{Clock, LocalDate}
import scala.concurrent.{ExecutionContext, Future}

class VPDSummaryService @Inject()(
                                   config: AppConfig,
                                   subscriptionConnector: SubscriptionConnector,
                                   getObligationsService: GetObligationsService,
                                   obligationService: ObligationService,
                                   getPaymentsService: GetPaymentsService,
                                   clock: Clock
)(implicit ec: ExecutionContext) extends Logging {

  def getVPDSummary(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[VPDSummary] = {
    val contactPreferencesFuture: Future[Option[SubscriptionContactPreferences]] =
      subscriptionConnector.getSubscriptionContactPreferences(vpdId).map(Some(_)).recover {
        case ex =>
          logger.warn(s"Failed to retrieve subscription contact preferences ${ex.getMessage}")
          None
      }
    val obligationDetailsFuture: Future[Seq[ObligationDetails]] =
      getObligationsService.getObligationDetails(vpdId)

    val returnsFuture: Future[Option[Returns]] =
      obligationDetailsFuture
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

    val safeObligationDetailsFuture: Future[Seq[ObligationDetails]] =
      obligationDetailsFuture.recover { case _ => Seq.empty }

    val paymentsFuture = getPaymentsService.getPayments()

    for {
      contactPreferences <- contactPreferencesFuture
      returns            <- returnsFuture
      obligationDetails  <- safeObligationDetailsFuture
      payments           <- paymentsFuture
    } yield createVPDSummary(vpdId, contactPreferences, returns, obligationDetails, payments)
  }

  private def createVPDSummary(
                                vpdId: VpdId,
                                contactPreferences: Option[SubscriptionContactPreferences],
                                returns: Option[Returns],
                                obligationDetails: Seq[ObligationDetails],
                                payments: Option[Payments]
  ): VPDSummary = {
    val hasSubscriptionSummaryError = contactPreferences.isEmpty
    val resolvedStatus              = contactPreferences.map(AccessApprovalStatus.fromSubscription)
    val isNoAccess                  = resolvedStatus.contains(AccessApprovalStatus.Insolvent)

    val links = buildLinks(vpdId, isNoAccess, hasSubscriptionSummaryError, returns, obligationDetails, payments)

    val (contactMethod, contactPreferenceStatus) =
      if (isNoAccess) (None, None)
      else
        contactPreferences match {
          case Some(contactPreferences) =>
            val cm = resolveContactMethod(contactPreferences)
            (Some(cm), resolveContactPreferenceStatus(cm, contactPreferences))
          case None                     =>
            (None, None)
        }

    val access =
      if (config.phase2Enabled)
        Some(
          if (hasSubscriptionSummaryError) Access(hasSubscriptionSummaryError = true)
          else Access(hasSubscriptionSummaryError = false, approvalStatus = resolvedStatus)
        )
      else
        None

    VPDSummary(
      service                 = ServiceInfo(config.serviceName, config.serviceId),
      identifiers             = Identifier(vpdId.toString),
      access                  = access,
      contactPreference       = contactMethod,
      contactPreferenceStatus = contactPreferenceStatus,
      returns                 = if (isNoAccess || hasSubscriptionSummaryError) None else returns,
      payments                = if (isNoAccess || hasSubscriptionSummaryError) None else payments,
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
    else None

  private def buildLinks(
                          vpdId: VpdId,
                          isNoAccess: Boolean,
                          hasSubscriptionSummaryError: Boolean,
                          returns: Option[Returns],
                          obligationDetails: Seq[ObligationDetails],
                          payments: Option[Payments]
  ): Links = {
    val self = Self(config.selfHref(vpdId), HttpVerbs.GET)

    if (isNoAccess) {
      Links(self = self)
    } else if (hasSubscriptionSummaryError) {
      Links(
        self             = self,
        makePayment      = Some(MakePayment(config.makePaymentUrl, HttpVerbs.GET)),
        setUpDirectDebit = Some(SetUpDirectDebit(config.startDirectDebitUrl, HttpVerbs.GET))
      )
    } else {
      buildFullAccessLinks(self, returns, obligationDetails, payments)
    }
  }

  private def buildFullAccessLinks(
                                    self: Self,
                                    returns: Option[Returns],
                                    obligationDetails: Seq[ObligationDetails],
                                    payments: Option[Payments]
  ): Links = {
    val (completeReturn, viewReturns) = buildReturnLinks(returns, obligationDetails)

    Links(
      self                    = self,
      manageContactPreference = Some(ManageContactPreference(config.manageContactPreferenceUrl, HttpVerbs.GET)),
      completeReturn          = completeReturn,
      viewReturns             = viewReturns,
      makePayment             = buildMakePaymentLink(payments),
      setUpDirectDebit        = Some(SetUpDirectDebit(config.startDirectDebitUrl, HttpVerbs.GET))
    )
  }

  private def buildReturnLinks(
                                returns: Option[Returns],
                                obligationDetails: Seq[ObligationDetails]
  ): (Option[CompleteReturn], Option[ViewReturns]) =
    returns match {
      case Some(r) =>
        (completeReturnLink(obligationDetails, r), viewReturnsLink(r))
      case None =>
        (None, None)
    }

  private def completeReturnLink(obligationDetails: Seq[ObligationDetails], returns: Returns): Option[CompleteReturn] =
    if (returns.dueReturnsCount.contains(1) && returns.overdueReturnsCount.contains(0)) {
      completeSingleReturnLink(returns)
    } else if (returns.dueReturnsCount.contains(0) && returns.overdueReturnsCount.contains(1)) {
      completeSingleReturnLink(returns)
    } else {
      None
    }

  private def completeSingleReturnLink(returns: Returns): Option[CompleteReturn] =
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
    val totalReturns  = due + overdue + completed
    if (totalReturns > 1 || completed > 0 || (due > 0 && overdue > 0)) {
      Some(ViewReturns(config.viewReturnsUrl, HttpVerbs.GET))
    } else {
      None
    }
  }

  private def buildMakePaymentLink(payments: Option[Payments]): Option[MakePayment] =
    payments match {
      case Some(p) if p.hasPaymentsError || p.balance.exists(_.amount > 0) =>
        Some(MakePayment(config.makePaymentUrl, HttpVerbs.GET))
      case _ =>
        None
    }
}
