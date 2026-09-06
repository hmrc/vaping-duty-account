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

package uk.gov.hmrc.vapingdutyaccount.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.payments.PaymentsConnector
import uk.gov.hmrc.vapingdutyaccount.models.payments.{OutstandingPayment, PaymentsResponse as UpstreamPaymentsResponse}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{PaymentBalance, Payments}

import scala.concurrent.Future

class GetPaymentsServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  val mockPaymentsConnector: PaymentsConnector = mock[PaymentsConnector]
  val mockAppConfig: AppConfig                 = mock[AppConfig]

  val service: GetPaymentsService = new GetPaymentsService(mockAppConfig, mockPaymentsConnector, new PaymentsService())

  "GetPaymentsService" - {
    "getPayments must" - {

      "return Some(Payments) mapped from the connector response when the phase-2-enabled feature switch is on" in {
        when(mockAppConfig.phase2Enabled).thenReturn(true)

        when(mockPaymentsConnector.getPayments()(using any()))
          .thenReturn(Future.successful(UpstreamPaymentsResponse(
            outstanding         = Seq(OutstandingPayment(Some("XVP123456789"), BigDecimal(4574.84), None, "Due")),
            totalAccountBalance = Some(BigDecimal(4574.84))
          )))

        service.getPayments()(using hc).futureValue mustBe
          Some(Payments(hasPaymentsError = false, balance = Some(PaymentBalance(BigDecimal(4574.84), isMultiplePaymentDue = false, Some("XVP123456789")))))
      }

      "return Some(Payments) with hasPaymentsError true when the connector fails" in {
        when(mockAppConfig.phase2Enabled).thenReturn(true)

        when(mockPaymentsConnector.getPayments()(using any()))
          .thenReturn(Future.failed(new InternalServerException("Failed to retrieve payments")))

        service.getPayments()(using hc).futureValue mustBe Some(Payments(hasPaymentsError = true, balance = None))
      }
    }
  }
}
