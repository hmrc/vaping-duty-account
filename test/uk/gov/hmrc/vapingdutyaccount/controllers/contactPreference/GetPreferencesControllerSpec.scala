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

package uk.gov.hmrc.vapingdutyaccount.controllers.contactPreference

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector

import scala.concurrent.Future

class GetPreferencesControllerSpec extends SpecBase {
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  val controller = new GetPreferencesController(
    cc,
    mockSubscriptionConnector,
    fakeAuthorisedAction,
    fakeCheckVpdIdAction
  )

  "getContactPreferences must" - {
    "return 200 OK and the contact preferences when successful" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesEmailSelected))

      val result = controller.getContactPreferences(vpdId)(fakeRequest)

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(contactPreferencesEmailSelected)
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector fails" in {
      val errorMessage = "Failed to get subscription contact preferences"
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.failed(InternalServerException(errorMessage)))

      val result = controller.getContactPreferences(vpdId)(fakeRequest)

      status(result)          mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe errorMessage
    }
  }
}