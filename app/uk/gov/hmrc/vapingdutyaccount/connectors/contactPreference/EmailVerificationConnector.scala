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

import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.EmailVerificationParser.{EmailVerificationDetailsType, GetEmailVerificationHttpReads}
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.EmailVerificationErrorResponse
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.CredentialId
import uk.gov.hmrc.vapingdutyaccount.utils.DownstreamLogging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationConnector @Inject()(
  config: AppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends DownstreamLogging {

  private val LOG_PREFIX = "[EmailVerificationConnector][getEmailVerification]"

  def getEmailVerification(credId: CredentialId)
                          (implicit hc: HeaderCarrier): Future[EmailVerificationDetailsType] =
      httpClient
        .get(url"${config.getVerifiedEmailsUrl(credId)}")
        .execute[EmailVerificationDetailsType](GetEmailVerificationHttpReads, ec)
        .recover { case ex: Exception =>
          val errMsg = logNonHttpError(LOG_PREFIX, hc, ex)
          Left(EmailVerificationErrorResponse(errMsg, None))
        }
}
