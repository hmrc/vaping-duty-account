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
import play.api.http.Status.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.GetVerificationStatusResponse
import uk.gov.hmrc.vapingdutyaccount.models.exceptions.*
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

  private val LOG_PREFIX = "[EmailVerificationConnector][getEmailVerification]"

  def getEmailVerification(credId: CredentialId)
                          (implicit hc: HeaderCarrier): Future[GetVerificationStatusResponse] =
      httpClient
        .get(url"${config.getVerifiedEmailsUrl(credId)}")
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .flatMap {
          case Right(response) =>
            Try(response.json.as[GetVerificationStatusResponse]) match {
              case Success(response) =>
                Future.successful(response)
              case Failure(error) =>
                logger.error(s"$LOG_PREFIX [BUG] Unable to parse email records for credId $credId - check our model", error)
                Future.failed(ParseException(s"Parse failure for $credId", error))
            }
          case Left(error)     =>
            error.statusCode match {
              case NOT_FOUND   =>
                logger.info(s"$LOG_PREFIX No verified emails found for credId $credId")
                Future.successful(GetVerificationStatusResponse(emails = List.empty))
              case BAD_REQUEST =>
                logger.error(s"$LOG_PREFIX [BUG] Invalid request for email verification for credId $credId - check our request")
                Future.failed(BadRequestException(s"Bad request for $credId"))
              case statusCode  =>
                logger.warn(s"$LOG_PREFIX Upstream error ($statusCode) for email verification credId $credId")
                Future.failed(UpstreamServiceException(s"Upstream error for $credId", statusCode))
            }
        }
        .recoverWith {
          case ex: ConnectorException =>
            Future.failed(ex)
          case ex =>
            logger.error(s"$LOG_PREFIX Unexpected exception fetching email verification for credId $credId", ex)
            Future.failed(UpstreamServiceException(s"Exception for $credId", 500, ex))
        }

}
