package de.fau.wisebed

import eu.wisebed.api.controller.Controller
import eu.wisebed.api._
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



class MoteMessage(val node:String, val data:Array[Byte], val time:GregorianCalendar){
	def this (m:common.Message){
		this(m.getSourceNodeId, m.getBinaryData, m.getTimestamp.toGregorianCalendar)
	}
}


class Experiment (_res:List[Reservation], implicit val tb:Testbed){
	
	val log = LoggerFactory.getLogger(this.getClass);
	
	val messagebuf = mutable.Buffer[MoteMessage]()
	
	val res = _res.map(_.copy)
	
	var active = true;
	
	val controller = new JobController {
		override def experimentEnded(): Unit = {
			active = false
			stopdel
			log.info("Experiment ended")
		}
		
		override def receive(msgs: java.util.List[common.Message]): Unit = {
			for(msg <- msgs){
				val mm = new MoteMessage(msg)
				messagebuf += mm 
				val str = mm.data.map(_.toChar).foldLeft(new StringBuilder)(_ + _)
				log.debug("Got message from " + mm.node + ": \"" +  str  + "\"")
				
				
			}
		}
	}

	
	val delegator = new DelegationController(controller);
		
	private def stopdel {
		//delegator.endpoint.stop
	} 
	
	
	log.debug("Local controller published on url: {}", delegator.endpointUrl);
	
	val wsnService:wsn.WSN = {	
		val keys = res.foldLeft(Buffer[rs.SecretReservationKey]())(_ ++= _.secretReservationKeys)
		
		
		val wsnEndpointURL:String = tb.sessionManagement.getInstance(
				seqAsJavaList(keys),
				delegator.endpointUrl);
		log.debug("Got a WSN instance URL, endpoint is: \"{}\"", wsnEndpointURL);
		WisebedServiceHelper.getWSNService(wsnEndpointURL);
	}
	
	
	
	//----------------------- End constructor ---------------------------------------------
	
	def flash(file:String, nodes:List[String]):FlashJob = {
		if(active == false) return null
		val prog = RichProgram(file);
		val map = List.fill(nodes.size){new java.lang.Integer(0)}
		val rid:String = wsnService.flashPrograms(nodes, map, List(prog))
		val job = new FlashJob(nodes)
		controller.addJob(rid -> job)		
		job
		
	}
	
	def areNodesAlive(nodes:List[String]):NodesAliveJob = {
		if(active == false) return null
		val job = new NodesAliveJob(nodes)
		val rid = wsnService.areNodesAlive(nodes);
		controller.addJob(rid -> job)
		job
	}
	
	def resetNodes(nodes:List[String]):ResetJob = {
		if(active == false) return null
		val job = new ResetJob(nodes)
		val rid = wsnService.resetNodes(nodes);
		controller.addJob(rid -> job)
		job
	}
	
}

