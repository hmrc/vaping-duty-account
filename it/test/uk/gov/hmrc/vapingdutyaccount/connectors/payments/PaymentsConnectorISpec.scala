/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutyaccount.connectors.payments

import org.scalatest.concurrent.IntegrationPatience
import play.api.http.Status.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingdutyaccount.base.ISpecBase
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.payments.{OutstandingPayment, PaymentsResponse}

class PaymentsConnectorISpec extends ISpecBase with IntegrationPatience {

  private val vpdId     = VpdId("GBWK1234567WK")
  private val connector = app.injector.instanceOf[PaymentsConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "PaymentsConnector" - {
    "getPayments" - {
      "must return PaymentsResponse when the API returns 200 OK with valid JSON" in {
        val responseBody =
          """{
            |  "outstanding": [
            |    { "chargeReference": "XVP123456789", "amountDue": 4574.84, "status": "Due" }
            |  ],
            |  "paymentOnAccount": [],
            |  "cleared": [],
            |  "totalAccountBalance": 4574.84
            |}""".stripMargin

        stubGet(
          "/vaping-duty-finance/financial-data/payments",
          OK,
          responseBody
        )

        val result = connector.getPayments().futureValue

        result mustBe PaymentsResponse(
          outstanding         = Seq(OutstandingPayment(Some("XVP123456789"), BigDecimal(4574.84), None, "Due")),
          totalAccountBalance = Some(BigDecimal(4574.84))
        )
      }

      "must fail with InternalServerException when the API returns 500 INTERNAL_SERVER_ERROR" in {
        stubGet(
          "/vaping-duty-finance/financial-data/payments",
          INTERNAL_SERVER_ERROR,
          ""
        )

        val result = connector.getPayments()

        whenReady(result.failed) { exception =>
          exception mustBe an[Exception]
          verifyGet("/vaping-duty-finance/financial-data/payments")
        }
      }

      "must fail with InternalServerException when the API returns invalid JSON" in {
        stubGet(
          "/vaping-duty-finance/financial-data/payments",
          OK,
          """{"invalid": "json"}"""
        )

        val result = connector.getPayments()

        whenReady(result.failed) { exception =>
          exception mustBe an[Exception]
          verifyGet("/vaping-duty-finance/financial-data/payments")
        }
      }
    }
  }
}
