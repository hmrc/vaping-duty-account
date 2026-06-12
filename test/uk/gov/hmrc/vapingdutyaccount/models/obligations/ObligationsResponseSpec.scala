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

package uk.gov.hmrc.vapingdutyaccount.models.obligations

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

import java.time.LocalDate

class ObligationsResponseSpec extends AnyFreeSpec with Matchers {

  "ObligationsResponse" - {
    "must deserialize from JSON correctly" in {
      val json = Json.parse("""
        {
          "obligation": [
            {
              "identification": {
                "referenceType": "VPD",
                "referenceNumber": "GBWK1234567WK",
                "incomeSourceType": "VPD"
              },
              "obligationDetails": {
                "openOrFulfilledStatus": "O",
                "iCFromDate": "2026-01-01",
                "iCToDate": "2026-01-31",
                "iCDateReceived": null,
                "iCDueDate": "2026-02-27",
                "periodKey": "26AA"
              }
            }
          ]
        }
      """)

      val result = json.as[ObligationsResponse]

      result.obligation should have size 1
      result.obligation.head.obligationDetails.periodKey shouldBe "26AA"
      result.obligation.head.obligationDetails.openOrFulfilledStatus shouldBe "O"
      result.obligation.head.obligationDetails.iCDueDate shouldBe LocalDate.of(2026, 2, 27)
    }

    "must deserialize from JSON with multiple obligations" in {
      val json = Json.parse("""
        {
          "obligation": [
            {
              "identification": null,
              "obligationDetails": {
                "openOrFulfilledStatus": "O",
                "iCFromDate": "2026-01-01",
                "iCToDate": "2026-01-31",
                "iCDateReceived": null,
                "iCDueDate": "2026-02-27",
                "periodKey": "26AA"
              }
            },
            {
              "identification": null,
              "obligationDetails": {
                "openOrFulfilledStatus": "F",
                "iCFromDate": "2025-12-01",
                "iCToDate": "2025-12-31",
                "iCDateReceived": "2026-01-15",
                "iCDueDate": "2026-01-27",
                "periodKey": "25AL"
              }
            }
          ]
        }
      """)

      val result = json.as[ObligationsResponse]

      result.obligation should have size 2
      result.obligation.head.obligationDetails.openOrFulfilledStatus shouldBe "O"
      result.obligation(1).obligationDetails.openOrFulfilledStatus shouldBe "F"
      result.obligation(1).obligationDetails.iCDateReceived shouldBe Some(LocalDate.of(2026, 1, 15))
    }

    "must deserialize from JSON with empty obligations" in {
      val json = Json.parse("""
        {
          "obligation": []
        }
      """)

      val result = json.as[ObligationsResponse]

      result.obligation shouldBe empty
    }
  }
}