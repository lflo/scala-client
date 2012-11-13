
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

@WebService(
		serviceName = "ControllerService",
		targetNamespace = "urn:ControllerService",
		portName = "ControllerPort",
		endpointInterface = "eu.wisebed.api.controller.Controller"
)
class MyController(controller:Controller) extends Controller {

  	val log = Logger.getLogger(this.getClass)

	

    @throws(classOf[MalformedURLException])
	def publish(endpointUrl:String):Unit   = {
		val bindAllInterfacesUrl = UrlUtils.convertHostToZeros(endpointUrl);

		log.debug("Starting DelegatingController...");
		log.debug("Endpoint URL: " + endpointUrl);
		log.debug("Binding  URL: " + bindAllInterfacesUrl);

		val endpoint = Endpoint.publish(bindAllInterfacesUrl, this);
		endpoint.setExecutor(Executors.newCachedThreadPool());

		log.debug("Successfully started DelegatingController at " + bindAllInterfacesUrl);
	}

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