package services

import play.api.Logger

import scala.collection._
import scala.util.control.Breaks

class MatchingQueue {
  /**
   * 1人でキューに参加しているルームのルームIDを格納
   */
  private[this] var solo = immutable.Seq.empty[String]
  /**
   * 2人でキューに参加しているルームのルームIDを格納
   */
  private[this] var duo = immutable.Seq.empty[String]
  /**
   * 3人でキューに参加しているルームのルームIDを格納
   */
  private[this] var triple = immutable.Seq.empty[String]
  /**
   * 4人でキューに参加しているルームのルームIDを格納
   */
  private[this] var quadra = immutable.Seq.empty[String]
  /**
   * 5一人でキューに参加しているルームのルームIDを格納
   */
  private[this] var penta = immutable.Seq.empty[String]

  /**
   * 検索に使用した数値を格納しておく(履歴)
   */
  private[this] var searchedPlayerHistory = mutable.Seq.empty[Int]

  /**
   * チーム1に所属しているプレイヤーの数を格納する
   */
  private[this] var team1CurrentAmount = 0
  /**
   * チーム2に所属しているプレイヤーの数を格納する
   */
  private[this] var team2CurrentAmount = 0
  /**
   * ルーム内の人数に合わせたリストにルームIDを追加する
   * @param roomId ルームID
   * @param playerCount プレイヤー人数
   */
  private[this] def add(roomId: String, playerCount: Int): Unit = {
    //ルームIDがすでに入っていなければキューに追加
    playerCount match {
      case 1 => this.solo = (this.solo :+ roomId).distinct
      case 2 => this.duo = (this.duo :+ roomId).distinct
      case 3 => this.triple = (this.triple :+ roomId).distinct
      case 4 => this.quadra = (this.quadra :+ roomId).distinct
      case 5 => this.penta = (this.penta :+ roomId).distinct
    }
  }

  /**
   * マッチング参加処理
   * @param roomId ルームID
   * @param playerCount ルームにいるプレイヤー数
   * @return マッチング結果
   */
  def join(roomId: String, playerCount: Int): immutable.Map[String, Any] = {
    //キューに参加させる
    Logger.debug("マッチング参加:: roomId: " + roomId + " 人数: " + playerCount)
    this.add(roomId, playerCount)
    //マッチング処理
    this.queuing
  }

  /**
   * マッチング処理開始処理
   * @return マッチング結果
   */
  private[this] def queuing: immutable.Map[String, Any] = {
    /**
     * キューに参加しているプレイヤーの数
     */
    val inQueuePlayerCount = (this.solo.length
                              + ( this.duo.length * 2 )
                              + ( this.triple.length * 3 )
                              + ( this.quadra.length * 4 )
                              + ( this.penta.length * 5 ))
    Logger.debug("マッチングに参加しているプレイヤー数:: " + inQueuePlayerCount)
    //キューに参加している人が10人いるかチェック
    10 > inQueuePlayerCount match {
      //10人以下の場合はマッチが成立しないので処理をスキップ
      case true => immutable.Map("matchFound" -> false)
      //10人以上いるのでマッチングを試みる
      case false => {
        //10人のマッチング開始
        val matchedRoom = findPlayer(mutable.Map[String, mutable.Seq[String]](
          "team1" -> mutable.Seq.empty[String],
          "team2" -> mutable.Seq.empty[String]
        ), 10)
        //マッチングに成功しているか判定
        if( matchedRoom.nonEmpty ) {
          Logger.debug("マッチング完了")
          //マッチングしたルームを返す
          immutable.Map("matchFound" -> true, "rooms" -> matchedRoom)
        } else {
          Logger.debug("マッチング失敗")
          immutable.Map("matchFound" -> false)
        }

      }
    }
  }

  /**
   * キュー状態を初期化する
   */
  private[this] def clearQueue() = {
    //検索履歴を初期化
    this.searchedPlayerHistory = mutable.Seq.empty[Int]
    //チーム所属人数を初期化
    this.team1CurrentAmount = 0
    this.team2CurrentAmount = 0
  }

  /**
   * マッチング失敗時にコールされる
   * @return 空のMap
   */
  private[this] def matchingFailed(): mutable.Map[String, mutable.Seq[String]] = {
    clearQueue()

    //マッチング失敗なので空のMapを返す
    mutable.Map.empty[String, mutable.Seq[String]]
  }

