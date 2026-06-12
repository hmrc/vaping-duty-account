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

package uk.gov.hmrc.vapingdutyaccount.connectors.obligations

import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.vapingdutyaccount.base.{ConnectorTestHelpers, SpecBase}
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.obligations.{Identification, ObligationDetails, ObligationItem, ObligationsResponse}

import java.time.LocalDate

class ObligationsConnectorSpec extends SpecBase with ConnectorTestHelpers with ScalaFutures {

  protected val endpointName = "vaping-duty"

  private val vpdId = VpdId("GBWK1234567WK")

  "ObligationsConnector" - {
    "getObligations must" - {
      "return Some(ObligationsResponse) when the API returns 200 OK with valid JSON" in new SetUp {
        val obligationsResponse = ObligationsResponse(
          Seq(
            ObligationItem(
              identification = Some(Identification("VPD", "GBWK1234567WK", Some("VPD"))),
              obligationDetails = ObligationDetails(
                openOrFulfilledStatus = "O",
                iCFromDate = LocalDate.of(2026, 1, 1),
                iCToDate = LocalDate.of(2026, 1, 31),
                iCDateReceived = None,
                iCDueDate = LocalDate.of(2026, 2, 27),
                periodKey = "26AA"
              )
            )
          )
        )

        stubGet(url, OK, Json.toJson(obligationsResponse).toString)

        whenReady(connector.getObligations(vpdId)) { result =>
          result mustBe Some(obligationsResponse)
          verifyGet(url)
        }
      }

      "return None when the API returns 200 OK with invalid JSON" in new SetUp {
        stubGet(url, OK, """{"invalid": "json"}""")

        whenReady(connector.getObligations(vpdId)) { result =>
          result mustBe None
          verifyGet(url)
        }
      }

      "return None when the API returns 404 NOT_FOUND" in new SetUp {
        stubGet(url, NOT_FOUND, "")

        whenReady(connector.getObligations(vpdId)) { result =>
          result mustBe None
          verifyGet(url)
        }
      }

      "return None when the API returns 500 INTERNAL_SERVER_ERROR" in new SetUp {
        stubGet(url, INTERNAL_SERVER_ERROR, "")

        whenReady(connector.getObligations(vpdId)) { result =>
          result mustBe None
          verifyGet(url)
        }
      }

      "return None when the API returns 503 SERVICE_UNAVAILABLE" in new SetUp {
        stubGet(url, SERVICE_UNAVAILABLE, "")

        whenReady(connector.getObligations(vpdId)) { result =>
          result mustBe None
          verifyGet(url)
        }
      }

      "return None when a network fault occurs" in new SetUp {
        stubGetFault(url)

        whenReady(connector.getObligations(vpdId)) { result =>
          result mustBe None
          verifyGet(url)
        }
      }

      "return Some(ObligationsResponse) with empty obligations" in new SetUp {
        val emptyResponse = ObligationsResponse(Seq.empty)

        stubGet(url, OK, Json.toJson(emptyResponse).toString)

        whenReady(connector.getObligations(vpdId)) { result =>
          result mustBe Some(emptyResponse)
          result.get.obligation mustBe empty
          verifyGet(url)
        }
      }

      "return Some(ObligationsResponse) with multiple obligations" in new SetUp {
        val multipleObligations = ObligationsResponse(
          Seq(
            ObligationItem(
              identification = None,
              obligationDetails = ObligationDetails(
                openOrFulfilledStatus = "O",
                iCFromDate = LocalDate.of(2026, 1, 1),
                iCToDate = LocalDate.of(2026, 1, 31),
                iCDateReceived = None,
                iCDueDate = LocalDate.of(2026, 2, 27),
                periodKey = "26AA"
              )
            ),
            ObligationItem(
              identification = None,
              obligationDetails = ObligationDetails(
                openOrFulfilledStatus = "F",
                iCFromDate = LocalDate.of(2025, 12, 1),
                iCToDate = LocalDate.of(2025, 12, 31),
                iCDateReceived = Some(LocalDate.of(2026, 1, 15)),
                iCDueDate = LocalDate.of(2026, 1, 27),
                periodKey = "25AL"
              )
            )
          )
        )

        stubGet(url, OK, Json.toJson(multipleObligations).toString)

        whenReady(connector.getObligations(vpdId)) { result =>
          result mustBe Some(multipleObligations)
          result.get.obligation must have size 2
          verifyGet(url)
        }
      }
    }
  }

  class SetUp extends ConnectorFixture {
    val connector: ObligationsConnector = appWithHttpClientV2.injector.instanceOf[ObligationsConnector]
    lazy val url: String                = config.getObligationsUrl(vpdId)
  }
}