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

package uk.gov.hmrc.vapingdutyaccount.controllers.contactPreference

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.vapingdutyaccount.base.ISpecBase
import uk.gov.hmrc.vapingdutyaccount.controllers.contactPreference.routes

class SubmitPreferencesIntegrationSpec extends ISpecBase {
  "SubmitPreferences when" - {
    "calling submitContactPreferences must" - {
      val submitPreferencesUrl = config.submitPreferencesUrl(vpdId)

      "return 200 OK and the submission response when successful" in {
        stubAuthorised(vpdId)

        stubPut(
          submitPreferencesUrl,
          OK,
          Json.toJson(contactPreferenceSubmissionEmail).toString,
          Json.toJson(testSubmissionSuccess).toString
        )

        val response = callRoute(
          FakeRequest("PUT", routes.SubmitPreferencesController.submitContactPreferences(vpdId).url)
            .withHeaders("Authorization" -> "Bearer 12345")
            .withBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

        status(response)        mustBe OK
        contentAsJson(response) mustBe Json.toJson(testSubmissionResponse)

        verifyPut(submitPreferencesUrl)
      }

      "return 500 INTERNAL_SERVER_ERROR if the response obtained cannot be parsed" in {
        stubAuthorised(vpdId)

        stubPut(
          submitPreferencesUrl,
          OK,
          Json.toJson(contactPreferenceSubmissionEmail).toString,
          "invalid"
        )

        val response = callRoute(
          FakeRequest("PUT", routes.SubmitPreferencesController.submitContactPreferences(vpdId).url)
            .withHeaders("Authorization" -> "Bearer 12345")
            .withBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

        status(response) mustBe INTERNAL_SERVER_ERROR
        verifyPut(submitPreferencesUrl)
      }

      Seq(BAD_REQUEST, NOT_FOUND, UNPROCESSABLE_ENTITY).foreach { statusCode =>
        s"return $statusCode if the API returns this error code" in {
          stubAuthorised(vpdId)

          stubPut(
            submitPreferencesUrl,
            statusCode,
            Json.toJson(contactPreferenceSubmissionEmail).toString,
            ""
          )

          val response = callRoute(
            FakeRequest("PUT", routes.SubmitPreferencesController.submitContactPreferences(vpdId).url)
              .withHeaders("Authorization" -> "Bearer 12345")
              .withBody(Json.toJson(contactPreferenceSubmissionEmail))
          )

          status(response) mustBe statusCode
          verifyPut(submitPreferencesUrl)
        }
      }

      "return 500 INTERNAL_SERVER_ERROR if the API returns another error" in {
        stubAuthorised(vpdId)

        stubPut(
          submitPreferencesUrl,
          BAD_GATEWAY,
          Json.toJson(contactPreferenceSubmissionEmail).toString,
          ""
        )

        val response = callRoute(
          FakeRequest("PUT", routes.SubmitPreferencesController.submitContactPreferences(vpdId).url)
            .withHeaders("Authorization" -> "Bearer 12345")
            .withBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

        status(response) mustBe INTERNAL_SERVER_ERROR
        verifyPut(submitPreferencesUrl)
      }
    }
  }
}
