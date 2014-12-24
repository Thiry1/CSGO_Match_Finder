package services

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import play.api.{Play, Logger}

import play.api.libs.concurrent.Akka
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.collection._
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.control.Breaks

object QueueService {

  implicit val timeout = Timeout(10 second)

  lazy val queue = new Queue

  lazy val queueActor = {
    Akka.system.actorOf(Props[QueueActor])
  }

  /**
   * マッチング用WebSocketへの接続処理、イベントルーティングを行う
   * @param roomId ルームID
   * @return {Iteratee, Enumerator}
   */
  def start(roomId: String): scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {

    (queueActor ? JoinQueueService(roomId)) map {
      case Connected(enumerator) => {
        val iteratee = Iteratee.foreach[JsValue] { data =>
          (data \ "event").asOpt[String] match {
            case Some(event) => {
              //JSONのeventの値を元に処理の振り分け
              event match {
                case "startQueue"  => startQueue(roomId, data)
                case "stopQueue"   => stopQueue(roomId)
                case _ => queueActor ! ForceQuitQueue(roomId, "[不正なキュー] eventが不正です")
              }
            }
            case _ => queueActor ! ForceQuitQueue(roomId, "[不正なキュー] eventが不正です")
          }
        } map { _ =>
          //キュー用WebSocketから切断された場合、マッチングのキューから該当ルームIDを削除
          stopQueue(roomId)
          queueActor ! NotifyQuitQueue(roomId)
        }

        (iteratee, enumerator)
      }

    }

  }

  /**
   * 該当ルームのマッチング開始処理
   * @param roomId ルームID
   * @param data ユーザーから送られてきたJsonデータ
   */
  private[this] def startQueue(roomId: String, data: JsValue) = {
    //プレイヤー人数を取得
    val playerCount = (data \ "playerCount").asOpt[Int]

    playerCount match {
      case Some(count) if count > 0 => {
        //マップ一覧を取得
        val maps = (data \ "maps").asOpt[Seq[String]]
        val breaker = new Breaks
        breaker.breakable {
          //マップごとにキューに参加処理
          maps match {
            case Some(mapList) if (!mapList.isEmpty) => {
              //マッチング開始をルームに通知
              queueActor ! NotifyMatchingStart(roomId)
              //マップ毎にマッチングに参加
              mapList foreach { mapName =>
                Logger.debug("map foreach")
                //マッチングに参加
                val matchingResult = queue.joinQueue(roomId, mapName, count)
                //マッチが見つかっていれば
                if( matchingResult("matchFound").asInstanceOf[Boolean] ) {
                  Logger.debug("見つかった屋で")
                  //マッチが見つかったことをルームに通知
                  queueActor ! NotifyMatchFound(matchingResult)
                  //該当ルームをマッチングから切断
                  val team1Rooms = matchingResult("team1").asInstanceOf[mutable.Seq[String]]
                  val team2Rooms = matchingResult("team2").asInstanceOf[mutable.Seq[String]]
                  queue.removeFromQueue(team1Rooms ++ team2Rooms)
                  //マッチが見つかったため以降のマップではマッチング参加処理をしない
                  breaker.break()
                }
              }
            }
            case _ => queueActor ! ForceQuitQueue(roomId, "[不正なキュー] mapが指定されていません")
          }
        }

      }
      case _ => queueActor ! ForceQuitQueue(roomId, "[不正なキュー] playerCountが不正です")
    }
  }

  /**
   * 該当ルームのマッチング停止処理
   * @param roomId ルームID
   */
  private[this] def stopQueue(roomId: String) = {
    //該当ルームをマッチングキューから削除する
    this.queue.removeFromQueue(roomId)
    //該当ルームが切断したことを通知
    queueActor ! NotifyQuitQueue(roomId)
  }
/*
  private[this] def verifyMatch(matchingId: String) = {
    //マッチの検証
    if( queue.verifyMatch(matchingId) ) {
      queueActor ! NotifyMatchSuccessful(queue.getMatchedRooms(matchingId))
      //マッチングIDに紐付いたルームを削除
      queue.removeMatchedRooms(matchingId)
    }
  }
*/
}

class Queue {
  /*//成功したマッチングのマッチIDと検証用数値を格納
  private[this] val matchIds = mutable.Map.empty[String, Int]
  //成功したマッチングのマッチIDをキーに、マッチしたルームIDを格納
  private[this] val matchedRooms = mutable.Map.empty[String, mutable.Seq[String]]*/
  //マップごとにマッチングクラスのインスタンスを生成する
  private[this] lazy val queueMaps = Play.application.configuration.getStringList("csgo.maps") match {
    case None => immutable.Map.empty[String, MatchingQueue]

    case Some(maps) => {
      import scala.collection.JavaConverters._

      var queueList = mutable.Map.empty[String, MatchingQueue]
      //マップ名を取り出し、インスタンス生成
      maps.asScala.toSeq foreach { mapName =>
        queueList = queueList ++ immutable.Map(mapName -> new services.MatchingQueue())
      }

      queueList
    }
  }

