package de.fau.wisebed.jobs

import scala.parallel.Future
import eu.wisebed.api.controller.Status




abstract class Job extends Future[Boolean]{
	var id:String = ""
	var st_done = false
	
	protected var _success = false;
	def success = _success

	def statusUpdate(stats:Traversable[Status]):Unit
		
	def isDone:Boolean = st_done

	def apply:Boolean  = synchronized {
		while (!st_done) wait()
		true
	}
	
	protected def done() = synchronized {
		st_done  = true
		notify()
	}
	
		
}