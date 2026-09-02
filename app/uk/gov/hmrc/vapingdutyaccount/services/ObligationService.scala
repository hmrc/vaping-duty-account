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

import uk.gov.hmrc.vapingdutyaccount.models.obligations.ObligationDetails
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{CurrentReturn, Returns}

import java.time.LocalDate
import javax.inject.Singleton

@Singleton
class ObligationService {

  private val STATUS_OPEN      = "O"
  private val STATUS_FULFILLED = "F"

  def processObligations(obligations: Seq[ObligationDetails]): Option[Returns] = {
    val today = LocalDate.now()

    val dueObligations       = obligations.filter(isDue(_, today))
    val overdueObligations   = obligations.filter(isOverdue(_, today))
    val completedObligations = obligations.filter(isCompleted)

    val outstandingObligations = dueObligations ++ overdueObligations

    val dueCount       = dueObligations.size
    val overdueCount   = overdueObligations.size
    val completedCount = completedObligations.size

    if (dueCount == 0 && overdueCount == 0 && completedCount == 0) {
      None
    } else {
      val currentReturn = if (outstandingObligations.length == 1) {
        outstandingObligations.headOption.map(o => CurrentReturn(periodKey = o.periodKey, dueDate = o.iCDueDate))
      } else {
        None
      }
      Some(Returns(
        hasReturnsError = false,
        currentReturn = currentReturn,
        dueReturnsCount = Some(dueCount),
        overdueReturnsCount = Some(overdueCount),
        completedReturnsCount = Some(completedCount)
      ))
    }
  }

  def isDue(obligation: ObligationDetails, today: LocalDate): Boolean =
    obligation.openOrFulfilledStatus == STATUS_OPEN && !obligation.iCDueDate.isBefore(today)

  def isOverdue(obligation: ObligationDetails, today: LocalDate): Boolean =
    obligation.openOrFulfilledStatus == STATUS_OPEN && obligation.iCDueDate.isBefore(today)

  def isCompleted(obligation: ObligationDetails): Boolean =
    obligation.openOrFulfilledStatus == STATUS_FULFILLED
}
