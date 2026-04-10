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
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.HIPHeaders

class SubscriptionConnectorISpec extends SpecBase with ConnectorTestHelpers {
  protected val endpointName = "subscription"

  "SubscriptionConnector must" - {
    "successfully get subscription contact preferences" in new SetUp {
      stubGet(url, OK, Json.toJson(contactPreferencesEmailSelected).toString)
      whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
        result mustBe Right(contactPreferencesEmailSelected)
        verifyGet(url)
      }
    }

    "return BAD_REQUEST if a bad request is received" in new SetUp {
      stubGet(url, BAD_REQUEST, Json.toJson(badRequest).toString)
      whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
        result.isLeft mustBe true
        result.left.toOption.get mustBe an[Exception]
        result.left.toOption.get.getMessage must include("You messed up")
        verifyGet(url)
      }
    }

    "return UNPROCESSABLE_ENTITY if a 422 is received" in new SetUp {
      stubGet(url, UNPROCESSABLE_ENTITY, Json.toJson(unprocessable).toString)
      whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
        result.isLeft mustBe true
        result.left.toOption.get mustBe an[Exception]
        result.left.toOption.get.getMessage must include("Unprocessable")
        verifyGet(url)
      }
    }

    "return NOT_FOUND if subscription summary data cannot be found" in new SetUp {
      stubGet(url, NOT_FOUND, Json.obj("error" -> "Not found").toString)
      whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
        result.isLeft mustBe true
        result.left.toOption.get mustBe an[Exception]
        result.left.toOption.get.getMessage must include("4XX error occurred")
        verifyGet(url)
      }
    }

    "return an error if the data retrieved cannot be parsed" in new SetUp {
      stubGet(url, OK, """{"wrongField": "value"}""")
      whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
        result.isLeft mustBe true
        result.left.toOption.get mustBe an[Exception]
        result.left.toOption.get.getMessage must include("Unable to parse JSON")
        verifyGet(url)
      }
    }

    "return an error if an INTERNAL_SERVER_ERROR is returned" in new SetUp {
      stubGet(url, INTERNAL_SERVER_ERROR, Json.toJson(internalServerError).toString)
      whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
        result.isLeft mustBe true
        result.left.toOption.get mustBe an[Exception]
        verifyGet(url)
      }
    }

    "return an error if an exception is thrown when fetching subscription summary" in new SetUp {
      stubGetFault(url)
      whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
        result.isLeft mustBe true
        result.left.toOption.get mustBe an[Exception]
        verifyGet(url)
      }
    }
  }

  class SetUp extends ConnectorFixture {
    val headers                          = new HIPHeaders(fakeUUIDGenerator, appConfig, clock)
    val connector: SubscriptionConnector = appWithHttpClientV2.injector.instanceOf[SubscriptionConnector]
    lazy val url: String                 = appConfig.getSubscriptionUrl(vpdId)
  }
}
