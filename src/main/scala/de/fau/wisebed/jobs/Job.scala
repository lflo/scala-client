package de.fau.wisebed.jobs

import scala.parallel.Future
import eu.wisebed.api.controller.Status
import eu.wisebed.api.controller.RequestStatus
import scala.actors.Actor
import org.slf4j.Logger
import scala.actors.TIMEOUT

abstract class Job extends Actor with Future[Boolean] {
	var id:String = ""
	var st_done = false
	val log:Logger
	
	protected var _success = false
	def success = _success

	def statusUpdate(su:Status):Unit
	
	def act(){
		log.debug("Job Actor started")

		loopWhile(!st_done) {
			react {
				case s:Status =>
					statusUpdate(s)
				case x =>
					log.error("Got unknown class: {}", x.getClass)
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
		notify()
	}
}
