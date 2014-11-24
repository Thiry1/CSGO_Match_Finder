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
  def isLoggedIn(steamId: String) : Boolean = Cache.get(steamId + ".SteamID") != None

  /**
   * ユーザー情報をキャッシュに登録する
   * @param userName ユーザー名
   * @param steamId SteamID
   * @param profileUrl プロフィールURL
   * @param avatar アバターURL
   */
  def register(userName: String, steamId: String, profileUrl: String, avatar: String) = {
    Cache.set(steamId + ".name", userName)
    Cache.set(steamId + ".SteamID", steamId)
    Cache.set(steamId + ".profileUrl", profileUrl)
    Cache.set(steamId + ".avatar", avatar)
  }

  /**
   * ユーザー情報をキャッシュから削除する
   */
  def logout(steamId: String) = {
    Cache.remove(steamId + ".SteamID")
    Cache.remove(steamId + ".name")
    Cache.remove(steamId + ".profileUrl")
    Cache.remove(steamId + ".avatar")
  }

  /**
   * ユーザー名を取得する
   * @param steamId SteamID
   * @return String ユーザー名
   */

  def name(steamId: String): String = Cache.getOrElse(steamId + ".name") {
    "Name Not Found"
  }

  /**
   * ユーザーのプロフィールURLを取得する
   * @param steamId SteamID
   * @return String URL
   */
  def profileUrl(steamId: String): String = Cache.getOrElse(steamId + ".profileUrl") {
    "profileUrl Not found"
  }

  /**
   * ユーザーのアバターのURLを取得する
   * @param steamId SteamID
   * @return String URL
   */
  def avatar(steamId: String): String = Cache.getOrElse(steamId + ".avatar") {
    "Avatar Not Found"
  }

  /**
   * ユーザー情報一覧を取得する
   * @param steamId SteamID
   * @return PlayerExpression ユーザー情報
   */
  def info(steamId: String): PlayerExpression = PlayerExpression(steamId, this.name(steamId), this.profileUrl(steamId), this.avatar(steamId))
}