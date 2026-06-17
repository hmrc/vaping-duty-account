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
import uk.gov.hmrc.vapingdutyaccount.models.obligations.{ObligationDetails, ObligationItem, ObligationsResponse}

import java.time.LocalDate
import scala.concurrent.Future

class GetObligationsServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  val mockObligationsConnector: ObligationsConnector = mock[ObligationsConnector]
  val service: GetObligationsService                    = new GetObligationsService(mockObligationsConnector)

  private val obligationDetails = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate            = LocalDate.now().minusMonths(1),
    iCToDate              = LocalDate.now(),
    iCDateReceived        = None,
    iCDueDate             = LocalDate.now().plusDays(10),
    periodKey             = "26AA"
  )

  "ObligationsService" - {
    "getObligationDetails must" - {

      "return the Seq[ObligationDetails] extracted from the connector response" in {
        when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(ObligationsResponse(Seq(ObligationItem(None, obligationDetails)))))

        service.getObligationDetails(vpdId).futureValue mustBe Seq(obligationDetails)
      }
    }
  }
}
