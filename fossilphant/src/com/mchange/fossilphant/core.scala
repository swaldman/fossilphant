package com.mchange.fossilphant

import java.time.Instant
import scala.util.{Try,Success,Failure}

type UjsonObjValue = upickle.core.LinkedHashMap[String,ujson.Value]

val MastodonDateTimeFormatter = java.time.format.DateTimeFormatter.ISO_INSTANT

val ReverseChronologicalPublished : Ordering[UjsonObjValue] =
  val forward =
    Ordering.by[UjsonObjValue,Instant] { jso =>
      val attempt = Try {
        val timestamp = jso("published").str
        val ta = MastodonDateTimeFormatter.parse(timestamp)
        Instant.from(ta)
      }
      attempt match
        case Success(i) => i
        case Failure(t) =>
          System.err.println("Assigning random early time, error reading publication date from " + jso)
          t.printStackTrace()
          Instant.ofEpochMilli(math.round(math.random * 1_000_000).toLong)
    }
  forward.reverse

case class FossilphantContext( rawOutbox : UjsonObjValue, reverseChronologicalPosts : List[UjsonObjValue] )
