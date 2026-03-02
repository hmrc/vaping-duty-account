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

import com.google.inject.Inject
import play.api.Logging
import play.api.http.ContentTypes
import play.api.libs.json.*
import play.api.mvc.*
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.vapingdutyaccount.models.requests.IdentifierRequest
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI.*
import uk.gov.hmrc.vapingdutyaccount.services.vpdSummaryAPI.VPDSummaryAPIService

import scala.concurrent.{ExecutionContext, Future}

class VPDSummaryAPIController @Inject() (
    config: AppConfig,
    cc: ControllerComponents,
    authorise: AuthorisedAction,
    vpdSummaryAPIService: VPDSummaryAPIService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  given OFormat[APIError] = APIErrorFormat

  def getVpdSummary(vpdId: String): Action[AnyContent] = authorise.async { implicit request =>
    if (!vpdId.matches(config.vpdIdPattern)) {
      // Bad VpdId
      Future.successful(
        buildErrorResponse(request, APIErrors.BadRequest)
      )
    } else {
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(
        session = request.session,
        request = request.request
      )

      vpdSummaryAPIService.getVPDSummary(vpdId).map { vpdSummary =>
        Ok(Json.toJson(vpdSummary)).withHeaders(extractHeaders(request).toSeq: _*).as(ContentTypes.JSON)
      } recover { case _ =>
        buildErrorResponse(request, APIErrors.InternalServerError)
      }
    }
  }

  private def extractHeaders(request: IdentifierRequest[_]) = {
    val xRequestId     = request.headers.get(HeaderNames.xRequestId)
    val xCorrelationId = request.headers.get(config.xCorrelationId)

    Map[String, String]() ++
      xRequestId.map(HeaderNames.xRequestId -> _) ++
      xCorrelationId.map(config.xCorrelationId -> _)
  }

  private def buildErrorResponse(request: IdentifierRequest[AnyContent], error: APIError) = {

    new Status(error.code)(Json.toJson(error))
      .withHeaders(extractHeaders(request).toSeq: _*)
      .as(ContentTypes.JSON)
  }
}
