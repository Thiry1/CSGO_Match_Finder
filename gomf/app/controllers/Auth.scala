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
   * ログインページを表示する
   */
  def login = Action.async { implicit request =>

    OpenID.redirectURL(
      "http://steamcommunity.com/openid",
      routes.Auth.steamOpenIDCallback.absoluteURL(),
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
        Logger.info(s"[user logged out] name: %s SteamID: %s".format(User.name(steamId), steamId))
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
  def steamOpenIDCallback = Action.async { implicit request =>
    OpenID.verifiedId.flatMap { info =>
        // SteamのコールバックからSteamIDを抽出
        val steamId = info.id.replace("http://steamcommunity.com/openid/id/", "")
        //SteamIDを元にユーザー情報を取得
        val userInfo: Future[models.Player] = SteamAPI.userInfo(steamId)
        userInfo.map { user =>
          Logger.info(s"[user logged in] name: %s SteamID: %s".format(user.personname, user.steamid))

          //ユーザー情報をキャッシュ
          User.register(user.personname, user.steamid, user.profileurl, user.avatarfull)

          //ユーザーセッションを発行し、ロビーページヘ転送
          Redirect(routes.Application.lobby).withSession("steamId" -> steamId)
        }
    }
    .recover {
      case t: Throwable =>
        Logger.error("Steam Auth Callback Error:" + t)
        InternalServerError("Steam Auth Callback Error. Please try again later.")
    }
  }

}
