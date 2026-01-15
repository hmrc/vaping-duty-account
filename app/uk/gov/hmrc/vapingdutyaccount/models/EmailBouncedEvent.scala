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

package uk.gov.hmrc.vapingdutyaccount.models

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import play.api.libs.json.Reads.verifying

import java.time.{Instant, LocalDateTime}
import java.util.UUID

case class EmailBouncedEvent(
  eventId: UUID,
  subject: String,
  groupId: String,
  timestamp: LocalDateTime,
  event: EventDetails
)

object EmailBouncedEvent {

  implicit val eventReads: Reads[EmailBouncedEvent] = (
    (JsPath \ "eventId").read[UUID] and
      (JsPath \ "subject").read[String](verifying[String](a => a.trim.nonEmpty)) and
      (JsPath \ "groupId").read[String] and
      (JsPath \ "timestamp").read[LocalDateTime] and
      (JsPath \ "event").read[EventDetails]
  )(EmailBouncedEvent.apply _)

  implicit val eventWrites: Writes[EmailBouncedEvent] = Json.writes[EmailBouncedEvent]

}

case class Tags(
  enrolment: Option[String]
)

object Tags {
  implicit val tagsFormat: OFormat[Tags] = Json.format[Tags]
}

case class EventDetails(
  event: String,
  emailAddress: String,
  detected: Instant,
  code: Int,
  reason: String,
  tags: Option[Tags]
)

object EventDetails {
  implicit val returnAndUserDetailsFormat: OFormat[EventDetails] = Json.format[EventDetails]
}
