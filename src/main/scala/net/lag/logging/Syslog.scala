/*
 * Copyright 2009 Robey Pointer <robeypointer@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lag.logging

import java.util.{logging => javalog}
import java.net.{DatagramPacket, DatagramSocket, InetAddress, InetSocketAddress, SocketAddress}
import java.text.SimpleDateFormat
import scala.actors._
import scala.actors.Actor._
import net.lag.extensions._


private[logging] object Syslog {
  val DEFAULT_PORT = 514

  val PRIORITY_USER = 8
  val PRIORITY_DAEMON = 24
  val PRIORITY_LOCAL0 = 128
  val PRIORITY_LOCAL1 = 136
  val PRIORITY_LOCAL2 = 144
  val PRIORITY_LOCAL3 = 152
  val PRIORITY_LOCAL4 = 160
  val PRIORITY_LOCAL5 = 168
  val PRIORITY_LOCAL6 = 176
  val PRIORITY_LOCAL7 = 184

  private val SEVERITY_EMERGENCY = 0
  private val SEVERITY_ALERT = 1
  private val SEVERITY_CRITICAL = 2
  private val SEVERITY_ERROR = 3
  private val SEVERITY_WARNING = 4
  private val SEVERITY_NOTICE = 5
  private val SEVERITY_INFO = 6
  private val SEVERITY_DEBUG = 7

  /**
   * Convert the java/scala log level into its closest syslog-ng severity.
   */
  private[logging] def severityForLogLevel(level: Int): Int = {
    if (level >= Level.FATAL.value) {
      SEVERITY_ALERT
    } else if (level >= Level.CRITICAL.value) {
      SEVERITY_CRITICAL
    } else if (level >= Level.ERROR.value) {
      SEVERITY_ERROR
    } else if (level >= Level.WARNING.value) {
      SEVERITY_WARNING
    } else if (level >= Level.INFO.value) {
      SEVERITY_INFO
    } else {
      SEVERITY_DEBUG
    }
  }
}


class SyslogFormatter(useIsoDateFormat: Boolean) extends Formatter {
  private val ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  private val OLD_SYSLOG_DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss")

  // user may override:
  var priority = Syslog.PRIORITY_USER

  // user may override:
  var hostname = InetAddress.getLocalHost().getHostName()

  private var _serverName: Option[String] = None
  def serverName = _serverName match {
    case None => ""
    case Some(name) => name
  }
  def serverName_=(name: String) = {
    _serverName = Some(name)
  }
  def clearServerName = {
    _serverName = None
  }

  override def dateFormat = if (useIsoDateFormat) ISO_DATE_FORMAT else OLD_SYSLOG_DATE_FORMAT
  override def lineTerminator = ""

  override def formatPrefix(level: javalog.Level, date: String, name: String): String = {
    val syslogLevel = level match {
      case Level(name, x) => Syslog.severityForLogLevel(x)
      case x: javalog.Level => Syslog.severityForLogLevel(x.intValue)
    }
    _serverName match {
      case None => "<%d>%s %s %s: ".format(priority | syslogLevel, date, hostname, name)
      case Some(serverName) => "<%d>%s %s [%s] %s: ".format(priority | syslogLevel, date, hostname, serverName, name)
    }
  }
}


class SyslogHandler(useIsoDateFormat: Boolean, server: String) extends Handler(new SyslogFormatter(useIsoDateFormat)) {
  private val socket = new DatagramSocket
  private[logging] val dest: SocketAddress = server.split(":", 2).toList match {
    case host :: port :: Nil => new InetSocketAddress(host, port.toInt)
    case host :: Nil => new InetSocketAddress(host, Syslog.DEFAULT_PORT)
    case _ => null
  }

  def flush() = { }
  def close() = { }

  override def formatter = getFormatter.asInstanceOf[SyslogFormatter]

  def priority = formatter.priority
  def priority_=(priority: Int) = {
    formatter.priority = priority
  }

  def serverName = formatter.serverName
  def serverName_=(name: String) {
    formatter.serverName = name
  }

  def clearServerName = formatter.clearServerName

  def publish(record: javalog.LogRecord) = {
    val data = formatter.format(record).getBytes
    val packet = new DatagramPacket(data, data.length, dest)
    Future {
      try {
        socket.send(packet)
      } catch {
        case e =>
          System.err.println(Formatter.formatStackTrace(e, 30).mkString("\n"))
      }
    }
  }
}

object Future {
  private case class Do(action: () => Unit)
  private case object Wait

  def apply(action: => Unit) = writer ! Do(() => action)
  def sync = writer !? Wait

  private lazy val writer = actor {
    while (true) {
      receive {
        case Wait => reply(Wait)
        case Do(action) => action()
      }
    }
  }
}
