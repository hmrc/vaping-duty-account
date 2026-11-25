package uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.internal

import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{Access, ContactMethod, ContactPreferenceStatus, ManageContactPreference}

case class ContactPreferencesAndLinks(isInsolvent                 : Boolean,
                                      access                      : Option[Access],
                                      contactMethod               : Option[ContactMethod],
                                      contactPreferenceStatus     : Option[ContactPreferenceStatus],
                                      manageContactPreferencesLink: Option[ManageContactPreference])
