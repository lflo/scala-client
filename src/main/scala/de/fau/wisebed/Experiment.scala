package de.fau.wisebed

import jobs._
import wrappers._
import eu.wisebed.api.controller.Controller
import eu.wisebed.api._
import eu.wisebed.api.common._
import eu.wisebed.api.controller._
import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory
import java.net.InetAddress
import scala.collection.mutable.Buffer
import Reservation.secretReservationKey_Rs2SM
import scala.collection._
import java.util.GregorianCalendar
import java.util.Date
import de.fau.wisebed.WisebedApiConversions._
import de.fau.wisebed.messages.MsgLiner
import de.fau.wisebed.messages.MessageLogger

class MoteMessage(val node:String, val data:Array[Byte], val time:GregorianCalendar) {
	def this (m:common.Message){
		this(m.getSourceNodeId, m.getBinaryData, m.getTimestamp.toGregorianCalendar)
	}

	def dataString = data.map(_.toChar).mkString
}

case class ExperimentEndedException(t:String) extends Exception(t)

/**
 * @todo There is a concurrency issue if a message is received before the id is added to the jobmap (this is unlikely, though)
 */	
class Experiment (res:List[Reservation], implicit val tb:Testbed) {
	val log = LoggerFactory.getLogger(this.getClass)

	var active = true

	val controller = new ExperimentController
	val endpoint = controller.start()

	if(log.isTraceEnabled) {
		val msghndl = new MessageLogger(mi => {
			import WrappedMessage._
			log.debug("Got message from " + mi.node + ": " + mi.dataString)
		}) with MsgLiner
		controller.addMessageInput(msghndl)
		controller.onEnd { controller.remMessageInput(msghndl) }
	}

	controller.onEnd {
		active = false
		stopdel()
		log.info("Experiment ended")
	}

	private def stopdel() {
		endpoint.stop()
	} 
	
	log.debug("Local controller published on url: {}", controller.url)
	
	val wsnService:wsn.WSN = {	
		val keys: Seq[eu.wisebed.api.sm.SecretReservationKey] = res.map(_.secretReservationKeys).flatten
		
		val wsnEndpointURL = tb.sessionManagement.getInstance(keys, controller.url)
		log.debug("Got a WSN instance URL, endpoint is: \"{}\"", wsnEndpointURL)
		WisebedServiceHelper.getWSNService(wsnEndpointURL)
	}
	
	//----------------------- End constructor ---------------------------------------------
	
	//----------------------- Jobs --------------------------------------------------------
	
	def flash(file:String, nodes:Seq[String]):FlashJob = {
		if(active == false) throw new ExperimentEndedException("Can not flash nodes, as the experiment ended")
		val prog = RichProgram(file)
		val map = List.fill(nodes.size){new java.lang.Integer(0)}
		val job = new FlashJob(nodes)
		controller.addJob(job, wsnService.flashPrograms(nodes, map, List(prog)))
		job	
	}
	
	def areNodesAlive(nodes:Seq[String]):NodesAliveJob = {
		if(active == false) throw new ExperimentEndedException("Can not access nodes, as the experiment ended")
		val job = new NodesAliveJob(nodes)
		controller.addJob(job, wsnService.areNodesAlive(nodes))
		job
	}
	
	def resetNodes(nodes:Seq[String]):NodeOkFailJob = {
		if(active == false) throw new ExperimentEndedException("Can not reset nodes, as the experiment ended")
		val job = new NodeOkFailJob("reset", nodes)
		controller.addJob(job, wsnService.resetNodes(nodes))
		job
	}
	
	def send(node:String,data:String):NodeOkFailJob = send(List(node), data)
	
	def send(nodes:Seq[String], data:String):NodeOkFailJob = {
		if(active == false) throw new ExperimentEndedException("Can not send data to nodes, as the experiment ended")
		val job = new NodeOkFailJob("send" , nodes)
		val msg = new common.Message
		msg.setBinaryData(data.toArray.map(_.toByte))
		msg.setSourceNodeId("urn:fauAPI:none:0xFFFF")
		msg.setTimestamp(new GregorianCalendar)
		controller.addJob(job, wsnService.send(nodes, msg))
		job
	}
	
	def setChannelHandler(nodes:Seq[String], cnf:wsn.ChannelHandlerConfiguration):NodeOkFailJob = {
		if(active == false) throw new ExperimentEndedException("Can not set channel handler, as the experiment ended")
		val cn = List.fill(nodes.size){cnf}
		val job =  new NodeOkFailJob("setChannelHandler", nodes)
		controller.addJob(job,wsnService.setChannelPipeline(nodes, cn))
		job
	}
	
	
	def supportedChannelHandlers:Seq[WrappedChannelHandlerDescription] = {
		import wrappers.WrappedChannelHandlerDescription._
		wsnService.getSupportedChannelHandlers.map(chd2wchd(_))
	}
	
	//-------------- other stuff -------------------------------------------------------------
	
	def addMessageInput(mi:messages.MessageInput) {
		controller.addMessageInput(mi)
	}
	
	def remMessageInput(mi:messages.MessageInput) {
		controller.remMessageInput(mi)
	}
}
