package de.fau.wisebed.wrappers

import eu.wisebed.api._
import de.fau.wisebed.WisebedApiConversions._
import java.util.GregorianCalendar

class WrappedMessage (_msg:common.Message) {
	def dataString = msg.getBinaryData.map(_.toChar).mkString
	def dataString_=(data:String)=msg.setBinaryData(data.map(_.toByte).toArray)
	
	def data = msg.getBinaryData
	def data_= (data:Array[Byte]) = msg.setBinaryData(data)
	
	def node = msg.getSourceNodeId
	def timestamp = msg.getTimestamp.toGregorianCalendar
	def timestamp_=(gc:GregorianCalendar) = msg.setTimestamp(gc)
	
	def msg = _msg
	
	def copy:common.Message = {
		val rv = new common.Message
		rv.setBinaryData(msg.getBinaryData)
		rv.setSourceNodeId(msg.getSourceNodeId)
		rv.setTimestamp(msg.getTimestamp)
		rv
	}
}

object WrappedMessage{
	implicit def msg2wmsg(msg:common.Message):WrappedMessage = new WrappedMessage(msg)
	implicit def wmsg2msg(wmsg:WrappedMessage):common.Message = wmsg.msg
}