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
import play.api.libs.json.Json

import java.time.LocalDate

class ReturnsSpec extends AnyFreeSpec with Matchers {

  "Returns" - {
    "must serialize to JSON correctly with currentReturn" in {
      val currentReturn = CurrentReturn("26AA", LocalDate.of(2026, 2, 27))
      val returns = Returns(
        currentReturn = Some(currentReturn),
        dueReturnsCount = 1,
        overdueReturnsCount = 0,
        completedReturnsCount = 0
      )

      val json = Json.toJson(returns)

      (json \ "currentReturn" \ "periodKey").as[String] shouldBe "26AA"
      (json \ "currentReturn" \ "dueDate").as[String] shouldBe "2026-02-27"
      (json \ "dueReturnsCount").as[Int] shouldBe 1
      (json \ "overdueReturnsCount").as[Int] shouldBe 0
      (json \ "completedReturnsCount").as[Int] shouldBe 0
    }

    "must serialize to JSON correctly without currentReturn" in {
      val returns = Returns(
        currentReturn = None,
        dueReturnsCount = 0,
        overdueReturnsCount = 2,
        completedReturnsCount = 1
      )

      val json = Json.toJson(returns)

      (json \ "currentReturn").asOpt[CurrentReturn] shouldBe None
      (json \ "dueReturnsCount").as[Int] shouldBe 0
      (json \ "overdueReturnsCount").as[Int] shouldBe 2
      (json \ "completedReturnsCount").as[Int] shouldBe 1
    }

    "must serialize to JSON with all counts as zero" in {
      val returns = Returns(
        currentReturn = None,
        dueReturnsCount = 0,
        overdueReturnsCount = 0,
        completedReturnsCount = 0
      )

      val json = Json.toJson(returns)

      (json \ "dueReturnsCount").as[Int] shouldBe 0
      (json \ "overdueReturnsCount").as[Int] shouldBe 0
      (json \ "completedReturnsCount").as[Int] shouldBe 0
    }
  }
}