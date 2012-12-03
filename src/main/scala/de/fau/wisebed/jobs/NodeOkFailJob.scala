package de.fau.wisebed.jobs

import scala.collection.Traversable
import eu.wisebed.api.controller.Status
import scala.collection._
import org.slf4j.LoggerFactory

object OKFailState extends Enumeration {
	type OKFailState = Value
	val OK, Failure, Unknown  = Value

	def idToEnum(id:Int):OKFailState = id match {
		case 1 => OK
		case 0 => Failure
		case -1 => Unknown
	}	
}
import OKFailState._

class NodeOkFailJob(name:String, nodes:Seq[String]) extends Job[OKFailState](nodes) {
	val log = LoggerFactory.getLogger(this.getClass + "." + name)

	val successValue = OK

	def update(node:String, v:Int) = Some(idToEnum(v))
}
