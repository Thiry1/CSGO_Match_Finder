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

  /**
   * ユーザー情報をキャッシュから削除する
   */
  def logout() = {
    Cache.remove("User.SteamID")
    Cache.remove("User.name")
    Cache.remove("User.profileUrl")
    Cache.remove("User.avatar")
  }


  /**
   * ユーザーのSteamIDを取得する
   * @return String SteamID
   */
  def steamId: String = Cache.getOrElse("User.SteamID") {
    "SteamId Not Found"
  }

  /**
   * ユーザー名を取得する
   * @return String ユーザー名
   */
  def name: String = Cache.getOrElse("User.name") {
    "Name Not Found"
  }

  /**
   * ユーザーのプロフィールURLを取得する
   * @return String URL
   */
  def profileUrl: String = Cache.getOrElse("User.profileUrl") {
    "profileUrl Not found"
  }

  /**
   * ユーザーのアバターのURLを取得する
   * @return String URL
   */
  def avatar: String = Cache.getOrElse("User.avatar") {
    "Avatar Not Found"
  }

  /**
   * ユーザー情報一覧を取得する
   * @return PlayerExpression ユーザー情報
   */
  def info: PlayerExpression = PlayerExpression(this.steamId, this.name, this.profileUrl, this.avatar)
}