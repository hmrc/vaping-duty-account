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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.connectors.obligations.ObligationsConnector

import scala.concurrent.Future

class GetObligationsServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  val mockObligationsConnector: ObligationsConnector = mock[ObligationsConnector]

  val service: GetObligationsService = new GetObligationsService(mockObligationsConnector)

  "GetObligationsService" - {
    "getObligationDetails must" - {

      "return the obligations from the connector" in {
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq(obligationDetails, obligationDetailsCompleted)))

        service.getObligationDetails(vpdId).futureValue mustBe Seq(obligationDetails, obligationDetailsCompleted)
      }

      "fail when the connector fails" in {
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.failed(InternalServerException("Failed to retrieve obligations")))

        service.getObligationDetails(vpdId).failed.futureValue mustBe an[InternalServerException]
      }
    }
  }
}
