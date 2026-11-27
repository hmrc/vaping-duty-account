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
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.obligations.Obligation
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{CompleteReturn, ViewReturns}
import uk.gov.hmrc.vapingdutyaccount.utils.helpers.TestData

import java.time.LocalDate

class VPDSummaryReturnsServiceSpec extends AnyFreeSpec with OptionValues with Matchers with MockitoSugar with TestData {

  val config: AppConfig = mock[AppConfig]
  val service: VPDSummaryReturnsService = new VPDSummaryReturnsService(config, clock)

  private val completeReturnUrlPrefix = "complete-return-url-prefix"
  private val viewReturnsUrl = "view-returns-url"
  private val SOLVENT = false
  private val INSOLVENT = true

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
    when(config.viewReturnsUrl).thenReturn(viewReturnsUrl)

    "when insolvent" - {
      val returnsAndLinks = service.buildReturnsAndLinks(Seq.empty, INSOLVENT)

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

    "when no obligations" - {
      val returnsAndLinks = service.buildReturnsAndLinks(Seq.empty, SOLVENT)

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
      val dueDate = LocalDate.now(clock).plusDays(10)
      val returnsAndLinks = service.buildReturnsAndLinks(Seq(createObligation("O", dueDate, "26AA")), SOLVENT)

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

    "when single overdue obligation" - {
      val dueDate = LocalDate.now(clock).minusDays(5)
      val returnsAndLinks = service.buildReturnsAndLinks(
        Seq(createObligation("O", dueDate, "25AL")), SOLVENT
      )
      "returns block" - {
        val returns = returnsAndLinks.returns.value

        "returns counts " in {
          returns.dueReturnsCount mustBe Some(0)
          returns.overdueReturnsCount mustBe Some(1)
          returns.completedReturnsCount mustBe Some(0)
        }
        "current return" in {
          returns.currentReturn mustBe defined
          returns.currentReturn.get.periodKey mustBe "25AL"
          returns.currentReturn.get.dueDate mustBe dueDate
        }
      }

      "complete-returns link does show" in {
        returnsAndLinks.completeReturnLink mustBe Some(CompleteReturn(href = s"$completeReturnUrlPrefix?period=25AL", method = "GET"))
      }
      "view-returns link does not show" in {
        returnsAndLinks.viewReturnsLink mustBe None
      }
    }

    "when multiple overdue obligations" - {
      val dueDate = LocalDate.now(clock).minusDays(5)
      val returnsAndLinks = service.buildReturnsAndLinks(
        Seq(
          createObligation("O", dueDate.minusDays(30), "25AK"),
          createObligation("O", dueDate, "25AL")
        ), SOLVENT
      )

      "returns block" - {
        val returns = returnsAndLinks.returns.value

        "returns counts" in {
          returns.dueReturnsCount mustBe Some(0)
          returns.overdueReturnsCount mustBe Some(2)
          returns.completedReturnsCount mustBe Some(0)
        }
        "current return must be absent" in {
          returns.currentReturn mustBe None
        }
      }

      "complete-returns link does not show" in {
        returnsAndLinks.completeReturnLink mustBe None
      }
      "view-returns link does not show" in {
        returnsAndLinks.viewReturnsLink mustBe Some(ViewReturns(viewReturnsUrl, "GET"))
      }
    }

    "when 2 completed obligations" - {
      val returnsAndLinks = service.buildReturnsAndLinks(
        Seq(
          createObligation("F", LocalDate.now(clock).minusDays(35), "25AK"),
          createObligation("F", LocalDate.now(clock).minusDays(5), "25AL")
        ), SOLVENT
      )

      "returns block" - {
        val returns = returnsAndLinks.returns.value

        "returns counts" in {
          returns.dueReturnsCount mustBe Some(0)
          returns.overdueReturnsCount mustBe Some(0)
          returns.completedReturnsCount mustBe Some(2)
        }
        "current return must be absent" in {
          returns.currentReturn mustBe None
        }
      }

      "complete-returns link does not show" in {
        returnsAndLinks.completeReturnLink mustBe None
      }
      "view-returns link does not show" in {
        returnsAndLinks.viewReturnsLink mustBe Some(ViewReturns(viewReturnsUrl, "GET"))
      }
    }

    "when single completed obligation" - {
      val returnsAndLinks = service.buildReturnsAndLinks(
        Seq(createObligation("F", LocalDate.now(clock).minusDays(5), "25AL")), SOLVENT
      )

      "returns block" - {
        val result = returnsAndLinks.returns.value

        "returns counts" in {
          result.dueReturnsCount mustBe Some(0)
          result.overdueReturnsCount mustBe Some(0)
          result.completedReturnsCount mustBe Some(1)
        }
        "current return must be absent" in {
          result.currentReturn mustBe None
        }
      }

      "complete-returns link does not show" in {
        returnsAndLinks.completeReturnLink mustBe None
      }
      "view-returns link does not show" in {
        returnsAndLinks.viewReturnsLink mustBe Some(ViewReturns(viewReturnsUrl, "GET"))
      }
    }

    "when mixed due, overdue, and completed obligations" - {
      val now = LocalDate.now(clock)
      val returnsAndLinks = service.buildReturnsAndLinks(Seq(
        createObligation("O", now.plusDays(10), "26AA"),

        createObligation("O", now.minusDays( 5), "25AL"),
        createObligation("O", now.minusDays(35), "25AK"),

        createObligation("F", now.minusDays( 65), "25AJ"),
        createObligation("F", now.minusDays( 95), "25AI"),
        createObligation("F", now.minusDays(120), "25AH")
      ), SOLVENT)

      "returns block" - {
        val returns = returnsAndLinks.returns.value

        "returns counts" in {
          returns.dueReturnsCount mustBe Some(1)
          returns.overdueReturnsCount mustBe Some(2)
          returns.completedReturnsCount mustBe Some(3)
        }
        "current return must be absent" in {
          returns.currentReturn mustBe None
        }
      }

      "complete-returns link does not show" in {
        returnsAndLinks.completeReturnLink mustBe None
      }
      "view-returns link does not show" in {
        returnsAndLinks.viewReturnsLink mustBe Some(ViewReturns(viewReturnsUrl, "GET"))
      }
    }
  }
}
