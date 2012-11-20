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


class Experiment (_res:List[Reservation], implicit val tb:Testbed){
	
	val log = LoggerFactory.getLogger(this.getClass);
	
	val localControllerEndpointURL = "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":" + Experiment.port.toString+ "/controller/" + Experiment.id
	
	val res = _res.map(_.copy)
	
	var active = true;
	
	val jobs = mutable.Map[String, Job]() 
	
	val controller = new Controller {
		var dg:DelegationController = null
		
		def receive(msg: java.util.List[common.Message]): Unit = {
			// nothing to do
		}
		def receiveStatus(requestStatuses: java.util.List[eu.wisebed.api.controller.RequestStatus]): Unit = {
			for(rs <- requestStatuses){
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
			for (msg <- msgs) {
				log.info("MSG: " + msg);
			}
		}
		def experimentEnded(): Unit = {
			active = false
			if(dg != null) dg.endpoint.stop
			log.info("Experiment ended")
		}
		
	}

	
	val delegator = new DelegationController(controller, localControllerEndpointURL);
		
	
	log.debug("Local controller published on url: {}", localControllerEndpointURL);
	
	val wsnService:wsn.WSN = {	
		val keys = res.foldLeft(Buffer[rs.SecretReservationKey]())(_ ++= _.secretReservationKeys)
		
		
		val wsnEndpointURL:String = tb.sessionManagement.getInstance(
				seqAsJavaList(keys),
				localControllerEndpointURL);
		log.debug("Got a WSN instance URL, endpoint is: \"{}\"", wsnEndpointURL);
		WisebedServiceHelper.getWSNService(wsnEndpointURL);
	}
	log.debug("Local Endpoint initiated")

	//----------------------- End constructor ---------------------------------------------
	
	def flash(file:String, nodes:Traversable[String]){
		val prog = RichProgram(file);
		val map = List.fill(nodes.size){new java.lang.Integer(0)}
		val rid:String = wsnService.flashPrograms(nodes.toList, map, List(prog))
		jobs += (rid -> new FlashJob(nodes))
		
	}
	
	
	
	
}

object Experiment{
	private var intid = 1;
	private def id:String = {(intid +=1); intid.toString}
	
	import java.net.ServerSocket
	private lazy val port:Int = {
		val socket= new ServerSocket(0)
		val port = socket.getLocalPort()
		socket.close();
		port
	}
	
	
}