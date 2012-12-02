package de.fau.wisebed.jobs

import scala.collection.Traversable
import eu.wisebed.api.controller.Status
import scala.collection._
import org.slf4j.LoggerFactory

object OKFailState extends Enumeration {
	type OKFailState = Value
	val OK, Failure, Unknown, NotSet  = Value

	def idToEnum(id:Int):OKFailState = id match {
		case 1 => OK
		case 0 => Failure
		case -1 => Unknown
	}	
}

class NodeOkFailJob(name:String, nodes:List[String]) extends Job {
	import OKFailState._
		
	val log = LoggerFactory.getLogger(this.getClass + "." + name)
	
	val stat = new mutable.ListMap[String, OKFailState]
	stat ++= nodes.map(_ -> NotSet)
	
	def status():Map[String,OKFailState] = {
		apply		
		//No need for sync, as apply continues once nothing changes any more
		stat.clone
	}

	override def statusUpdate(s:Status):Unit = {					
		val v = s.getValue()
		val node = s.getNodeId
		log.debug("Got state for " + node + ": " + v )
		stat(node) =  idToEnum(v)
		
		checkdone
	}
	
	private def checkdone = {			
		if (stat.forall(_._2 != NotSet)) {
			//From here on do not need to synchronize any more as no more changes are to be expected.
			
			def getNodeState(sb: StringBuilder, st:OKFailState):StringBuilder = {
				val mts = stat.filter(_._2 == st).map(_._1)
				if (mts.size > 0) sb ++= " " + st + ": " + mts.mkString(", ")
				sb
			}
						
			_success = stat.forall(v => { v._2 == OK })
			
			val sb = OKFailState.values.foldLeft(new StringBuilder)(getNodeState(_ , _))
			log.debug("Mote States are:" + sb.toString)
			done()
		}
	}
}
