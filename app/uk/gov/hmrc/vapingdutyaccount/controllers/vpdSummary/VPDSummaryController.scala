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
import play.api.Logging
import play.api.http.ContentTypes
import play.api.libs.json.*
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.requests.VersionedIdentifierRequest
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*
import uk.gov.hmrc.vapingdutyaccount.services.VPDSummaryService

import scala.concurrent.{ExecutionContext, Future}

class VPDSummaryController @Inject()(
                                      config: AppConfig,
                                      cc: ControllerComponents,
                                      authorise: AuthorisedAction,
                                      versionAction: VPDSummaryVersionAction,
                                      vpdSummaryService: VPDSummaryService
                                       )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with Logging {

  given Writes[APIError] = APIErrorFormat

  def getVpdSummary(vpdId: VpdId): Action[AnyContent] = (authorise andThen versionAction).async { implicit request =>
    if (config.vpdSummaryRESTAPIEnabled) {
      vpdSummaryService.getVPDSummary(vpdId)
        .map(vpdSummary => buildSuccessResponse(vpdSummary, request))
        .recover { case _ =>
          buildErrorResponse(request, APIErrors.InternalServerError)
        }
    } else {
      logger.warn("[getVpdSummary] Returning 503 ServiceUnavailable because config key `bta.tile.api` == false")
      Future.successful(buildErrorResponse(request, APIErrors.ServiceUnavailable))
    }
  }

  private def buildErrorResponse(request: VersionedIdentifierRequest[AnyContent], error: APIError) = {

    new Status(error.code)(Json.toJson(error))
      .withHeaders(ResponseHeaders.extract(request.headers, config.xCorrelationId).toSeq: _*)
      .as(ContentTypes.JSON)
  }

  private def buildSuccessResponse(vpdSummary: VPDSummary, request: VersionedIdentifierRequest[_]): Result =

      Ok(Json.toJson(vpdSummary))
        .withHeaders(ResponseHeaders.extract(request.headers, config.xCorrelationId).toSeq: _*)
        .as(ContentTypes.JSON)
}
