package utils

import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.layout.DefaultLayoutStrategy
import org.fusesource.scalate.util.FileResourceLoader
import play.api.Play
import play.api.templates.Html
import play.api.Play.current

object Scalate {
  lazy val templateEngine = {
    val engine = new TemplateEngine
    engine.resourceLoader = new FileResourceLoader(Some(Play.getFile("app/views")))
    engine.layoutStrategy = new DefaultLayoutStrategy(engine, "app/views/default.jade")
    engine.workingDirectory = Play.getFile("tmp")
    engine.classpath = engine.workingDirectory.toString + "/classes"
    engine.combinedClassPath = true
    engine.classLoader = Play.classloader
    engine
  }

  case class Template(name: String) {
    def render(args: Map[Symbol, Any]) = Html(templateEngine.layout(name, args.map(e => (e._1.name, e._2))))
  }
}