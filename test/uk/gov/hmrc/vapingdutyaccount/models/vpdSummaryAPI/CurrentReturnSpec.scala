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

package uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

import java.time.LocalDate

class CurrentReturnSpec extends AnyFreeSpec with Matchers {

  "CurrentReturn" - {
    "must serialize to JSON correctly" in {
      val currentReturn = CurrentReturn("26AA", LocalDate.of(2026, 2, 27))

      val json = Json.toJson(currentReturn)

      (json \ "periodKey").as[String] shouldBe "26AA"
      (json \ "dueDate").as[String] shouldBe "2026-02-27"
    }

    "must serialize to JSON with different period keys" in {
      val testCases = Seq(
        ("26AA", LocalDate.of(2026, 2, 27)),
        ("26AB", LocalDate.of(2026, 3, 27)),
        ("25AL", LocalDate.of(2026, 1, 27))
      )

      testCases.foreach { case (periodKey, dueDate) =>
        val currentReturn = CurrentReturn(periodKey, dueDate)
        val json = Json.toJson(currentReturn)

        (json \ "periodKey").as[String] shouldBe periodKey
        (json \ "dueDate").as[String] shouldBe dueDate.toString
      }
    }
  }
}