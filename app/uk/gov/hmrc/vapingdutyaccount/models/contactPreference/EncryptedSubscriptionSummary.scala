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

package uk.gov.hmrc.vapingdutyaccount.models.contactPreference

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

/** The subscription summary held in the user answers, with its personal data encrypted for storage. */
final case class EncryptedSubscriptionSummary(
  paperlessPreference: Boolean,
  emailAddress: Option[SensitiveString],
  emailVerification: Option[Boolean],
  bouncedEmail: Option[Boolean],
  correspondenceAddress: SensitiveString
)

object EncryptedSubscriptionSummary {

  implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[EncryptedSubscriptionSummary] =
    (
      (__ \ "paperlessPreference").format[Boolean] and
        (__ \ "emailAddress").formatNullable[SensitiveString] and
        (__ \ "emailVerification").formatNullable[Boolean] and
        (__ \ "bouncedEmail").formatNullable[Boolean] and
        (__ \ "correspondenceAddress").format[SensitiveString]
    )(EncryptedSubscriptionSummary.apply, o => Tuple.fromProductTyped(o))

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
}
