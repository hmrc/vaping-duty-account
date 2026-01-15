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

package uk.gov.hmrc.vapingdutyaccount.models

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class PaperlessPreferenceSubmission(
  paperlessPreference: Boolean,
  emailAddress: Option[String],
  emailVerification: Option[Boolean],
  bouncedEmail: Option[Boolean]
)

object PaperlessPreferenceSubmission {
  import JsonHelpers.{booleanReads, booleanWrites}

  implicit val paperlessPreferenceFormat: OFormat[PaperlessPreferenceSubmission] =
    Json.format[PaperlessPreferenceSubmission]
}

case class PaperlessPreferenceSubmittedSuccess(success: PaperlessPreferenceSubmittedResponse)

object PaperlessPreferenceSubmittedSuccess {
  implicit val paperlessPreferenceSubmittedSuccessFormat: OFormat[PaperlessPreferenceSubmittedSuccess] =
    Json.format[PaperlessPreferenceSubmittedSuccess]
}

case class PaperlessPreferenceSubmittedResponse(processingDate: Instant, formBundleNumber: String)

object PaperlessPreferenceSubmittedResponse {
  implicit val paperlessPreferenceSubmittedResponseFormat: OFormat[PaperlessPreferenceSubmittedResponse] =
    Json.format[PaperlessPreferenceSubmittedResponse]
}
