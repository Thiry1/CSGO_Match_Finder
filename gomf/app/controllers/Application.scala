package controllers

import play.api.Play
import play.api.libs.iteratee.{Iteratee, Done, Input, Enumerator}
import play.api.libs.json.{JsArray, JsString, JsObject, JsValue}
import play.api.mvc._
import models.User

import scala.concurrent.Future

//import models.WS.Room
import scala.concurrent.ExecutionContext.Implicits.global
import collection._

import services.{ChatService, QueueService}

object Application extends Controller with ChatService {

  def index = Action {
    Redirect(routes.Application.lobbyWithoutRoomId)
  }

  /**
   * ルームID未指定でロビーページにアクセスした場合にロビーIDにSteamIDを与える
   */
  def lobbyWithoutRoomId = Action { implicit request =>
    session.get("steamId") match {
      //ログインしていない場合ログインページへリダイレクト
      case None => Redirect(routes.Auth.login)

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
    import play.api.Play.current

    roomId.matches("""^[0-9a-zA-Z]+$""") match {
      case false => InternalServerError("ルームIDは半角英数字のみで構成してください")
      case true  => {
        session.get("steamId") match {
          //ログインしていない場合ログインページへリダイレクト
          case None => Redirect(routes.Auth.loginWithRedirect(roomId))

          case Some(steamId) => {
            User.isLoggedIn(steamId) match {
              //ユーザーデータが見つからない場合はトップページへリダイレクト
              case false => Redirect(routes.Auth.modify)

              case true => {
                val player = User.info(steamId)
                //コンフィグからマップ一覧を取得
                Play.application.configuration.getStringList("csgo.maps") match {
                  case None => InternalServerError("マップ一覧ファイルの取得に失敗しました")
                  //マップ取得に成功した場合はページを表示する
                  case Some(maps) => {
                    import scala.collection.JavaConverters._

                    val params = immutable.Map ('player -> player, 'roomId -> roomId, 'maps -> maps.asScala.toSeq)
                    Ok( utils.Scalate.Template ("lobby.jade").render(params) )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * room用のJSを返す
   */
  def roomJs(roomId: String) = Action { implicit request =>
    session.get("steamId") match {
      //ログインしていない場合トップページへリダイレクト
      case None => InternalServerError("Login Required")

      case Some(steamId) => {
        Ok(views.js.socket(request, roomId, steamId)).as("text/javascript")
      }
    }
  }

  /**
   * queue用のJSを返す
   */
  def queueJs(roomId: String) = Action { implicit request =>
    Ok(views.js.queue(request, roomId)).as("text/javascript")
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
  def queue(roomId: String) = WebSocket.async[JsValue] { implicit request =>
    QueueService.start(roomId)
  }
}