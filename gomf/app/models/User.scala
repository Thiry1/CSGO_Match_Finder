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

  /**
   * ユーザー情報をキャッシュに登録する
   * @param userName ユーザー名
   * @param steamId SteamID
   * @param profileUrl プロフィールURL
   * @param avatar アバターURL
   */
  def register(userName: String, steamId: String, profileUrl: String, avatar: String) = {
    Cache.set("User.name", userName)
    Cache.set("User.SteamID", steamId)
    Cache.set("User.profileUrl", profileUrl)
    Cache.set("User.avatar", avatar)
  }
}