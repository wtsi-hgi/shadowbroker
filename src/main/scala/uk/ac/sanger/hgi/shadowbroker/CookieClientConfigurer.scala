package uk.ac.sanger.hgi.shadowbroker

import org.apache.http.client.HttpClient
import org.apache.camel.component.http4.HttpClientConfigurer
import org.apache.http.client.CookieStore
import org.apache.http.impl.client.AbstractHttpClient

class CookieClientConfigurer(cs: CookieStore) extends HttpClientConfigurer {

  /**
   * Not very nice, relying on a runtime type check. But what can you do?
   */
  def configureHttpClient(client: HttpClient) {
    client match {
      case client: AbstractHttpClient => client.setCookieStore(cs)
    }
  }

}