package de.fau.wisebed.jobs

import de.fau.wisebed._
import eu.wisebed.api.controller.Status
import scala.collection._
import org.slf4j.LoggerFactory

object MoteFlashState extends Enumeration {
	type MoteFlashState = Value
	val OK, NotFound, InProgress, Unknown, Error  = Value

	def idToEnum(id:Int):MoteFlashState = id match {
		case 100  => OK
		case -2  => NotFound
		case -1 => Error
		case _  => InProgress
	}
}
import MoteFlashState._

class FlashJob(nodes:Seq[Node]) extends Job[MoteFlashState](nodes) {
	val log = LoggerFactory.getLogger(this.getClass)

	val successValue = OK

	def update(node:Node, v:Int) = {
		if(v == -1) {
			log.warn("Failed to Flash mote: ", node)
			Some(Error)
		} else if(v == -2) {
			log.warn("Mote " + node + " not found")
			Some(NotFound)
		} else if(v == 100) {
			Some(OK)
		} else {
			log.info("Flashing mote " + node + " " + v + "%")
			None
		}
	}
}
