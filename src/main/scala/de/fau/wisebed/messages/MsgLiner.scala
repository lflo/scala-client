package de.fau.wisebed.messages

import eu.wisebed.api._
import de.fau.wisebed.wrappers.WrappedMessage._
import scala.collection.mutable
import eu.wisebed.api.common.Message

trait MsgLiner extends MessageInput {

	private var mbuf = mutable.Map[String, common.Message]()

	abstract override def handleMsg(msg: common.Message) {
		val wmsg = mbuf.remove(msg.node) match {
			case o: Some[Message] => {
				val old = o.get
				old.data ++= msg.data
				old.timestamp = msg.timestamp
				old
			}
			case _ => msg
		}
		
		val str = wmsg.dataString
		val split = str.split("\n").toList
		
		split.foreach(x => {
			val snd = wmsg.copy
			snd.dataString = x
			if(x != split.last || str.last == '\n'){
				super.handleMsg(snd)
			} else {
				mbuf += wmsg.node -> wmsg
			}
		})
		

	}

}

