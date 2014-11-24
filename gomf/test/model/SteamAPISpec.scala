package model

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._
import play.api.Play.current
import models.SteamAPI
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * SteamAPIテスト
 */
@RunWith(classOf[JUnitRunner])
class SteamAPISpec extends Specification {

  "SteamAPI Model" should {

    "userInfo" should {

      "SteamID(76561198005627722)を渡し、意図したデータが返ってくること" in new WithApplication {
        val steamId = "76561198005627722"

        SteamAPI.userInfo(steamId).map { user =>
          user.steamid must equalTo("76561198005627722")
          user.profilestate must beBetween(0, 1)
          user.personname must equalTo("Thiry")
          user.profileurl must equalTo("http://steamcommunity.com/id/bosanoba/")
          user.avatar must equalTo("http://media.steampowered.com/steamcommunity/public/images/avatars/ed/edecf26e03203984c2b25f9475e901d88700b535.jpg")
        }
      }

      "存在しないSteamIDを渡し、例外が発生すること" in new WithApplication {
        val steamId = "INVALID STEAM ID"

        SteamAPI.userInfo(steamId).recover {
          case t: Throwable => {
            t must throwA[play.api.libs.json.JsResultException]
          }
        }
      }

    }

  }
}
