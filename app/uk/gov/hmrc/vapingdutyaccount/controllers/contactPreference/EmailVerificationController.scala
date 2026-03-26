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

package uk.gov.hmrc.vapingdutyaccount.controllers.contactPreference

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.EmailVerificationConnector
import uk.gov.hmrc.vapingdutyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.vapingdutyaccount.models.CredentialId
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.GetVerificationStatusResponse

import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationController @Inject() (
  cc: ControllerComponents,
  emailVerificationConnector: EmailVerificationConnector,
  authorise: AuthorisedAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getEmailVerification(credId: CredentialId): Action[AnyContent] = authorise.async { implicit request =>
    emailVerificationConnector.getEmailVerification(credId).flatMap {
      case Left(errorResponse: ErrorResponse)                    => Future.successful(InternalServerError(s"Error: ${errorResponse.message}"))
      case Right(successResponse: GetVerificationStatusResponse) => Future.successful(Ok(Json.toJson(successResponse)))
    }
  }

}
