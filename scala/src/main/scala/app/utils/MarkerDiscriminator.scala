package app.utils

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.sift.AbstractDiscriminator
import net.logstash.logback.marker.ObjectAppendingMarker
import org.slf4j.Marker

/** Extracts data put into marker values by [[com.emarsys.logger.internal.LoggingContextUtil.toMarker]]. */
class MarkerDiscriminator extends AbstractDiscriminator[ILoggingEvent] {
  private var key = "not-set"

  def check(marker: Marker): Option[String] = marker match {
    case marker: ObjectAppendingMarker if marker.getFieldName == key =>
      Some(marker.getFieldValue.toString)
    case _ =>
      None
  }

  override def getDiscriminatingValue(e: ILoggingEvent): String = {
    val marker = e.getMarker
    check(marker) match {
      case Some(value) => return value
      case None =>
    }

    val iterator = marker.iterator()
    while (iterator.hasNext) {
      check(iterator.next()) match {
        case Some(value) => return value
        case None =>
      }
    }

    "not-found"
  }

  override def getKey = key
  def setKey(key: String): Unit = this.key = key
}
