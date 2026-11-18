/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutyaccount.utils.helpers

import play.api.libs.json.{JsObject, Json, OFormat}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.vapingdutyaccount.models.*
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.*
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.{CredentialId, InternalId, VpdId}
import uk.gov.hmrc.vapingdutyaccount.models.obligations.{Identification, ObligationDetails, ObligationItem, ObligationsResponse}
import uk.gov.hmrc.vapingdutyaccount.models.payments.{ClearedPayment, OutstandingPayment, PaymentOnAccount, PaymentsResponse}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{PaymentBalance, Payments}
import uk.gov.hmrc.vapingdutyaccount.utils.generators.ModelGenerators

import java.time.*

trait TestData extends ModelGenerators {
  val clockMillis: Long = 1718118467838L
  val clock: Clock      = Clock.fixed(Instant.ofEpochMilli(clockMillis), ZoneId.of("UTC"))
  val today: LocalDate  = LocalDate.now(clock)

  val dummyUUID = "01234567-89ab-cdef-0123-456789abcdef"

  val regime: String           = "ZVPD"
  val vpdId: VpdId             = VpdId(id = vpdIdGen.sample.get)
  val internalId: InternalId   = InternalId(id = "internalId")
  val userDetails: UserDetails = UserDetails(vpdId.toString, internalId.toString)
  val credId: CredentialId     = CredentialId("TESTCREDID00000")

  val emailAddress          = "john.doe@example.com"
  val correspondenceAddress = "Flat 123\n1 Example Road\nAB1 2CD"

  val subscriptionSummaryEmailSelected: SubscriptionSummary =
    SubscriptionSummary(
      paperlessPreference = true,
      emailAddress = Some(emailAddress),
      verifiedEmail = Some(true),
      bouncedEmail = Some(false),
      addressLine1 = Some("Flat 123"),
      addressLine2 = Some("1 Example Road"),
      addressLine3 = None,
      addressLine4 = None,
      addressLine5 = None,
      postCode = Some("AB1 2CD"),
      approvalStatus = SubscriptionApprovalStatus.Approved,
      insolvencyStatus = None
    )
  val subscriptionSummaryPostNoEmail: SubscriptionSummary   =
    SubscriptionSummary(
      paperlessPreference = false,
      emailAddress = None,
      verifiedEmail = None,
      bouncedEmail = None,
      addressLine1 = Some("Flat 123"),
      addressLine2 = Some("1 Example Road"),
      addressLine3 = None,
      addressLine4 = None,
      addressLine5 = None,
      postCode = Some("AB1 2CD"),
      approvalStatus = SubscriptionApprovalStatus.Approved,
      insolvencyStatus = None
    )

  val subscriptionSummaryApproved: SubscriptionSummary        =
    subscriptionSummaryEmailSelected.copy(approvalStatus = SubscriptionApprovalStatus.Approved, insolvencyStatus = Some("N"))
  val subscriptionSummaryDeregistered: SubscriptionSummary    =
    subscriptionSummaryEmailSelected.copy(approvalStatus = SubscriptionApprovalStatus.DeRegistered, insolvencyStatus = Some("N"))
  val subscriptionSummaryRevoked: SubscriptionSummary         =
    subscriptionSummaryEmailSelected.copy(approvalStatus = SubscriptionApprovalStatus.Revoked, insolvencyStatus = Some("N"))
  val subscriptionSummaryInsolvent: SubscriptionSummary       =
    subscriptionSummaryEmailSelected.copy(approvalStatus = SubscriptionApprovalStatus.Approved, insolvencyStatus = Some("Y"))
  val subscriptionSummaryRevokedInsolvent: SubscriptionSummary =
    subscriptionSummaryEmailSelected.copy(approvalStatus = SubscriptionApprovalStatus.Revoked, insolvencyStatus = Some("Y"))
  val subscriptionSummaryDeregisteredInsolvent: SubscriptionSummary =
    subscriptionSummaryEmailSelected.copy(approvalStatus = SubscriptionApprovalStatus.DeRegistered, insolvencyStatus = Some("Y"))

