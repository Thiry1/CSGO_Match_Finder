package controllers

import play.api.libs.iteratee.{Iteratee, Done, Input, Enumerator}
import play.api.libs.json.{JsArray, JsString, JsObject, JsValue}
import play.api.mvc._
import models.User

import scala.concurrent.Future

//import models.WS.Room
import scala.concurrent.ExecutionContext.Implicits.global
import collection._

import services.ChatService

object Application extends Controller with ChatService {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /**
   * ルームID未指定でロビーページにアクセスした場合にロビーIDにSteamIDを与える
   */
  def lobbyWithoutRoomId = Action { implicit request =>
    session.get("steamId") match {
      //ログインしていない場合トップページへリダイレクト
      case None => Redirect(routes.Application.index)

      case Some(steamId) => {
        //ルームIDにSteamIDを渡す
        Redirect(routes.Application.lobby(steamId))
      }
    }
  }

  /**
   * ロビーページ
   */
  def lobby(roomId: String) = Action { implicit request =>

    session.get("steamId") match {
      //ログインしていない場合トップページへリダイレクト
      case None => Redirect(routes.Application.index)

      case Some(steamId) => {
        User.isLoggedIn(steamId) match {
          //ユーザーデータが見つからない場合はトップページへリダイレクト
          case false => Redirect(routes.Auth.modify)
          case true => {
            val player = User.info(steamId)
            val params = immutable.Map ('player -> player, 'roomId -> roomId)
            Ok(utils.Scalate.Template ("lobby.jade").render(params) )
          }
        }
      }
    }

  }

  /**
   * WebSocket用のJSを返す
   */
  def socketJs(roomId: String) = Action { implicit request =>
    session.get("steamId") match {
      //ログインしていない場合トップページへリダイレクト
      case None => InternalServerError("Login Required")

      case Some(steamId) => {
        Ok(views.js.socket(request, roomId, steamId)).as("text/javascript")
      }
    }
  }

  /**
   * ルーム内のチャット等
   */
  def room(roomId: String) = WebSocket.async[JsValue] { implicit request =>
    //SteamID取得
    session.get("steamId") match {
      //SteamIDが取得できない場合
      case None => {
        val in = Iteratee.ignore[JsValue]
        val out = Enumerator[JsValue](JsObject(Seq("error" -> JsString("Login Required")))).andThen(Enumerator.enumInput(Input.EOF))

        Future(in, out)
      }

      case Some(steamId) => chatService.start(roomId, steamId)
    }

  }

  /**
   * 試合募集キュー
   */
  def queue = TODO
}