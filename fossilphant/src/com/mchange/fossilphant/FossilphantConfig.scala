package com.mchange.fossilphant

import java.time.ZoneId

case class FossilphantConfig(
  userDisplayName     : Option[String],
  newTagHost          : Option[String],
  newSelfHost         : Option[String],
  toFollowersAsPublic : Boolean = false,
  sensitiveAsPublic   : Boolean = false,
  timestampTimezone   : ZoneId = ZoneId.systemDefault()
)

