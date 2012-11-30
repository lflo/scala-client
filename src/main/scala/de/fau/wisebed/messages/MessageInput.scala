
package de.fau.wisebed.messages

import eu.wisebed.api._
import scala.actors.Actor
import org.slf4j.Logger

abstract trait MessageInput extends Actor{
	val log:Logger
	
	protected def handleMsg(msg:common.Message):Unit
	
	def act(){
		log.debug("Job Actor started")
		loopWhile(true) {
			react{				
				case s:common.Message =>
					handleMsg(s)						
				case x =>
					log.error("Got unknow class: {}", x.getClass.toString)
			}
		}
		log.debug("Job Actor stopped")
	}
}