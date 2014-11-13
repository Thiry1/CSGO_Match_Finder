package models
import play.api.Play.current
import play.api.cache.Cache

/**
 * ユーザー情報関連
 */
object User {
  /**
   * ユーザーがログインしているかをチェックする
   * @return boolean ログインしていればtrueを、していなければfalseを返す
   */
  def isLoggedIn : Boolean = Cache.get("User.SteamID") != None


}