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

import org.mockito.Mockito.when
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.obligations.Obligation
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.CompleteReturn

import java.time.LocalDate

class VPDSummaryReturnsServiceSpec extends SpecBase {

  val config: AppConfig = mock[AppConfig]
  val service: VPDSummaryReturnsService = new VPDSummaryReturnsService(config)

  private val completeReturnUrlPrefix = "complete-return-url-prefix"

  private def createObligation(status: String, dueDate: LocalDate, periodKey: String): Obligation =
    Obligation(
      openOrFulfilledStatus = status,
      iCFromDate            = dueDate.minusMonths(3),
      iCToDate              = dueDate.minusMonths(1),
      iCDateReceived        = None,
      iCDueDate             = dueDate,
      periodKey             = periodKey
    )

  "VPDSummaryReturnsService" - {
    when(config.completeReturnUrlPrefix).thenReturn(completeReturnUrlPrefix)

    "when no obligations" - {
      val returnsAndLinks = service.buildReturnsAndLinks(Seq.empty, false)

      "returns section is absent" in {
        returnsAndLinks.returns mustBe None
      }
      "complete-returns link does not show" in {
        returnsAndLinks.completeReturnLink mustBe None 
      }
      "view-returns link does not show" in {
        returnsAndLinks.viewReturnsLink mustBe None 
      }
    }

    "when single due obligation" - {
      val dueDate = LocalDate.now().plusDays(10)
      val returnsAndLinks = service.buildReturnsAndLinks(Seq(createObligation("O", dueDate, "26AA")), false)

      " returns block " - {
        val returns = returnsAndLinks.returns.value

        "returns counts " in {
          returns.hasReturnsError mustBe false
          returns.dueReturnsCount mustBe Some(1)
          returns.overdueReturnsCount mustBe Some(0)
          returns.completedReturnsCount mustBe Some(0)
        }
        "current return" in {
          val currentReturn = returns.currentReturn

          currentReturn mustBe defined
          currentReturn.get.periodKey mustBe "26AA"
          currentReturn.get.dueDate mustBe dueDate
        }
      }

      "complete-returns link does show" in {
        returnsAndLinks.completeReturnLink mustBe Some(CompleteReturn(href = s"$completeReturnUrlPrefix?period=26AA", method = "GET"))
      }
      "view-returns link does not show" in {
        returnsAndLinks.viewReturnsLink mustBe None
      }
    }

    "buildReturns must" - {
      "return Some(Returns) with overdueCount=1 and currentReturn when single overdue obligation" in {
        val dueDate = LocalDate.now().minusDays(5)
        val result = service.buildReturns(
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
        val result = service.buildReturns(
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
        val result = service.buildReturns(
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
        val result = service.buildReturns(
          Seq(createObligation("F", LocalDate.now().minusDays(5), "25AL"))
        ).value

        result.dueReturnsCount mustBe Some(0)
        result.overdueReturnsCount mustBe Some(0)
        result.completedReturnsCount mustBe Some(1)
        result.currentReturn mustBe None
      }

      "return correct counts for mixed due, overdue, and completed obligations" in {
        val result = service.buildReturns(Seq(
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

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)

    "isDue must" - {

      "return true when status is Open and due date is today" in {
        service.isDue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today    ), today) mustBe true
      }

      "return true when status is Open and due date is in the future" in {
        service.isDue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = tomorrow ), today) mustBe true
      }

      "return false when status is Open and due date is in the past" in {
        service.isDue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = yesterday), today) mustBe false
      }

      "return false when status is Fulfilled regardless of due date" in {
        service.isDue(obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = tomorrow ), today) mustBe false
        service.isDue(obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = yesterday), today) mustBe false
      }
    }

    "isOverdue must" - {

      "return true when status is Open and due date is in the past" in {
        service.isOverdue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = yesterday), today) mustBe true
      }

      "return false when status is Open and due date is today" in {
        service.isOverdue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today    ), today) mustBe false
      }

      "return false when status is Open and due date is in the future" in {
        service.isOverdue(obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = tomorrow ), today) mustBe false
      }

      "return false when status is Fulfilled regardless of due date" in {
        service.isOverdue(obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = yesterday), today) mustBe false
        service.isOverdue(obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = tomorrow ), today) mustBe false
      }
    }

    "isCompleted must" - {

      "return true when status is Fulfilled" in {
        service.isCompleted(obligationDetailsCompleted) mustBe true
      }

      "return false when status is Open" in {
        service.isCompleted(obligationDetails) mustBe false
      }
    }
  }
}
