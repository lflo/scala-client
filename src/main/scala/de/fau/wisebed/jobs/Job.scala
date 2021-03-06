package de.fau.wisebed.jobs

import scala.parallel.Future
import eu.wisebed.api.controller.Status
import eu.wisebed.api.controller.RequestStatus
import scala.actors.Actor
import org.slf4j.Logger
import scala.actors.TIMEOUT
import scala.actors.OutputChannel
import de.fau.wisebed.RemJob
import de.fau.wisebed.StopAct

class Holder[S] extends Future[S] {
	private var res: Option[S] = None
	private var waiting: Boolean = false

	def isDone:Boolean = res != None

	def set(r: S): Unit = synchronized {
		res = Some(r)
		if(waiting) notify()
	}

	def apply():S = synchronized {
		waiting = true
		if(!isDone) wait()
		res.get
	}
}

abstract class Job[S](nodes: Seq[String]) extends Actor with Future[Map[String, S]] {
	val log:Logger
	
	private[jobs] var states = Map[String, Holder[S]](nodes.map(_ -> new Holder[S]) : _*)

	private[jobs] def update(node: String, v:Int, msg:String):Option[S]
	
	val successValue: S

	def isDone:Boolean = states.values.forall(_.isDone)

	def statusUpdate(s:Status) {
		log.debug("Got state for " + s.getNodeId + ": " + s.getValue)
		update(s.getNodeId, s.getValue, s.getMsg) match {
			case Some(stat) => states(s.getNodeId).set(stat)
			case None => // no status update
		}
	}

	def act(){
		log.debug("Job actor started: {}", this.toString)
		loop {
			react{				
				case s:Status =>
					statusUpdate(s)			
					if(isDone) sender ! RemJob(this)
				case StopAct =>
					log.debug("Job actor stopped: {}", this.toString)
					exit
				case x =>
					log.error("Got unknown class: {}", x.getClass)
			}
		}
	}
	
	def apply():Map[String, S] = {
		states.mapValues(_.apply())
	}
		
	def status = apply

	def success:Boolean = apply().values.forall(_ == successValue)
}
