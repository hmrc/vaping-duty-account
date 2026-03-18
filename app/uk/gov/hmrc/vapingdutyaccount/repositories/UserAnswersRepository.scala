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

package uk.gov.hmrc.vapingdutyaccount.repositories

import org.mongodb.scala.model.*
import play.api.libs.json.Format
import uk.gov.hmrc.mdc.Mdc
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.crypto.CryptoProvider
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.UserAnswers

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

sealed trait UpdateResult
case object UpdateSuccess extends UpdateResult
case object UpdateFailure extends UpdateResult

@Singleton
class UserAnswersRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  cryptoProvider: CryptoProvider,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[UserAnswers](
      collectionName = "user-answers",
      mongoComponent = mongoComponent,
      domainFormat = UserAnswers.format(cryptoProvider.getCrypto),
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.dbTimeToLiveInSeconds, TimeUnit.SECONDS)
        ),
        IndexModel(
          Indexes.ascending("vpdId"),
          IndexOptions()
            .name("vpdId")
        ),
        IndexModel(
          Indexes.ascending("userId"),
          IndexOptions()
            .name("userId")
        )
      ),
      extraCodecs = Seq.empty,
      replaceIndexes = true
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private val replaceDontUpsert = ReplaceOptions().upsert(false)
  private val replaceUpsert     = ReplaceOptions().upsert(true)

  private def byVpdId(vpdId: String) = Filters.equal("vpdId", vpdId)
  private def byInternalId(internalId: String) = Filters.equal("userId", internalId)

  private def keepAlive(vpdId: String): Future[Boolean] =
    collection
      .updateOne(
        filter = byVpdId(vpdId),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => true)

  def get(vpdId: String): Future[Option[UserAnswers]] = {
    keepAlive(vpdId).flatMap { _ =>
      Mdc.preservingMdc {
        collection
          .find(byVpdId(vpdId))
          .headOption()
      }
    }
  }

  def set(answers: UserAnswers): Future[UpdateResult] = {

    val updatedAnswers = answers.copy(
      lastUpdated = Instant.now(clock)
    )

    collection
      .replaceOne(
        filter = byVpdId(updatedAnswers.vpdId),
        replacement = updatedAnswers,
        options = replaceDontUpsert
      )
      .toFuture()
      .map(res => if (res.getModifiedCount == 1) UpdateSuccess else UpdateFailure)
  }

  def add(answers: UserAnswers): Future[UserAnswers] = {

    val updatedAnswers = answers.copy(
      lastUpdated = Instant.now(clock),
      validUntil = Some(Instant.now(clock).plusSeconds(appConfig.dbTimeToLiveInSeconds))
    )

    collection
      .replaceOne(
        filter = byVpdId(updatedAnswers.vpdId),
        replacement = updatedAnswers,
        options = replaceUpsert
      )
      .toFuture()
      .map(_ => updatedAnswers)
  }

  def clearUserAnswersById(internalId: String): Future[Unit] =
    collection.deleteOne(filter = byInternalId(internalId)).toFuture().map(_ => ())
}
