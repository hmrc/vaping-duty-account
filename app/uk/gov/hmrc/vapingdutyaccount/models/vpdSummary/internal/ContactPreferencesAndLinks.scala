package uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.internal

import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{Access, ContactMethod, ContactPreferenceStatus}

case class ContactPreferencesAndLinks(isInsolvent            : Boolean,
                                      access                 : Option[Access],
                                      contactMethod          : Option[ContactMethod],
                                      contactPreferenceStatus: Option[ContactPreferenceStatus])
