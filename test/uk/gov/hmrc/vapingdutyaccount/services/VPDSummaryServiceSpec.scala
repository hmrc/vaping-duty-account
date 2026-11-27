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
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.connectors.obligations.ObligationsConnector
import uk.gov.hmrc.vapingdutyaccount.connectors.payments.PaymentsConnector
import uk.gov.hmrc.vapingdutyaccount.models.obligations.Obligation
import uk.gov.hmrc.vapingdutyaccount.models.payments.{ClearedPayment, OutstandingPayment, PaymentsResponse}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*

import java.time.LocalDate
import scala.concurrent.Future

class VPDSummaryServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  private val mockSubscriptionConnector : SubscriptionConnector     = mock[SubscriptionConnector]
  private val mockPaymentsConnector     : PaymentsConnector         = mock[PaymentsConnector]
  private val mockObligationsConnector  : ObligationsConnector      = mock[ObligationsConnector]
  private val mockConfig                : AppConfig                 = mock[AppConfig]
  private val paymentsService           : VPDSummaryPaymentsService = VPDSummaryPaymentsService(mockConfig)

  when(mockConfig.selfHref(any())           ).thenReturn(s"/vaping-duty-account/vpd/summary/$vpdId")
  when(mockConfig.manageContactPreferenceUrl).thenReturn("/vaping-duty/contact-preferences/how-should-we-contact-you")
  when(mockConfig.completeReturnUrlPrefix   ).thenReturn("/vaping-duty/complete-return/before-you-start")
  when(mockConfig.viewReturnsUrl            ).thenReturn("/vaping-duty/view-your-returns")
  when(mockConfig.makePaymentUrl(any())).thenAnswer { (invocation: InvocationOnMock) =>
    invocation.getArgument[Option[String]](0) match {
      case Some(chargeReference) => s"/vaping-duty/start-bta-payment/$chargeReference"
      case None                  => "/vaping-duty/start-bta-payment"
    }
  }
  when(mockConfig.claimRepaymentUrl         ).thenReturn("/vaping-duty/claim-repayment")
  when(mockConfig.startDirectDebitUrl       ).thenReturn("/vaping-duty-finance/direct-debit/bta/start")
  when(mockConfig.viewPaymentsUrl           ).thenReturn("/vaping-duty/view-payments")
  when(mockConfig.serviceName               ).thenReturn("Vaping Products Duty")
  when(mockConfig.serviceId                 ).thenReturn("VPD")

  val vpdSummaryService: VPDSummaryService =
    new VPDSummaryService(mockConfig, mockSubscriptionConnector, mockPaymentsConnector, mockObligationsConnector, VPDSummaryContactPreferencesService(mockConfig), VPDSummaryReturnsService(mockConfig, clock), paymentsService)

  override def beforeEach(): Unit =
    when(mockPaymentsConnector.getPayments()(using any()))
      .thenReturn(Future.successful(
        PaymentsResponse(
          outstanding         = Seq(OutstandingPayment(chargeReference = Some("XX charge Ref xx"), amountDue = BigDecimal(500), dueDate = Some(LocalDate.of(2026, 11, 20)), status = "Due")),
          paymentOnAccount    = Seq.empty,
          cleared             = Seq.empty,
          totalAccountBalance = Some(BigDecimal(500))
        )
      ))

  val paymentsWithOutstandingBalance: PaymentsResponse = PaymentsResponse(
    outstanding         = Seq(OutstandingPayment(chargeReference = Some("XVP123"), amountDue = BigDecimal(100), dueDate = Some(LocalDate.of(2026, 11, 20)), status = "Due")),
    paymentOnAccount    = Seq.empty,
    cleared             = Seq.empty,
    totalAccountBalance = Some(BigDecimal(100))
  )

  "VPDSummaryService" - {
    "getVPDSummary" - {
      "Return section must" - {
        "return VPDSummary with single due return and completeReturn link" in {
          val now = LocalDate.now(clock)
          val dueObligation = Obligation(
            openOrFulfilledStatus = "O",
            iCFromDate            = now.minusMonths(1),
            iCToDate              = now,
            iCDateReceived        = None,
            iCDueDate             = now.plusDays(10),
            periodKey             = "26AA"
          )

          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesEmailSelected))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(dueObligation)))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.completeReturn mustBe defined
          result.links.completeReturn.get.href must include("period=26AA")
          result.links.viewReturns mustBe None
        }

        "return VPDSummary with single overdue return and completeReturn link" in {
          val now = LocalDate.now(clock)
          val overdueObligation = Obligation(
            openOrFulfilledStatus = "O",
            iCFromDate            = now.minusMonths(2),
            iCToDate              = now.minusMonths(1),
            iCDateReceived        = None,
            iCDueDate             = now.minusDays(5),
            periodKey             = "25AL"
          )

          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesEmailSelected))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(overdueObligation)))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.completeReturn mustBe defined
          result.links.completeReturn.get.href must include("period=25AL")
          result.links.viewReturns mustBe None
        }

        "return VPDSummary with due and overdue returns and viewReturns link" in {
          val now = LocalDate.now(clock)
          val dueObligation = Obligation(
            openOrFulfilledStatus = "O",
            iCFromDate            = now.minusMonths(1),
            iCToDate              = now,
            iCDateReceived        = None,
            iCDueDate             = now.plusDays(10),
            periodKey             = "26AA"
          )
          val overdueObligation = Obligation(
            openOrFulfilledStatus = "O",
            iCFromDate            = now.minusMonths(2),
            iCToDate              = now.minusMonths(1),
            iCDateReceived        = None,
            iCDueDate             = now.minusDays(5),
            periodKey             = "25AL"
          )

          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesEmailSelected))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(dueObligation, overdueObligation)))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.completeReturn mustBe None
          result.links.viewReturns mustBe defined
          result.links.viewReturns.get.href mustBe "/vaping-duty/view-your-returns"
        }

        "return VPDSummary with multiple overdue returns and viewReturns link" in {
          val now = LocalDate.now(clock)
          val overdueObligation1 = Obligation(
            openOrFulfilledStatus = "O",
            iCFromDate            = now.minusMonths(3),
            iCToDate              = now.minusMonths(2),
            iCDateReceived        = None,
            iCDueDate             = now.minusDays(30),
            periodKey             = "25AK"
          )
          val overdueObligation2 = Obligation(
            openOrFulfilledStatus = "O",
            iCFromDate            = now.minusMonths(2),
            iCToDate              = now.minusMonths(1),
            iCDateReceived        = None,
            iCDueDate             = now.minusDays(5),
            periodKey             = "25AL"
          )

          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesEmailSelected))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(overdueObligation1, overdueObligation2)))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.completeReturn mustBe None
          result.links.viewReturns mustBe defined
        }

        "return VPDSummary with completed returns only and viewReturns link" in {
          val now = LocalDate.now(clock)
          val fulfilledObligation = Obligation(
            openOrFulfilledStatus = "F",
            iCFromDate            = now.minusMonths(2),
            iCToDate              = now.minusMonths(1),
            iCDateReceived        = Some(now.minusDays(10)),
            iCDueDate             = now.minusDays(5),
            periodKey             = "25AL"
          )

          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesEmailSelected))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(fulfilledObligation)))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.completeReturn mustBe None
          result.links.viewReturns mustBe defined
        }

        "return VPDSummary with mixed returns and viewReturns link" in {
          val now = LocalDate.now(clock)
          val dueObligation = Obligation(
            openOrFulfilledStatus = "O",
            iCFromDate            = now.minusMonths(1),
            iCToDate              = now,
            iCDateReceived        = None,
            iCDueDate             = now.plusDays(10),
            periodKey             = "26AA"
          )
          val overdueObligation = Obligation(
            openOrFulfilledStatus = "O",
            iCFromDate            = now.minusMonths(3),
            iCToDate              = now.minusMonths(2),
            iCDateReceived        = None,
            iCDueDate             = now.minusDays(30),
            periodKey             = "25AK"
          )
          val fulfilledObligation = Obligation(
            openOrFulfilledStatus = "F",
            iCFromDate            = now.minusMonths(4),
            iCToDate              = now.minusMonths(3),
            iCDateReceived        = Some(now.minusDays(60)),
            iCDueDate             = now.minusDays(55),
            periodKey             = "25AJ"
          )

          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesEmailSelected))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(dueObligation, overdueObligation, fulfilledObligation)))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.completeReturn mustBe None
          result.links.viewReturns mustBe defined
        }
      }

      "Contact preference section must" - {
        "return contactPreference Email and contactPreferenceStatus with bouncedEmail false when PaperlessPreference is true" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesEmailSelected))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.contactPreference mustBe Some(ContactMethod.Email)
          result.contactPreferenceStatus mustBe Some(ContactPreferenceStatus(false))
        }

        "return contactPreference Email and contactPreferenceStatus with bouncedEmail true when the email has bounced" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesEmailSelected.copy(bouncedEmail = Some(true))))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.contactPreference mustBe Some(ContactMethod.Email)
          result.contactPreferenceStatus mustBe Some(ContactPreferenceStatus(true))
        }

        "return contactPreference Post and no contactPreferenceStatus when PaperlessPreference is false" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.contactPreference mustBe Some(ContactMethod.Post)
          result.contactPreferenceStatus mustBe None
        }
      }

      "Failure must" - {
        "propagate a failure when the subscription connector fails" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.failed(new InternalServerException("Subscription service error")))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(openObligation)))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(paymentsWithOutstandingBalance))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).failed.futureValue

          result mustBe a[InternalServerException]
        }
      }

      "Payments section must" - {
        "return viewPayments link when balance is present" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(paymentsWithOutstandingBalance))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.viewPayments mustBe Some(ViewPayments("/vaping-duty/view-payments", "GET"))
        }

        "not return viewPayments link when payments has error" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.failed(InternalServerException("failure!")))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.viewPayments mustBe None
        }

        "not return viewPayments link when manufacturer is insolvent" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesInsolvent))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(openObligation)))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(PaymentsResponse(
              outstanding         = Seq.empty,
              paymentOnAccount    = Seq.empty,
              cleared             = Seq.empty,
              totalAccountBalance = Some(BigDecimal(0))
            )))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.viewPayments mustBe None
        }

        "not return viewPayments link when there is no financial data to show" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(openObligation)))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(PaymentsResponse(
              outstanding         = Seq.empty,
              paymentOnAccount    = Seq.empty,
              cleared             = Seq.empty,
              totalAccountBalance = Some(BigDecimal(0))
            )))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.viewPayments mustBe None
        }

        "return viewPayments link when balance is zero" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(
              PaymentsResponse(
                outstanding = Seq(OutstandingPayment(chargeReference = Some("XX charge Ref xx"), amountDue = BigDecimal(500), dueDate = Some(LocalDate.of(2026, 11, 20)), status = "Due")),
                paymentOnAccount = Seq.empty,
                cleared = Seq.empty,
                totalAccountBalance = Some(BigDecimal(500))
              )
            ))
          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.viewPayments mustBe Some(ViewPayments("/vaping-duty/view-payments", "GET"))
        }
      }

      "API failures must" - {
        "return Some(Returns) with hasReturnsError true and no counts/completeReturn/viewReturns links when the obligations call fails" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.failed(new InternalServerException("Failed to retrieve obligations")))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.returns mustBe Some(Returns(
            hasReturnsError       = true,
            currentReturn         = None,
            dueReturnsCount       = None,
            overdueReturnsCount   = None,
            completedReturnsCount = None
          ))
          result.links.completeReturn mustBe None
          result.links.viewReturns mustBe None
        }

        "return an empty Seq when the obligations connector returns none" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.links.completeReturn mustBe None
          result.links.viewReturns mustBe None
        }
      }

      "Payment behaviour must" - {
        "return a single charge reference and makePayment link when one charge is outstanding" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(
              PaymentsResponse(
                outstanding         = Seq(OutstandingPayment(chargeReference = Some("XVP123456789"), amountDue = BigDecimal(4574.84), dueDate = Some(LocalDate.of(2026, 11, 20)), status = "Due")),
                paymentOnAccount    = Seq.empty,
                cleared             = Seq.empty,
                totalAccountBalance = Some(BigDecimal(4574.84))
              )
            ))
  
          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.payments mustBe defined
          result.payments.get.hasPaymentsError mustBe false
          result.payments.get.balance mustBe Some(PaymentBalance(BigDecimal(4574.84), isMultiplePaymentDue = false, Some("XVP123456789")))
          result.links.makePayment mustBe Some(MakePayment("/vaping-duty/start-bta-payment/XVP123456789", "GET"))
          result.links.claimRepayment mustBe None
          result.links.setUpDirectDebit mustBe Some(SetUpDirectDebit("/vaping-duty-finance/direct-debit/bta/start", "GET"))
        }

        "return isMultiplePaymentDue true and no charge reference on the makePayment link when multiple charges are outstanding" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(
              PaymentsResponse(
                outstanding         = Seq(
                  OutstandingPayment(chargeReference = Some("XVP123456789"), amountDue = BigDecimal("4000.00"), dueDate = Some(LocalDate.of(2026, 11, 20)), status = "Due"),
                  OutstandingPayment(chargeReference = Some("XVP123456780"), amountDue = BigDecimal("4250.00"), dueDate = Some(LocalDate.of(2026, 11, 21)), status = "Due")
                ),
                paymentOnAccount    = Seq.empty,
                cleared             = Seq.empty,
                totalAccountBalance = Some(BigDecimal("8250.00"))
              )
            ))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.payments.get.balance mustBe Some(PaymentBalance(BigDecimal(8250), isMultiplePaymentDue = true, None))
          result.links.makePayment mustBe Some(MakePayment("/vaping-duty/start-bta-payment", "GET"))
          result.links.claimRepayment mustBe None
          result.links.setUpDirectDebit mustBe Some(SetUpDirectDebit("/vaping-duty-finance/direct-debit/bta/start", "GET"))
        }
  
        "return a negative balance and claimRepayment link when the account is in credit" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(
              PaymentsResponse(
                outstanding         = Seq(OutstandingPayment(chargeReference = Some("XVP123456789"), amountDue = BigDecimal(-325.50), dueDate = Some(LocalDate.of(2026, 11, 20)), status = "Due")),
                paymentOnAccount    = Seq.empty,
                cleared             = Seq.empty,
                totalAccountBalance = Some(BigDecimal(-325.50))
              )
            ))
  
          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.payments.get.balance mustBe Some(PaymentBalance(BigDecimal(-325.50), isMultiplePaymentDue = false, None))
          result.links.makePayment mustBe None
          result.links.viewPayments mustBe Some(ViewPayments("/vaping-duty/view-payments", "GET"))
          result.links.claimRepayment mustBe Some(ClaimRepayment("/vaping-duty/claim-repayment", "GET"))
          result.links.setUpDirectDebit mustBe Some(SetUpDirectDebit("/vaping-duty-finance/direct-debit/bta/start", "GET"))
        }
  
        "return a zero balance and no makePayment or claimRepayment link when nothing is owed" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(
              PaymentsResponse(
                outstanding         = Seq.empty,
                paymentOnAccount    = Seq.empty,
                cleared             = Seq(ClearedPayment(chargeReference = Some("XVP123456789"), amountPaid = BigDecimal(100), clearedDate = Some(LocalDate.of(2026, 11, 20)))),
                totalAccountBalance = Some(BigDecimal(0))
              )
            ))
  
          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.payments.get.balance mustBe Some(PaymentBalance(BigDecimal(0), isMultiplePaymentDue = false, None))
          result.links.makePayment mustBe None
          result.links.claimRepayment mustBe None
          result.links.viewPayments mustBe Some(ViewPayments("/vaping-duty/view-payments", "GET"))
          result.links.setUpDirectDebit mustBe Some(SetUpDirectDebit("/vaping-duty-finance/direct-debit/bta/start", "GET"))
        }

        "return hasPaymentsError true and a generic makePayment link when payments are unavailable" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesPostNoEmail))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.failed(InternalServerException("failure!")))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.payments mustBe Some(Payments(hasPaymentsError = true, balance = None))
          result.links.makePayment mustBe Some(MakePayment("/vaping-duty/start-bta-payment", "GET"))
          result.links.setUpDirectDebit mustBe Some(SetUpDirectDebit("/vaping-duty-finance/direct-debit/bta/start", "GET"))
        }
      }

      "Access section must" - {
        "return access APPROVED when the subscription is approved and not insolvent" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesApproved))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.access mustBe Some(Access(approvalStatus = Some(AccessApprovalStatus.Approved)))
        }

        "return access DEREGISTERED when the subscription is deregistered" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesDeregistered))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.access mustBe Some(Access(approvalStatus = Some(AccessApprovalStatus.Deregistered)))
        }

        "return access REVOKED when the subscription is revoked" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesRevoked))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq.empty))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.access mustBe Some(Access(approvalStatus = Some(AccessApprovalStatus.Revoked)))
        }

        "return access INSOLVENT and a minimal response with no sections or action links when the subscription is approved but insolvent" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesInsolvent))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(openObligation)))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(paymentsWithOutstandingBalance))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.access mustBe Some(Access(approvalStatus = Some(AccessApprovalStatus.Insolvent)))
          result.contactPreference mustBe None
          result.contactPreferenceStatus mustBe None
          result.returns mustBe None
          result.payments mustBe None
          result.links.self mustBe Self(s"/vaping-duty-account/vpd/summary/$vpdId", "GET")
          result.links.manageContactPreference mustBe None
          result.links.completeReturn mustBe None
          result.links.viewReturns mustBe None
          result.links.makePayment mustBe None
          result.links.viewPayments mustBe None
          result.links.setUpDirectDebit mustBe None
        }

        "return access INSOLVENT and a minimal response when the subscription is revoked but insolvent" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesRevokedInsolvent))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(openObligation)))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(paymentsWithOutstandingBalance))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.access mustBe Some(Access(approvalStatus = Some(AccessApprovalStatus.Insolvent)))
          result.returns mustBe None
          result.payments mustBe None
          result.links.manageContactPreference mustBe None
          result.links.setUpDirectDebit mustBe None
        }

        "return access INSOLVENT and a minimal response when the subscription is deregistered but insolvent" in {
          when(mockSubscriptionConnector.getSubscriptionDisplay(eqTo(vpdId))(any()))
            .thenReturn(Future.successful(contactPreferencesDeregisteredInsolvent))
          when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
            .thenReturn(Future.successful(Seq(openObligation)))
          when(mockPaymentsConnector.getPayments()(using any()))
            .thenReturn(Future.successful(paymentsWithOutstandingBalance))

          val result = vpdSummaryService.getVPDSummary(vpdId)(hc).futureValue

          result.access mustBe Some(Access(approvalStatus = Some(AccessApprovalStatus.Insolvent)))
          result.returns mustBe None
          result.payments mustBe None
          result.links.manageContactPreference mustBe None
          result.links.setUpDirectDebit mustBe None
        }
      }
    }
  }
}
