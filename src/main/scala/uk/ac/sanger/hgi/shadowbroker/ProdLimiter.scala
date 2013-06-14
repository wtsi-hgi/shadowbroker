package uk.ac.sanger.hgi.shadowbroker

import java.util.Date
import java.text.SimpleDateFormat
import scala.math.max
import org.apache.camel.Body

/**
 * Governs whether to prod people in the chatroom or not, based on how recently we prodded them versus how long the ticket has been waiting.
 *
 * @param initialPatience How patient to be initially. (milliseconds)
 * @param irateRate How quickly to get irate.
 */
class ProdLimiter {

  val initialPatience = 1800000l
  val irateRate = 1l
  val minInterval = 300000l

  val sdf = new SimpleDateFormat("EEE MMM d kk:mm:ss yyyy")
  var lastProdded = Map[String, Long]()

  def shouldIProd(@Body body: Map[String, String]) = {

    val a = for {
      created <- body.get("Created")
      cdate <- Option(sdf.parse(created)).map(_.getTime())
      ticketid <- body.get("id")
    } yield {
      val now = (new Date()).getTime()
      val b = lastProdded.get(ticketid) match {
        case Some(date) => currentPatience(cdate, now) < now - date
        case None => true
      }

      if (b == true) {
        lastProdded += (ticketid -> now)
      }
      b
    }

    // Sometimes, we clean the last prodded map
    if (scala.util.Random.nextInt(50) == 1) {
      cleanUp()
    }

    a.getOrElse(false)
  }

  private def currentPatience(created: Long, now: Long): Long = max(initialPatience - (now - created) * irateRate, minInterval)

  // Clean up the map of anything that has been around for ever
  private def cleanUp() {
    val now = (new Date()).getTime()
    val toKeep = lastProdded.filterNot { case (_, b) => b + initialPatience < now }
    lastProdded = toKeep
  }

}