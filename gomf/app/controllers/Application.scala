package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /**
   * ロビーページ
   */
  def lobby = Action {
    Ok(utils.Scalate.Template("lobby.jade").render(Map()))
  }
}