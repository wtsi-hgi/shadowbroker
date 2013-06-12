package uk.ac.sanger.hgi.shadowbroker

import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.component.xmpp.XmppEndpoint
import org.apache.camel.builder.RouteBuilder

object Main {

  def main(args : Array[String]) {
    val context = new DefaultCamelContext
    context.addRoutes(routes(context))
    context.start()
    while(true) {}
  }

  def routes(context : CamelContext) = {
    // We share the endpoint here 
    val xmppEndpoint = context.getEndpoint(
      "xmpp://nc6@hgi-im.internal.sanger.ac.uk?room=hgi-team&password=",
      classOf[XmppEndpoint]
      )

    new RouteBuilder {
      def configure() = {
        from("imaps://nc6@imapproxy.sanger.ac.uk?password=") to xmppEndpoint
        from(xmppEndpoint) to "file://test"
      }
    }
  }
}