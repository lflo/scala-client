package de.fau.wisebed.messages

import eu.wisebed.api.common._
import scala.actors.Actor
import org.slf4j.Logger

abstract trait MessageInput extends Actor {
	val log:Logger
	
	protected def handleMsg(msg:Message):Unit
	
	def act() {
		log.debug("Job actor started")

		loop {
			react {				
				case s:Message =>
					handleMsg(s)						
				case x =>
					log.error("Got unknown class: {}", x.getClass)
			}
		}
		
		log.debug("Job actor stopped")
	}
}
