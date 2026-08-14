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
    val obligationDetailsFuture  = getObligationsService.getObligationDetails(vpdId)
      .recover {
        case ex =>
          logger.warn(s"Failed to retrieve obligations ${ex.getMessage}")
          Seq.empty
      }
    val paymentsFuture = getPaymentsService.getPayments()

    for {
      contactPreferences <- contactPreferencesFuture
      obligationDetails  <- obligationDetailsFuture
      payments           <- paymentsFuture
    } yield createVPDSummary(vpdId, contactPreferences, obligationDetails, payments)
  }

  private def createVPDSummary(
                                vpdId: VpdId,
                                contactPreferences: Option[SubscriptionContactPreferences],
                                obligationDetails: Seq[ObligationDetails],
                                payments: Option[Payments]
  ): VPDSummary = {
    val returns = obligationService.processObligations(obligationDetails)
    val links   = buildLinks(vpdId, returns, obligationDetails, payments)

    val (contactMethod, contactPreferenceStatus) = contactPreferences match {
      case Some(contactPreferences) =>
        val cm = resolveContactMethod(contactPreferences)
        (Some(cm), resolveContactPreferenceStatus(cm, contactPreferences))
      case None                     =>
        (None, None)
    }

    val access =
      if (config.phase2Enabled)
        Some(contactPreferences match {
          case Some(contactPreferences) =>
            Access(hasSubscriptionSummaryError = false, approvalStatus = Some(AccessApprovalStatus.fromSubscription(contactPreferences)))
          case None                     =>
            Access(hasSubscriptionSummaryError = true)
        })
      else
        None

    VPDSummary(
      service                 = ServiceInfo(config.serviceName, config.serviceId),
      identifiers             = Identifier(vpdId.toString),
      access                  = access,
      contactPreference       = contactMethod,
      contactPreferenceStatus = contactPreferenceStatus,
      returns                 = returns,
      payments                = payments,
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
                          returns: Option[Returns],
                          obligationDetails: Seq[ObligationDetails],
                          payments: Option[Payments]
  ): Links = {
    val self                    = Self(config.selfHref(vpdId), HttpVerbs.GET)
    val manageContactPreference = ManageContactPreference(config.manageContactPreferenceUrl, HttpVerbs.GET)

    val (completeReturn, viewReturns) = returns match {
      case Some(r) =>
        val totalReturns = r.dueReturnsCount + r.overdueReturnsCount + r.completedReturnsCount

        val completeReturnLink =
          if (r.dueReturnsCount == 1 && r.overdueReturnsCount == 0) {
            r.currentReturn.map(current =>
              CompleteReturn(
                s"${config.completeReturnUrlPrefix}?period=${current.periodKey}",
                HttpVerbs.GET
              )
            )
          } else if (r.overdueReturnsCount == 1 && r.dueReturnsCount == 0) {
            obligationDetails
              .find(obligationService.isOverdue(_, LocalDate.now(clock)))
              .map(obligation =>
                CompleteReturn(
                  s"${config.completeReturnUrlPrefix}?period=${obligation.periodKey}",
                  HttpVerbs.GET
                )
              )
          } else {
            None
          }

        val viewReturnsLink =
          if (totalReturns > 1 || r.completedReturnsCount > 0 || (r.dueReturnsCount > 0 && r.overdueReturnsCount > 0)) {
            Some(ViewReturns(config.viewReturnsUrl, HttpVerbs.GET))
          } else {
            None
          }

        (completeReturnLink, viewReturnsLink)

      case None =>
        (None, None)
    }

    val makePayment = payments match {
      case Some(p) if p.hasPaymentsError || p.balance.exists(_.amount > 0) =>
        Some(MakePayment(config.makePaymentUrl, HttpVerbs.GET))
      case _ =>
        None
    }

    Links(
      self                    = self,
      manageContactPreference = manageContactPreference,
      completeReturn          = completeReturn,
      viewReturns             = viewReturns,
      makePayment             = makePayment
    )
  }
}
