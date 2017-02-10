package uk.co.appministry.scathon.models.sse

import java.nio.charset.StandardCharsets
import java.util.Scanner

object ServerSentEventParser {

  private val linePattern = """([^:]+): ?(.*)""".r

  def parse(bytes: Array[Byte]): Option[ServerSentEvent] = {
    parse(new String(bytes, StandardCharsets.UTF_8))
  }

  def parse(message: String): Option[ServerSentEvent] = {
    var event = ServerSentEvent()
    var finished = false
    val sc = new Scanner(message)
    while (sc.hasNextLine) {
      val line = sc.nextLine().trim()
      line match {
        case "" => finished = true
        case linePattern(f, v) if finished == false =>
          f match {
            case "id" => event = event.copy(id = Some(v))
            case "event" => event = event.copy(eventType = Some(v))
            case "data" => event = event.copy(data = Some(v))
            case "repeat" => event = event.copy(repeat = Some(v))
            case _ =>
          }
        case _ =>
      }
    }
    if (finished) {
      Some(event)
    } else {
      None
    }
  }

}

case class ServerSentEvent(val id: Option[String] = None,
                           val eventType: Option[String] = None,
                           val data: Option[String] = None,
                           val repeat: Option[String] = None) {

  override def toString(): String = {
    List(
      id match {
        case Some(v) => Some(s"id: $v")
        case None => None
      },
      eventType match {
        case Some(v) => Some(s"event: $v")
        case None => None
      },
      data match {
        case Some(v) => Some(s"data: $v")
        case None => None
      },
      repeat match {
        case Some(v) => Some(s"repeat: $v")
        case None => None
      }
    ).flatten.mkString("\n") + "\n\n"
  }

}