  val userAnswers: UserAnswers = UserAnswers(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = EncryptedSubscriptionSummary(
      paperlessPreference = true,
      emailAddress = Some(SensitiveString(emailAddress)),
      emailVerification = Some(true),
      bouncedEmail = Some(false),
      correspondenceAddress = SensitiveString(correspondenceAddress)
    ),
    emailAddress = Some(SensitiveString(emailAddress)),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val decryptedUA: DecryptedUA = DecryptedUA(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = DecryptedSubscriptionSummary(
      paperlessPreference = true,
      emailAddress = Some(emailAddress),
      emailVerification = Some(true),
      bouncedEmail = Some(false),
      correspondenceAddress = correspondenceAddress
    ),
    emailAddress = Some(emailAddress),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswers: UserAnswers = UserAnswers(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = EncryptedSubscriptionSummary(
      paperlessPreference = true,
      emailAddress = Some(SensitiveString(emailAddress)),
      emailVerification = Some(true),
      bouncedEmail = Some(false),
      correspondenceAddress = SensitiveString(correspondenceAddress)
    ),
    emailAddress = None,
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswersNoEmail: UserAnswers = UserAnswers(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = EncryptedSubscriptionSummary(
      paperlessPreference = false,
      emailAddress = None,
      emailVerification = None,
      bouncedEmail = None,
      correspondenceAddress = SensitiveString(correspondenceAddress)
    ),
    emailAddress = None,
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswersBouncedEmail: UserAnswers = UserAnswers(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = EncryptedSubscriptionSummary(
      paperlessPreference = false,
      emailAddress = Some(SensitiveString(emailAddress)),
      emailVerification = Some(true),
      bouncedEmail = Some(true),
      correspondenceAddress = SensitiveString(correspondenceAddress)
    ),
    emailAddress = None,
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val getVerificationStatusResponse = GetVerificationStatusResponse(
    List(
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "john.doe@example.com",
        verified = true,
        locked = false
      ),
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "jane.doe@example.com",
        verified = false,
        locked = true
      ),
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "john.doe2@example.com",
        verified = false,
        locked = false
      ),
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "jane.doe2@example.com",
        verified = true,
        locked = true
      )
    )
  )

  val contactPreferenceSubmissionEmail = PaperlessPreferenceSubmission(
    paperlessPreference = true,
    emailAddress = Some(emailAddress),
    emailVerification = Some(true),
    bouncedEmail = Some(false)
  )

  val contactPreferenceSubmissionBouncedEmail = PaperlessPreferenceSubmission(
    paperlessPreference = false,
    emailAddress = None,
    emailVerification = None,
    bouncedEmail = Some(true)
  )

  val testSubmissionResponse = PaperlessPreferenceSubmittedResponse(Instant.now(clock), "910000000000")
  val testSubmissionSuccess  = PaperlessPreferenceSubmittedSuccess(testSubmissionResponse)

  case class DownstreamErrorDetails(code: String, message: String, logID: String)

  object DownstreamErrorDetails {
    implicit val downstreamErrorDetailsWrites: OFormat[DownstreamErrorDetails] = Json.format[DownstreamErrorDetails]
  }

  val badRequest          = DownstreamErrorDetails("400", "You messed up", "id")
  val unprocessable       = DownstreamErrorDetails("422", "Unprocessable", "id")
  val internalServerError = DownstreamErrorDetails("500", "Computer says No!", "id")

  val obligationDetails: ObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate = LocalDate.of(2024, 1, 1),
    iCToDate = LocalDate.of(2024, 3, 31),
    iCDateReceived = None,
    iCDueDate = LocalDate.of(2024, 4, 30),
    periodKey = "24A1"
  )

