//> using scala 3.3.0
//> using dep "com.lihaoyi::os-lib:0.9.1"
//> using dep "com.lihaoyi::ujson:3.1.2"

val NoteVal = ujson.Str("Note")
val AnnounceVal = ujson.Str("AnnounceVal")

val archiveDirPath = os.Path(args(0))
val outboxJsonPath = archiveDirPath / "outbox.json"

// we are very brittlely assuming everything goes right...
val outbox = ujson.read(os.read.stream(outboxJsonPath) )
val items = outbox.obj("orderedItems").arr.map( _.obj )
val grouped = items.groupBy( _("type").str )

//val objects = outbox.obj("orderedItems").arr.map( _.obj("object") )
//val notes = objects.filter( _.obj("type") == NoteVal )
//val notObj = objects.filter:
//  case _ : ujson.Obj => false
//  case _             => true

//println( notes.size )
//println( objects.take(3) )
//println( notes )
//println( ujson.write( notObj.take(3) ) )
println( grouped.keys )



