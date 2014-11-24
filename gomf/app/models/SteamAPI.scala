package models

import play.Play
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.WS
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
/**
 * SteamAPIにアクセスして各種情報を取得する
 */
object SteamAPI {

  /**
   * STEAM API KEY
   */
  val STEAM_API_KEY = Play.application.configuration.getString("steam.apiKey")

  /**
   * SteamIDを元にユーザー情報を取得する
   * @return Future[Player] ユーザー情報
   */
  def userInfo(steamId: String): Future[Player] = {
    //SteamAPIのURL
    val holder : WSRequestHolder = WS.url("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/")
                                     .withQueryString(("key" -> STEAM_API_KEY), ("steamids" -> steamId))

    holder.get().map { response =>
      //JSONからプレーヤーデータ配列のみ取り出し
      val user: JsArray = (response.json \ "response" \ "players").as[JsArray]

      Player(
        steamId,
        (user(0) \ "communityvisibilitystate").as[Int],
        (user(0) \ "profilestate").as[Int],
        (user(0) \ "personaname").as[String],
        (user(0) \ "lastlogoff").as[Int],
        (user(0) \ "commentpermission").as[Int],
        (user(0) \ "profileurl").as[String],
        (user(0) \ "avatar").as[String],
        (user(0) \ "avatarmedium").as[String],
        (user(0) \ "avatarfull").as[String],
        (user(0) \ "personastate").as[Int],
        (user(0) \ "primaryclanid").as[String],
        (user(0) \ "timecreated").as[Int],
        (user(0) \ "personastateflags").as[Int],
        (user(0) \ "loccountrycode").as[String],
        (user(0) \ "locstatecode").as[String]
      )
    }

  }

}
