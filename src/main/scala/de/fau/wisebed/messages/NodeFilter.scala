package de.fau.wisebed.messages

import de.fau.wisebed._
import eu.wisebed.api._
import scala.collection.mutable
import eu.wisebed.api.common.Message
import de.fau.wisebed.wrappers.WrappedMessage._

trait NodeFilter extends MessageInput {
	var nodes = Set[Node]()
	
	abstract override def handleMsg(msg:Message) {
		if(nodes.contains(msg.node)) super.handleMsg(msg)
	}
	
	def setNodeFilter(f:Set[Node]) {
		nodes = f
	}
}
