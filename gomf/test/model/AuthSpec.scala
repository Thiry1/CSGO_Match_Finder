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
         User.isLoggedIn must beFalse
       }

       "ログインチェックをログイン状態で呼び出してtrueが返ること" in new WithApplication {
         Cache.set("User.SteamID", -1)
         User.isLoggedIn must beTrue
       }
     }

   }
 }
