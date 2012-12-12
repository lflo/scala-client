package de.fau.wisebed.messages

import eu.wisebed.api.common._
import de.fau.wisebed.RemMes
import de.fau.wisebed.StopAct
import scala.actors.Actor
import org.slf4j.Logger
import scala.ref.WeakReference







abstract trait MessageInput extends Actor {
	val log:Logger
	private var state  = 0
	
	protected def handleMsg(msg:Message):Unit
	protected def stopInput() { if(state == 0) state = 1 }
	
	def isWeak = false
	
	private val ctr = MessageInput.getctr
	
	def act() {
		log.debug("Message actor {} started: {}", ctr.toString, this.toString)

		loop {
			react {				
				case s:Message =>
					log.trace("MA {} got {}", ctr.toString, s.hashCode)
					if(state == 0) handleMsg(s)
					if(state == 1){
						sender ! RemMes(this)
						state = 2
					} 
				case StopAct =>
					log.debug("Message actor {} stopped: {}", ctr.toString, this.toString)
					exit
				case x =>
					log.error("Got unknown class: {}", x.getClass)
			}
		}
	}
}


object MessageInput {
	var ctr = 0
	def getctr = {ctr += 1 ; ctr -1}
}
