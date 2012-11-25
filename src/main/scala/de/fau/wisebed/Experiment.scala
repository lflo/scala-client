package de.fau.wisebed

import eu.wisebed.api.controller.Controller
import eu.wisebed.api._
import eu.wisebed.api.common._
import eu.wisebed.api.controller._
import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory
import java.net.InetAddress
import scala.collection.mutable.Buffer
import Reservation.secretReservationKey_Rs2SM
import de.fau.wisebed.jobs.Job
import de.fau.wiseml.wrappers.RichProgram
import scala.collection._
import jobs._
import java.util.GregorianCalendar
import java.util.Date


class MoteMessage(val node:String, val data:Array[Byte], val time:GregorianCalendar) {
	def this (m:common.Message){
		this(m.getSourceNodeId, m.getBinaryData, m.getTimestamp.toGregorianCalendar)
	}

	def dataString = data.map(_.toChar).mkString
}


class Experiment (res:List[Reservation], implicit val tb:Testbed) {
	val log = LoggerFactory.getLogger(this.getClass)
	
	var messages = List[MoteMessage]()

	var active = true

	val controller = new ExperimentController

	controller.onMessage { msg: Message =>
		val mm = new MoteMessage(msg)
		messages ::= mm
		// val time = (new GregorianCalendar).getTimeInMillis - msg.getTimestamp.toGregorianCalendar.getTimeInMillis
		log.debug("Got message from " + mm.node + ": " + mm.dataString)
	}

	controller.onEnd {
		active = false
		stopdel()
		log.info("Experiment ended")
	}

	private def stopdel() {
		// controller.endpoint.stop()
	} 
	
	log.debug("Local controller published on url: {}", controller.url)
	
	val wsnService:wsn.WSN = {	
		val keys: Seq[eu.wisebed.api.sm.SecretReservationKey] = res.map(_.secretReservationKeys).flatten
		
		val wsnEndpointURL = tb.sessionManagement.getInstance(keys, controller.url)
		log.debug("Got a WSN instance URL, endpoint is: \"{}\"", wsnEndpointURL)
		WisebedServiceHelper.getWSNService(wsnEndpointURL)
	}
	
	//----------------------- End constructor ---------------------------------------------
	
	def flash(file:String, nodes:List[String]):FlashJob = {
		if(active == false) return null
		val prog = RichProgram(file);
		val map = List.fill(nodes.size){new java.lang.Integer(0)}
		val rid:String = wsnService.flashPrograms(nodes, map, List(prog))
		val job = new FlashJob(nodes)
		controller.onStatus(rid) { s: Status =>
			job.statusUpdate(List(s))
		}
		job
		
	}
	
	def areNodesAlive(nodes:List[String]):NodesAliveJob = {
		if(active == false) return null
		val job = new NodesAliveJob(nodes)
		val rid = wsnService.areNodesAlive(nodes)
		controller.onStatus(rid) { s: Status =>
			job.statusUpdate(List(s))
		}
		job
	}
	
	def resetNodes(nodes:List[String]):ResetJob = {
		if(active == false) return null
		val job = new ResetJob(nodes)
		val rid = wsnService.resetNodes(nodes);
		controller.onStatus(rid) { s: Status =>
			job.statusUpdate(List(s))
		}
		job
	}
	
	def send(nodes:List[String], data:String):SendJob = {
		import CalConv._
		if(active == false) return null
		val job = new SendJob(nodes)
		val msg = new common.Message
		msg.setBinaryData(data.toArray.map(_.toByte))
		msg.setSourceNodeId("urn:fauAPI:none:0xFFFF")
		msg.setTimestamp(new GregorianCalendar)
		val rid = wsnService.send(nodes, msg)
		controller.onStatus(rid) { s: Status =>
			job.statusUpdate(List(s))
		}
		job
	}
}
