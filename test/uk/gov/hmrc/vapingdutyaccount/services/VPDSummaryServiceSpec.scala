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
import org.mockito.Mockito.{never, reset, verify, when}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionSummary
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*

import scala.concurrent.Future

class VPDSummaryServiceSpec extends SpecBase {

  private val mockConfig: AppConfig                                             = mock[AppConfig]
  private val mockSubscriptionConnector: SubscriptionConnector                  = mock[SubscriptionConnector]
  private val mockContactPreferenceSummaryService: ContactPreferenceSummaryService = mock[ContactPreferenceSummaryService]
  private val mockReturnsSummaryService: ReturnsSummaryService                  = mock[ReturnsSummaryService]
  private val mockPaymentsSummaryService: PaymentsSummaryService                = mock[PaymentsSummaryService]

  when(mockConfig.selfHref(any())).thenReturn(testSelfHref)
  when(mockConfig.startDirectDebitUrl).thenReturn(testStartDirectDebitUrl)
  when(mockConfig.serviceName).thenReturn(testServiceName)
  when(mockConfig.serviceId).thenReturn(testServiceId)

  private val vpdSummaryService = new VPDSummaryService(
    mockConfig,
    mockSubscriptionConnector,
    mockContactPreferenceSummaryService,
    mockReturnsSummaryService,
    mockPaymentsSummaryService
  )

  private val selfLink             = Self(testSelfHref, httpGet)
  private val setUpDirectDebitLink = SetUpDirectDebit(testStartDirectDebitUrl, httpGet)
  private val serviceInfo          = ServiceInfo(testServiceName, testServiceId)

  private val contactPreferenceSection = ContactPreferenceSection(
    contactMethod           = ContactMethod.Email,
    contactPreferenceStatus = Some(ContactPreferenceStatus(bouncedEmail = false)),
    manageContactPreference = ManageContactPreference(testManageContactPreferenceUrl, httpGet)
  )

  private val returnsSection = ReturnsSection(
    returns        = Some(Returns(
      hasReturnsError       = false,
      currentReturn         = Some(CurrentReturn(obligationDetails.periodKey, obligationDetails.iCDueDate)),
      dueReturnsCount       = Some(1),
      overdueReturnsCount   = Some(0),
      completedReturnsCount = Some(0)
    )),
    completeReturn = Some(CompleteReturn(s"$testCompleteReturnUrlPrefix?period=${obligationDetails.periodKey}", httpGet)),
    viewReturns    = Some(ViewReturns(testViewReturnsUrl, httpGet))
  )

  private val paymentsSection = PaymentsSection(
    payments       = paymentsWithOutstandingBalance,
    makePayment    = Some(MakePayment(makePaymentHref(Some(singleChargeReference)), httpGet)),
    claimRepayment = Some(ClaimRepayment(testClaimRepaymentUrl, httpGet)),
    viewPayments   = Some(ViewPayments(testViewPaymentsUrl, httpGet))
  )

  private def givenSubscriptionSummary(subscriptionSummary: SubscriptionSummary): Unit =
    when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
      .thenReturn(Future.successful(subscriptionSummary))

  private def givenPhase1(): Unit =
    when(mockConfig.phase2Enabled).thenReturn(false)

  private def givenReturnsAndPaymentsDisabled(): Unit =
    when(mockConfig.returnsAndPaymentsEnabled).thenReturn(false)

  private def verifyReturnsAndPaymentsNotRequested(): Unit = {
    verify(mockReturnsSummaryService, never).getReturnsSection(any())(using any())
    verify(mockPaymentsSummaryService, never).getPaymentsSection()(using any())
  }

