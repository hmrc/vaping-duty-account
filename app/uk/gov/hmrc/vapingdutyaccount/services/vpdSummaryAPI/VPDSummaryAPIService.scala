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

package uk.gov.hmrc.vapingdutyaccount.services.vpdSummaryAPI

import com.google.inject.Inject
import play.api.http.HttpVerbs
import play.api.i18n.Lang.logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.connectors.obligations.ObligationsConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.obligations.{ObligationDetails, ObligationsResponse}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI.*

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class VPDSummaryAPIService @Inject() (
  config: AppConfig,
  subscriptionConnector: SubscriptionConnector,
  obligationsConnector: ObligationsConnector
)(implicit ec: ExecutionContext) {

  private val STATUS_OPEN      = "O"
  private val STATUS_FULFILLED = "F"

  def getVPDSummary(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[VPDSummary] = {
    val subscriptionFuture  = subscriptionConnector.getSubscriptionContactPreferences(vpdId)
    val obligationsFuture   = obligationsConnector.getObligations(vpdId)

    for {
      subscription <- subscriptionFuture
      obligations  <- obligationsFuture
    } yield createVPDSummary(vpdId, subscription, obligations)
  }

  private def createVPDSummary(
    vpdId: VpdId,
    subscription: SubscriptionContactPreferences,
    obligations: Option[ObligationsResponse]
  ): VPDSummary = {
    val returns = obligations.flatMap(processObligations)
    val links   = buildLinks(vpdId, returns, obligations)

    VPDSummary(
      service = ServiceInfo(config.serviceName, config.serviceId),
      identifiers = Identifier(vpdId.toString),
      contactPreference = ContactMethod.resolve(paperlessPreference = subscription.paperlessPreference),
      returns = returns,
      links = links
    )
  }

  private def processObligations(obligations: ObligationsResponse): Option[Returns] = {
    val today = LocalDate.now()
    val allObligations = obligations.obligation.map(_.obligationDetails)

    val dueObligations       = allObligations.filter(isDue(_, today))
    val overdueObligations   = allObligations.filter(isOverdue(_, today))
    val completedObligations = allObligations.filter(isCompleted)

    val dueCount       = dueObligations.size
    val overdueCount   = overdueObligations.size
    val completedCount = completedObligations.size

    if (dueCount == 0 && overdueCount == 0 && completedCount == 0) {
      None
    } else {
      val currentReturn = if (dueCount == 1) {
        dueObligations.headOption.map(obligation =>
          CurrentReturn(
            periodKey = obligation.periodKey,
            dueDate = obligation.iCDueDate
          )
        )
      } else {
        None
      }

      Some(
        Returns(
          currentReturn = currentReturn,
          dueReturnsCount = dueCount,
          overdueReturnsCount = overdueCount,
          completedReturnsCount = completedCount
        )
      )
    }
  }

  private def isDue(obligation: ObligationDetails, today: LocalDate): Boolean =
    obligation.openOrFulfilledStatus == STATUS_OPEN && !obligation.iCDueDate.isBefore(today)

  private def isOverdue(obligation: ObligationDetails, today: LocalDate): Boolean =
    obligation.openOrFulfilledStatus == STATUS_OPEN && obligation.iCDueDate.isBefore(today)

  private def isCompleted(obligation: ObligationDetails): Boolean =
    obligation.openOrFulfilledStatus == STATUS_FULFILLED

  private def buildLinks(
    vpdId: VpdId,
    returns: Option[Returns],
    obligations: Option[ObligationsResponse]
  ): Links = {
    val self = Self(s"/vaping-duty-account/vpd/summary/$vpdId", HttpVerbs.GET)
    val manageContactPreference = ManageContactPreference(
      "/vaping-duty/contact-preferences/how-should-we-contact-you",
      HttpVerbs.GET
    )

    val (completeReturn, viewReturns) = returns match {
      case Some(r) =>
        val totalReturns = r.dueReturnsCount + r.overdueReturnsCount + r.completedReturnsCount

        // completeReturn: present when exactly 1 actionable return exists AND no overdue returns
        val completeReturnLink =
          if (r.dueReturnsCount == 1 && r.overdueReturnsCount == 0) {
            r.currentReturn.map(current =>
              CompleteReturn(
                s"${config.completeReturnUrlPrefix}?period=${current.periodKey}",
                HttpVerbs.GET
              )
            )
          } else if (r.overdueReturnsCount == 1 && r.dueReturnsCount == 0) {
            // Single overdue return also gets completeReturn link
            obligations.flatMap(_.obligation
              .map(_.obligationDetails)
              .find(isOverdue(_, LocalDate.now()))
              .map(obligation =>
                CompleteReturn(
                  s"${config.completeReturnUrlPrefix}?period=${obligation.periodKey}",
                  HttpVerbs.GET
                )
              ))
          } else {
            None
          }

        // viewReturns: present when multiple returns OR completed returns exist OR mixed due+overdue
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

    Links(
      self = self,
      manageContactPreference = manageContactPreference,
      completeReturn = completeReturn,
      viewReturns = viewReturns
    )
  }
}
