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

package uk.gov.hmrc.vapingdutyaccount.models.identifiers

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.PathBindable

case class CredentialId(id: String) {
  override def toString: String = id
}

object CredentialId {
  given PathBindable[CredentialId] = new PathBindable[CredentialId] {

    override def bind(key: String, value: String): Either[String, CredentialId] = {
      value match {
        case _: String if value.length < 17 => Right(CredentialId(value))
        case _                              => Left("Invalid CredentialId")
      }
    }

    override def unbind(key: String, value: CredentialId): String = value.toString
  }
  given OFormat[CredentialId] = Json.format[CredentialId]
}
