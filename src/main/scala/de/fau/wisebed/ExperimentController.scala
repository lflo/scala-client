package de.fau.wisebed

import java.lang.Override
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import scala.collection.JavaConversions.asScalaBuffer
import org.slf4j.LoggerFactory
import de.uniluebeck.itm.tr.util.UrlUtils
import eu.wisebed.api.common.Message
import eu.wisebed.api.controller.Controller
import eu.wisebed.api.controller.RequestStatus
import eu.wisebed.api.controller.Status
import javax.jws.WebParam
import javax.jws.WebService
import javax.xml.ws.Endpoint
import scala.collection.mutable.Buffer
import scala.collection.mutable.SynchronizedBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import de.fau.wisebed.jobs.Job
import com.google.common.collect.Synchronized
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.ListMap
import scala.concurrent.SyncVar
import scala.concurrent.Lock
import scala.actors.Actor

case object ReqJob
case class AddJob[S](id:String, job:Job[S])
case class RemJob[S](job:Job[S])

@WebService(
	serviceName = "ControllerService",
	targetNamespace = "urn:ControllerService",
	portName = "ControllerPort",
	endpointInterface = "eu.wisebed.api.controller.Controller"
)
class ExperimentController extends Controller {
	val log = LoggerFactory.getLogger(this.getClass)

	val url = "http://" + InetAddress.getLocalHost.getCanonicalHostName + ":" + ExperimentController.port + "/controller/" + ExperimentController.id
	val bindAllInterfacesUrl = UrlUtils.convertHostToZeros(url)

	log.debug("Starting ExperimentController...")
	log.debug("Endpoint URL: {}", url)
	log.debug("Binding  URL: {}", bindAllInterfacesUrl)

	val endpoint = Endpoint.publish(bindAllInterfacesUrl, this)
	endpoint.setExecutor(Executors.newCachedThreadPool())

	log.debug("Successfully started ExperimentController at " + bindAllInterfacesUrl)
	
	val messageHandlers = new ArrayBuffer[messages.MessageInput] with SynchronizedBuffer[messages.MessageInput] 
	var notificationCallbacks = List[String => Unit]()
	var endCallbacks = List[() => Unit]()
	
	val sDisp = new Actor {
		private var rjob = 0
		private var rsBuf = List[RequestStatus]()
		private val jobs =  new ListMap[String, Job[_]]
		
		private def sendJob(rs:RequestStatus){
			jobs.get(rs.getRequestId) match {
				case x:Some[Job[_]] => rs.getStatus.foreach(s => {
						// send to Job
						x.get ! s
						log.debug("Dispatching {}", rs.getRequestId)
					})
				case _ => { 
					if(rjob > 0){
						rsBuf ::= rs
					} else	{
						log.error("Got Job id {} without Job", rs.getRequestId)
					}
				}	
			}  				
		}

		def act () {
			log.debug("Actor Started")
			/** @todo terminate? */
			loop {
				react {
					case s:RequestStatus => sendJob(s)
					case ReqJob => rjob+=1
					case AddJob(s,j) =>
						log.debug("Adding job {}", s)
						jobs += s->j
						j.start
						rjob -= 1
						val buf = rsBuf
						rsBuf = List[RequestStatus]()
						buf.foreach(sendJob(_))
					case RemJob(j) =>
  						jobs.find(_._2 == j) match {
  							case Some(kv) =>
  								log.debug("Removing job {}.", kv._1)
  								jobs.remove(kv._1)
  							case None => log.error("RemJob: Job {} not found.", j)
  						}
					case x => log.error("Got unknown class: {}", x.getClass)
				}
			}
		}
	}

	sDisp.start
	
	@Override
	def receive(@WebParam(name = "msg", targetNamespace = "") msg:java.util.List[Message]) {
		for(cb <- messageHandlers; m <- msg) cb ! m
	}

	def onMessage(mi:messages.MessageInput) {
		messageHandlers +=  mi
		mi.start
	}

	@Override
	def receiveStatus(@WebParam(name = "status", targetNamespace = "") status:java.util.List[RequestStatus]) {
		//Send to dispetcher	
		status.foreach( rs => {
			log.debug("Got Message for {} - sending to Dispatcher", rs.getRequestId)
			sDisp ! rs	
		})
	}

	def addJob[S](j:Job[S], rid: => String) {
		//Send JRequest
		sDisp ! ReqJob
		//Get Job
		val id:String = rid		
		sDisp ! AddJob(id, j)
	}
	
	
	@Override
	def receiveNotification(@WebParam(name = "msg", targetNamespace = "") msg:java.util.List[String]) {
		for(cb <- notificationCallbacks; s <- msg) cb(s)
	}

	def onNotification(callback: String => Unit) {
		notificationCallbacks.synchronized{
			notificationCallbacks ::= callback
		}
	}

	@Override
	def experimentEnded() {
		for(cb <- endCallbacks) cb()
	}

	def onEnd(callback: => Unit) {
		endCallbacks ::= ( () => callback )
	}
}

object ExperimentController {
	private var intid = 0

	private def id:String = {(intid +=1); intid.toString}
	
	private lazy val port:Int = {
		val socket = new ServerSocket(0)
		val port = socket.getLocalPort()
		socket.close()
		port
	}
}
