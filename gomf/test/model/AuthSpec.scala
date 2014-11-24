package model

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._
import play.api.Play.current
import models.User
import play.api.cache.Cache
/**
 * 認証ページ
 */
@RunWith(classOf[JUnitRunner])
class AuthSpec extends Specification {

   "Auth Model" should{

     "isLoggedIn" should {
       "ログインチェックをログアウト状態で呼び出してfalseが返ること" in new WithApplication {
         //ダミーデータを削除してログアウト状態にする
         Cache.remove("DUMMY_STEAM_ID" + ".SteamID")
         User.isLoggedIn("DUMMY_STEAM_ID") must beFalse
       }

       "ログインチェックをログイン状態で呼び出してtrueが返ること" in new WithApplication {
         Cache.set("DUMMY_STEAM_ID" + ".SteamID", -1)
         User.isLoggedIn("DUMMY_STEAM_ID") must beTrue
         Cache.remove("DUMMY_STEAM_ID" + ".SteamID")
       }
     }

   }
 }
