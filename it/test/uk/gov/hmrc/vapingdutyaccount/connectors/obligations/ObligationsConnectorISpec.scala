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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.vapingdutyaccount.base.ISpecBase
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.obligations.{Identification, ObligationDetails, ObligationItem, ObligationsResponse}

import java.time.LocalDate

class ObligationsConnectorISpec extends ISpecBase with ScalaFutures with IntegrationPatience {

  private val vpdId = VpdId("GBWK1234567WK")
  private val connector = app.injector.instanceOf[ObligationsConnector]

  "ObligationsConnector" - {
    "getObligations" - {
      "must return Some(ObligationsResponse) when the API returns 200 OK with valid JSON" in {
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

        stubGetWithResponseBody(
          s"/vaping-duty/obligations/$vpdId",
          OK,
          Json.toJson(obligationsResponse).toString
        )

        val result = connector.getObligations(vpdId).futureValue

        result mustBe Some(obligationsResponse)
      }

      "must return None when the API returns 404 NOT_FOUND" in {
        stubGetWithResponseBody(
          s"/vaping-duty/obligations/$vpdId",
          NOT_FOUND,
          ""
        )

        val result = connector.getObligations(vpdId).futureValue

        result mustBe None
      }

      "must return None when the API returns 500 INTERNAL_SERVER_ERROR" in {
        stubGetWithResponseBody(
          s"/vaping-duty/obligations/$vpdId",
          INTERNAL_SERVER_ERROR,
          ""
        )

        val result = connector.getObligations(vpdId).futureValue

        result mustBe None
      }

      "must return None when the API returns invalid JSON" in {
        stubGetWithResponseBody(
          s"/vaping-duty/obligations/$vpdId",
          OK,
          """{"invalid": "json"}"""
        )

        val result = connector.getObligations(vpdId).futureValue

        result mustBe None
      }
    }
  }
}
