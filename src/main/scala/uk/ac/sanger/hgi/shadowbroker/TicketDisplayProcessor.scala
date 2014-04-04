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
    * Neither the name of the <organization> nor the
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

import org.apache.camel.Exchange
import org.apache.camel.Processor

/**
 * Processes ticket data into a suitable format for display in the MUC.
 */
object TicketDisplayProcessor extends Processor {

  def process(exc: Exchange) {

    val properties = exc.getIn().getBody(classOf[Map[String, String]])

    val bodyText = for {
      queue <- properties.get("Queue")
      creator <- properties.get("Creator")
      subject <- properties.get("Subject")
      created <- properties.get("Created")
      ticketid <-properties.get("id")
    } yield s"""
      There is an unclaimed ticket in the $queue queue:
      $subject
      Requested by $creator on $created
      
      View ticket details: https://rt.sanger.ac.uk/Ticket/Display.html?id=$ticketid
      Take this ticket: https://rt.sanger.ac.uk/Ticket/Display.html?Action=Take&id=$ticketid
      """

    bodyText.foreach(exc.getOut().setBody(_))

  }
}