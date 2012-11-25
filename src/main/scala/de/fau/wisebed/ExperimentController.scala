package de.fau.wisebed


import eu.wisebed.api.controller.Controller
import org.apache.log4j.Logger
import java.net.MalformedURLException
import de.uniluebeck.itm.tr.util.UrlUtils
import javax.xml.ws.Endpoint
import java.util.concurrent.Executors
import javax.jws.WebParam
import eu.wisebed.api.common.Message
import eu.wisebed.api.controller._
import javax.jws.WebService
import javax.xml.bind.annotation.XmlSeeAlso
import eu.wisebed.api.controller.ObjectFactory
import java.net.InetAddress
import java.net.ServerSocket
import scala.collection.JavaConversions._

@WebService(
		serviceName = "ControllerService",
		targetNamespace = "urn:ControllerService",
		portName = "ControllerPort",
		endpointInterface = "eu.wisebed.api.controller.Controller"
)
class ExperimentController extends Controller {
  	val log = Logger.getLogger(this.getClass)

  	val url = "http://" + InetAddress.getLocalHost.getCanonicalHostName + ":" + ExperimentController.port + "/controller/" + ExperimentController.id
	val bindAllInterfacesUrl = UrlUtils.convertHostToZeros(url)

	log.debug("Starting ExperimentController...")
	log.debug("Endpoint URL: " + url)
	log.debug("Binding  URL: " + bindAllInterfacesUrl)

	val endpoint = Endpoint.publish(bindAllInterfacesUrl, this)
	endpoint.setExecutor(Executors.newCachedThreadPool())

	log.debug("Successfully started ExperimentController at " + bindAllInterfacesUrl)
	
	var messageCallbacks = List[Message => Unit]()
	var statusCallbacks = List[RequestStatus => Unit]()
	var requestStatusCallbacks = Map[String, Status => Unit]()
	var notificationCallbacks = List[String => Unit]()
	var endCallbacks = List[() => Unit]()

	@Override
	def receive(@WebParam(name = "msg", targetNamespace = "") msg:java.util.List[Message]) {
		for(cb <- messageCallbacks; m <- msg) cb(m)
	}

	def onMessage(callback: Message => Unit) {
		messageCallbacks ::= callback
	}

	@Override
	def receiveStatus(@WebParam(name = "status", targetNamespace = "") status:java.util.List[RequestStatus]) {
		for(cb <- statusCallbacks; s <- status) cb(s)
		for(s <- status; cb <- requestStatusCallbacks.get(s.getRequestId); rs <- s.getStatus) cb(rs)
	}

	def onStatus(callback: RequestStatus => Unit) {
		statusCallbacks ::= callback
	}

	def onStatus(requestID: String)(callback: Status => Unit) {
		requestStatusCallbacks += requestID -> callback
	}

	@Override
	def receiveNotification(@WebParam(name = "msg", targetNamespace = "") msg:java.util.List[String]) {
		for(cb <- notificationCallbacks; s <- msg) cb(s)
	}

	def onNotification(callback: String => Unit) {
		notificationCallbacks ::= callback
	}

	@Override
	def experimentEnded() {
		for(cb <- endCallbacks) cb()
	}

	def onEnd(callback: => Unit) {
		endCallbacks ::= ( () => callback )
	}
}


object ExperimentController{
	private var intid = 0

	private def id:String = {(intid +=1); intid.toString}
	
	private lazy val port:Int = {
		val socket = new ServerSocket(0)
		val port = socket.getLocalPort()
		socket.close()
		port
	}
}
