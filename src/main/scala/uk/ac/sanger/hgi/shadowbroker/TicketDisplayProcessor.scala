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