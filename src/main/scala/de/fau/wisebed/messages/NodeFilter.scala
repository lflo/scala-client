package de.fau.wisebed.messages

import eu.wisebed.api._
import scala.collection.mutable
import eu.wisebed.api.common.Message
import de.fau.wisebed.wrappers.WrappedMessage._

trait NodeFilter extends MessageInput {
	var nodes = Set[String]()
	
	abstract override def handleMsg(msg:common.Message) {
		if(nodes.contains(msg.node)) super.handleMsg(msg)
	}
	
	def setNodeFilter(f:Set[String]) {
		nodes = f
	}
}
