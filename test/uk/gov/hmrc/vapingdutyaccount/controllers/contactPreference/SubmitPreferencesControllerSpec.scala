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
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubmitPreferencesConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{PaperlessPreferenceSubmittedResponse, SubmitPreferencesErrorResponse}
import uk.gov.hmrc.vapingdutyaccount.utils.ErrorResponseHandler

import scala.concurrent.Future

class SubmitPreferencesControllerSpec extends SpecBase {
  val mockSubmitPreferencesConnector: SubmitPreferencesConnector = mock[SubmitPreferencesConnector]
  val errorHandler: ErrorResponseHandler = ErrorResponseHandler()

  val controller = new SubmitPreferencesController(
    cc,
    mockSubmitPreferencesConnector,
    fakeAuthorisedAction,
    fakeCheckVpdIdAction
  )

  "submitContactPreferences must" - {
    "return 200 OK and the submission response when successful" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Right(testSubmissionResponse)))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(testSubmissionResponse)
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector returns SubmitPreferencesErrorResponse for parse error" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Left(SubmitPreferencesErrorResponse("Parse error", Some("422")))))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector returns SubmitPreferencesErrorResponse for bad request" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Left(SubmitPreferencesErrorResponse("Bad request", Some("400")))))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector returns SubmitPreferencesErrorResponse for not found" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Left(SubmitPreferencesErrorResponse("Not found", Some("404")))))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector returns SubmitPreferencesErrorResponse for upstream error" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.successful(Left(SubmitPreferencesErrorResponse("Upstream error", Some("500")))))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
