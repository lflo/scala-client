package de.fau.wisebed.messages

import eu.wisebed.api._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.fau.wisebed.wrappers.WrappedMessage._

class MessageLogger(f:(common.Message)=>Unit) extends MessageInput {
	val log = LoggerFactory.getLogger(this.getClass)

	def handleMsg(mi:common.Message) {		
		f(mi)
	}
}
