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

import org.mockito.Mockito.verify
import play.api.inject.bind
import play.api.libs.json.Json
import uk.gov.hmrc.vapingdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.{HIPHeaders, SubscriptionConnectorLogger}

class SubscriptionConnectorISpec extends SpecBase with ConnectorTestHelpers {
  protected val endpointName = "subscription"

  private val mockLogger = mock[SubscriptionConnectorLogger]

  "SubscriptionConnector must" - {
    "successfully get subscription contact preferences" in new SetUp {
      stubGet(url, OK, Json.toJson(contactPreferencesEmailSelected).toString)
      whenReady(connector.getSubscriptionContactPreferences(vpdId)) { result =>
        result mustBe contactPreferencesEmailSelected
        verifyGet(url)
      }
    }

    "fail with InternalServerException if a bad request received" in new SetUp {
      stubGet(url, BAD_REQUEST, "")
      
      val result = connector.getSubscriptionContactPreferences(vpdId)
      
      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
        verifyGet(url)
      }
    }

    "fail with InternalServerException if a 422 is received" in new SetUp {
      stubGet(url, UNPROCESSABLE_ENTITY, unprocessableEntityBody)

      val result = connector.getSubscriptionContactPreferences(vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
        verifyGet(url)
        verify(mockLogger).warn("Subscription summary API returned 422 Unprocessable Entity. code=001, text=Inactive VPD Reference")
      }
    }

    "fail with InternalServerException if a 422 is received with a body that cannot be parsed" in new SetUp {
      stubGet(url, UNPROCESSABLE_ENTITY, "not json")

      val result = connector.getSubscriptionContactPreferences(vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
        verifyGet(url)
        verify(mockLogger).warn("Subscription summary API returned 422 Unprocessable Entity but the error body could not be parsed. Body: not json")
      }
    }

    "fail with InternalServerException if subscription summary data cannot be found" in new SetUp {
      stubGet(url, NOT_FOUND, "")
      
      val result = connector.getSubscriptionContactPreferences(vpdId)
      
      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
        verifyGet(url)
      }
    }

    "fail with InternalServerException if the data retrieved cannot be parsed" in new SetUp {
      stubGet(url, OK, "blah")
      
      val result = connector.getSubscriptionContactPreferences(vpdId)
      
      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
        verifyGet(url)
      }
    }

    "fail with InternalServerException if an error other than BAD_REQUEST or NOT_FOUND or UNPROCESSABLE_ENTITY is returned" in new SetUp {
      stubGet(url, INTERNAL_SERVER_ERROR, "")
      
      val result = connector.getSubscriptionContactPreferences(vpdId)
      
      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
        verifyGet(url)
      }
    }

    "fail with InternalServerException if an exception is thrown when fetching subscription summary" in new SetUp {
      stubGetFault(url)
      
      val result = connector.getSubscriptionContactPreferences(vpdId)
      
      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
        verifyGet(url)
      }
    }
  }

  class SetUp extends ConnectorFixture {
    val headers                          = new HIPHeaders(fakeUUIDGenerator, appConfig, clock)
    val connector: SubscriptionConnector =
      appWithHttpClientV2Builder
        .overrides(bind[SubscriptionConnectorLogger].toInstance(mockLogger))
        .build()
        .injector.instanceOf[SubscriptionConnector]
    lazy val url: String                 = appConfig.getSubscriptionUrl(vpdId)

    val unprocessableEntityBody: String =
      """{
        |  "errors": {
        |    "processingDate": "2025-01-31T09:26:17Z",
        |    "code": "001",
        |    "text": "Inactive VPD Reference"
        |  }
        |}""".stripMargin
  }
}
