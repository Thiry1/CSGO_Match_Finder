package services

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import models.GameServer
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
  private[this] def startQueue(roomId: String, data: JsValue): Unit = {

    (data \ "steamIds").asOpt[Seq[String]] match {
      case Some(steamIds) => {
        //SteamIDとルームIDを紐付けて保管
        queue.addRoomData(roomId, steamIds)
      }
      case None => {
        queueActor ! ForceQuitQueue(roomId, "[不正なキュー] SteamIDが指定されていません")
        return
      }
    }

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
                //マッチングに参加
                val matchingResult = queue.joinQueue(roomId, mapName, count)
                //マッチが見つかっていれば
                if( matchingResult("matchFound").asInstanceOf[Boolean] ) {
                  Logger.debug("match found")
                  //ルームID
                  val rooms = matchingResult("rooms").asInstanceOf[mutable.Map[String, mutable.Seq[String]]]
                  val team1Rooms = rooms("team1")
                  val team2Rooms = rooms("team2")

                  //SteamID一覧取得
                  val steamIds = queue.getSteamIds(team1Rooms ++ team2Rooms)
                  Logger.debug("search game server")
                  //ゲームサーバーの確保
                  val server = GameServer.reserve(steamIds, mapName)
                  //サーバーの確保に成功していれば
                  if( server.nonEmpty ) {
                    Logger.debug("server found")
                    //マッチが見つかったことをルームに通知
                    queueActor ! NotifyMatchFound(
                      matchingResult,
                      server("host").toString,
                      server("port").asInstanceOf[Int],
                      server("svPassword").toString
                    )
                    //該当ルームをマッチングから切断
                    queue.removeFromQueue(team1Rooms ++ team2Rooms)
                    //ルームIDに紐付いたSteamID一覧を削除
                    (team1Rooms ++ team2Rooms) foreach { roomId =>
                      queue.removeRoomData(roomId)
                    }
                  } else {
                    //サーバーが見つからない場合マッチングから切断させる
                    (team1Rooms ++ team2Rooms) foreach { roomId =>
                      queueActor ! ForceQuitQueue(roomId, "マッチは見つかりましたが、空いているサーバーがありませんでした")
                    }
                  }

                  //マッチが見つかったため以降のマップではマッチング参加処理をしない
                  breaker.break()
                }
              }
            }
            case _ => {
              //ルームIDに紐付いたSteamIDリストを削除
              queue.removeRoomData(roomId)
              queueActor ! ForceQuitQueue(roomId, "[不正なキュー] mapが指定されていません")
            }
          }
        }

      }
      case _ => {
        //ルームIDに紐付いたSteamIDリストを削除
        queue.removeRoomData(roomId)
        queueActor ! ForceQuitQueue(roomId, "[不正なキュー] playerCountが不正です")
      }
    }
  }

  /**
   * 該当ルームのマッチング停止処理
   * @param roomId ルームID
   */
  private[this] def stopQueue(roomId: String) = {
    //該当ルームをマッチングキューから削除する
    this.queue.removeFromQueue(roomId)
    //ルームIDに紐付いたSteamIDリストを削除
    queue.removeRoomData(roomId)
    //該当ルームが切断したことを通知
    queueActor ! NotifyQuitQueue(roomId)
  }
}

class Queue {
  //ルームIDに紐づけてSteamIDを格納
  private[this] val steamIdList = mutable.Map.empty[String, Seq[String]]

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
   * ルームIDリストを元にSteamIDリストを作成する
   * @param rooms ルームIDリスト
   * @return SteamIDリスト
   */
  def getSteamIds(rooms: mutable.Seq[String]): immutable.Seq[String] = {
    var steamIds = Seq.empty[String]

    rooms foreach { roomId =>
      val ids = this.steamIdList(roomId)
      steamIds = steamIds ++ ids
    }
    //immutableにコンバート
    immutable.Seq(steamIds: _*)
  }
  /**
   * ルームIDをSteamIDリストを紐付けて格納する
   * @param roomId ルームID
   * @param steamIds SteamIDリスト
   */
  def addRoomData(roomId: String, steamIds: Seq[String]) = {
    steamIdList.update(roomId, steamIds)
  }

  /**
   * ルームIDに紐付いたSteamIDリストを削除する
   * @param roomId ルームID
   */
  def removeRoomData(roomId: String) = {
    steamIdList.remove(roomId)
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

  def receive = {
    case JoinQueueService(roomId) => sender ! Connected(enumerator)
    case NotifyQuitQueue(roomId) => notifyQuitQueue(roomId)
    case ForceQuitQueue(roomId, reason) => forceQuitQueue(roomId, reason)
    case NotifyMatchingStart(roomId) => notifyMatchingStart(roomId)
    case NotifyMatchFound(matchingResult, host, port, password) => notifyMatchFound(matchingResult, host, port, password)
  }

  /**
   * マッチが見つかったことをルームに通知する
   * @param matchingResult マッチング結果
   * @param host サーバーホスト
   * @param port サーバーポート
   * @param password サーバーパスワード
   */
  private[this] def notifyMatchFound(matchingResult: immutable.Map[String, Any], host: String, port: Int, password: String) = {
    val rooms = matchingResult("rooms").asInstanceOf[mutable.Map[String, mutable.Seq[String]]]
    //どちらかのチームに所属しているルームID
    val teams = rooms("team1") union rooms("team2")
    val msg = JsObject(
      Seq(
        "event"          -> JsString("matchFound"),
        "members"        -> Json.toJson(teams),
        "serverAddress"  -> JsString(host),
        "serverPort"     -> JsString(port.toString),
        "serverPassword" -> JsString(password)
      )
    )
    Logger.debug("Notify Match Found:: " + msg.toString)
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
case class NotifyMatchFound(matchingResult: immutable.Map[String, Any], host: String, port: Int, password: String)
case class NotifyMatchSuccessful(roomIds: mutable.Seq[String])