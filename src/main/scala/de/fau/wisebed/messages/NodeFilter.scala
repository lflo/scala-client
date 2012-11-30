import de.fau.wisebed.messages.MessageInput
import eu.wisebed.api._

import scala.collection.mutable
import eu.wisebed.api.common.Message
import de.fau.wisebed.wrappers.WrappedMessage._


trait NodeFilter extends MessageInput {
	
	
	var nodes = Set[String]()
	
	abstract override def handleMsg(msg:common.Message) {
		if(nodes.filterNot(_ == msg.node).isEmpty) super.handleMsg(msg)
	}
		
	
	def setNodeFilter(f:Set[String]) {
		nodes = f
	}

	

}