  val obligationDetailsCompleted: ObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "F",
    iCFromDate = LocalDate.of(2023, 10, 1),
    iCToDate = LocalDate.of(2023, 12, 31),
    iCDateReceived = Some(LocalDate.of(2024, 1, 15)),
    iCDueDate = LocalDate.of(2024, 1, 31),
    periodKey = "23A4"
  )

  val obligationItem: ObligationItem = ObligationItem(
    identification = Some(Identification(
      referenceType = "ZVPD",
      referenceNumber = vpdId.toString,
      incomeSourceType = Some("ZVPD")
    )),
    obligationDetails = Seq(obligationDetails)
  )

  val obligationsResponse: ObligationsResponse = ObligationsResponse(
    obligation = Seq(obligationItem)
  )

  val emptyObligationsResponse: ObligationsResponse = ObligationsResponse(
    obligation = Seq.empty
  )

  val paymentsWithOutstandingBalance: Payments = Payments(
    hasPaymentsError = false,
    balance = Some(PaymentBalance(BigDecimal(100), isMultiplePaymentDue = false, Some("XVP123")))
  )

  val clearedPayment: ClearedPayment = ClearedPayment(
    chargeReference = Some("XVP123456789"),
    amountPaid = BigDecimal(100.00),
    clearedDate = Some(LocalDate.now(clock))
  )

  val paymentOnAccount: PaymentOnAccount = PaymentOnAccount(
    amount = BigDecimal(50.00),
    paymentDate = Some(LocalDate.now(clock))
  )

  val singleChargeReference: String         = "XVP123456789"
  val firstMultipleChargeReference: String  = "XVP111111111"
  val secondMultipleChargeReference: String = "XVP222222222"
  val outstandingStatusDue: String          = "Due"

  val singleChargeAmount: BigDecimal          = BigDecimal(4574.84)
  val firstMultipleChargeAmount: BigDecimal   = BigDecimal(5000)
  val secondMultipleChargeAmount: BigDecimal  = BigDecimal(3250)
  val multipleChargesTotalAmount: BigDecimal  = firstMultipleChargeAmount + secondMultipleChargeAmount
  val creditBalanceAmount: BigDecimal         = BigDecimal(-325.50)
  val zeroBalanceAmount: BigDecimal           = BigDecimal(0)

  val upstreamPaymentsSingleCharge: PaymentsResponse = PaymentsResponse(
    outstanding         = Seq(OutstandingPayment(Some(singleChargeReference), singleChargeAmount, None, outstandingStatusDue)),
    paymentOnAccount    = Seq.empty,
    cleared             = Seq.empty,
    totalAccountBalance = Some(singleChargeAmount)
  )

  val upstreamPaymentsMultipleCharges: PaymentsResponse = PaymentsResponse(
    outstanding = Seq(
      OutstandingPayment(Some(firstMultipleChargeReference), firstMultipleChargeAmount, None, outstandingStatusDue),
      OutstandingPayment(Some(secondMultipleChargeReference), secondMultipleChargeAmount, None, outstandingStatusDue)
    ),
    paymentOnAccount    = Seq.empty,
    cleared             = Seq.empty,
    totalAccountBalance = Some(multipleChargesTotalAmount)
  )

  val upstreamPaymentsInCredit: PaymentsResponse = PaymentsResponse(
    outstanding         = Seq.empty,
    paymentOnAccount    = Seq(paymentOnAccount),
    cleared             = Seq.empty,
    totalAccountBalance = Some(creditBalanceAmount)
  )

  val upstreamPaymentsNothingOwed: PaymentsResponse = PaymentsResponse(
    outstanding         = Seq.empty,
    paymentOnAccount    = Seq.empty,
    cleared             = Seq(clearedPayment),
    totalAccountBalance = Some(zeroBalanceAmount)
  )

  val upstreamPaymentsNoBalance: PaymentsResponse = PaymentsResponse(
    outstanding         = Seq.empty,
    paymentOnAccount    = Seq.empty,
    cleared             = Seq(clearedPayment),
    totalAccountBalance = None
  )

  val upstreamPaymentsNoFinancialData: PaymentsResponse = PaymentsResponse(
    outstanding         = Seq.empty,
    paymentOnAccount    = Seq.empty,
    cleared             = Seq.empty,
    totalAccountBalance = None
  )

  val testSelfHref: String                   = s"/vaping-duty-account/vpd/summary/$vpdId"
  val testManageContactPreferenceUrl: String = "/vaping-duty/contact-preferences/how-should-we-contact-you"
  val testCompleteReturnUrlPrefix: String    = "/vaping-duty/complete-return/before-you-start"
  val testViewReturnsUrl: String             = "/vaping-duty/view-your-returns"
  val testMakePaymentUrl: String             = "/vaping-duty/start-bta-payment"
  val testClaimRepaymentUrl: String          = "/vaping-duty/claim-repayment"
  val testStartDirectDebitUrl: String        = "/vaping-duty-finance/direct-debit/bta/start"
  val testViewPaymentsUrl: String            = "/vaping-duty/view-payments"
  val testServiceName: String                = "Vaping Products Duty"
  val testServiceId: String                  = "VPD"
  val httpGet: String                        = "GET"

  def makePaymentHref(chargeReference: Option[String]): String =
    chargeReference.fold(testMakePaymentUrl)(ref => s"$testMakePaymentUrl/$ref")

  def obligationDueIn(days: Long, periodKey: String): ObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate            = today.minusMonths(1),
    iCToDate              = today,
    iCDateReceived        = None,
    iCDueDate             = today.plusDays(days),
    periodKey             = periodKey
  )

  def obligationOverdueBy(days: Long, periodKey: String): ObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate            = today.minusMonths(2),
    iCToDate              = today.minusMonths(1),
    iCDateReceived        = None,
    iCDueDate             = today.minusDays(days),
    periodKey             = periodKey
  )

  def obligationFulfilled(daysAgo: Long, periodKey: String): ObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "F",
    iCFromDate            = today.minusMonths(2),
    iCToDate              = today.minusMonths(1),
    iCDateReceived        = Some(today.minusDays(daysAgo + 5)),
    iCDueDate             = today.minusDays(daysAgo),
    periodKey             = periodKey
  )
}
