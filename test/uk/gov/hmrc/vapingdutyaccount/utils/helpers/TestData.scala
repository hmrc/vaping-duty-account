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
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{DecryptedUA, GetVerificationStatusResponse, GetVerificationStatusResponseEmailAddressDetails, PaperlessPreferenceSubmission, PaperlessPreferenceSubmittedResponse, PaperlessPreferenceSubmittedSuccess, SubscriptionContactPreferences, SubscriptionSummary, SubscriptionSummaryBackend, UserAnswers}
import uk.gov.hmrc.vapingdutyaccount.utils.generators.ModelGenerators

import java.time.*

trait TestData extends ModelGenerators {
  val clockMillis: Long = 1718118467838L
  val clock: Clock      = Clock.fixed(Instant.ofEpochMilli(clockMillis), ZoneId.of("UTC"))

  val dummyUUID = "01234567-89ab-cdef-0123-456789abcdef"

  val regime: String           = "ZVPD"
  val vpdId: String            = vpdIdGen.sample.get
  val userId: String           = "userId"
  val userDetails: UserDetails = UserDetails(vpdId, userId)
  val credId: String           = "TESTCREDID00000"

  val emailAddress          = "john.doe@example.com"
  val correspondenceAddress = "Flat 123\n1 Example Road\nLondon\nAB1 2CD"
  val countryCode           = "GB"

  val contactPreferencesEmailSelected: SubscriptionContactPreferences =
    SubscriptionContactPreferences(
      paperlessPreference = true,
      emailAddress = Some(emailAddress),
      verifiedEmail = Some(true),
      bouncedEmail = Some(false),
      addressLine1 = Some("Flat 123"),
      addressLine2 = Some("1 Example Road"),
      addressLine3 = None,
      addressLine4 = Some("London"),
      postcode = Some("AB1 2CD"),
      country = Some(countryCode)
    )
  val contactPreferencesPostNoEmail: SubscriptionContactPreferences   =
    SubscriptionContactPreferences(
      paperlessPreference = false,
      emailAddress = None,
      verifiedEmail = None,
      bouncedEmail = None,
      addressLine1 = Some("Flat 123"),
      addressLine2 = Some("1 Example Road"),
      addressLine3 = None,
      addressLine4 = Some("London"),
      postcode = Some("AB1 2CD"),
      country = Some(countryCode)
    )

  val userAnswers: UserAnswers = UserAnswers(
    vpdId = vpdId,
    userId = userId,
    subscriptionSummary = SubscriptionSummaryBackend(
      paperlessPreference = true,
      emailAddress = Some(SensitiveString(emailAddress)),
      emailVerification = Some(true),
      bouncedEmail = Some(false),
      correspondenceAddress = SensitiveString(correspondenceAddress),
      countryCode = Some(SensitiveString(countryCode))
    ),
    emailAddress = Some(SensitiveString(emailAddress)),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val decryptedUA: DecryptedUA = DecryptedUA(
    vpdId = vpdId,
    userId = userId,
    subscriptionSummary = SubscriptionSummary(
      paperlessPreference = true,
      emailAddress = Some(emailAddress),
      emailVerification = Some(true),
      bouncedEmail = Some(false),
      correspondenceAddress = correspondenceAddress,
      countryCode = Some(countryCode)
    ),
    emailAddress = Some(emailAddress),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswers: UserAnswers = UserAnswers(
    vpdId = vpdId,
    userId = userId,
    subscriptionSummary = SubscriptionSummaryBackend(
      paperlessPreference = true,
      emailAddress = Some(SensitiveString(emailAddress)),
      emailVerification = Some(true),
      bouncedEmail = Some(false),
      correspondenceAddress = SensitiveString(correspondenceAddress),
      countryCode = Some(SensitiveString(countryCode))
    ),
    emailAddress = None,
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswersNoEmail: UserAnswers = UserAnswers(
    vpdId = vpdId,
    userId = userId,
    subscriptionSummary = SubscriptionSummaryBackend(
      paperlessPreference = false,
      emailAddress = None,
      emailVerification = None,
      bouncedEmail = None,
      correspondenceAddress = SensitiveString(correspondenceAddress),
      countryCode = Some(SensitiveString(countryCode))
    ),
    emailAddress = None,
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswersBouncedEmail: UserAnswers = UserAnswers(
    vpdId = vpdId,
    userId = userId,
    subscriptionSummary = SubscriptionSummaryBackend(
      paperlessPreference = false,
      emailAddress = Some(SensitiveString(emailAddress)),
      emailVerification = Some(true),
      bouncedEmail = Some(true),
      correspondenceAddress = SensitiveString(correspondenceAddress),
      countryCode = Some(SensitiveString(countryCode))
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
}
