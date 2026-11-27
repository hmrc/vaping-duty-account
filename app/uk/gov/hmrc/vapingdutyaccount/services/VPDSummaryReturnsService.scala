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

import play.api.http.HttpVerbs
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.obligations.Obligation
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.internal.ReturnsAndLinks
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{CompleteReturn, CurrentReturn, Returns, ViewReturns}

import java.time.LocalDate
import javax.inject.Inject

class VPDSummaryReturnsService @Inject (config: AppConfig) {

  def buildReturnsAndLinks(obligations: Seq[Obligation],
                           isInsolvent: Boolean): ReturnsAndLinks = {
    if (isInsolvent)
      ReturnsAndLinks(None, None, None)
    else {
      val returns = buildReturns(obligations)
      val (completeReturn, viewReturns) = buildReturnLinks(returns)

      ReturnsAndLinks(
        returns            = returns,
        completeReturnLink = completeReturn,
        viewReturnsLink    = viewReturns
      )
    }
  }

  def buildReturns(obligations: Seq[Obligation]): Option[Returns] = {
    val today = LocalDate.now()

    val dueObligations       = obligations.filter(_.isDue(today))
    val overdueObligations   = obligations.filter(_.isOverdue(today))
    val completedObligations = obligations.filter(_.isCompleted)

    val outstandingObligations = dueObligations ++ overdueObligations

    val dueCount       = dueObligations.size
    val overdueCount   = overdueObligations.size
    val completedCount = completedObligations.size

    if (dueCount == 0 && overdueCount == 0 && completedCount == 0) {
      None
    } else {
      Some(Returns(
        hasReturnsError       = false,
        currentReturn         = currentReturn(outstandingObligations),
        dueReturnsCount       = Some(dueCount),
        overdueReturnsCount   = Some(overdueCount),
        completedReturnsCount = Some(completedCount)
      ))
    }
  }

  private def currentReturn(outstandingObligations: Seq[Obligation]) =
    if (outstandingObligations.length == 1)
      outstandingObligations.headOption.map(o => CurrentReturn(periodKey = o.periodKey, dueDate = o.iCDueDate))
    else
      None


  private def buildReturnLinks(returns: Option[Returns]): (Option[CompleteReturn], Option[ViewReturns]) =
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

}