  override def beforeEach(): Unit = {
    reset(mockSubscriptionConnector, mockContactPreferenceSummaryService, mockReturnsSummaryService, mockPaymentsSummaryService)
    when(mockConfig.phase2Enabled).thenReturn(true)
    when(mockConfig.returnsAndPaymentsEnabled).thenReturn(true)
    when(mockContactPreferenceSummaryService.build(any())).thenReturn(contactPreferenceSection)
    when(mockReturnsSummaryService.getReturnsSection(eqTo(vpdId))(using any())).thenReturn(Future.successful(returnsSection))
    when(mockPaymentsSummaryService.getPaymentsSection()(using any())).thenReturn(Future.successful(paymentsSection))
  }

  private val insolventSubscriptionSummaries = Seq(
    "approved"     -> subscriptionSummaryInsolvent,
    "revoked"      -> subscriptionSummaryRevokedInsolvent,
    "deregistered" -> subscriptionSummaryDeregisteredInsolvent
  )

  private val accessStatuses = Seq(
    subscriptionSummaryApproved     -> AccessApprovalStatus.Approved,
    subscriptionSummaryDeregistered -> AccessApprovalStatus.Deregistered,
    subscriptionSummaryRevoked      -> AccessApprovalStatus.Revoked
  )

  "VPDSummaryService" - {

    "when the phase-2-enabled feature switch is off" - {

      "return only the phase 1 elements, built from the contact preference section" in {
        givenPhase1()
        givenSubscriptionSummary(subscriptionSummaryApproved)

        vpdSummaryService.getVPDSummary(vpdId).futureValue mustBe VPDSummary(
          service           = serviceInfo,
          identifiers       = Identifier(vpdId.id),
          contactPreference = Some(contactPreferenceSection.contactMethod),
          links             = Links(self = selfLink, manageContactPreference = Some(contactPreferenceSection.manageContactPreference))
        )
        verify(mockContactPreferenceSummaryService).build(subscriptionSummaryApproved)
        verifyReturnsAndPaymentsNotRequested()
      }

      "return only the self link for an insolvent subscriber" in {
        givenPhase1()
        givenSubscriptionSummary(subscriptionSummaryInsolvent)

        vpdSummaryService.getVPDSummary(vpdId).futureValue mustBe VPDSummary(
          service           = serviceInfo,
          identifiers       = Identifier(vpdId.id),
          contactPreference = None,
          links             = Links(self = selfLink)
        )
        verifyReturnsAndPaymentsNotRequested()
      }

      "propagate a failure when the subscription connector fails" in {
        givenPhase1()
        when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
          .thenReturn(Future.failed(new InternalServerException("Subscription service error")))

        vpdSummaryService.getVPDSummary(vpdId).failed.futureValue mustBe a[InternalServerException]
      }
    }

    "when the returnsAndPaymentsEnabled feature switch is off" - {

      "return access, the contact preference section and the direct debit link, with no returns or payments" in {
        givenReturnsAndPaymentsDisabled()
        givenSubscriptionSummary(subscriptionSummaryApproved)

        vpdSummaryService.getVPDSummary(vpdId).futureValue mustBe VPDSummary(
          service                 = serviceInfo,
          identifiers             = Identifier(vpdId.id),
          access                  = Some(Access(approvalStatus = Some(AccessApprovalStatus.Approved))),
          contactPreference       = Some(contactPreferenceSection.contactMethod),
          contactPreferenceStatus = contactPreferenceSection.contactPreferenceStatus,
          links                   = Links(
            self                    = selfLink,
            manageContactPreference = Some(contactPreferenceSection.manageContactPreference),
            setUpDirectDebit        = Some(setUpDirectDebitLink)
          )
        )
        verifyReturnsAndPaymentsNotRequested()
      }

      "return INSOLVENT access and only the self link for an insolvent subscriber" in {
        givenReturnsAndPaymentsDisabled()
        givenSubscriptionSummary(subscriptionSummaryInsolvent)

        vpdSummaryService.getVPDSummary(vpdId).futureValue mustBe VPDSummary(
          service           = serviceInfo,
          identifiers       = Identifier(vpdId.id),
          access            = Some(Access(approvalStatus = Some(AccessApprovalStatus.Insolvent))),
          contactPreference = None,
          links             = Links(self = selfLink)
        )
        verifyReturnsAndPaymentsNotRequested()
      }

      "propagate a failure when the subscription connector fails" in {
        givenReturnsAndPaymentsDisabled()
        when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
          .thenReturn(Future.failed(new InternalServerException("Subscription service error")))

        vpdSummaryService.getVPDSummary(vpdId).failed.futureValue mustBe a[InternalServerException]
      }
    }

    "when all feature switches are on" - {

      "assemble the full summary, taking each section and its links from its own service" in {
        givenSubscriptionSummary(subscriptionSummaryApproved)

        vpdSummaryService.getVPDSummary(vpdId).futureValue mustBe VPDSummary(
          service                 = serviceInfo,
          identifiers             = Identifier(vpdId.id),
          access                  = Some(Access(approvalStatus = Some(AccessApprovalStatus.Approved))),
          contactPreference       = Some(contactPreferenceSection.contactMethod),
          contactPreferenceStatus = contactPreferenceSection.contactPreferenceStatus,
          returns                 = returnsSection.returns,
          payments                = Some(paymentsSection.payments),
          links                   = Links(
            self                    = selfLink,
            manageContactPreference = Some(contactPreferenceSection.manageContactPreference),
            completeReturn          = returnsSection.completeReturn,
            viewReturns             = returnsSection.viewReturns,
            makePayment             = paymentsSection.makePayment,
            viewPayments            = paymentsSection.viewPayments,
            claimRepayment          = paymentsSection.claimRepayment,
            setUpDirectDebit        = Some(setUpDirectDebitLink)
          )
        )
      }

      "include the returns error section unchanged when the obligations could not be retrieved" in {
        givenSubscriptionSummary(subscriptionSummaryApproved)
        when(mockReturnsSummaryService.getReturnsSection(eqTo(vpdId))(using any())).thenReturn(Future.successful(ReturnsSection.error))

        val result = vpdSummaryService.getVPDSummary(vpdId).futureValue

        result.returns mustBe ReturnsSection.error.returns
        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe None
        result.payments mustBe Some(paymentsSection.payments)
      }

      "omit the returns section when there are no obligations" in {
        givenSubscriptionSummary(subscriptionSummaryApproved)
        when(mockReturnsSummaryService.getReturnsSection(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(ReturnsSection(returns = None, completeReturn = None, viewReturns = None)))

        val result = vpdSummaryService.getVPDSummary(vpdId).futureValue

        result.returns mustBe None
        result.links.completeReturn mustBe None
        result.links.viewReturns mustBe None
      }

      accessStatuses.foreach { case (subscriptionSummary, expectedStatus) =>
        s"return access $expectedStatus with every section when the subscription is $expectedStatus and not insolvent" in {
          givenSubscriptionSummary(subscriptionSummary)

          val result = vpdSummaryService.getVPDSummary(vpdId).futureValue

          result.access mustBe Some(Access(approvalStatus = Some(expectedStatus)))
          result.returns mustBe returnsSection.returns
          result.payments mustBe Some(paymentsSection.payments)
        }
      }

      insolventSubscriptionSummaries.foreach { case (approvalStatus, subscriptionSummary) =>
        s"return INSOLVENT access and a minimal response with no sections or action links when the subscription is $approvalStatus but insolvent" in {
          givenSubscriptionSummary(subscriptionSummary)

          vpdSummaryService.getVPDSummary(vpdId).futureValue mustBe VPDSummary(
            service           = serviceInfo,
            identifiers       = Identifier(vpdId.id),
            access            = Some(Access(approvalStatus = Some(AccessApprovalStatus.Insolvent))),
            contactPreference = None,
            links             = Links(self = selfLink)
          )
        }
      }

      "propagate a failure when the subscription connector fails" in {
        when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
          .thenReturn(Future.failed(new InternalServerException("Subscription service error")))

        vpdSummaryService.getVPDSummary(vpdId).failed.futureValue mustBe a[InternalServerException]
      }
    }
  }
}
