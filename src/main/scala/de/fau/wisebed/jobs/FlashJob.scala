package de.fau.wisebed.jobs

import eu.wisebed.api.controller.Status
import scala.collection._
import org.slf4j.LoggerFactory

class FlashJob(nodes:Traversable[String]) extends Job {
	val log = LoggerFactory.getLogger(this.getClass)
	
	val stat = mutable.Map[String, Int]()
	stat ++= nodes.map(_ -> 0)
	
	def statusUpdate(s:Status):Unit = {
		
		val v = s.getValue()
		val node = s.getNodeId 
		
		stat(node) = v
		
		if(v == -1){
			log.warn("Failed to Flash mote: ", node)
		} else if(v == -2){
			log.warn("Mote " + node + " not found")
		} else {
			log.info("Flashing mote " + node + " " + v + "%" )
		}
			
		
		checkdone
	}
	
	def checkdone = {
		if(stat.forall(v => {v._2 == 100 || v._2 == -1 || v._2 == -2})){
			_success = stat.forall(v => {v._2 == 100 })
			done()
			log.info("Finished flashing motes: " + {if(_success) "OK" else "Failed"})
		} 
	}
}
