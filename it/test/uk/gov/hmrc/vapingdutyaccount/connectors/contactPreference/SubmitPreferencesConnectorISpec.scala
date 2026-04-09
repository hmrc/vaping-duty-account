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

package uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference

import play.api.libs.json.Json
import uk.gov.hmrc.vapingdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubmitPreferencesConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubmitPreferencesErrorResponse

class SubmitPreferencesConnectorISpec extends SpecBase with ConnectorTestHelpers {
  protected val endpointName = "submit-preferences"

  "SubmitPreferencesConnector when" - {
    "submitContactPreferences is called must" - {
      "successfully submit contact preferences" in new SetUp {
        stubPut(
          submitReturnUrl,
          OK,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          Json.toJson(testSubmissionSuccess).toString()
        )
        whenReady(connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)) { result =>
          result mustBe Right(testSubmissionResponse)
          verifyPut(submitReturnUrl)
        }
      }

      "return an UnexpectedResponse error if the call returns an invalid response json" in new SetUp {
        stubPut(submitReturnUrl, OK, Json.toJson(contactPreferenceSubmissionEmail).toString(), """{"wrongField": "value"}""")
        whenReady(connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)) { result =>
          result.isLeft mustBe true
          result.left.toOption.get mustBe a[SubmitPreferencesErrorResponse]
          result.left.toOption.get.asInstanceOf[SubmitPreferencesErrorResponse].error must include("Unable to parse JSON as PaperlessPreferenceSubmittedSuccess")
          verifyPut(submitReturnUrl)
        }
      }

      "return a BadRequest error without retry if the call returns a 400 response" in new SetUp {
        stubPut(
          submitReturnUrl,
          BAD_REQUEST,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          Json.toJson(badRequest).toString()
        )
        whenReady(connectorWithRetry.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)) {
          result =>
            result mustBe Left(SubmitPreferencesErrorResponse("Bad request", None, Some(400)))
            verifyPutWithoutRetry(submitReturnUrl)
        }
      }

      "return a NotFound error without retry if the call returns a 404 response" in new SetUp {
        stubPut(submitReturnUrl, NOT_FOUND, Json.toJson(contactPreferenceSubmissionEmail).toString(), "")
        whenReady(connectorWithRetry.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)) {
          result =>
            result mustBe Left(SubmitPreferencesErrorResponse("Entity not found", None, Some(404)))
            verifyPutWithoutRetry(submitReturnUrl)
        }
      }

      "return an UnprocessableEntity error without retry if the call returns a 422 response" in new SetUp {
        stubPut(
          submitReturnUrl,
          UNPROCESSABLE_ENTITY,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          Json.toJson(unprocessable).toString()
        )
        whenReady(connectorWithRetry.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)) {
          result =>
            result mustBe Left(SubmitPreferencesErrorResponse("Unprocessable entity", None, Some(422)))
            verifyPutWithoutRetry(submitReturnUrl)
        }
      }

      "return an UnexpectedResponse error with retry if the call returns a 500 response" in new SetUp {
        stubPut(
          submitReturnUrl,
          INTERNAL_SERVER_ERROR,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          Json.toJson(internalServerError).toString()
        )
        whenReady(connectorWithRetry.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)) {
          result =>
            result.isLeft mustBe true
            result.left.toOption.get mustBe a[SubmitPreferencesErrorResponse]
            verifyPutWithRetry(submitReturnUrl)
        }
      }
    }
  }

  abstract class SetUp extends ConnectorFixture {
    val connector          = appWithHttpClientV2.injector.instanceOf[SubmitPreferencesConnector]
    val connectorWithRetry = appWithHttpClientV2WithRetry.injector.instanceOf[SubmitPreferencesConnector]
    val submitReturnUrl    = config.submitPreferencesUrl(vpdId)
  }
}
