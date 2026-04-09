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
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{SubscriptionErrorResponse, SubscriptionNotFound}
import uk.gov.hmrc.vapingdutyaccount.utils.ErrorResponseHandler

import scala.concurrent.Future

class GetPreferencesControllerSpec extends SpecBase {
  val mockSubmitPreferencesConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val errorHandler: ErrorResponseHandler = ErrorResponseHandler()
  
  val controller = new GetPreferencesController(
    cc,
    mockSubmitPreferencesConnector,
    fakeAuthorisedAction,
    fakeCheckVpdIdAction,
    errorHandler
  )

  "getContactPreferences must" - {
    "return 200 OK and the response when successful" in {
      when(
        mockSubmitPreferencesConnector
          .getSubscriptionContactPreferences(eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Right(contactPreferencesEmailSelected)))

      val result = controller.getContactPreferences(vpdId)(fakeRequest)

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(contactPreferencesEmailSelected)
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector returns SubscriptionErrorResponse for parse error" in {
      when(
        mockSubmitPreferencesConnector
          .getSubscriptionContactPreferences(eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Left(SubscriptionErrorResponse("Parse error", Some("422")))))

      val result: Future[Result] =
        controller.getContactPreferences(vpdId)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector returns SubscriptionErrorResponse for bad request" in {
      when(
        mockSubmitPreferencesConnector
          .getSubscriptionContactPreferences(eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Left(SubscriptionErrorResponse("Bad request", Some("400")))))

      val result: Future[Result] =
        controller.getContactPreferences(vpdId)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector returns SubscriptionNotFound" in {
      when(
        mockSubmitPreferencesConnector
          .getSubscriptionContactPreferences(eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Left(SubscriptionNotFound())))

      val result: Future[Result] =
        controller.getContactPreferences(vpdId)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector returns SubscriptionErrorResponse for upstream error" in {
      when(
        mockSubmitPreferencesConnector
          .getSubscriptionContactPreferences(eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Left(SubscriptionErrorResponse("Upstream error", Some("500")))))

      val result: Future[Result] =
        controller.getContactPreferences(vpdId)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
