package com.mchange.fossilphant

import scala.collection.*
import java.time.ZoneId

case class FossilphantConfig(
  archivePath             : Option[String]               = None,
  mainTitle               : Option[String]               = None,
  mainTagline             : Option[String]               = None,
  overrideDisplayName     : Option[String]               = None,
  newSelfUrl              : Option[String]               = None,
  themeName               : String                       = "shatter",
  themeConfig             : immutable.Map[String,String] = immutable.Map.empty,
  pageLength              : Int                          = 20, // for themes that support paging
  contentTransformer      : ContentTransformer           = identity,
  toFollowersAsPublic     : Boolean                      = false,
  sensitiveAsPublic       : Boolean                      = false,
  suppressLinksToOriginal : Boolean                      = false,
  timestampTimezone       : ZoneId                       = ZoneId.systemDefault()
)

