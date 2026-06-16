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
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.models.obligations.ObligationDetails
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*

import java.time.{Clock, LocalDate}
import scala.concurrent.Future

class VPDSummaryServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val mockGetObligationsService: GetObligationsService       = mock[GetObligationsService]
  val mockConfig: AppConfig                            = mock[AppConfig]

  when(mockConfig.selfHref(any())).thenReturn(s"/vaping-duty-account/vpd/summary/$vpdId")
  when(mockConfig.manageContactPreferenceUrl).thenReturn("/vaping-duty/contact-preferences/how-should-we-contact-you")
  when(mockConfig.completeReturnUrlPrefix).thenReturn("/vaping-duty/complete-return/before-you-start")
  when(mockConfig.viewReturnsUrl).thenReturn("/vaping-duty/view-your-returns")

  val vpdSummaryService: VPDSummaryService =
    new VPDSummaryService(mockConfig, mockSubscriptionConnector, mockGetObligationsService, new ObligationService(), Clock.systemDefaultZone())

  "VPDSummaryService" - {
    "getVPDSummary must" - {
      "return VPDSummary with single due return and completeReturn link" in {
        val dueObligation = ObligationDetails(
          openOrFulfilledStatus = "O",
          iCFromDate            = LocalDate.now().minusMonths(1),
          iCToDate              = LocalDate.now(),
          iCDateReceived        = None,
          iCDueDate             = LocalDate.now().plusDays(10),
          periodKey             = "26AA"
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq(dueObligation)))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe defined
        result.links.completeReturn.get.href must include("period=26AA")
        result.links.viewReturns mustBe None
      }

      "return VPDSummary with single overdue return and completeReturn link" in {
        val overdueObligation = ObligationDetails(
          openOrFulfilledStatus = "O",
          iCFromDate            = LocalDate.now().minusMonths(2),
          iCToDate              = LocalDate.now().minusMonths(1),
          iCDateReceived        = None,
          iCDueDate             = LocalDate.now().minusDays(5),
          periodKey             = "25AL"
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq(overdueObligation)))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe defined
        result.links.completeReturn.get.href must include("period=25AL")
        result.links.viewReturns mustBe None
      }

      "return VPDSummary with due and overdue returns and viewReturns link" in {
        val dueObligation = ObligationDetails(
          openOrFulfilledStatus = "O",
          iCFromDate            = LocalDate.now().minusMonths(1),
          iCToDate              = LocalDate.now(),
          iCDateReceived        = None,
          iCDueDate             = LocalDate.now().plusDays(10),
          periodKey             = "26AA"
        )
        val overdueObligation = ObligationDetails(
          openOrFulfilledStatus = "O",
          iCFromDate            = LocalDate.now().minusMonths(2),
          iCToDate              = LocalDate.now().minusMonths(1),
          iCDateReceived        = None,
          iCDueDate             = LocalDate.now().minusDays(5),
          periodKey             = "25AL"
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq(dueObligation, overdueObligation)))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe defined
        result.links.viewReturns.get.href mustBe "/vaping-duty/view-your-returns"
      }

      "return VPDSummary with multiple overdue returns and viewReturns link" in {
        val overdueObligation1 = ObligationDetails(
          openOrFulfilledStatus = "O",
          iCFromDate            = LocalDate.now().minusMonths(3),
          iCToDate              = LocalDate.now().minusMonths(2),
          iCDateReceived        = None,
          iCDueDate             = LocalDate.now().minusDays(30),
          periodKey             = "25AK"
        )
        val overdueObligation2 = ObligationDetails(
          openOrFulfilledStatus = "O",
          iCFromDate            = LocalDate.now().minusMonths(2),
          iCToDate              = LocalDate.now().minusMonths(1),
          iCDateReceived        = None,
          iCDueDate             = LocalDate.now().minusDays(5),
          periodKey             = "25AL"
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq(overdueObligation1, overdueObligation2)))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe defined
      }

      "return VPDSummary with completed returns only and viewReturns link" in {
        val fulfilledObligation = ObligationDetails(
          openOrFulfilledStatus = "F",
          iCFromDate            = LocalDate.now().minusMonths(2),
          iCToDate              = LocalDate.now().minusMonths(1),
          iCDateReceived        = Some(LocalDate.now().minusDays(10)),
          iCDueDate             = LocalDate.now().minusDays(5),
          periodKey             = "25AL"
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq(fulfilledObligation)))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe defined
      }

      "return VPDSummary with mixed returns and viewReturns link" in {
        val dueObligation = ObligationDetails(
          openOrFulfilledStatus = "O",
          iCFromDate            = LocalDate.now().minusMonths(1),
          iCToDate              = LocalDate.now(),
          iCDateReceived        = None,
          iCDueDate             = LocalDate.now().plusDays(10),
          periodKey             = "26AA"
        )
        val overdueObligation = ObligationDetails(
          openOrFulfilledStatus = "O",
          iCFromDate            = LocalDate.now().minusMonths(3),
          iCToDate              = LocalDate.now().minusMonths(2),
          iCDateReceived        = None,
          iCDueDate             = LocalDate.now().minusDays(30),
          periodKey             = "25AK"
        )
        val fulfilledObligation = ObligationDetails(
          openOrFulfilledStatus = "F",
          iCFromDate            = LocalDate.now().minusMonths(4),
          iCToDate              = LocalDate.now().minusMonths(3),
          iCDateReceived        = Some(LocalDate.now().minusDays(60)),
          iCDueDate             = LocalDate.now().minusDays(55),
          periodKey             = "25AJ"
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq(dueObligation, overdueObligation, fulfilledObligation)))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe defined
      }

      "return ContactMethod.Email when PaperlessPreference is true" in {
        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq.empty))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.contactPreference mustBe ContactMethod.Email
      }

      "return ContactMethod.Post when PaperlessPreference is false" in {
        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesPostNoEmail))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq.empty))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.contactPreference mustBe ContactMethod.Post
      }

      "return InternalServerError when subscription connector fails" in {
        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.failed(new InternalServerException("Subscription service error")))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq.empty))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc)

        ScalaFutures.whenReady(result.failed) { e =>
          e shouldBe a[InternalServerException]
        }
      }

      "return an empty Seq when the obligations connector returns none" in {
        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesPostNoEmail))
        when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq.empty))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe None
      }
    }
  }
}
