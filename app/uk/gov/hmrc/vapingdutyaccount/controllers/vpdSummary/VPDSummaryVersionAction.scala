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

package uk.gov.hmrc.vapingdutyaccount.controllers.vpdSummary

import com.google.inject.Inject
import play.api.http.{ContentTypes, HeaderNames as PlayHeaderNames}
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Results.Status
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.requests.{IdentifierRequest, VersionedIdentifierRequest}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{APIError, APIErrorFormat, ApiVersion}

import scala.concurrent.{ExecutionContext, Future}

class VPDSummaryVersionAction @Inject() (config: AppConfig)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[IdentifierRequest, VersionedIdentifierRequest] {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, VersionedIdentifierRequest[A]]] =
    Future.successful {
      ApiVersion.fromAcceptHeader(request.headers.get(PlayHeaderNames.ACCEPT)) match {
        case Right(version) => Right(VersionedIdentifierRequest(request, version))
        case Left(error)    => Left(notAcceptable(request, error))
      }
    }

  private def notAcceptable[A](request: IdentifierRequest[A], error: APIError): Result =
    new Status(error.code)(Json.toJson(error))
      .withHeaders(ResponseHeaders.extract(request.headers, config.xCorrelationId).toSeq*)
      .as(ContentTypes.JSON)
}
