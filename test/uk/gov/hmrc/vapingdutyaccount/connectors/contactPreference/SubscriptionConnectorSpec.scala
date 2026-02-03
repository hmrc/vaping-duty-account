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
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.vapingdutyaccount.models.ErrorCodes
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionSummarySuccess

class SubscriptionConnectorSpec extends SpecBase with ConnectorTestHelpers {
  protected val endpointName = "subscription"

  "SubscriptionConnector must" - {
    "successfully get subscription contact preferences" in new SetUp {
      stubGet(url, OK, Json.toJson(SubscriptionSummarySuccess(contactPreferencesEmailSelected)).toString)
      whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
        result mustBe Right(contactPreferencesEmailSelected)
        verifyGet(url)
      }
    }

    "return BAD_REQUEST if a bad request received with no retry" in new SetUp {
      stubGet(url, BAD_REQUEST, Json.toJson(badRequest).toString)
      whenReady(connectorWithRetry.getSubscriptionContactPreferences(vpdId)) { result =>
        result mustBe Left(ErrorResponse(BAD_REQUEST, "Bad request"))
        verifyGetWithoutRetry(url)
      }
    }

    "return UNPROCESSABLE_ENTITY if a 422 is received with no retry" in new SetUp {
      stubGet(url, UNPROCESSABLE_ENTITY, Json.toJson(unprocessable).toString)
      whenReady(connectorWithRetry.getSubscriptionContactPreferences(vpdId)) { result =>
        result mustBe Left(ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable entity"))
        verifyGetWithoutRetry(url)
      }
    }

    "return NOT_FOUND if subscription summary data cannot be found with no retry" in new SetUp {
      stubGet(url, NOT_FOUND, "")
      whenReady(connectorWithRetry.getSubscriptionContactPreferences(vpdId)) { result =>
        result mustBe Left(ErrorResponse(NOT_FOUND, "Subscription summary not found"))
        verifyGetWithoutRetry(url)
      }
    }

    "return INTERNAL_SERVER_ERROR" - {
      "if the data retrieved cannot be parsed" in new SetUp {
        stubGet(url, OK, "blah")
        whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
          result mustBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse subscription summary success"))
          verifyGet(url)
        }
      }

      "if an error other than BAD_REQUEST or NOT_FOUND or UNPROCESSABLE_ENTITY is returned" in new SetUp {
        stubGet(url, INTERNAL_SERVER_ERROR, Json.toJson(internalServerError).toString)
        whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
          verifyGet(url)
        }
      }

      "if an error other than BAD_REQUEST or NOT_FOUND or UNPROCESSABLE_ENTITY is returned, the connector will invoke a retry" in new SetUp {
        stubGet(url, INTERNAL_SERVER_ERROR, Json.toJson(internalServerError).toString)
        whenReady(connectorWithRetry.getSubscriptionContactPreferences(vpdId)) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
          verifyGetWithRetry(url)
        }
      }

      "if an exception is thrown when fetching subscription summary" in new SetUp {
        stubGetFault(url)
        whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
          result mustBe Left(ErrorCodes.unexpectedResponse)
          verifyGet(url)
        }
      }
    }
  }

  class SetUp extends ConnectorFixture {
    val headers                                   = new HIPHeaders(fakeUUIDGenerator, appConfig, clock)
    val connector: SubscriptionConnector          = appWithHttpClientV2.injector.instanceOf[SubscriptionConnector]
    val connectorWithRetry: SubscriptionConnector =
      appWithHttpClientV2WithRetry.injector.instanceOf[SubscriptionConnector]
    lazy val url: String                          = appConfig.getSubscriptionUrl(vpdId)
  }
}
