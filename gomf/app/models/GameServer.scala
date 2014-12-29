package models
import com.github.koraktor.steamcondenser.steam.servers.SourceServer
import play.api.Play
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

  def list(): ServerList = {

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
}