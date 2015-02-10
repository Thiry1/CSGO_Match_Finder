package models

import com.github.koraktor.steamcondenser.steam.servers.SourceServer
import play.api.Logger
import play.api.Play.current
import play.api.libs.json._

import scala.collection._

/**
 * ゲームサーバー情報
 */
object GameServer {
  /**
   * サーバーに誰も接続していないかチェックする
   * @param host サーバーホスト
   * @param port サーバーポート
   * @return サーバーに誰も接続していなければtrueを返す
   */
  def isEmpty(host: String, port: Int): Boolean = {
    try{
      //ゲームサーバーへ接続
      val server: SourceServer = new SourceServer(host, port)
      server.initialize()

      //サーバー接続人数が0ならばtrueを返す
      if( server.getServerInfo().get("numberOfPlayers").toString == "0" ) {
        true
      } else {
        false
      }
    } catch {
      case _: Throwable => false
    }
  }

  def list: ServerList = {

    import scala.collection.JavaConversions._

    var servers = immutable.Seq.empty[immutable.Map[String, Any]]
    //コンフィグからサーバー情報を取り出す
    play.Play.application.configuration.getConfigList("csgo.servers") foreach { server =>
      val host = server.getString("host")
      val port = server.getInt("port")
      val svPassword = server.getString("svPassword")
      val rconPassword = server.getString("rconPassword")

      servers = servers :+ immutable.Map(
        "host" -> host,
        "port" -> port,
        "svPassword" -> svPassword,
        "rconPassword" -> rconPassword,
        "isEmpty" -> isEmpty(host, port)
      )
    }

    ServerList(servers)
  }

  def reserve(steamIds: immutable.Seq[String], mapName: String): immutable.Map[String, Any] = {
    list.reserve(steamIds, mapName)
  }
}

case class ServerList(servers: immutable.Seq[immutable.Map[String, Any]]) {
  /**
   * サーバー情報をJSON形式で返す
   * @return JsArray サーバー情報
   */
  def asJson = {
    var srv = JsArray()
    servers foreach { server =>
      srv = srv.prepend(
        JsObject(
          Seq(
            "host"    -> JsString(server("host").toString),
            "port"    -> JsNumber(server("port").asInstanceOf[Int]),
            "isEmpty" -> JsBoolean(server("isEmpty").asInstanceOf[Boolean])
          )
        )
      )
    }
    srv
  }

  /**
   * ゲームサーバー一覧から誰も人がいないサーバーを探し、使用予約する
   * @return サーバーの接続情報
   */
  def reserve(steamIds: immutable.Seq[String], mapName: String): immutable.Map[String, Any] = {
    this.synchronized{
      var srv = immutable.Map.empty[String, Any]
      val breaker = scala.util.control.Breaks

      breaker.breakable {
        servers foreach { server =>
          Logger.debug("Server search: " + server.toString)
          if( server("isEmpty").asInstanceOf[Boolean] ) {
            val rcon = new Rcon(server("host").toString, server("port").asInstanceOf[Int], server("rconPassword").toString)
            //サーバーの予約を試みる
            if( rcon.reserveServer(steamIds) ) {
              Logger.debug("changelevel to " + mapName)
              //マップを変更
              rcon.changelevel(mapName)
              srv = immutable.Map(
                "host"         -> server("host"),
                "port"         -> server("port"),
                "rconPassword" -> server("rconPassword"),
                "svPassword"   -> server("svPassword")
              )

              Logger.debug("Server Found: " + srv.toString)

              //サーバーの確保に成功したので以降の処理をスキップ
              breaker.break()
            }
          }
        }
      }

      srv
    }
  }
}

/**
 * サーバーへのRCON通信を行う
 * @param host ホスト
 * @param port ポート
 * @param rconPassword RCONパスワード
 */
class Rcon(host: String, port: Int, rconPassword: String) {
  lazy val server = new SourceServer(host, port)
  //ゲームサーバーへ接続
  val rconAuthSuccessful = try {
    server.rconAuth(rconPassword)
    true
  }
  catch {
    case _: Throwable => false
  }

  /**
   * サーバーが予約状態かチェックする
   * @return 空いていればtrueを、予約状態ならfalseを返す
   */
  def isNotReserved: Boolean = {
    if( rconAuthSuccessful ) {
      val reservedStatus = exec("gomf_get_reserve_status").getOrElse("")
      //予約ステータスが空きならば
      if( reservedStatus == "[GOMF] Server is Free" ) {
        true
      } else {
        //サーバーが予約済みならば
        false
      }
    } else {
      false
    }
  }

  /**
   * RCONコマンドを実行し、レスポンスを整形する
   * @param command RCONコマンド
   * @return レスポンス
   */
  def exec(command: String): Option[String] = {
    //コマンド実行
    val response = server.rconExec(command)
    Logger.debug("RESPONSE:: " + response)
    if( response.isEmpty ) {
      None
    } else {
      val lines = response.lines.toList
      //レスポンスから不要な文字列を排除してもどす
      if( lines.nonEmpty ) Some( lines.head ) else None
    }
  }

  /**
   * サーバーを予約する
   * @return 予約成功の場合はtrueを、失敗の場合falseを返す
   */
  def reserveServer(steamIds: immutable.Seq[String]): Boolean = {
    if( rconAuthSuccessful ) {
      if( isNotReserved ) {
        //サーバーを予約するコマンドを生成
        val command = "gomf_reserve " + steamIds.mkString(" ")
        val status = exec(command).getOrElse("")
        if( status == "[GOMF] reserve successful") {
          //予約成功ならば
          true
        } else {
          false
        }
      } else {
        false
      }
    } else {
      false
    }
  }

  /**
   * サーバーの予約状態を開放する
   */
  def freeServer: Boolean = {
    if( rconAuthSuccessful ) {
      val status = exec("gomf_free").getOrElse("")
      if( status == "[GOMF] free successful" ) {
        true
      } else {
        false
      }
    } else {
      false
    }
  }
  /**
   * マップを変更する
   * @param mapName マップ名
   */
  def changelevel(mapName: String) = {
    if( rconAuthSuccessful ) {
      exec("changelevel de_" + mapName)
    }
  }

}