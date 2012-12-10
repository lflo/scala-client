package de.fau.wisebed.messages

import eu.wisebed.api._
import de.fau.wisebed.wrappers.WrappedMessage._
import scala.collection
import eu.wisebed.api.common.Message

trait MsgLiner extends MessageInput {
	private var mbuf = Map[String, Message]()

	abstract override def handleMsg(msg: Message) {
		val wmsg = mbuf.get(msg.node) match {
			case Some(old) => {
				old.data ++= msg.data
				old.timestamp = msg.timestamp
				old
			}
			case _ => msg
		}
		
		val str = wmsg.dataString
		val split = str.split("\n")
		
		split.foreach(x => {
			val snd = wmsg.copy
			snd.dataString = x
			if(x != split.last || str.last == '\n'){
				super.handleMsg(snd)
			} else {
				mbuf += wmsg.node -> snd
			}
		})
	}
}
