package de.fau.wisebed.jobs

import scala.collection.Traversable
import eu.wisebed.api.controller.Status
import scala.collection._
import org.slf4j.LoggerFactory
import java.util.Collections.SynchronizedMap

object MoteAliveState extends Enumeration {
	type MoteAliveState = Value
	val Alive, Dead, Unknown, Error  = Value

	def idToEnum(id:Int):MoteAliveState = id match {
		case 1  => Alive
		case 0  => Dead
		case -1 => Unknown
		case _  => Error
	}
}
import MoteAliveState._

class NodesAliveJob(nodes:Seq[String]) extends Job[MoteAliveState](nodes) {		
	val log = LoggerFactory.getLogger(this.getClass)

	val successValue = Alive

	def update(node:String, v:Int, msg:String) = Some(idToEnum(v))
}
