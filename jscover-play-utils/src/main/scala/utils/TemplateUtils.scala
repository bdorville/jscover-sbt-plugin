package utils

object TemplateUtils {
  def jsInstrumented(assetsPath:String):String = {
    val prefix:String = (play.api.Play.isDev(play.api.Play.current) || play.api.Play.isTest(play.api.Play.current)) match {
      case true => "jscover/"
      case false => ""
    }
    prefix + assetsPath
  }
}
