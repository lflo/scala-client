package de.fau.wisebed.jobs

import scala.collection.Traversable
import eu.wisebed.api.controller.Status
import scala.collection._
import org.slf4j.LoggerFactory




object MoteResetState extends Enumeration {
     type MoteResetState = Value
     val OK, Failure, Unknown, NotSet  = Value
     
     
     def idToEnum(id:Int):MoteResetState = {
    	 id match {
    		 case 1 => OK
    		 case 0 => Failure
    		 case -1 => Unknown
    	 }
     }
     
}





class ResetJob(nodes:List[String]) extends Job {
	import MoteResetState._
	
	
	val log = LoggerFactory.getLogger(this.getClass)
	
	val stat = new mutable.ListMap[String, MoteResetState]
	stat ++= nodes.map(_ -> NotSet)
	
	
	
	def status():Map[String,MoteResetState] = {
		apply		
		//No need for sync, as apply continues once nothing changes any more
		stat.clone
	}

	def statusUpdate(stats:Traversable[Status]):Unit = {
		for(s <- stats){			
			val v = s.getValue()
			val node = s.getNodeId
			log.debug("Got state for " + node + ": " + v )
			synchronized {stat(node) =  idToEnum(v)}
		}
		checkdone
	}
	
	

	private def checkdone = {
				
		if (synchronized{stat.forall(_._2 != NotSet)}) {
			//From here on do not need to synchronize any more as no more changes are to be expected.
			
			def getNodeState(sb: StringBuilder, st:MoteResetState):StringBuilder = {
				val mts = stat.filter(_._2 == st).map(_._1)
				if (mts.size > 0) sb ++= " " + st + ": " + mts.mkString(", ")
				sb
			}
						
			_success = stat.forall(v => { v._2 == OK })
			
			
			val sb = MoteResetState.values.foldLeft(new StringBuilder)(getNodeState(_ , _))
			log.debug("Mote States are:" + sb.toString)
			done()

		}
	}

}