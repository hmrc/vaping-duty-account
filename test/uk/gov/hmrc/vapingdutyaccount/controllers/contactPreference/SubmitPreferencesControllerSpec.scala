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
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubmitPreferencesConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{PaperlessPreferenceSubmission, PaperlessPreferenceSubmittedResponse}

import scala.concurrent.Future

class SubmitPreferencesControllerSpec extends SpecBase {

  val mockConnector: SubmitPreferencesConnector = mock[SubmitPreferencesConnector]

  val controller = new SubmitPreferencesController(
    cc,
    mockConnector,
    fakeAuthorisedAction,
    fakeCheckVpdIdAction
  )

  "submitContactPreferences must" - {
    "return 200 OK when the connector successfully submits preferences" in {
      when(mockConnector.submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any()))
        .thenReturn(Future.successful(testSubmissionResponse))

      val result = controller.submitContactPreferences(vpdId)(fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail)))

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(testSubmissionResponse)
    }

    "return the failed future for the framework to log when an error is returned from the connector" in {
      when(mockConnector.submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(vpdId))(any()))
        .thenReturn(Future.failed(InternalServerException("Failed to submit contact preferences")))

      val result = controller.submitContactPreferences(vpdId)(fakeRequestWithJsonBody(Json.toJson(contactPreferenceSubmissionEmail)))

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage mustBe "Failed to submit contact preferences"
      }
    }
  }
}
