package services

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._

import scala.collection.mutable.ArrayBuffer

//import services.MatchingQueue

import scala.collection._

/**
 * マッチング処理クラスのテスト
 */
@RunWith(classOf[JUnitRunner])
class QueueSpec extends Specification {

  "Queue" should{

    "マッチング" should {

      "5人グループ2つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)
        //更に5人参加させる
        val matchResult2  = queue.join("taric", 5)

        matchResult2("matchFound") must equalTo(true)
        val rooms = matchResult2("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team2").contains("taric") must beTrue
      }

      "5人グループ1つ、4人グループ１つ、1人グループ1つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult = queue.join("teemo", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)

        //更に4人参加させる
        val matchResult2 = queue.join("taric", 4)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3 = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(true)
        val rooms = matchResult3("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team2").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
      }

      "5人グループ1つ、3人グループ１つ、2人グループ1つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)

        //更に3人参加させる
        val matchResult2  = queue.join("taric", 3)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 2)
        matchResult3("matchFound") must equalTo(true)
        val rooms = matchResult3("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team2").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
      }

      "5人グループ1つ、3人グループ１つ、1人グループ2つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)

        //更に3人参加させる
        val matchResult2  = queue.join("taric", 3)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(true)
        val rooms = matchResult4("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1")(0).contains("teemo") must beTrue
        rooms("team2")(0).contains("taric") must beTrue
        rooms("team2")(1).contains("ashe") must beTrue
        rooms("team2")(2).contains("draven") must beTrue
      }

      "5人グループ1つ、2人グループ2つ、1人グループ1つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult2  = queue.join("taric", 2)
        //9人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult3  = queue.join("ashe", 2)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(true)
        val rooms = matchResult4("rooms").asInstanceOf[Map[String, Seq[String]]]
        println(rooms.toString)
        rooms("team1").contains("teemo") must beTrue
        rooms("team2").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
      }

      "5人グループ1つ、2人グループ1つ、1人グループ3つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult2  = queue.join("taric", 2)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("sona", 1)
        matchResult5("matchFound") must equalTo(true)
        val rooms = matchResult5("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team2").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("sona") must beTrue
      }

      "5人グループ1つ、1人グループ5つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult2  = queue.join("taric", 1)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("sona", 1)
        matchResult5("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult6  = queue.join("thresh", 1)

        matchResult6("matchFound") must equalTo(true)
        val rooms = matchResult6("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team2").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("sona") must beTrue
        rooms("team2").contains("thresh") must beTrue
      }

      //4人グループベース
      "4人グループ2つ、1人グループ2つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が4人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 4)
        matchResult("matchFound") must equalTo(false)

        //更に4人参加させる
        val matchResult2  = queue.join("taric", 4)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)

        matchResult4("matchFound") must equalTo(true)
        val rooms = matchResult4("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("ashe") must beTrue
        rooms("team2").contains("taric") must beTrue
        rooms("team2").contains("draven") must beTrue
      }

      "4人グループ1つ、1人グループ1つ、3人グループ1つ、2人グループ1つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が4人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 4)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult2  = queue.join("taric", 1)
        matchResult("matchFound") must equalTo(false)

        //更に3人参加させる
        val matchResult3  = queue.join("ashe", 3)
        matchResult3("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult4  = queue.join("draven",2)

        matchResult4("matchFound") must equalTo(true)
        val rooms = matchResult4("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
      }

      "4人グループ1つ、1人グループ1つ、2人グループ2つ、1人グループ1つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が4人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 4)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult2  = queue.join("taric", 1)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult3  = queue.join("ashe", 2)
        matchResult3("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult4  = queue.join("draven", 2)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("sona", 1)

        matchResult5("matchFound") must equalTo(true)
        val rooms = matchResult5("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("sona") must beTrue
      }

      "4人グループ1つ、1人グループ1つ、2人グループ1つ、1人グループ3つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が4人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 4)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult2  = queue.join("taric", 1)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult3  = queue.join("ashe", 2)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("sona", 1)
        matchResult5("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult6  = queue.join("thresh", 1)

        matchResult6("matchFound") must equalTo(true)
        val rooms = matchResult6("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("sona") must beTrue
        rooms("team2").contains("thresh") must beTrue
      }

      "4人グループ1つ、1人グループ6つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が4人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 4)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)

        //更に4人参加させる
        val matchResult2  = queue.join("taric", 1)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("sona", 1)
        matchResult5("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult6  = queue.join("thresh", 1)
        matchResult6("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult7  = queue.join("nasus", 1)

        matchResult7("matchFound") must equalTo(true)
        val rooms = matchResult7("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("sona") must beTrue
        rooms("team2").contains("thresh") must beTrue
        rooms("team2").contains("nasus") must beTrue
      }

      //3人グループ
      "3人グループ2つ、2人グループ2つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が3人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 3)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult2  = queue.join("taric", 2)
        matchResult("matchFound") must equalTo(false)

        //更に3人参加させる
        val matchResult3  = queue.join("ashe", 3)
        matchResult3("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult4  = queue.join("draven",2)

        matchResult4("matchFound") must equalTo(true)
        val rooms = matchResult4("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
      }

      "3人グループ2つ、2人グループ1つ、1人グループ2つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が3人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 3)
        matchResult("matchFound") must equalTo(false)

        //更に3人参加させる
        val matchResult2  = queue.join("taric", 2)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult3  = queue.join("ashe", 3)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("nasus", 1)

        matchResult5("matchFound") must equalTo(true)
        val rooms = matchResult5("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("nasus") must beTrue
      }
      "3人グループ2つ、1人グループ4つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が3人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 3)
        matchResult("matchFound") must equalTo(false)

        //更に3人参加させる
        val matchResult2  = queue.join("taric", 1)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 3)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("thresh", 1)
        matchResult5("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult6  = queue.join("nasus", 1)

        matchResult6("matchFound") must equalTo(true)
        val rooms = matchResult6("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team1").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("thresh") must beTrue
        rooms("team2").contains("nasus") must beTrue
      }

      "3人グループ1つ、2人グループ3つ、1人グループ１つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が3人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 3)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult2  = queue.join("taric", 2)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult3  = queue.join("ashe", 2)
        matchResult3("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult4  = queue.join("draven", 2)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("nasus", 1)

        matchResult5("matchFound") must equalTo(true)
        val rooms = matchResult5("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("nasus") must beTrue
      }

      "3人グループ1つ、2人グループ2つ、1人グループ3つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が3人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 3)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult2  = queue.join("taric", 2)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult3  = queue.join("ashe", 2)
        matchResult3("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("thresh", 1)
        matchResult5("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult6  = queue.join("nasus", 1)

        matchResult6("matchFound") must equalTo(true)
        val rooms = matchResult6("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("nasus") must beTrue
        rooms("team2").contains("thresh") must beTrue
        rooms("team2").contains("nasus") must beTrue
      }

      "3人グループ1つ、2人グループ1つ、1人グループ5つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が3人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 3)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult2  = queue.join("taric", 2)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("thresh", 1)
        matchResult5("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult6  = queue.join("nasus", 1)
        matchResult6("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult7  = queue.join("mundo", 1)

        matchResult7("matchFound") must equalTo(true)
        val rooms = matchResult7("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team2").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("nasus") must beTrue
        rooms("team2").contains("thresh") must beTrue
        rooms("team2").contains("nasus") must beTrue
        rooms("team2").contains("mundo") must beTrue
      }

      "3人グループ1つ、1人グループ7つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が3人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 3)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult2  = queue.join("taric", 1)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("thresh", 1)
        matchResult5("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult6  = queue.join("nasus", 1)
        matchResult6("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult7  = queue.join("mundo", 1)
        matchResult7("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult8  = queue.join("lux", 1)

        matchResult8("matchFound") must equalTo(true)
        val rooms = matchResult8("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team1").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("nasus") must beTrue
        rooms("team2").contains("thresh") must beTrue
        rooms("team2").contains("nasus") must beTrue
        rooms("team2").contains("mundo") must beTrue
        rooms("team2").contains("lux") must beTrue
      }

      //2人グループ
      "2人グループ4つ、1人グループ2つを入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が2人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 2)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult2  = queue.join("taric", 2)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult4  = queue.join("draven", 2)
        matchResult4("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult5  = queue.join("thresh", 2)
        matchResult5("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult6  = queue.join("nasus", 1)

        matchResult6("matchFound") must equalTo(true)
        val rooms = matchResult6("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team1").contains("ashe") must beTrue
        rooms("team2").contains("draven") must beTrue
        rooms("team2").contains("thresh") must beTrue
        rooms("team2").contains("nasus") must beTrue
      }

      //1人グループ
      "1人グループ10個を入れてマッチングされること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が1人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 1)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult2  = queue.join("taric", 1)
        matchResult("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult3  = queue.join("ashe", 1)
        matchResult3("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult4  = queue.join("draven", 1)
        matchResult4("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult5  = queue.join("thresh", 1)
        matchResult5("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult6  = queue.join("nasus", 1)
        matchResult6("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult7  = queue.join("mundo", 1)
        matchResult7("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult8  = queue.join("lux", 1)
        matchResult8("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult9  = queue.join("ziggs", 1)
        matchResult9("matchFound") must equalTo(false)

        //更に1人参加させる
        val matchResult10  = queue.join("amumu", 1)

        matchResult10("matchFound") must equalTo(true)
        val rooms = matchResult10("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team1").contains("taric") must beTrue
        rooms("team1").contains("ashe") must beTrue
        rooms("team1").contains("draven") must beTrue
        rooms("team1").contains("thresh") must beTrue
        rooms("team2").contains("nasus") must beTrue
        rooms("team2").contains("mundo") must beTrue
        rooms("team2").contains("lux") must beTrue
        rooms("team2").contains("ziggs") must beTrue
        rooms("team2").contains("amumu") must beTrue
      }


      //連続マッチング
      "5人グループ4つを入れてマッチングが２回されること" in new WithApplication {
        val queue = new services.MatchingQueue()

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)
        //更に5人参加させる
        val matchResult2  = queue.join("taric", 5)

        matchResult2("matchFound") must equalTo(true)
        val rooms = matchResult2("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms("team1").contains("teemo") must beTrue
        rooms("team2").contains("taric") must beTrue

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult3 = queue.join("ashe", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)
        //更に5人参加させる
        val matchResult4  = queue.join("draven", 5)

        matchResult4("matchFound") must equalTo(true)
        val rooms2 = matchResult4("rooms").asInstanceOf[Map[String, Seq[String]]]
        rooms2("team1").contains("ashe") must beTrue
        rooms2("team2").contains("draven") must beTrue
      }

      //失敗

      "5人グループ1つ、4人グループ１つ、2人グループ1つを入れてマッチングが失敗すること" in new WithApplication {
        var queue = new services.MatchingQueue()

        //人が5人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 5)
        //5人しかいないのでマッチング失敗
        matchResult("matchFound") must equalTo(false)
        //更に4人参加させる
        val matchResult2  = queue.join("taric", 4)
        //9人しかいないのでマッチング失敗
        matchResult2("matchFound") must equalTo(false)

        val matchResult3  = queue.join("ashe", 2)
        //組が作れないのでマッチング失敗
        matchResult2("matchFound") must equalTo(false)
      }

      "2人グループ5つを入れてマッチングが失敗すること" in new WithApplication {
        var queue = new services.MatchingQueue()

        //人が2人いるダミールームをマッチングに参加させる
        val matchResult  = queue.join("teemo", 2)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult2  = queue.join("taric", 2)
        matchResult("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult3  = queue.join("ashe", 2)
        matchResult3("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult4  = queue.join("draven", 2)
        matchResult4("matchFound") must equalTo(false)

        //更に2人参加させる
        val matchResult5  = queue.join("thresh", 2)
        matchResult5("matchFound") must equalTo(false)
      }

    }
  }
}