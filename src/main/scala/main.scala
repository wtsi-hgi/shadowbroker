package uk.ac.sanger.hgi.shadowbroker

import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.scala.dsl.builder.RouteBuilder

object Main {

  def main(args : Array[String]) {
    val context = new DefaultCamelContext
    context.addRoutes(TestRoutes)
    context.start()
  }

}

object TestRoutes extends RouteBuilder {
  "file://test" -->
  "xmpp://nc6@hgi-im.internal.sanger.ac.uk?room=hgi-team&password="
}
