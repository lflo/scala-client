package de.fau.wisebed.jobs

import scala.collection._
import eu.wisebed.api.controller.Controller
import de.fau.wisebed.DelegationController
import eu.wisebed.api._
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._

class JobController extends Controller {
	val log = LoggerFactory.getLogger(this.getClass);
	
	//Active Jobs
	val jobs = mutable.Map[String, Job]()

	private def cleanjobs(){
		val done = jobs.filter(_._2.isDone).map(_._1)
		jobs --= done
	}
	
	def receive(msgs: java.util.List[common.Message]): Unit = {
		log.error("Got Message form " + msgs.map(_.getSourceNodeId).mkString(", ") + ". This should not happen, as this function must be overridden!")
	}

	def receiveStatus(requestStatuses: java.util.List[eu.wisebed.api.controller.RequestStatus]): Unit = {
		for (rs <- requestStatuses) {
			val id = rs.getRequestId
			jobs.get(id) match {
				case Some(j) => {
					j.statusUpdate(rs.getStatus)
				}
				case None => {
					log.error("Got requestId without job: " + id)
				}

			}
		}
	}
	def receiveNotification(msgs: java.util.List[String]): Unit = {
		log.error("Got Message  \"" + msgs.mkString("\", \"") + "\". This should not happen, as this function must be overridden!")
	}

	def experimentEnded() {
		log.error("Got experimentEnded. This should not happen, as this function must be overridden!")
	}
	
	def addJob(job:Tuple2[String, Job]) {
		jobs += job
	}

}