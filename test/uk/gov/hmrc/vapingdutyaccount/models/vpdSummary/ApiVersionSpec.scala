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

package uk.gov.hmrc.vapingdutyaccount.models.vpdSummary

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ApiVersionSpec extends AnyFreeSpec with Matchers {

  "ApiVersion.fromAcceptHeader" - {
    "must return V1_0 for the 1.0 media type" in {
      ApiVersion.fromAcceptHeader(Some("application/vnd.hmrc.vpd-summary.1.0+json")) shouldBe Right(ApiVersion.V1_0)
    }

    "must return V1_4 for the 1.4 media type" in {
      ApiVersion.fromAcceptHeader(Some("application/vnd.hmrc.vpd-summary.1.4+json")) shouldBe Right(ApiVersion.V1_4)
    }

    "must return Left(NotAcceptable) when the header is absent" in {
      ApiVersion.fromAcceptHeader(None) shouldBe Left(APIErrors.NotAcceptable)
    }

    "must return Left(NotAcceptable) when the header is a different media type entirely" in {
      ApiVersion.fromAcceptHeader(Some("application/json")) shouldBe Left(APIErrors.NotAcceptable)
    }

    "must return Left(NotAcceptable) when the version segment is malformed" in {
      ApiVersion.fromAcceptHeader(Some("application/vnd.hmrc.vpd-summary.one+json")) shouldBe Left(APIErrors.NotAcceptable)
    }

    "must return Left(NotAcceptable) when the version is well-formed but unsupported" in {
      ApiVersion.fromAcceptHeader(Some("application/vnd.hmrc.vpd-summary.2.0+json")) shouldBe Left(APIErrors.NotAcceptable)
      ApiVersion.fromAcceptHeader(Some("application/vnd.hmrc.vpd-summary.1.1+json")) shouldBe Left(APIErrors.NotAcceptable)
    }
  }
}
