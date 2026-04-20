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

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.vapingdutyaccount.base.ISpecBase
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.GetVerificationStatusResponse

class EmailVerificationIntegrationSpec extends ISpecBase {

  "the get email verification endpoint must" - {
    "respond with OK if able to fetch a list of email verification statuses" in new SetUp {
      stubAuthorised(vpdId.toString)
      stubGet(url, OK, Json.toJson(getVerificationStatusResponse).toString)

      val response = callRoute(
        FakeRequest("GET", routes.EmailVerificationController.getEmailVerification(credId).url)
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(getVerificationStatusResponse)

      verifyGet(url)
    }

    "respond with OK if no email verification records are found" in new SetUp {
      stubAuthorised(vpdId.toString)
      stubGet(url, NOT_FOUND, Json.toJson(GetVerificationStatusResponse(List.empty)).toString)

      val response = callRoute(
        FakeRequest("GET", routes.EmailVerificationController.getEmailVerification(credId).url)
      )

      status(response)        mustBe OK
      contentAsJson(response) mustBe Json.toJson(GetVerificationStatusResponse(List.empty))

      verifyGet(url)
    }

    "respond with INTERNAL_SERVER_ERROR if the data retrieved cannot be parsed" in new SetUp {
      stubAuthorised(vpdId.toString)
      stubGet(url, OK, "blah")

      val response = callRoute(
        FakeRequest("GET", routes.EmailVerificationController.getEmailVerification(credId).url)
      )

      status(response)          mustBe INTERNAL_SERVER_ERROR
      contentAsString(response) mustBe "Error: Unable to parse email records successful response"

      verifyGet(url)
    }

    "respond with INTERNAL_SERVER_ERROR if a bad request is returned" in new SetUp {
      stubAuthorised(vpdId.toString)
      stubGet(url, BAD_REQUEST, "")

      val response = callRoute(
        FakeRequest("GET", routes.EmailVerificationController.getEmailVerification(credId).url)
      )

      status(response)          mustBe INTERNAL_SERVER_ERROR
      contentAsString(response) mustBe "Error: Unexpected response for email verification list"

      verifyGet(url)
    }

    "respond with INTERNAL_SERVER_ERROR if another error is returned from the get email verification api call" in new SetUp {
      stubAuthorised(vpdId.toString)
      stubGet(url, INTERNAL_SERVER_ERROR, "")

      val response = callRoute(
        FakeRequest("GET", routes.EmailVerificationController.getEmailVerification(credId).url)
      )

      status(response)          mustBe INTERNAL_SERVER_ERROR
      contentAsString(response) mustBe "Error: Unexpected response for email verification list"

      verifyGet(url)
    }
  }

  class SetUp {
    val url = "http://localhost:8142/email-verification/verification-status/TESTCREDID00000"
  }
}
