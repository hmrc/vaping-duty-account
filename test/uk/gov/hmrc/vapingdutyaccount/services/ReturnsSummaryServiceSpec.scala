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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.obligations.ObligationDetails
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*

import scala.concurrent.Future

class ReturnsSummaryServiceSpec extends SpecBase {

  private val mockConfig: AppConfig                                = mock[AppConfig]
  private val mockGetObligationsService: GetObligationsService     = mock[GetObligationsService]

  when(mockConfig.completeReturnUrlPrefix).thenReturn(testCompleteReturnUrlPrefix)
  when(mockConfig.viewReturnsUrl).thenReturn(testViewReturnsUrl)

  private val service = new ReturnsSummaryService(mockConfig, mockGetObligationsService, new ObligationService(clock))

  private val duePeriodKey       = "26AA"
  private val overduePeriodKey   = "25AL"
  private val oldOverduePeriodKey = "25AK"
  private val fulfilledPeriodKey = "25AJ"

  private val dueObligation        = obligationDueIn(days = 10, duePeriodKey)
  private val overdueObligation    = obligationOverdueBy(days = 5, overduePeriodKey)
  private val oldOverdueObligation = obligationOverdueBy(days = 30, oldOverduePeriodKey)
  private val fulfilledObligation  = obligationFulfilled(daysAgo = 55, fulfilledPeriodKey)

  private val viewReturnsLink = ViewReturns(testViewReturnsUrl, httpGet)
  private def completeReturnLink(periodKey: String) = CompleteReturn(s"$testCompleteReturnUrlPrefix?period=$periodKey", httpGet)

  private def givenObligations(obligations: Seq[ObligationDetails]): Unit =
    when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
      .thenReturn(Future.successful(obligations))

  override def beforeEach(): Unit = reset(mockGetObligationsService)

  "ReturnsSummaryService" - {
    "getReturnsSection must" - {

      "return the current return and a completeReturn link, but no viewReturns link, for a single due return" in {
        givenObligations(Seq(dueObligation))

        service.getReturnsSection(vpdId).futureValue mustBe ReturnsSection(
          returns = Some(Returns(
            hasReturnsError       = false,
            currentReturn         = Some(CurrentReturn(duePeriodKey, dueObligation.iCDueDate)),
            dueReturnsCount       = Some(1),
            overdueReturnsCount   = Some(0),
            completedReturnsCount = Some(0)
          )),
          completeReturn = Some(completeReturnLink(duePeriodKey)),
          viewReturns    = None
        )
      }

      "return a completeReturn link, but no viewReturns link, for a single overdue return" in {
        givenObligations(Seq(overdueObligation))

        val result = service.getReturnsSection(vpdId).futureValue

        result.returns.flatMap(_.currentReturn) mustBe Some(CurrentReturn(overduePeriodKey, overdueObligation.iCDueDate))
        result.completeReturn mustBe Some(completeReturnLink(overduePeriodKey))
        result.viewReturns mustBe None
      }

      "return a viewReturns link, but no completeReturn link, for one due and one overdue return" in {
        givenObligations(Seq(dueObligation, overdueObligation))

        val result = service.getReturnsSection(vpdId).futureValue

        result.returns.flatMap(_.currentReturn) mustBe None
        result.completeReturn mustBe None
        result.viewReturns mustBe Some(viewReturnsLink)
      }

      "return a viewReturns link, but no completeReturn link, for multiple overdue returns" in {
        givenObligations(Seq(oldOverdueObligation, overdueObligation))

        val result = service.getReturnsSection(vpdId).futureValue

        result.returns.flatMap(_.overdueReturnsCount) mustBe Some(2)
        result.completeReturn mustBe None
        result.viewReturns mustBe Some(viewReturnsLink)
      }

      "return a viewReturns link, but no completeReturn link, when there are only completed returns" in {
        givenObligations(Seq(fulfilledObligation))

        val result = service.getReturnsSection(vpdId).futureValue

        result.returns.flatMap(_.completedReturnsCount) mustBe Some(1)
        result.completeReturn mustBe None
        result.viewReturns mustBe Some(viewReturnsLink)
      }

      "return both a completeReturn and a viewReturns link for a single open return with completed returns" in {
        givenObligations(Seq(dueObligation, fulfilledObligation))

        val result = service.getReturnsSection(vpdId).futureValue

        result.completeReturn mustBe Some(completeReturnLink(duePeriodKey))
        result.viewReturns mustBe Some(viewReturnsLink)
      }

      "return the counts and a viewReturns link for a mix of due, overdue and completed returns" in {
        givenObligations(Seq(dueObligation, oldOverdueObligation, fulfilledObligation))

        service.getReturnsSection(vpdId).futureValue mustBe ReturnsSection(
          returns = Some(Returns(
            hasReturnsError       = false,
            currentReturn         = None,
            dueReturnsCount       = Some(1),
            overdueReturnsCount   = Some(1),
            completedReturnsCount = Some(1)
          )),
          completeReturn = None,
          viewReturns    = Some(viewReturnsLink)
        )
      }

      "return no returns and no links when there are no obligations" in {
        givenObligations(Seq.empty)

        service.getReturnsSection(vpdId).futureValue mustBe ReturnsSection(returns = None, completeReturn = None, viewReturns = None)
      }

      "return hasReturnsError true and no links when the obligations call fails" in {
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.failed(new InternalServerException("Failed to retrieve obligations")))

        service.getReturnsSection(vpdId).futureValue mustBe ReturnsSection.error
        ReturnsSection.error.returns mustBe Some(Returns(
          hasReturnsError       = true,
          currentReturn         = None,
          dueReturnsCount       = None,
          overdueReturnsCount   = None,
          completedReturnsCount = None
        ))
      }
    }
  }
}
