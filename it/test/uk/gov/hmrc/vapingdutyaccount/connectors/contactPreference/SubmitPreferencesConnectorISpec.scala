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
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubmitPreferencesConnector

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
          result mustBe testSubmissionResponse
          verifyPut(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns an invalid response json" in new SetUp {
        stubPut(submitReturnUrl, OK, Json.toJson(contactPreferenceSubmissionEmail).toString(), "invalid")
        
        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)
        
        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verifyPut(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns a 400 response" in new SetUp {
        stubPut(
          submitReturnUrl,
          BAD_REQUEST,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          ""
        )
        
        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)
        
        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verifyPut(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns a 404 response" in new SetUp {
        stubPut(submitReturnUrl, NOT_FOUND, Json.toJson(contactPreferenceSubmissionEmail).toString(), "")
        
        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)
        
        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verifyPut(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns a 422 response" in new SetUp {
        stubPut(
          submitReturnUrl,
          UNPROCESSABLE_ENTITY,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          unprocessableEntityBody
        )

        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)

        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verifyPut(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns a 422 response with a body that cannot be parsed" in new SetUp {
        stubPut(
          submitReturnUrl,
          UNPROCESSABLE_ENTITY,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          "not json"
        )

        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)

        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verifyPut(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns a 500 response" in new SetUp {
        stubPut(
          submitReturnUrl,
          INTERNAL_SERVER_ERROR,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          ""
        )
        
        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)
        
        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verifyPut(submitReturnUrl)
        }
      }
    }
  }

  private def assertExceptionMessage(exception: Throwable, expectedMessage: String) = {
    exception match {
      case ex: InternalServerException =>
        ex.getMessage must include(expectedMessage)
      case _ =>
        fail(s"Expected an InternalServerException but got ${exception.getClass.getSimpleName}")
    }
  }

  abstract class SetUp extends ConnectorFixture {
    val connector       = appWithHttpClientV2.injector.instanceOf[SubmitPreferencesConnector]
    val submitReturnUrl = config.submitPreferencesUrl(vpdId)

    val unprocessableEntityBody: String =
      """{
        |  "errors": {
        |    "processingDate": "2025-01-31T09:26:17Z",
        |    "code": "014",
        |    "text": "Email Address missing or invalid"
        |  }
        |}""".stripMargin
  }
}
