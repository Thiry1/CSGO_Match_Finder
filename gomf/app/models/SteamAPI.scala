package models

import play.Play

/**
 * SteamAPIにアクセスして各種情報を取得する
 */
object SteamAPI {
  /**
   * SteamAPI Keyをコンフィグから取得する
   * @return String SteamAPI Key
   */
  private[this] def apiKey: String = Play.application().configuration().getString("steam.apiKey")

}
