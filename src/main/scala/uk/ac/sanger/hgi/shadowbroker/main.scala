package uk.ac.sanger.hgi.shadowbroker

import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import scala.collection.JavaConversions.asJavaList
import org.apache.camel.component.http4.HttpComponent
import org.apache.camel.Exchange
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
        from("timer://foo?fixedRate=true&delay=0&period=10000")
          .to("https4://rt.sanger.ac.uk/REST/1.0/ticket/335729/show")
          .to("file://test")
      }
    }
    context.addRoutes(routes)
    context.start()
    System.in.read()
    context.stop()
  }
}