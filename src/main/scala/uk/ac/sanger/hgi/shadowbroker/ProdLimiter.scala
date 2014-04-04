/*
Copyright (c) 2013, Genome Research Limited
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Genome Research Limited nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
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