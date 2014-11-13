package controllers

import play.api.Logger
//import play.api.libs.openid.OpenID
import utils.OpenID
import play.api.mvc._
import models.User
import play.api.libs.concurrent.Execution.Implicits._



/**
 * 認証処理関連クラス
 */
object Auth extends Controller {
  /**
   * 認証トップページ。ログインしていればトップページへ、ログインしていなければログインページへ転送する
   */
  def index = Action {
    //ログインしていればトップページへリダイレクト
    if( User.isLoggedIn ) {
      Redirect(routes.Application.index.url)

    } else {
      //認証ページへ転送
      Redirect(routes.Auth.login.url)
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
   * Steam OpenIDのコールバック
   */
  def steamOpenIDCallback = Action.async { implicit request =>
    OpenID.verifiedId.map(info => Ok(info.id))
    .recover {
      case t: Throwable =>
        Logger.error("Steam Auth Callback Error" + t)
        InternalServerError("Steam Auth Callback Error. Please try again later.")
    }
  }

}
