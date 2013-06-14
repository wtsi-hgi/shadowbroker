package uk.ac.sanger.hgi.shadowbroker

import scala.collection.JavaConversions.asJavaList

import org.apache.camel.Body
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.http4.HttpComponent
import org.apache.camel.impl.DefaultCamelContext
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.slf4j.LoggerFactory

object Main {
  val LOG = LoggerFactory.getLogger("SHADOWBROKER")

  def main(args: Array[String]) {

    val username = args(0)
    val password = args(1)

    val context = new DefaultCamelContext

    locally {
      // First we need to grab some authentication cookie stuff!
      val httpClient = new DefaultHttpClient
      val post = new HttpPost("https://rt.sanger.ac.uk")
      val nvps = List(new BasicNameValuePair("user", username), new BasicNameValuePair("pass", password))
      post.setEntity(new UrlEncodedFormEntity(asJavaList(nvps)))
      httpClient.execute(post)
      val ccc = new CookieClientConfigurer(httpClient.getCookieStore())
      val httpComp = context.getComponent("https4", classOf[HttpComponent])
      httpComp.setHttpClientConfigurer(ccc)
    }

    val routes = new RouteBuilder {
      def configure() = {
        from("timer://foo?fixedRate=true&delay=0&period=300000")
          .setHeader("Referer", constant("https://rt.sanger.ac.uk"))
          .multicast().to(
            "direct://a",
            "direct://b")

        from("direct://a").to("https4://rt.sanger.ac.uk/REST/1.0/search/ticket?query=Queue='hgi' AND Owner='Nobody' AND ( Status = 'new' OR Status = 'open' )").to("seda://a")
        from("direct://b").to("https4://rt.sanger.ac.uk/REST/1.0/search/ticket?query=Queue='humgen-storage-request' AND Owner='Nobody' AND ( Status = 'new' OR Status = 'open' )").to("seda://a")

        from("seda://a")
          .filter(body().isNotNull())
          .process(new Processor {
            def process(exc: Exchange) {
              val body = exc.getIn.getBody(classOf[String])
              exc.getIn.setBody(body.lines.drop(2).mkString("\n"))
            }
          })
          .filter().method(classOf[FilterNoMatchingResults], "filter")
          .split(body(classOf[String]).tokenize("\n")).streaming() // Each ticket
          .process(new Processor {
            def process(exc: Exchange) {
              val body = exc.getIn.getBody(classOf[String])
              body.split(":", 2) match {
                case Array(a, b) => {
                  exc.getIn.setHeader(Exchange.HTTP_URI, constant(s"https://rt.sanger.ac.uk/REST/1.0/ticket/$a"))
                }
                case _ => //no-op
              }
            }
          })
          .to("https4://to_be_replaced")
          // Convert the body to a set of properties.
          .process(new Processor {
            def process(exc: Exchange) {
              val properties = exc.getIn().getBody(classOf[String]).split("\n").collect {
                _.split(":", 2) match {
                  case Array("id", b) => "id" -> b.trim().drop(7) // Get the id without '/ticket'
                  case Array(a, b) => a.trim() -> b.trim()
                }
              }.toMap
              exc.getIn().setBody(properties)
            }
          })
          .filter().method(classOf[ProdLimiter], "shouldIProd")
          .process(TicketDisplayProcessor)
          .to(s"xmpp://$username@hgi-im.internal.sanger.ac.uk?room=hgi-tickets&password=$password")
      }
    }
    context.addRoutes(routes)
    context.start()
    System.in.read()
    context.stop()
  }
}

class FilterNoMatchingResults {
  def filter(@Body body: String) = !body.trim().equals("No matching results.")
}