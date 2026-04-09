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
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.EmailVerificationConnector
import uk.gov.hmrc.vapingdutyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.CredentialId

import scala.concurrent.ExecutionContext

class EmailVerificationController @Inject() (
  cc: ControllerComponents,
  emailVerificationConnector: EmailVerificationConnector,
  authorise: AuthorisedAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getEmailVerification(credId: CredentialId): Action[AnyContent] = authorise.async { implicit request =>
    emailVerificationConnector.getEmailVerification(credId)
      .map {
        case Right(response) =>
          Ok(Json.toJson(response))
        case Left(error) =>
          logger.warn(s"[getEmailVerification] Connector failure for credId $credId: ${error}")
          InternalServerError("Internal server error")
      }
  }

}
