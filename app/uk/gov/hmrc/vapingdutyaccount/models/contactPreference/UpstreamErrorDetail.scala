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

package uk.gov.hmrc.vapingdutyaccount.models.contactPreference

import play.api.libs.json.{Json, Reads}

case class UpstreamErrorDetail(code: String, text: String)

object UpstreamErrorDetail {
  implicit val reads: Reads[UpstreamErrorDetail] = Json.reads[UpstreamErrorDetail]
}

case class UpstreamErrorResponseBody(errors: UpstreamErrorDetail)

object UpstreamErrorResponseBody {
  implicit val reads: Reads[UpstreamErrorResponseBody] = Json.reads[UpstreamErrorResponseBody]
}
