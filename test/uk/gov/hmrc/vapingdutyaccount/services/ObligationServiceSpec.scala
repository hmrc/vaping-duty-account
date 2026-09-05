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
import uk.gov.hmrc.vapingdutyaccount.models.obligations.ObligationDetails

import java.time.LocalDate

class ObligationServiceSpec extends SpecBase {

  val service: ObligationService = new ObligationService()

  private def createObligation(status: String, dueDate: LocalDate, periodKey: String): ObligationDetails =
    ObligationDetails(
      openOrFulfilledStatus = status,
      iCFromDate            = dueDate.minusMonths(3),
      iCToDate              = dueDate.minusMonths(1),
      iCDateReceived        = None,
      iCDueDate             = dueDate,
      periodKey             = periodKey
    )

  "ObligationService" - {

    "processObligations must" - {

      "return None when obligations list is empty" in {
        service.processObligations(Seq.empty) mustBe None
      }

      "return None when all obligations have an unrecognised status" in {
        service.processObligations(Seq(createObligation("X", LocalDate.now().plusDays(5), "26AA"))) mustBe None
      }

      "return Some(Returns) with dueCount=1 and currentReturn when single due obligation" in {
        val dueDate = LocalDate.now().plusDays(10)
        val result  = service.processObligations(Seq(createObligation("O", dueDate, "26AA"))).value

        result.hasReturnsError mustBe false
        result.dueReturnsCount mustBe Some(1)
        result.overdueReturnsCount mustBe Some(0)
        result.completedReturnsCount mustBe Some(0)
        result.currentReturn mustBe defined
        result.currentReturn.get.periodKey mustBe "26AA"
        result.currentReturn.get.dueDate mustBe dueDate
      }

      "return Some(Returns) with no currentReturn when multiple due obligations" in {
        val result = service.processObligations(Seq(
          createObligation("O", LocalDate.now().plusDays( 5), "26AA"),
          createObligation("O", LocalDate.now().plusDays(10), "26AB")
        )).value

        result.dueReturnsCount mustBe Some(2)
        result.currentReturn mustBe None
      }

      "return Some(Returns) with overdueCount=1 and currentReturn when single overdue obligation" in {
        val dueDate = LocalDate.now().minusDays(5)
        val result = service.processObligations(
          Seq(createObligation("O", dueDate, "25AL"))
        ).value

        result.dueReturnsCount mustBe Some(0)
        result.overdueReturnsCount mustBe Some(1)
        result.completedReturnsCount mustBe Some(0)
        result.currentReturn mustBe defined
        result.currentReturn.get.periodKey mustBe "25AL"
        result.currentReturn.get.dueDate mustBe dueDate
      }

      "return Some(Returns) with no currentReturn when multiple overdue obligations" in {
        val dueDate = LocalDate.now().minusDays(5)
        val result = service.processObligations(
          Seq(
            createObligation("O", dueDate.minusDays(30), "25AK"),
            createObligation("O", dueDate, "25AL")
          )
        ).value

        result.dueReturnsCount mustBe Some(0)
        result.overdueReturnsCount mustBe Some(2)
        result.completedReturnsCount mustBe Some(0)
        result.currentReturn mustBe None
      }

      "return Some(Returns) with completedCount=2 when 2 completed obligations" in {
        val result = service.processObligations(
          Seq(
            createObligation("F", LocalDate.now().minusDays(35), "25AK"),
            createObligation("F", LocalDate.now().minusDays( 5), "25AL")
          )
        ).value

        result.dueReturnsCount mustBe Some(0)
        result.overdueReturnsCount mustBe Some(0)
        result.completedReturnsCount mustBe Some(2)
        result.currentReturn mustBe None
      }

      "return Some(Returns) with completedCount=1 when single completed obligation" in {
        val result = service.processObligations(
          Seq(createObligation("F", LocalDate.now().minusDays(5), "25AL"))
        ).value

        result.dueReturnsCount mustBe Some(0)
        result.overdueReturnsCount mustBe Some(0)
        result.completedReturnsCount mustBe Some(1)
        result.currentReturn mustBe None
      }

      "return correct counts for mixed due, overdue, and completed obligations" in {
        val result = service.processObligations(Seq(
          createObligation("O", LocalDate.now().plusDays(10), "26AA"),

          createObligation("O", LocalDate.now().minusDays( 5), "25AL"),
          createObligation("O", LocalDate.now().minusDays(35), "25AK"),

          createObligation("F", LocalDate.now().minusDays( 65), "25AJ"),
          createObligation("F", LocalDate.now().minusDays( 95), "25AI"),
          createObligation("F", LocalDate.now().minusDays(120), "25AH")
        )).value

        result.dueReturnsCount mustBe Some(1)
        result.overdueReturnsCount mustBe Some(2)
        result.completedReturnsCount mustBe Some(3)
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
