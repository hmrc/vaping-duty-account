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

/** The returns part of the VPD Summary, with the links that belong to it. Internal only, never serialised. */
final case class ReturnsSection(
  returns: Option[Returns],
  completeReturn: Option[CompleteReturn],
  viewReturns: Option[ViewReturns]
)

object ReturnsSection {

  /** Used when the obligations could not be retrieved: flag the error and offer no return links. */
  val error: ReturnsSection = ReturnsSection(
    returns = Some(Returns(
      hasReturnsError       = true,
      currentReturn         = None,
      dueReturnsCount       = None,
      overdueReturnsCount   = None,
      completedReturnsCount = None
    )),
    completeReturn = None,
    viewReturns    = None
  )
}
