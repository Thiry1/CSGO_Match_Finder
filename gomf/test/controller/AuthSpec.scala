package controllers

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.cache.Cache
import play.api.test._
import play.api.test.Helpers._

/**
 * 認証ページ
 */
@RunWith(classOf[JUnitRunner])
class AuthSpec extends Specification {

  "Auth" should {

    "トップページ" should {

      "ログイン状態で認証トップページにアクセスし、トップページへリダイレクトされることを確認する" in new WithApplication {
        //ログイン状態を定義
        Cache.set("User.SteamID", -1)

        val Some(result) = route(FakeRequest(GET, "/auth"))

        //ステータスコードが303である
        status(result) must equalTo(303)
        //リダイレクト先チェック
        redirectLocation(result).mustEqual(Some("/"))

      }

      "ログアウト状態で認証トップページにアクセスし、ログインページへリダイレクトされることを確認する" in new WithApplication {
        val Some(result) = route(FakeRequest(GET, "/auth"))

        //ステータスコードが303である
        status(result) must equalTo(303)
        //リダイレクト先チェック
        redirectLocation(result).mustEqual(Some("/auth/login"))
      }

    }

  }
}
