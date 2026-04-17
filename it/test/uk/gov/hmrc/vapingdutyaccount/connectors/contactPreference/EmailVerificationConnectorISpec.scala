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
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.GetVerificationStatusResponse

class EmailVerificationConnectorISpec extends SpecBase with ConnectorTestHelpers {
  protected val endpointName = "email-verification"

  "EmailVerificationConnector must" - {
    "successfully get a list of email verification statuses" in new SetUp {
      stubGet(url, OK, Json.toJson(getVerificationStatusResponse).toString)
      whenReady(connector.getEmailVerificationLight(credId)) { result =>
        result mustBe getVerificationStatusResponse
        verifyGet(url)
      }
    }

    "return a successful response with an empty list if no records are found" in new SetUp {
      stubGet(url, NOT_FOUND, Json.toJson(GetVerificationStatusResponse(List.empty)).toString)
      whenReady(connector.getEmailVerificationLight(credId)) { result =>
        result mustBe GetVerificationStatusResponse(List.empty)
        verifyGet(url)
      }
    }

    "return INTERNAL_SERVER_ERROR" - {
      "if the data retrieved cannot be parsed" in new SetUp {
        stubGet(url, OK, "blah")
        whenReady(connector.getEmailVerificationLight(credId).failed) { result =>
          assertExceptionMessage(result, "Unable to parse email records successful response")
          verifyGet(url)
        }
      }

      "if BAD_REQUEST is returned" in new SetUp {
        stubGet(url, BAD_REQUEST, Json.toJson(badRequest).toString)
        whenReady(connector.getEmailVerificationLight(credId).failed) { result =>
          assertExceptionMessage(result, "Error: Invalid request for email verification list")
          verifyGet(url)
        }
      }

      "if an error other than BAD_REQUEST or NOT_FOUND is returned" in new SetUp {
        stubGet(url, INTERNAL_SERVER_ERROR, Json.toJson(internalServerError).toString)
        whenReady(connector.getEmailVerificationLight(credId).failed) { result =>
          assertExceptionMessage(result, "Error: Unexpected response for email verification list")
          verifyGet(url)
        }
      }

      "if an exception is thrown when fetching email verification statuses" in new SetUp {
        stubGetFault(url)
        whenReady(connector.getEmailVerificationLight(credId).failed) { result =>
          assertExceptionMessage(result, "Error: Exception returned while trying to fetch email verification list")
          verifyGet(url)
        }
      }
    }
  }

  private def assertExceptionMessage(result: Throwable, expectedMessage: String) = {
    result match {
      case ex: InternalServerException =>
        ex.getMessage must include(expectedMessage)
      case _ =>
        fail(s"Expected an InternalServerException but got ${result.getClass.getSimpleName}")
    }
  }

  class SetUp extends ConnectorFixture {
    val connector        = new EmailVerificationConnector(config = config, httpClient = httpClientV2)
    lazy val url: String = appConfig.getVerifiedEmailsUrl(credId)
  }
}