  /**
   * キューへの参加処理
   * @param roomId ルームID
   * @param mapName マップ名
   * @param playerCount 参加人数
   * @return マッチング結果
   */
  def joinQueue(roomId: String, mapName: String, playerCount: Int): immutable.Map[String, Any] = {
    this.synchronized {
      Logger.debug(s"該当ルーム$roomId をマッチングキューに追加")
      //マップ名を元にキューを取り出し
      this.queueMaps.get(mapName) match {
        case Some(queue) => {
          val matchingResult = queue.join(roomId, playerCount)
          Logger.debug("マッチング結果:: " + matchingResult.toString())

          //マッチングに成功していれば
          if( matchingResult("matchFound").asInstanceOf[Boolean] ) {
            matchingResult
          } else {
            immutable.Map("matchFound" -> false)
          }
        }
        case None => {
          //self ! forceQuitQueue(roomId, "不正なマップが指定されました")
          immutable.Map("matchFound" -> false)
        }
      }
    }
  }

  /**
   * 該当ルームをマッチングキューから削除する
   * @param roomIds ルームIDのリスト
   */
  def removeFromQueue(roomIds: mutable.Seq[String]) = {
    roomIds foreach { roomId =>
      Logger.debug(s"該当ルーム$roomId をマッチングキューから削除")
      //各マップのキューから削除する
      this.queueMaps foreach { case (mapName, queue) =>
        queue.remove(roomId)
      }
    }
  }

  /**
   * 該当ルームをマッチングキューから削除する
   * @param roomId ルームID
   */
  def removeFromQueue(roomId: String) = {
    Logger.debug(s"該当ルーム$roomId をマッチングキューから削除")
    //各マップのキューから削除する
    this.queueMaps foreach { case (mapName, queue) =>
      queue.remove(roomId)
    }
  }
}

class QueueActor extends Actor {
  val (enumerator, channel) = Concurrent.broadcast[JsValue]
  //マップごとにキュー用クラスのインスタンスを作成、リスト化する


  def receive = {
    case JoinQueueService(roomId) => sender ! Connected(enumerator)
    case NotifyQuitQueue(roomId) => notifyQuitQueue(roomId)
    case ForceQuitQueue(roomId, reason) => forceQuitQueue(roomId, reason)
    case NotifyMatchingStart(roomId) => notifyMatchingStart(roomId)
    case NotifyMatchFound(matchingResult) => notifyMatchFound(matchingResult)
  }

  private[this] def notifyMatchFound(matchingResult: immutable.Map[String, Any]) = {
    Logger.debug("NOTIFY MATCH FOUND")
    val rooms = matchingResult("rooms").asInstanceOf[mutable.Map[String, mutable.Seq[String]]]
    //どちらかのチームに所属しているルームID
    val teams = rooms("team1") union rooms("team2")
    val msg = JsObject(
      Seq(
        "event"          -> JsString("matchFound"),
        "members"        -> Json.toJson(teams),
        "serverAddress"  -> JsString("next.five-seven.net"),
        "serverport"     -> JsString("27015"),
        "serverPassword" -> JsString("next")
      )
    )
    channel.push(msg)
  }

  private[this] def notifyQueueStatus() = {
    val msg = JsObject(
      Seq(
        "event"   -> JsString("queueStatusChanged"),
        "members" -> JsString("test")
      )
    )
    channel.push(msg)
  }

  /**
   * 該当ルームをマッチングから切断させる
   * @param roomId ルームID
   * @param reason 理由
   */
  private[this] def forceQuitQueue(roomId: String, reason: String) = {
    val msg = JsObject(
      Seq(
        "event"   -> JsString("forceQuitQueue"),
        "roomId"  -> JsString(roomId),
        "reason"  -> JsString(reason)
      )
    )
    channel.push(msg)
  }

  /**
   * 該当ルームがマッチングから切断したことを通知する
   * @param roomId ルームID
   */
  private[this] def notifyAnyoneQuitQueue(roomId: String) = {
    val msg = JsObject(
      Seq(
        "event"  -> JsString("anyoneQuitQueue"),
        "roomId" -> JsString(roomId)
      )
    )
    channel.push(msg)
  }


  /**
   * 該当ルームに対してマッチングが開始したことを通知する
   * @param roomId ルームID
   */
  private[this] def notifyMatchingStart(roomId: String) = {
    val msg = JsObject(
      Seq(
        "event"  -> JsString("matchingStart"),
        "roomId" -> JsString(roomId)
      )
    )
    channel.push(msg)
  }

  /**
   * マッチングから切断されたことを通知する
   * @param roomId ルームID
   */
  private[this] def notifyQuitQueue(roomId: String) = {
    //キューからの切断を通知
    self ! notifyAnyoneQuitQueue(roomId)
    //キューステータスを通知
    self ! notifyQueueStatus()
  }
}

case class JoinQueueService(roomId: String)
case class Quit(roomId: String)
case class NotifyQuitQueue(roomId: String)
case class ForceQuitQueue(roomId: String, reason: String)
case class NotifyMatchingStart(roomId: String)
case class Connected(enumerator: Enumerator[JsValue])
case class NotifyMatchFound(matchingResult: immutable.Map[String, Any])
case class NotifyMatchSuccessful(roomIds: mutable.Seq[String])