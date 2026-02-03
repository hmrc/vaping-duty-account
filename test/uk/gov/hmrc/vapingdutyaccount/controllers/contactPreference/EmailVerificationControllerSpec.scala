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
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.EmailVerificationConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.GetVerificationStatusResponse

import scala.concurrent.Future

class EmailVerificationControllerSpec extends SpecBase {
  val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]

  val controller = new EmailVerificationController(
    cc,
    mockEmailVerificationConnector,
    fakeAuthorisedAction
  )

  "getEmailVerification must" - {
    "return 200 OK when GetVerificationStatusResponse is returned for the credId" in {
      when(mockEmailVerificationConnector.getEmailVerification(eqTo(credId))(any()))
        .thenReturn(Future[Either[ErrorResponse, GetVerificationStatusResponse]](Right(getVerificationStatusResponse)))

      val result: Future[Result] = controller.getEmailVerification(credId)(fakeRequest)

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(getVerificationStatusResponse)
    }

    "return 500 INTERNAL_SERVER_ERROR when an error is returned from the connector" in {
      when(mockEmailVerificationConnector.getEmailVerification(eqTo(credId))(any()))
        .thenReturn(
          Future.successful(Left(
            ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected response for email verification list")
          ))
        )

      val result: Future[Result] = controller.getEmailVerification(credId)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
