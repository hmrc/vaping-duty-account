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
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.mvc.*
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.http
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
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
      logger.info(s"[SummaryAPI] [ƒ: getVpdSummary] Bad VpdId received; did not satisfy regex validation. VpdId=[${vpdId}]")
      // Bad VpdId
      buildErrorResponse(request, APIErrors.BadRequest)
    } else {
      logger.info(s"[SummaryAPI] [ƒ: getVpdSummary]: VpdId validated; initiating request to API#5786 for SubscriptionSummary data...")

      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(
        session = request.session,
        request = request.request
      )

      vpdSummaryAPIService.getVPDSummary(vpdId)
        .flatMap((transformedApiResponse: Either[ErrorResponse, VPDSummary]) => 
          transformedApiResponse
          .match {
            case Right(vpdSummary: VPDSummary) => {
                logger.info(
                  s"[SummaryAPI] [ƒ: getVpdSummary] Successfully retrieved SubscriptionSummary data from API#5786 for VpdId=[$vpdId]"
                )
      
                Future.successful(
                  Ok(Json.toJson(vpdSummary)).
                    withHeaders(extractHeaders(request).toSeq: _*).as(ContentTypes.JSON)
                )
            }
            case Left(error: ErrorResponse)                      => { // Unable to retrieve data
              logger.info(
                s"[SummaryAPI] [ƒ: getVpdSummary] Unable to retrieve SubscriptionSummary data for VpdId=[$vpdId]. Received statusCode=[${error.statusCode}]"
              )
    
              error.statusCode match {
                case INTERNAL_SERVER_ERROR => buildErrorResponse(request, APIErrors.InternalServerError)
                case BAD_REQUEST           => buildErrorResponse(request, APIErrors.BadRequest)
                case UNAUTHORIZED          => buildErrorResponse(request, APIErrors.Unauthorised)
                case _: Int                => buildErrorResponse(request, APIErrors.ServiceUnavailable)
              }
            }
          }
        )
    }
  }

  private def extractHeaders(request: IdentifierRequest[_]) = {
    val xRequestId = request.headers.get(HeaderNames.xRequestId)
    val xCorrelationId = request.headers.get(config.xCorrelationId)

    Map[String, String]() ++
      xRequestId.map(HeaderNames.xRequestId -> _) ++
      xCorrelationId.map(config.xCorrelationId -> _)
  }

  private def buildErrorResponse(request: IdentifierRequest[AnyContent], error: APIError) = {
    logger.info(s"[SummaryAPI] [getVpdSummary] [ƒ: buildErrorResponse]: Sending error for Request ${request.id} with message \"${error.message}\"")

    Future.successful(
      new Status(error.code)(Json.toJson(error)).as(ContentTypes.JSON)
    )
  }
}
