package de.fau.wisebed.jobs

import scala.parallel.Future
import eu.wisebed.api.controller.Status
import eu.wisebed.api.controller.RequestStatus
import scala.actors.Actor
import org.slf4j.Logger
import scala.actors.TIMEOUT
import de.fau.wisebed.RemJob
import scala.actors.OutputChannel


abstract class Job extends Actor with Future[Boolean] {
	var id:String = ""
	var st_done = false
	val log:Logger
	var expc:Option[OutputChannel[Any]] = None
	
	protected var _success = false;
	def success = _success

	def statusUpdate(su:Status):Unit
	
	def act(){
		log.debug("Job Actor started")
		loopWhile(!st_done) {
			react{				
				case s:Status =>
					if(!expc.isDefined) expc = new Some(sender)
					statusUpdate(s)						
				case x =>
					log.error("Got unknow class: {}", x.getClass.toString)
			}
		}
		log.debug("Job Actor stopped")
	}
	
		
	def isDone:Boolean = st_done

	def apply:Boolean  = synchronized {
		while (!st_done) wait()
		_success
	}
	
	protected def done() = synchronized {
		st_done  = true
		if(expc.isDefined) expc.get ! RemJob(this)
		notify()
	}
}
