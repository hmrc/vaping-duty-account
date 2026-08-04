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

package uk.gov.hmrc.vapingdutyaccount.models.vpdSummary

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class PaymentsSpec extends AnyFreeSpec with Matchers {

  "Payments" - {
    "must serialize to JSON correctly with a balance" in {
      val payments = Payments(
        hasPaymentsError = false,
        balance = Some(PaymentBalance(BigDecimal(4574.84), isMultiplePaymentDue = false, Some("XVP123456789")))
      )

      val json = Json.toJson(payments)

      (json \ "hasPaymentsError").as[Boolean] shouldBe false
      (json \ "balance" \ "amount").as[BigDecimal] shouldBe BigDecimal(4574.84)
      (json \ "balance" \ "isMultiplePaymentDue").as[Boolean] shouldBe false
      (json \ "balance" \ "chargeReference").as[String] shouldBe "XVP123456789"
    }

    "must serialize to JSON correctly without a balance when hasPaymentsError is true" in {
      val payments = Payments(hasPaymentsError = true, balance = None)

      val json = Json.toJson(payments)

      (json \ "hasPaymentsError").as[Boolean] shouldBe true
      (json \ "balance").toOption shouldBe None
    }

    "must serialize a multiple-payments-due balance without a chargeReference" in {
      val payments = Payments(
        hasPaymentsError = false,
        balance = Some(PaymentBalance(BigDecimal(8250), isMultiplePaymentDue = true, None))
      )

      val json = Json.toJson(payments)

      (json \ "balance" \ "isMultiplePaymentDue").as[Boolean] shouldBe true
      (json \ "balance" \ "chargeReference").asOpt[String] shouldBe None
    }

    "must serialize a negative (credit) balance" in {
      val payments = Payments(
        hasPaymentsError = false,
        balance = Some(PaymentBalance(BigDecimal(-325.50), isMultiplePaymentDue = false, None))
      )

      val json = Json.toJson(payments)

      (json \ "balance" \ "amount").as[BigDecimal] shouldBe BigDecimal(-325.50)
    }
  }
}
