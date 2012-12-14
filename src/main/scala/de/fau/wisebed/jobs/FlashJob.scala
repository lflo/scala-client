package de.fau.wisebed.jobs

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
		case n if 0 until 100 contains n  => InProgress
		case n  => Error 
	}
}
import MoteFlashState._

class FlashJob(nodes:Seq[String]) extends Job[MoteFlashState](nodes) {
	val log = LoggerFactory.getLogger(this.getClass)

	val successValue = OK

	def update(node:String, v:Int, msg:String) = {
		v match {				
			case -2  =>	
				log.warn("Mote " + node + " not found")
				Some(NotFound)
			case 100 =>
				Some(OK)
			case n if n >= 0 && n  < 100 =>
				log.info("Flashing mote " + node + " " + v + "%")
				None
			case n => log.warn("Failed to Flash node {}: {}", node, msg )
				Some(Error)
		}
	}
}
