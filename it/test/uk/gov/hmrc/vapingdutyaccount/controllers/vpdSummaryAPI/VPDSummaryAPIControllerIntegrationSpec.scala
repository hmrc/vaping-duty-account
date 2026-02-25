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

package uk.gov.hmrc.vapingdutyaccount.controllers.vpdSummaryAPI

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

import play.api.test.FakeRequest
import uk.gov.hmrc.vapingdutyaccount.base.ISpecBase
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI.{APIErrorFormat, APIErrors}
import play.api.libs.json.Json
import play.api.mvc.Result

import scala.concurrent.Future

class VPDSummaryAPIIntegrationSpec extends ISpecBase {

  "the VPD Summary API must " - {
    "return 401 when incoming requests are not appropriately authorised" in new SetUp {
      stubNotAuthorised(vpdId)

      val response: Future[Result] = callRoute(
        FakeRequest("GET", routes.VPDSummaryAPIController.getVpdSummary(vpdId).url)
      )

      status(response)        mustBe UNAUTHORIZED
      contentAsJson(response) mustBe Json.toJson(APIErrors.Unauthorised)
    }
  }

  class SetUp {
    val url = "http://localhost:8142/vpd/summary/XIWK1104405WK"
  }
}
