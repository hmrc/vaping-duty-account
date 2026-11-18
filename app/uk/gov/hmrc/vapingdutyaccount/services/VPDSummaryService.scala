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
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionSummary
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

/** Assembles the VPD Summary from its contact preference, returns and payments sections.
  *
  * The feature switches and the insolvency rule live here. Each section, and its links, is built by its own service.
  */
class VPDSummaryService @Inject()(
  config                          : AppConfig,
  subscriptionConnector           : SubscriptionConnector,
  contactPreferenceSummaryService : ContactPreferenceSummaryService,
  returnsSummaryService           : ReturnsSummaryService,
  paymentsSummaryService          : PaymentsSummaryService
)(implicit ec: ExecutionContext) extends Logging {

  def getVPDSummary(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[VPDSummary] = {
    // Start every call before waiting on any of them, so they run in parallel
    val subscriptionSummaryFuture = getSubscriptionSummary(vpdId)
    val returnsFuture             = whenReturnsAndPaymentsEnabled(returnsSummaryService.getReturnsSection(vpdId))
    val paymentsFuture            = whenReturnsAndPaymentsEnabled(paymentsSummaryService.getPaymentsSection())

    for {
      subscriptionSummary <- subscriptionSummaryFuture
      returns             <- returnsFuture
      payments            <- paymentsFuture
    } yield
      if (!config.phase2Enabled) phase1Summary(vpdId, subscriptionSummary)
      else phase2Summary(vpdId, subscriptionSummary, returns, payments)
  }

  private def getSubscriptionSummary(vpdId: VpdId)(using HeaderCarrier): Future[SubscriptionSummary] =
    subscriptionConnector.getSubscriptionSummary(vpdId).andThen { case Failure(ex) =>
      logger.warn(s"[VPDSummaryService][getSubscriptionSummary] Failed to retrieve subscription summary. Message: ${ex.getMessage}")
    }

  /** None means the section is switched off, not that it failed. Failures are reported inside each section. */
  private def whenReturnsAndPaymentsEnabled[A](section: => Future[A]): Future[Option[A]] =
    if (config.phase2Enabled && config.returnsAndPaymentsEnabled) section.map(Some(_))
    else Future.successful(None)

  private def phase1Summary(vpdId: VpdId, subscriptionSummary: SubscriptionSummary): VPDSummary = {
    val contactPreference = Option.unless(subscriptionSummary.isInsolvent)(contactPreferenceSummaryService.build(subscriptionSummary))

    VPDSummary(
      service           = serviceInfo,
      identifiers       = Identifier(vpdId.toString),
      contactPreference = contactPreference.map(_.contactMethod),
      links             = Links(self = selfLink(vpdId), manageContactPreference = contactPreference.map(_.manageContactPreference))
    )
  }

  private def phase2Summary(
    vpdId: VpdId,
    subscriptionSummary: SubscriptionSummary,
    returns: Option[ReturnsSection],
    payments: Option[PaymentsSection]
  ): VPDSummary = {
    val approvalStatus = AccessApprovalStatus.fromSubscription(subscriptionSummary)
    val access         = Some(Access(approvalStatus = Some(approvalStatus)))

    if (approvalStatus == AccessApprovalStatus.Insolvent)
      VPDSummary(
        service           = serviceInfo,
        identifiers       = Identifier(vpdId.toString),
        access            = access,
        contactPreference = None,
        links             = Links(self = selfLink(vpdId))
      )
    else {
      val contactPreference = contactPreferenceSummaryService.build(subscriptionSummary)

      VPDSummary(
        service                 = serviceInfo,
        identifiers             = Identifier(vpdId.toString),
        access                  = access,
        contactPreference       = Some(contactPreference.contactMethod),
        contactPreferenceStatus = contactPreference.contactPreferenceStatus,
        returns                 = returns.flatMap(_.returns),
        payments                = payments.map(_.payments),
        links                   = Links(
          self                    = selfLink(vpdId),
          manageContactPreference = Some(contactPreference.manageContactPreference),
          completeReturn          = returns.flatMap(_.completeReturn),
          viewReturns             = returns.flatMap(_.viewReturns),
          makePayment             = payments.flatMap(_.makePayment),
          viewPayments            = payments.flatMap(_.viewPayments),
          claimRepayment          = payments.flatMap(_.claimRepayment),
          setUpDirectDebit        = Some(SetUpDirectDebit(config.startDirectDebitUrl, HttpVerbs.GET))
        )
      )
    }
  }

  private def serviceInfo: ServiceInfo = ServiceInfo(config.serviceName, config.serviceId)

  private def selfLink(vpdId: VpdId): Self = Self(config.selfHref(vpdId), HttpVerbs.GET)
}