  private[this] def find(matchedRoom: mutable.Map[String, mutable.Seq[String]], remainingPlayer: Int, searchPlayerCount: Int, isLooped: Boolean = false): mutable.Map[String, mutable.Seq[String]] = {
    var team1: mutable.Seq[String] = matchedRoom("team1")
    var team2: mutable.Seq[String] = matchedRoom("team2")

    val searchList = searchPlayerCount match {
      case 5 => this.penta
      case 4 => this.quadra
      case 3 => this.triple
      case 2 => this.duo
      case 1 => this.solo
    }

    //ループの限界値を超えていないかチェック
    val isOverLoopLimit: Boolean = this.searchedPlayerHistory.startsWith(searchPlayerCount match {
      //5は２回まで許容される
      case 5 => Seq(5).padTo(2, 5)
      //4は２回まで許容される
      case 4 => Seq(4).padTo(2, 4)
      //3は3回まで許容される
      case 3 => Seq(3).padTo(3, 3)
      //2は4回まで許容される
      case 2 => Seq(2).padTo(4, 2)
      //1は10回まで許容される
      case 1 => Seq(1).padTo(10, 1)
    })

    //ループの限界値を超えるループを行った場合
    if( isOverLoopLimit ) {
      Logger.debug("前回と同じ残り人数の為、検索用の数値を変更")
      if( (searchPlayerCount - 1) == 0) {
        Logger.debug("検索数値が0になったためマッチング失敗")
        //マッチング失敗
        matchingFailed()
      } else {
        Logger.debug("searchPlayerCountに指定された人数のグループが見つからなかった場合、検索人数を一つ下げて検索を行う" + (searchPlayerCount - 1))
        //searchPlayerCountに指定された人数のグループが見つからなかった場合、検索人数を一つ下げて検索を行う
        find(matchedRoom, remainingPlayer, searchPlayerCount - 1, isLooped = true)
      }
    } else {
      var isRoomFound = false
      val breaker = new Breaks
      breaker.breakable {
        searchList foreach { roomId =>
          //該当ルームがすでにマッチング対象になっているかチェック
          if (!team1.contains(roomId) && !team2.contains(roomId)) {
            //チーム1の残り人数
            val team1Remaining = 5 - this.team1CurrentAmount
            //チーム2の残り人数
            val team2Remaining = 5 - this.team2CurrentAmount
            //残り人数を元にどちらのチームに入れるか選択
            if (team1Remaining >= searchPlayerCount) {
              team1 = team1 :+ roomId
              this.team1CurrentAmount += searchPlayerCount
            } else if (team2Remaining >= searchPlayerCount) {
              team2 = team2 :+ roomId
              this.team2CurrentAmount += searchPlayerCount
            }
            //変更データを反映
            matchedRoom("team1") = team1
            matchedRoom("team2") = team2
            isRoomFound = true
            //リスト内検索から抜ける
            breaker.break()
          }
        }
      }
      //この時点での残りプレイヤー数
      val currentRemainingPlayerCount = if (isRoomFound) {
        remainingPlayer - searchPlayerCount
      } else {
        remainingPlayer
      }

      //次の検索人数を決定
      val nextSearchPlayerCount = if (isLooped && !isRoomFound) {
        //ループ後初回の検索でルームが見つかっていない場合
        searchPlayerCount match {
          case 1 => searchPlayerCount
          case _ => searchPlayerCount - 1
        }
      } else {
        //同じ人数のグループを検索
        searchPlayerCount
      }


      Logger.debug("次の検索人数:: " + nextSearchPlayerCount)
      //残り人数の更新
      this.searchedPlayerHistory = searchPlayerCount +: this.searchedPlayerHistory

      Logger.debug("残りの必要人数:: " + currentRemainingPlayerCount)

      currentRemainingPlayerCount match {

        case 0 => {
          clearQueue()
          //マッチング結果を返す
          matchedRoom
        }
        case _ if( nextSearchPlayerCount <= 0 ) => {
          //マッチング失敗
          matchingFailed()
        }
        case _ => find(matchedRoom, currentRemainingPlayerCount, nextSearchPlayerCount)
      }
    }
  }

  private[this] def findPlayer(matchedRoom: mutable.Map[String, mutable.Seq[String]], remainingPlayer: Int): mutable.Map[String, mutable.Seq[String]] = {
    Logger.debug("残りの必要人数:: " + remainingPlayer)
    //残りの必要人数に応じて検索
    val searchPlayerCount = if( remainingPlayer >= 5 ) 5 else remainingPlayer
    find(matchedRoom, remainingPlayer, searchPlayerCount)
  }
}
