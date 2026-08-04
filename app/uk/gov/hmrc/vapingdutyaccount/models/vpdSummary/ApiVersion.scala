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

/** HMRC media-type version of the VPD Summary API response, negotiated via the `Accept` header
  * (e.g. `application/vnd.hmrc.vpd-summary.1.1+json`).
  * 1.0 is defaulted to 1.4
  * anything else will return 406 Not Acceptable
  */
enum ApiVersion(val major: Int, val minor: Int) {
  case V1_0 extends ApiVersion(1, 0)
  case V1_4 extends ApiVersion(1, 4)
}

object ApiVersion {

  private val mediaTypePattern = """^application/vnd\.hmrc\.vpd-summary\.(\d+)\.(\d+)\+json$""".r

  def fromAcceptHeader(acceptHeader: Option[String]): Either[APIError, ApiVersion] =
    acceptHeader match {
      case Some(mediaTypePattern(major, minor)) =>
        values.find(v => v.major.toString == major && v.minor.toString == minor).toRight(APIErrors.NotAcceptable)
      case _                                    =>
        Left(APIErrors.NotAcceptable)
    }
}
