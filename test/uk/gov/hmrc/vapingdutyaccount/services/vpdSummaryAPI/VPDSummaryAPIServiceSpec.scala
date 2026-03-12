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

package uk.gov.hmrc.vapingdutyaccount.services.vpdSummaryAPI

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalactic.Prettifier
import org.scalactic.source.Position
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.*
import play.api.test.Helpers.await
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI.*

import scala.concurrent.Future

class VPDSummaryAPIServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val config: AppConfig                                = mock[AppConfig]

  val vpdSummaryAPIService: VPDSummaryAPIService   = new VPDSummaryAPIService(config, mockSubscriptionConnector)

  "SummaryAPIService must " - {
    "must return correct URLs as part of VPDSummary" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferencesLight(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesEmailSelected))

      val result = vpdSummaryAPIService.getVPDSummary(vpdId)(hc).futureValue

      result.links.manageContactPreference.href mustEqual "/vaping-duty/contact-preferences/how-should-we-contact-you"
      result.links.self.href                    mustEqual s"/vaping-duty-account/vpd/summary/$vpdId"
    }

    "must return ContactMethod.Email when PaperlessPreference is true" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferencesLight(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesEmailSelected))

      val result = vpdSummaryAPIService.getVPDSummary(vpdId)(hc).futureValue

      result.contactPreference mustBe ContactMethod.Email
    }

    "must return ContactMethod.Post when PaperlessPreference is false" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferencesLight(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesPostNoEmail))

      val result = vpdSummaryAPIService.getVPDSummary(vpdId)(hc).futureValue

      result.contactPreference mustBe ContactMethod.Post
    }

    "return APIErrors.InternalServerError when appropriate status code received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferencesLight(eqTo(vpdId))(any()))
        .thenReturn(Future.failed(new InternalServerException("look I'm an internal server exception! fear me!")))

      val result = vpdSummaryAPIService.getVPDSummary(vpdId)(hc)

      ScalaFutures.whenReady(result.failed) { e =>
        e shouldBe a[InternalServerException]
      }
    }
  }

}
