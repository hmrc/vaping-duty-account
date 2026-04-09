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
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.PaperlessPreferenceSubmittedResponse
import uk.gov.hmrc.vapingdutyaccount.models.exceptions.*
import uk.gov.hmrc.vapingdutyaccount.utils.ErrorResponseHandler

import scala.concurrent.Future

class SubmitPreferencesControllerSpec extends SpecBase {
  val mockSubmitPreferencesConnector: SubmitPreferencesConnector = mock[SubmitPreferencesConnector]
  val errorHandler: ErrorResponseHandler = ErrorResponseHandler()

  val controller = new SubmitPreferencesController(
    cc,
    mockSubmitPreferencesConnector,
    fakeAuthorisedAction,
    fakeCheckVpdIdAction,
    errorHandler
  )

  "submitContactPreferences must" - {
    "return 200 OK and the submission response when successful" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.successful(testSubmissionResponse))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(testSubmissionResponse)
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector throws UnprocessableEntityException" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.failed(UnprocessableEntityException("Parse error")))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector throws BadRequestException" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.failed(BadRequestException("Bad request")))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector throws EntityNotFoundException" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.failed(EntityNotFoundException("Not found")))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector throws UpstreamServiceException" in {
      when(
        mockSubmitPreferencesConnector
          .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any())
      ).thenReturn(Future.failed(UpstreamServiceException("Upstream error", 500)))

      val result: Future[Result] =
        controller.submitContactPreferences(vpdId)(
          fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail))
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
