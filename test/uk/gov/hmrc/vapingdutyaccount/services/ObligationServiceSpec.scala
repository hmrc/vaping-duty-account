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

import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.models.obligations.{ObligationDetails, ObligationItem, ObligationsResponse}

import java.time.LocalDate

class ObligationServiceSpec extends SpecBase {

  val service = new ObligationService()

  private def obligation(status: String, dueDate: LocalDate, periodKey: String): ObligationItem =
    ObligationItem(
      identification = None,
      obligationDetails = ObligationDetails(
        openOrFulfilledStatus = status,
        iCFromDate = dueDate.minusMonths(3),
        iCToDate = dueDate.minusMonths(1),
        iCDateReceived = None,
        iCDueDate = dueDate,
        periodKey = periodKey
      )
    )

  "ObligationService" - {

    "processObligations must" - {

      "return None when obligations list is empty" in {
        service.processObligations(ObligationsResponse(Seq.empty)) mustBe None
      }

      "return None when all obligations have an unrecognised status" in {
        val obs = ObligationsResponse(Seq(obligation("X", LocalDate.now().plusDays(5), "26AA")))
        service.processObligations(obs) mustBe None
      }

      "return Some(Returns) with dueCount=1 and currentReturn when single due obligation" in {
        val dueDate = LocalDate.now().plusDays(10)
        val result  = service.processObligations(ObligationsResponse(Seq(obligation("O", dueDate, "26AA")))).value

        result.dueReturnsCount mustBe 1
        result.overdueReturnsCount mustBe 0
        result.completedReturnsCount mustBe 0
        result.currentReturn mustBe defined
        result.currentReturn.get.periodKey mustBe "26AA"
        result.currentReturn.get.dueDate mustBe dueDate
      }

      "return Some(Returns) with no currentReturn when multiple due obligations" in {
        val obs = ObligationsResponse(Seq(
          obligation("O", LocalDate.now().plusDays(5), "26AA"),
          obligation("O", LocalDate.now().plusDays(10), "26AB")
        ))
        val result = service.processObligations(obs).value

        result.dueReturnsCount mustBe 2
        result.currentReturn mustBe None
      }

      "return Some(Returns) with overdueCount=1 and no currentReturn when single overdue obligation" in {
        val result = service.processObligations(
          ObligationsResponse(Seq(obligation("O", LocalDate.now().minusDays(5), "25AL")))
        ).value

        result.dueReturnsCount mustBe 0
        result.overdueReturnsCount mustBe 1
        result.completedReturnsCount mustBe 0
        result.currentReturn mustBe None
      }

      "return Some(Returns) with completedCount=1 when single completed obligation" in {
        val result = service.processObligations(
          ObligationsResponse(Seq(obligation("F", LocalDate.now().minusDays(5), "25AL")))
        ).value

        result.dueReturnsCount mustBe 0
        result.overdueReturnsCount mustBe 0
        result.completedReturnsCount mustBe 1
        result.currentReturn mustBe None
      }

      "return correct counts for mixed due, overdue, and completed obligations" in {
        val obs = ObligationsResponse(Seq(
          obligation("O", LocalDate.now().plusDays(10), "26AA"),
          obligation("O", LocalDate.now().minusDays(5), "25AL"),
          obligation("F", LocalDate.now().minusDays(30), "25AK")
        ))
        val result = service.processObligations(obs).value

        result.dueReturnsCount mustBe 1
        result.overdueReturnsCount mustBe 1
        result.completedReturnsCount mustBe 1
      }
    }

    "isDue must" - {

      val today = LocalDate.now()

      "return true when status is O and due date is today" in {
        service.isDue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today), today) mustBe true
      }

      "return true when status is O and due date is in the future" in {
        service.isDue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today.plusDays(1)), today) mustBe true
      }

      "return false when status is O and due date is in the past" in {
        service.isDue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today.minusDays(1)), today) mustBe false
      }

      "return false when status is F regardless of due date" in {
        service.isDue(obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = today.plusDays(1)), today) mustBe false
      }
    }

    "isOverdue must" - {

      val today = LocalDate.now()

      "return true when status is O and due date is in the past" in {
        service.isOverdue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today.minusDays(1)), today) mustBe true
      }

      "return false when status is O and due date is today" in {
        service.isOverdue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today), today) mustBe false
      }

      "return false when status is O and due date is in the future" in {
        service.isOverdue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today.plusDays(1)), today) mustBe false
      }

      "return false when status is F regardless of due date" in {
        service.isOverdue(obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = today.minusDays(1)), today) mustBe false
      }
    }

    "isCompleted must" - {

      "return true when status is F" in {
        service.isCompleted(obligationDetailsCompleted) mustBe true
      }

      "return false when status is O" in {
        service.isCompleted(obligationDetails) mustBe false
      }
    }
  }
}
