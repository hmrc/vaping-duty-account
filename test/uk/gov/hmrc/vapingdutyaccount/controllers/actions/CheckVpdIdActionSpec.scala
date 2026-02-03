/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutyaccount.controllers.actions

import play.api.mvc.{AnyContentAsEmpty, Result}
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.models.requests.IdentifierRequest

import scala.concurrent.Future

class CheckVpdIdActionSpec extends SpecBase {
  val wrongVpdId: String                                               = vpdId + "1"
  val fakeIdentifierRequest: IdentifierRequest[AnyContentAsEmpty.type] =
    IdentifierRequest(fakeRequest, vpdId, userId)
  val testContent                                                      = "Test"

  val testAction: IdentifierRequest[_] => Future[Result] = { request =>
    request mustBe fakeIdentifierRequest

    Future(Ok(testContent))
  }

  "CheckVpdIdAction must" - {
    "succeed if vpdId matches that in the enrolment" in {
      val checkVpdIdAction = new CheckVpdIdAction
      val result            = checkVpdIdAction(vpdId).invokeBlock(fakeIdentifierRequest, testAction)

      status(result)          mustBe OK
      contentAsString(result) mustBe testContent
    }

    "fail if vpdId doesn't match that in the enrolment" in {
      val checkVpdIdAction  = new CheckVpdIdAction
      val result            = checkVpdIdAction(wrongVpdId).invokeBlock(fakeIdentifierRequest, testAction)

      status(result) mustBe UNAUTHORIZED
    }
  }
}
