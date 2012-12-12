package de.fau.wisebed.jobs

import de.fau.wisebed._
import eu.wisebed.api.controller._
import scala.parallel.Future
import scala.concurrent._
import scala.collection._
import scala.actors._
import org.slf4j.Logger

abstract class Job[S](nodes: Seq[Node]) extends Actor with Future[Map[Node, S]] {
	val log:Logger

	private[jobs] var states = Map[Node, SyncVar[S]](nodes.map(_ -> new SyncVar[S]) : _*)

	private[jobs] def update(node: Node, v:Int):Option[S]

	val successValue: S

	def isDone:Boolean = states.values.forall(_.isSet)

	def statusUpdate(s:Status) {
		log.debug("Got state for " + s.getNodeId + ": " + s.getValue)

		update(s.getNodeId, s.getValue) match {
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

	def apply():Map[Node, S] = states.mapValues(_.get)
	def status = apply

	def success:Boolean = apply().values.forall(_ == successValue)
}
