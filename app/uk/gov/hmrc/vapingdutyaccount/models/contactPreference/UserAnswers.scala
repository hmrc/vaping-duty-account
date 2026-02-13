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
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.vapingdutyaccount.models.*

import java.time.{Clock, Instant}

case class DecryptedUA(
                        vpdId: String,
                        userId: String,
                        subscriptionSummary: SubscriptionSummary,
                        emailAddress: Option[String],
                        data: JsObject = Json.obj(),
                        startedTime: Instant,
                        lastUpdated: Instant,
                        validUntil: Option[Instant] = None
)

object DecryptedUA {
  def fromUA(userAnswers: UserAnswers): DecryptedUA =
    DecryptedUA(
      vpdId = userAnswers.vpdId,
      userId = userAnswers.userId,
      subscriptionSummary = SubscriptionSummary(
        userAnswers.subscriptionSummary.paperlessPreference,
        userAnswers.subscriptionSummary.emailAddress.map(_.decryptedValue),
        userAnswers.subscriptionSummary.emailVerification,
        userAnswers.subscriptionSummary.bouncedEmail,
        userAnswers.subscriptionSummary.correspondenceAddress.decryptedValue,
        userAnswers.subscriptionSummary.countryCode.map(_.decryptedValue)
      ),
      emailAddress = userAnswers.emailAddress.map(_.decryptedValue),
      data = userAnswers.data,
      startedTime = userAnswers.startedTime,
      lastUpdated = userAnswers.lastUpdated,
      validUntil = userAnswers.validUntil
    )

  implicit val format: OFormat[DecryptedUA] = (
    (__ \ "vpdId").format[String] and
      (__ \ "userId").format[String] and
      (__ \ "subscriptionSummary").format[SubscriptionSummary] and
      (__ \ "emailAddress").formatNullable[String] and
      (__ \ "data").formatWithDefault[JsObject](Json.obj()) and
      (__ \ "startedTime").format(MongoJavatimeFormats.instantFormat) and
      (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat) and
      (__ \ "validUntil").formatNullable(MongoJavatimeFormats.instantFormat)
  )(DecryptedUA.apply, o => Tuple.fromProductTyped(o))

}

case class UserAnswers(vpdId: String,
                        userId: String,
                        subscriptionSummary: SubscriptionSummaryBackend,
                        emailAddress: Option[SensitiveString],
                        data: JsObject = Json.obj(),
                        startedTime: Instant,
                        lastUpdated: Instant,
                        validUntil: Option[Instant] = None)

object UserAnswers {
  def createUserAnswers(
    userDetails: UserDetails,
    contactPreferences: SubscriptionContactPreferences,
    clock: Clock
  ): UserAnswers = {
    val existingEmail: Option[SensitiveString] = contactPreferences.emailAddress.map(SensitiveString.apply)

    val correspondenceAddress: String = Seq(
      contactPreferences.addressLine1,
      contactPreferences.addressLine2,
      contactPreferences.addressLine3,
      contactPreferences.addressLine4,
      contactPreferences.postcode
    ).flatten.mkString("\n")

    UserAnswers(
      vpdId = userDetails.vpdId,
      userId = userDetails.userId,
      subscriptionSummary = SubscriptionSummaryBackend(
        contactPreferences.paperlessPreference,
        existingEmail,
        contactPreferences.verifiedEmail,
        contactPreferences.bouncedEmail,
        SensitiveString(correspondenceAddress),
        contactPreferences.country.map(SensitiveString.apply)
      ),
      emailAddress = None,
      startedTime = Instant.now(clock),
      lastUpdated = Instant.now(clock)
    )
  }

  def fromDecryptedUA(decryptedUA: DecryptedUA): UserAnswers =
    UserAnswers(
      vpdId = decryptedUA.vpdId,
      userId = decryptedUA.userId,
      subscriptionSummary = SubscriptionSummaryBackend(
        decryptedUA.subscriptionSummary.paperlessPreference,
        decryptedUA.subscriptionSummary.emailAddress.map(SensitiveString.apply),
        decryptedUA.subscriptionSummary.emailVerification,
        decryptedUA.subscriptionSummary.bouncedEmail,
        SensitiveString(decryptedUA.subscriptionSummary.correspondenceAddress),
        decryptedUA.subscriptionSummary.countryCode.map(SensitiveString.apply)
      ),
      emailAddress = decryptedUA.emailAddress.map(SensitiveString.apply),
      data = decryptedUA.data,
      startedTime = decryptedUA.startedTime,
      lastUpdated = decryptedUA.lastUpdated,
      validUntil = decryptedUA.validUntil
    )

  implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[UserAnswers] =
    (
      (__ \ "vpdId").format[String] and
        (__ \ "userId").format[String] and
        (__ \ "subscriptionSummary").format[SubscriptionSummaryBackend] and
        (__ \ "emailAddress").formatNullable[SensitiveString] and
        (__ \ "data").formatWithDefault[JsObject](Json.obj()) and
        (__ \ "startedTime").format(MongoJavatimeFormats.instantFormat) and
        (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat) and
        (__ \ "validUntil").formatNullable(MongoJavatimeFormats.instantFormat)
    )(UserAnswers.apply, o => Tuple.fromProductTyped(o))

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
}

case class SubscriptionSummary(
  paperlessPreference: Boolean,
  emailAddress: Option[String],
  emailVerification: Option[Boolean],
  bouncedEmail: Option[Boolean],
  correspondenceAddress: String,
  countryCode: Option[String]
)

object SubscriptionSummary {
  implicit val subscriptionSummaryFormat: OFormat[SubscriptionSummary] = Json.format[SubscriptionSummary]
}

case class SubscriptionSummaryBackend(
  paperlessPreference: Boolean,
  emailAddress: Option[SensitiveString],
  emailVerification: Option[Boolean],
  bouncedEmail: Option[Boolean],
  correspondenceAddress: SensitiveString,
  countryCode: Option[SensitiveString]
)

object SubscriptionSummaryBackend {

  implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[SubscriptionSummaryBackend] =
    (
      (__ \ "paperlessPreference").format[Boolean] and
        (__ \ "emailAddress").formatNullable[SensitiveString] and
        (__ \ "emailVerification").formatNullable[Boolean] and
        (__ \ "bouncedEmail").formatNullable[Boolean] and
        (__ \ "correspondenceAddress").format[SensitiveString] and
        (__ \ "countryCode").formatNullable[SensitiveString]
    )(SubscriptionSummaryBackend.apply, o => Tuple.fromProductTyped(o))

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

}
