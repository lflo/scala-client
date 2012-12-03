package de.fau.wisebed.messages

import eu.wisebed.api._
import de.fau.wisebed.wrappers.WrappedMessage._
import scala.collection
import eu.wisebed.api.common.Message

trait MsgLiner extends MessageInput {
	private var mbuf = Map[String, String]()

	abstract override def handleMsg(msg: Message) {
		val strings = (mbuf.getOrElse(msg.node, "") ++ msg.dataString).split('\n')

		for(s <- strings) {
			if(s != strings.last) {
				val m = msg.copy
				m.dataString = s
				super.handleMsg(m)
			} else {
				mbuf += msg.node -> s
			}
		}
	}
}
