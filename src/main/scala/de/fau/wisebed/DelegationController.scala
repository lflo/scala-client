package de.fau.wisebed


import eu.wisebed.api.controller.Controller
import org.apache.log4j.Logger
import java.net.MalformedURLException
import de.uniluebeck.itm.tr.util.UrlUtils
import javax.xml.ws.Endpoint
import java.util.concurrent.Executors
import javax.jws.WebParam
import eu.wisebed.api.common.Message
import eu.wisebed.api.controller.RequestStatus
import javax.jws.WebService
import javax.xml.bind.annotation.XmlSeeAlso
import eu.wisebed.api.controller.ObjectFactory
import java.net.InetAddress

@WebService(
		serviceName = "ControllerService",
		targetNamespace = "urn:ControllerService",
		portName = "ControllerPort",
		endpointInterface = "eu.wisebed.api.controller.Controller"
)
class DelegationController(controller:Controller) extends Controller {

  	val log = Logger.getLogger(this.getClass)

  	val url = "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":" + DelegationController.port.toString+ "/controller/" + DelegationController.id
	val bindAllInterfacesUrl = UrlUtils.convertHostToZeros(url);

	log.debug("Starting DelegatingController...");
	log.debug("Endpoint URL: " + url);
	log.debug("Binding  URL: " + bindAllInterfacesUrl);

	val endpoint = Endpoint.publish(bindAllInterfacesUrl, this);
	endpoint.setExecutor(Executors.newCachedThreadPool());

	log.debug("Successfully started DelegatingController at " + bindAllInterfacesUrl);

	
	def endpointUrl = url
	

	@Override
	def receive(@WebParam(name = "msg", targetNamespace = "")  msg:java.util.List[Message] ) {
		controller.receive(msg);
	}

	@Override
	def receiveStatus(@WebParam(name = "status", targetNamespace = "")  status:java.util.List[RequestStatus] ) {
		controller.receiveStatus(status);
	}

	@Override
	def  receiveNotification(@WebParam(name = "msg", targetNamespace = "")msg:java.util.List[String]) {
		controller.receiveNotification(msg);
	}

	@Override
	def experimentEnded() {
		controller.experimentEnded();
	}

  
}


object DelegationController{
	private var intid = 0;
	private def id:String = {(intid +=1); intid.toString}
	
	import java.net.ServerSocket
	private lazy val port:Int = {
		val socket= new ServerSocket(0)
		val port = socket.getLocalPort()
		socket.close();
		port
	}
	
	
}
