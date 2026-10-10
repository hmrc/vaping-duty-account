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

import com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.*
import play.api.inject.bind
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.SubmitPreferencesConnectorLogger

import java.net.SocketException

class SubmitPreferencesConnectorISpec extends SpecBase with ConnectorTestHelpers {
  protected val endpointName = "submit-preferences"

  private val mockLogger = mock[SubmitPreferencesConnectorLogger]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockLogger)
  }

  "SubmitPreferencesConnector when" - {
    "submitContactPreferences is called must" - {
      "successfully submit contact preferences" in new SetUp {
        stubPut(
          submitPreferencesUrl,
          OK,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          Json.toJson(testSubmissionSuccess).toString()
        )
        whenReady(connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)) { result =>
          result mustBe testSubmissionResponse
          verifyNoInteractions(mockLogger)
        }
      }

      "fail with InternalServerException if the call returns an invalid response json" in new SetUp {
        stubPut(submitPreferencesUrl, OK, Json.toJson(contactPreferenceSubmissionEmail).toString(), "invalid")
        
        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)
        
        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verify(mockLogger).warn("Parsing failed for submission response")
          verifyNoMoreInteractions(mockLogger)
        }
      }

      "fail with InternalServerException if the call returns a 400 response" in new SetUp {
        stubPut(
          submitPreferencesUrl,
          BAD_REQUEST,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          "BAD-REQUEST-RESPONSE-BODY"
        )
        
        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)
        
        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verify(mockLogger).warn("Unexpected response from contact preference submission API. Status=400 Body: BAD-REQUEST-RESPONSE-BODY")
          verifyNoMoreInteractions(mockLogger)
        }
      }

      "fail with InternalServerException if the call returns a 404 response" in new SetUp {
        stubPut(submitPreferencesUrl, NOT_FOUND, Json.toJson(contactPreferenceSubmissionEmail).toString(), "")
        
        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)
        
        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verify(mockLogger).warn("Unexpected response from contact preference submission API. Status: 404")
          verifyNoMoreInteractions(mockLogger)
        }
      }

      "fail with InternalServerException if the call returns a 422 response" in new SetUp {
        stubPut(
          submitPreferencesUrl,
          UNPROCESSABLE_ENTITY,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          """{
            |  "errors": {
            |    "processingDate": "2025-01-31T09:26:17Z",
            |    "code": "014",
            |    "text": "Email Address missing or invalid"
            |  }
            |}""".stripMargin
        )

        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)

        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verify(mockLogger).warn("Contact preference submission API returned 422 Unprocessable Entity. code=014, text=Email Address missing or invalid")
          verifyNoMoreInteractions(mockLogger)
        }
      }

      "fail with InternalServerException if the call returns a 422 response with a body that cannot be parsed" in new SetUp {
        stubPut(
          submitPreferencesUrl,
          UNPROCESSABLE_ENTITY,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          "not json"
        )

        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)

        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verify(mockLogger).warn("Contact preference submission API returned 422 Unprocessable Entity but the error body could not be parsed. Body: not json")
          verifyNoMoreInteractions(mockLogger)
        }
      }

      "fail with InternalServerException if the call returns a 500 response" in new SetUp {
        stubPut(
          submitPreferencesUrl,
          INTERNAL_SERVER_ERROR,
          Json.toJson(contactPreferenceSubmissionEmail).toString(),
          "INTERNAL-SERVER-ERROR-BODY"
        )
        
        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)
        
        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit contact preferences")
          verify(mockLogger).warn("Unexpected response from contact preference submission API. Status=500 Body: INTERNAL-SERVER-ERROR-BODY")
          verifyNoMoreInteractions(mockLogger)
        }
      }

      "Allow network failure exception to pass through" in new SetUp {
        stubPutFault(
          submitPreferencesUrl,
          INTERNAL_SERVER_ERROR,
          requestBody = Json.toJson(contactPreferenceSubmissionEmail).toString,
          fault = CONNECTION_RESET_BY_PEER
        )
        
        val result = connector.submitContactPreferences(contactPreferenceSubmissionEmail, vpdId)
        
        whenReady(result.failed) { exception =>
          exception mustBe a[SocketException]
          exception.getMessage mustBe "Connection reset"
          verify(mockLogger).warn(eqTo("An exception was returned while trying to submit contact preferences"), any[SocketException])
          verifyNoMoreInteractions(mockLogger)
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
    val connector =
      appWithHttpClientV2Builder
        .overrides(bind[SubmitPreferencesConnectorLogger].toInstance(mockLogger))
        .build()
        .injector.instanceOf[SubmitPreferencesConnector]
    val submitPreferencesUrl = config.submitPreferencesUrl(vpdId)
  }
}
