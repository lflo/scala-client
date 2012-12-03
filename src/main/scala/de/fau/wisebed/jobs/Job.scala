package de.fau.wisebed.jobs

import de.fau.wisebed._
import eu.wisebed.api.controller._
import scala.parallel.Future
import scala.concurrent._
import scala.collection._
import scala.actors._
import org.slf4j.Logger

abstract class Job[S](nodes: Seq[String]) extends Actor with Future[Map[String, S]] {
	val log:Logger

	var expc:Option[OutputChannel[Any]] = None

	private[jobs] var states = Map[String, SyncVar[S]](nodes.map(_ -> new SyncVar[S]) : _*)

	private[jobs] def update(node: String, v:Int):Option[S]

	val successValue: S

	def isDone:Boolean = states.values.forall(_.isSet)

	def statusUpdate(s:Status) {
		log.debug("Got state for " + s.getNodeId + ": " + s.getValue)

		update(s.getNodeId, s.getValue) match {
			case Some(stat) => states(s.getNodeId).set(stat)
			case None => // no status update
		}

		if(expc.isDefined && isDone) expc.get ! RemJob(this)
	}

	def act() {
		log.debug("Job actor started")

		loopWhile(!isDone) {
			react {
				case s:Status =>
					if(!expc.isDefined) expc = new Some(sender)
					statusUpdate(s)
				case x =>
					log.error("Got unknown class: {}", x.getClass)
			}
		}

		log.debug("Job actor stopped")
	}

	def apply():Map[String, S] = states.mapValues(_.get)
	def status = apply

	def success:Boolean = apply().values.forall(_ == successValue)
}
