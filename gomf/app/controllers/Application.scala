package controllers

import play.api._
import play.api.mvc._
import models.User

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /**
   * ロビーページ
   */
  def lobby = Action { implicit request =>
    session.get("steamId") match {
      //ログインしていない場合トップページへリダイレクト
      case None => Redirect(routes.Application.index)

      case Some(steamId) => {
        User.isLoggedIn(steamId) match {
          //ユーザーデータが見つからない場合はトップページへリダイレクト
          case false => Redirect (routes.Application.index)
          case true => {
            val player = User.info(steamId)
            val params = Map ('player -> player)
            Ok(utils.Scalate.Template ("lobby.jade").render(params) )
          }
        }
      }

    }
  }

}