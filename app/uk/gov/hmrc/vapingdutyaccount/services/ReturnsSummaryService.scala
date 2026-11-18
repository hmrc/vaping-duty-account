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

package uk.gov.hmrc.vapingdutyaccount.services

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.HttpVerbs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{CompleteReturn, Returns, ReturnsSection, ViewReturns}

import scala.concurrent.{ExecutionContext, Future}

/** Builds the returns section of the VPD Summary, and its links, from the manufacturer's obligations. */
@Singleton
class ReturnsSummaryService @Inject()(
  config: AppConfig,
  getObligationsService: GetObligationsService,
  obligationService: ObligationService
)(implicit ec: ExecutionContext) extends Logging {

  private val PERIOD_QUERY_PARAMETER = "period"
  private val SINGLE_OPEN_RETURN     = 1
  private val NO_COMPLETED_RETURNS   = 0
  private val NO_RETURNS             = 0

  def getReturnsSection(vpdId: VpdId)(using HeaderCarrier): Future[ReturnsSection] =
    getObligationsService.getObligationDetails(vpdId)
      .map(obligationService.processObligations)
      .map(buildSection)
      .recover { case ex =>
        logger.warn(s"[ReturnsSummaryService][getReturnsSection] Failed to retrieve obligations. Message: ${ex.getMessage}")
        ReturnsSection.error
      }

  private def buildSection(returns: Option[Returns]): ReturnsSection =
    ReturnsSection(
      returns        = returns,
      completeReturn = returns.flatMap(completeReturnLink),
      viewReturns    = returns.flatMap(viewReturnsLink)
    )

  private def completeReturnLink(returns: Returns): Option[CompleteReturn] =
    returns.currentReturn.map(current =>
      CompleteReturn(s"${config.completeReturnUrlPrefix}?$PERIOD_QUERY_PARAMETER=${current.periodKey}", HttpVerbs.GET)
    )

  private def viewReturnsLink(returns: Returns): Option[ViewReturns] = {
    val openReturns      = returns.dueReturnsCount.getOrElse(NO_RETURNS) + returns.overdueReturnsCount.getOrElse(NO_RETURNS)
    val completedReturns = returns.completedReturnsCount.getOrElse(NO_RETURNS)

    Option.when(openReturns > SINGLE_OPEN_RETURN || completedReturns > NO_COMPLETED_RETURNS)(
      ViewReturns(config.viewReturnsUrl, HttpVerbs.GET)
    )
  }
}
