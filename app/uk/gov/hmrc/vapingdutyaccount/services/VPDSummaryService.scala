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
import play.api.i18n.Lang.logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.connectors.obligations.ObligationsConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.obligations.ObligationsResponse
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*

import java.time.{Clock, LocalDate}
import scala.concurrent.{ExecutionContext, Future}

class VPDSummaryService @Inject()(
                                   config: AppConfig,
                                   subscriptionConnector: SubscriptionConnector,
                                   obligationsConnector: ObligationsConnector,
                                   obligationService: ObligationService,
                                   clock: Clock
                                 )(implicit ec: ExecutionContext) {

  def getVPDSummary(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[VPDSummary] = {
    val contactPreferencesFuture = subscriptionConnector.getSubscriptionContactPreferences(vpdId)
    val obligationsFuture: Future[Option[ObligationsResponse]] =
      obligationsConnector.getObligations(vpdId)
        .map(Some(_))
        .recover {
          case ex =>
            logger.warn("Failed to retrieve obligations")
            None
        }

    for {
      contactPreferences <- contactPreferencesFuture
      obligations <- obligationsFuture
    } yield createVPDSummary(vpdId, contactPreferences, obligations)
  }

  private def createVPDSummary(
                                vpdId: VpdId,
                                contactPreferences: SubscriptionContactPreferences,
                                obligations: Option[ObligationsResponse]
                              ): VPDSummary = {
    val returns = obligations.flatMap(obligationService.processObligations)
    val links = buildLinks(vpdId, returns, obligations)

    VPDSummary(
      service = ServiceInfo(config.serviceName, config.serviceId),
      identifiers = Identifier(vpdId.toString),
      contactPreference = ContactMethod.resolve(paperlessPreference = contactPreferences.paperlessPreference),
      returns = returns,
      links = links
    )
  }

  private def buildLinks(
                          vpdId: VpdId,
                          returns: Option[Returns],
                          obligations: Option[ObligationsResponse]
                        ): Links = {
    val self = Self(config.selfHref(vpdId), HttpVerbs.GET)
    val manageContactPreference = ManageContactPreference(config.manageContactPreferenceUrl, HttpVerbs.GET)

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
              .find(obligationService.isOverdue(_, LocalDate.now(clock)))
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
