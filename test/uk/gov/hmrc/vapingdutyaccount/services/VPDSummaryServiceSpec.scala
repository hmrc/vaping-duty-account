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
import uk.gov.hmrc.vapingdutyaccount.connectors.obligations.ObligationsConnector
import uk.gov.hmrc.vapingdutyaccount.models.obligations.{ObligationDetails, ObligationItem, ObligationsResponse}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*

import java.time.{Clock, LocalDate}
import scala.concurrent.Future

class VPDSummaryServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val mockObligationsConnector: ObligationsConnector   = mock[ObligationsConnector]
  val mockConfig: AppConfig                            = mock[AppConfig]

  // Configure mock config
  when(mockConfig.selfHref(any())).thenReturn(s"/vaping-duty-account/vpd/summary/$vpdId")
  when(mockConfig.manageContactPreferenceUrl).thenReturn("/vaping-duty/contact-preferences/how-should-we-contact-you")
  when(mockConfig.completeReturnUrlPrefix).thenReturn("/vaping-duty/complete-return/before-you-start")
  when(mockConfig.viewReturnsUrl).thenReturn("/vaping-duty/view-your-returns")

  val vpdSummaryService: VPDSummaryService =
    new VPDSummaryService(mockConfig, mockSubscriptionConnector, mockObligationsConnector, new ObligationService(), Clock.systemDefaultZone())

  "VPDSummaryService" - {
    "getVPDSummary must" - {
      "return VPDSummary with single due return and completeReturn link" in {
        val dueObligation = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.now().minusMonths(1),
            iCToDate = LocalDate.now(),
            iCDateReceived = None,
            iCDueDate = LocalDate.now().plusDays(10),
            periodKey = "26AA"
          )
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(ObligationsResponse(Seq(dueObligation))))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe defined
        result.links.completeReturn.get.href must include("period=26AA")
        result.links.viewReturns mustBe None
      }

      "return VPDSummary with single overdue return and completeReturn link" in {
        val overdueObligation = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.now().minusMonths(2),
            iCToDate = LocalDate.now().minusMonths(1),
            iCDateReceived = None,
            iCDueDate = LocalDate.now().minusDays(5),
            periodKey = "25AL"
          )
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(ObligationsResponse(Seq(overdueObligation))))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe defined
        result.links.completeReturn.get.href must include("period=25AL")
        result.links.viewReturns mustBe None
      }

      "return VPDSummary with due and overdue returns and viewReturns link" in {
        val dueObligation = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.now().minusMonths(1),
            iCToDate = LocalDate.now(),
            iCDateReceived = None,
            iCDueDate = LocalDate.now().plusDays(10),
            periodKey = "26AA"
          )
        )
        val overdueObligation = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.now().minusMonths(2),
            iCToDate = LocalDate.now().minusMonths(1),
            iCDateReceived = None,
            iCDueDate = LocalDate.now().minusDays(5),
            periodKey = "25AL"
          )
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(ObligationsResponse(Seq(dueObligation, overdueObligation))))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe defined
        result.links.viewReturns.get.href mustBe "/vaping-duty/view-your-returns"
      }

      "return VPDSummary with multiple overdue returns and viewReturns link" in {
        val overdue1 = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.now().minusMonths(3),
            iCToDate = LocalDate.now().minusMonths(2),
            iCDateReceived = None,
            iCDueDate = LocalDate.now().minusDays(30),
            periodKey = "25AK"
          )
        )
        val overdue2 = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.now().minusMonths(2),
            iCToDate = LocalDate.now().minusMonths(1),
            iCDateReceived = None,
            iCDueDate = LocalDate.now().minusDays(5),
            periodKey = "25AL"
          )
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(ObligationsResponse(Seq(overdue1, overdue2))))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe defined
      }

      "return VPDSummary with completed returns only and viewReturns link" in {
        val completed = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "F",
            iCFromDate = LocalDate.now().minusMonths(2),
            iCToDate = LocalDate.now().minusMonths(1),
            iCDateReceived = Some(LocalDate.now().minusDays(10)),
            iCDueDate = LocalDate.now().minusDays(5),
            periodKey = "25AL"
          )
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(ObligationsResponse(Seq(completed))))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe defined
      }

      "return VPDSummary with mixed returns and viewReturns link" in {
        val due = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.now().minusMonths(1),
            iCToDate = LocalDate.now(),
            iCDateReceived = None,
            iCDueDate = LocalDate.now().plusDays(10),
            periodKey = "26AA"
          )
        )
        val overdue = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.now().minusMonths(3),
            iCToDate = LocalDate.now().minusMonths(2),
            iCDateReceived = None,
            iCDueDate = LocalDate.now().minusDays(30),
            periodKey = "25AK"
          )
        )
        val completed = ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "F",
            iCFromDate = LocalDate.now().minusMonths(4),
            iCToDate = LocalDate.now().minusMonths(3),
            iCDateReceived = Some(LocalDate.now().minusDays(60)),
            iCDueDate = LocalDate.now().minusDays(55),
            periodKey = "25AJ"
          )
        )

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(ObligationsResponse(Seq(due, overdue, completed))))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe defined
      }

      "return ContactMethod.Email when PaperlessPreference is true" in {
        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.failed(new InternalServerException("Obligations not available")))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.contactPreference mustBe ContactMethod.Email
      }

      "return ContactMethod.Post when PaperlessPreference is false" in {
        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesPostNoEmail))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.failed(new InternalServerException("Obligations not available")))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.contactPreference mustBe ContactMethod.Post
      }

      "return InternalServerError when subscription connector fails" in {
        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.failed(new InternalServerException("Subscription service error")))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.failed(new InternalServerException("Obligations not available")))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc)

        ScalaFutures.whenReady(result.failed) { e =>
          e shouldBe a[InternalServerException]
        }
      }

      "return VPDSummary with empty returns when obligations connector fails" in {
        when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(contactPreferencesEmailSelected))
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.failed(new InternalServerException("Obligations service error")))

        val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

        result.returns mustBe None
        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe None
        result.contactPreference mustBe ContactMethod.Email
      }
    }
  }
}
