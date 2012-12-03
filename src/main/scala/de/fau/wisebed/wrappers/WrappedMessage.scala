package de.fau.wisebed.wrappers

import de.fau.wisebed._
import eu.wisebed.api.common._
import de.fau.wisebed.WisebedApiConversions._
import java.util.GregorianCalendar

class WrappedMessage(val msg:Message) {
	def dataString = msg.getBinaryData.map(_.toChar).mkString
	def dataString_=(data:String) = msg.setBinaryData(data.map(_.toByte).toArray)
	
	def data = msg.getBinaryData
	def data_=(data:Array[Byte]) = msg.setBinaryData(data)
	
	def node = Node(msg.getSourceNodeId)

	def timestamp = msg.getTimestamp.toGregorianCalendar
	def timestamp_=(gc:GregorianCalendar) = msg.setTimestamp(gc)
	
	def copy:Message = {
		val rv = new Message
		rv.setBinaryData(msg.getBinaryData)
		rv.setSourceNodeId(msg.getSourceNodeId)
		rv.setTimestamp(msg.getTimestamp)
		rv
	}
}

object WrappedMessage {
	implicit def msg2wmsg(msg:Message):WrappedMessage = new WrappedMessage(msg)
	implicit def wmsg2msg(wmsg:WrappedMessage):Message = wmsg.msg
}
