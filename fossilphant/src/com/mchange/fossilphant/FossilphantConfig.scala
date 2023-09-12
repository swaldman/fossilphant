package com.mchange.fossilphant

import java.time.ZoneId

case class FossilphantConfig(
  archivePath         : String,
  mainTitle           : String = "Mastodon Archive",
  mainTagline         : String = "",
  userDisplayName     : Option[String] = None,
  newTagHost          : Option[String] = None,
  newSelfHost         : Option[String] = None,
  toFollowersAsPublic : Boolean = false,
  sensitiveAsPublic   : Boolean = false,
  timestampTimezone   : ZoneId = ZoneId.systemDefault()
)

