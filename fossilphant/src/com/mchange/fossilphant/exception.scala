package com.mchange.fossilphant

class FossilphantException( msg : String, cause : Throwable = null ) extends Exception( msg, cause )
class BadArchivePath( msg : String ) extends FossilphantException(msg)
class BadThemeUntemplate( msg : String ) extends FossilphantException(msg)


