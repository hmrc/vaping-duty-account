/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference

import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.GetVerificationStatusResponse
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.CredentialId

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class EmailVerificationConnector @Inject()(
  config: AppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  def getEmailVerification(credId: CredentialId)
                          (implicit hc: HeaderCarrier): Future[GetVerificationStatusResponse] = {
    getEmailVerificationResponse(credId).flatMap {
      case Right(successResponse: GetVerificationStatusResponse) =>
        Future.successful(successResponse)

      case Left(errorResponse: ErrorResponse) =>
        Future.failed(InternalServerException(s"Error: ${errorResponse.message}"))
    }
  }

  private def getEmailVerificationResponse(credId: CredentialId)
                                          (implicit hc: HeaderCarrier): Future[Either[ErrorResponse, GetVerificationStatusResponse]] =
    
      httpClient
        .get(url"${config.getVerifiedEmailsUrl(credId)}")
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map(response =>  emailVerificationParser(credId, response))
        .recoverWith { case _: Exception =>
          logger.warn(s"An exception was returned while trying to fetch the email verification list for credId $credId")
          Future.successful(
            Left(
              ErrorResponse(INTERNAL_SERVER_ERROR, "Exception returned while trying to fetch email verification list")
            )
          )
        }

  private def emailVerificationParser(credId: CredentialId, response: Either[UpstreamErrorResponse, HttpResponse]) = {
    response.match {
      case Right(response) =>
        Try {
          response.json
            .as[GetVerificationStatusResponse]
        } match {
          case Success(response) =>
            Right(response)
          case Failure(_) =>
            logger.warn(s"Unable to parse email records successful response for credId $credId")
            Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse email records successful response"))
        }
      case Left(error) =>
        error.statusCode match {
          case NOT_FOUND =>
            Right(GetVerificationStatusResponse(emails = List.empty))
          case _ =>
            logger.warn(
              s"Unexpected response for email verification list for credId: $credId. status: ${error.statusCode}"
            )
            Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected response for email verification list"))
        }
    }
  }
}
