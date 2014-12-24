package controllers

import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
//import play.api.libs.openid.OpenID
import utils.OpenID
import play.api.mvc._
import models.{SteamAPI, User}




/**
 * 認証処理関連クラス
 */
object Auth extends Controller {
  /**
   * 認証トップページ。ログインしていればトップページへ、ログインしていなければログインページへ転送する
   */
  def index = Action { implicit request =>

    session.get("steamId") match {
      //ログインしていればトップページへリダイレクト
      case Some(steamId) => Redirect(routes.Application.index)
      //ログインしていなければログインページヘリダイレクト
      case None => Redirect(routes.Auth.login)
    }
  }

  /**
   * ログイン後に指定されたルームへリダイレクトする
   */
  def loginWithRedirect(roomId: String) = Action.async { implicit request =>

    OpenID.redirectURL(
      "http://steamcommunity.com/openid",
      routes.Auth.steamOpenIDCallback(roomId).absoluteURL(),
      claimedId = Some("http://specs.openid.net/auth/2.0/identifier_select"),
      realm = Some(routes.Application.index.absoluteURL())
    )
      .map(url => Redirect(url))
      .recover {
      case t: Throwable =>
        Logger.error("Steam Auth Error" + t)
        InternalServerError("Steam Auth Error. Please try again later.")
    }

  }

  /**
   * ログインページを表示する
   */
  def login = Action.async { implicit request =>

    OpenID.redirectURL(
      "http://steamcommunity.com/openid",
      routes.Auth.steamOpenIDCallback("useSteamIdToLobbyName").absoluteURL(),
      claimedId = Some("http://specs.openid.net/auth/2.0/identifier_select"),
      realm = Some(routes.Application.index.absoluteURL())
    )
    .map(url => Redirect(url))
    .recover {
      case t: Throwable =>
        Logger.error("Steam Auth Error" + t)
        InternalServerError("Steam Auth Error. Please try again later.")
    }

  }

  /**
   * ログアウト処理
   */
  def logout = Action { implicit request =>

    session.get("steamId") foreach { steamId =>
      //ユーザーがログインしていればログアウト処理
      if( User.isLoggedIn(steamId) ) {
        //ユーザー名取得
        val userName = User.name(steamId)

        Logger.info(s"[user logged out] name: $userName SteamID: $steamId")

        //キャッシュから処理
        User.logout(steamId)
      }
    }

    //セッションを破棄しトップページへリダイレクト
    Redirect(routes.Application.index).withNewSession

  }

  /**
   * Steam OpenIDのコールバック
   */
  def steamOpenIDCallback(roomId: String) = Action.async { implicit request =>
    OpenID.verifiedId.flatMap { info =>
        // SteamのコールバックからSteamIDを抽出
        val steamId = info.id.replace("http://steamcommunity.com/openid/id/", "")
        //SteamIDを元にユーザー情報を取得
        val userInfo: Future[models.Player] = SteamAPI.userInfo(steamId)
        userInfo.map { user =>
          Logger.info(s"[user logged in] name: $user.personname SteamID: $user.steamid")

          //ユーザー情報をキャッシュ
          User.register(user.personname, user.steamid, user.profileurl, user.avatarfull)

          //リダイレクト先を決定。RoomID未指定ならばSteamIDをキーにルーム生成
          val redirectTo = if( roomId == "useSteamIdToLobbyName" ) {
            routes.Application.lobby(steamId)
          } else {
            routes.Application.lobby(roomId)
          }
          //ユーザーセッションを発行し、ロビーページヘ転送
          Redirect(redirectTo).withSession("steamId" -> steamId)
        }
    }
    .recover {
      case t: Throwable =>
        Logger.error("Steam Auth Callback Error:" + t)
        InternalServerError("Steam Auth Callback Error. Please try again later.")
    }
  }

  /**
   * ログイン済みユーザーのユーザー情報を再取得する
   */
  def modify = Action.async { implicit request =>
    session.get("steamId") match {
      //ログインしていない場合トップページへリダイレクト
      case None => Future(Redirect(routes.Auth.index))

      case Some(steamId) => {
        //SteamIDを元にユーザー情報を取得
        val userInfo: Future[models.Player] = SteamAPI.userInfo(steamId)
        userInfo.map { user =>
          Logger.info(s"[user modified] name: $user.personname SteamID: $user.steamid")

          //ユーザー情報をキャッシュ
          User.register(user.personname, user.steamid, user.profileurl, user.avatarfull)

          //ユーザーセッションを発行し、ロビーページヘ転送
          Redirect(routes.Application.lobby(steamId)).withSession("steamId" -> steamId)
        }
      }
    }
  }

}
