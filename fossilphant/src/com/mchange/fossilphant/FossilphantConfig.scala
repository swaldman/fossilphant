package com.mchange.fossilphant

import java.time.ZoneId

case class FossilphantConfig(
  archivePath         : Option[String]     = None,
  mainTitle           : Option[String]     = None,
  mainTagline         : Option[String]     = None,
  overrideDisplayName : Option[String]     = None,
  newSelfUrl          : Option[String]     = None,
  themeName           : String             = "tower",
  themeConfig         : Map[String,String] = Map.empty,
  pageLength          : Int                = 100, // for themes that support paging
  contentTransformer  : String => String   = identity,
  toFollowersAsPublic : Boolean            = false,
  sensitiveAsPublic   : Boolean            = false,
  timestampTimezone   : ZoneId             = ZoneId.systemDefault()
)

