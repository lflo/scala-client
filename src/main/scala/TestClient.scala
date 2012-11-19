

import java.net.InetAddress
import java.net.MalformedURLException
import java.util.Calendar
import java.util.GregorianCalendar

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.Buffer

import org.apache.log4j.Level
import org.apache.log4j.PatternLayout
import org.slf4j.LoggerFactory

import de.fau.wiseml.wrappers.RichProgram
import de.fau.wiseml.wrappers.RichWiseMLHelper
import de.fau.wisebed.DelegationController
import de.uniluebeck.itm.tr.util.Logging
import de.uniluebeck.itm.tr.util.StringUtils
import eu.wisebed.api.WisebedServiceHelper
import eu.wisebed.api.common
import eu.wisebed.api.controller.Controller
import eu.wisebed.api.rs.ConfidentialReservationData
import eu.wisebed.api.rs.GetReservations
import eu.wisebed.api.rs.SecretReservationKey
import eu.wisebed.api.sm.UnknownReservationIdException_Exception
import eu.wisebed.api.snaa.AuthenticationTriple
import javax.jws.WebService
import javax.xml.datatype.DatatypeFactory

object MyExperiment {
	//Config
	val smEndpointURL = "http://i4dr.informatik.uni-erlangen.de:10011/sessions"
	val snaaEndpointURL = "http://i4dr.informatik.uni-erlangen.de:20011/snaa"
	val rsEndpointURL = "http://i4dr.informatik.uni-erlangen.de:30011/rs"
	val localControllerEndpointURL = "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":8089/controller";
	val moteType = List("telosb")

	val moteList = Buffer("")
	val moteFindex = Buffer(0,0,0)

	implicit def greg2XMLGreg(greg: GregorianCalendar) = {
		DatatypeFactory.newInstance().newXMLGregorianCalendar(greg);
	}

	implicit def data2Key(dat: java.util.List[eu.wisebed.api.rs.Data]): Buffer[SecretReservationKey] = {

		val map = asScalaBuffer(dat).map(x => {
			val rv = new SecretReservationKey
			rv.setSecretReservationKey(x.getSecretReservationKey)
			rv.setUrnPrefix(x.getUrnPrefix)
			rv

		})
		map.toBuffer
	}

	implicit def SecretReservationKeyRs2SM(rs: eu.wisebed.api.rs.SecretReservationKey): eu.wisebed.api.sm.SecretReservationKey = {
		val rv = new eu.wisebed.api.sm.SecretReservationKey;
		rv.setSecretReservationKey(rs.getSecretReservationKey)
		rv.setUrnPrefix(rs.getUrnPrefix)
		rv
	}

	def main(args: Array[String]) {
		Logging.setLoggingDefaults(Level.ALL, new PatternLayout("%-11d{HH:mm:ss,SSS} %-5p - %m%n"))

		val log = LoggerFactory.getLogger("MyExperiment");

		//Get Motes

		log.debug("Calling SessionManagement.getNetwork() on {}", smEndpointURL)
		val sessionManagement = WisebedServiceHelper.getSessionManagementService(smEndpointURL)
		val wiseML = sessionManagement.getNetwork();
		val nodeUrns: List[String] = RichWiseMLHelper.getNodeUrns(wiseML, moteType)
		println("Found nodes:")
		println(nodeUrns.mkString("\n"))

		//Reservcer
		val credentials = new AuthenticationTriple();
		credentials.setUrnPrefix("urn:fau:");
		credentials.setUsername("morty");
		credentials.setPassword("WSN");
		val credentialsList = List[AuthenticationTriple](credentials)

		// do the authentication
		log.debug("Authenticating...");
		val authenticationSystem = WisebedServiceHelper.getSNAAService(snaaEndpointURL);
		val secretAuthenticationKeys = authenticationSystem.authenticate(credentialsList);
		log.debug("Successfully authenticated!");

		// create reservation request data to wb-reserve all iSense nodes for 10 minutes
		val reservation = new GetReservations
		val reservationData = new ConfidentialReservationData();
		reservationData.getNodeURNs().addAll(nodeUrns);
		val gc = new GregorianCalendar
		reservation.setFrom(gc)
		reservationData.setFrom(gc);
		gc.add(Calendar.MINUTE, 120)
		reservation.setTo(gc)
		reservationData.setTo(gc);
		reservationData.setUserData(credentials.getUsername)

		// do the reservation
		val reservationSystem = WisebedServiceHelper.getRSService(rsEndpointURL);
		log.debug("Trying to reserve the following nodes");
		try {

			val secretAuthKeys = Buffer[eu.wisebed.api.rs.SecretAuthenticationKey]();

			for (snaaKey <- secretAuthenticationKeys) {

				val key = new eu.wisebed.api.rs.SecretAuthenticationKey

				key.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey());
				key.setUrnPrefix(snaaKey.getUrnPrefix());
				key.setUsername(snaaKey.getUsername());

				secretAuthKeys += key
			}

			val ress = reservationSystem.getConfidentialReservations(secretAuthKeys, reservation)

			var secretReservationKeys: Buffer[SecretReservationKey] = null

			log.debug("L1: " + ress.length)

			if (ress.length > 0) {

				secretReservationKeys = ress.foldLeft(Buffer[SecretReservationKey]())(_ ++ _.getData)

			} else {
				/*
		    	for(res <- ress){
		    	  val srk = ress.foldLeft(Buffer[SecretReservationKey]())(_ ++ _.getData)
		    	  reservationSystem.deleteReservation(secretAuthKeys,srk)
		    	}*/

				secretReservationKeys = reservationSystem.makeReservation(
					secretAuthKeys,
					reservationData);

			}

			log.info("Reservation: " + secretReservationKeys.map(_.getSecretReservationKey).mkString("\n"));

			// Falsh nodes

			//val  wsn = WSNAsyncWrapper.of(wsnService);

			val controller = new Controller {
				def receive(msg: java.util.List[common.Message]): Unit = {
					// nothing to do
				}
				def receiveStatus(requestStatuses: java.util.List[eu.wisebed.api.controller.RequestStatus]): Unit = {
					for(rs <- requestStatuses){
						log.info("RS:" + rs.getRequestId())
						for(stat <- rs.getStatus){
							log.info("  Node:   " + stat.getNodeId )
							log.info("  Status: " + stat.getMsg + "(" + stat.getValue() + ")" )
							
							
						}
					}
				}
				def receiveNotification(msgs: java.util.List[String]): Unit = {
					for (msg <- msgs) {
						log.info("MSG: " + msg);
					}
				}
				def experimentEnded(): Unit = {
					log.info("Experiment ended");
					System.exit(0);
				}
			}
			// try to connect via unofficial protocol buffers API if hostname and port are set in the configuration

			val delegator = new DelegationController(controller, localControllerEndpointURL);
			
			log.debug("Local controller published on url: {}", localControllerEndpointURL);

			
			
			log.debug("Using the following parameters for calling getInstance(): \"{}\", \"{}\"",
				StringUtils.jaxbMarshal(seqAsJavaList(secretReservationKeys)),
				localControllerEndpointURL);
			var wsnEndpointURL: String = null;
			try {
				wsnEndpointURL = sessionManagement.getInstance(
					seqAsJavaList(secretReservationKeys.map(SecretReservationKeyRs2SM(_))),
					localControllerEndpointURL);
			} catch {
				case e: UnknownReservationIdException_Exception => {
					log.warn("There was no reservation found with the given secret reservation key. Exiting.");
					System.exit(1);
				}
				case e =>{
					log.warn("Other exception.");
					e.printStackTrace()
					System.exit(1);				
				}
					
			}
			log.debug("Got a WSN instance URL, endpoint is: \"{}\"", wsnEndpointURL);
			val wsnService = WisebedServiceHelper.getWSNService(wsnEndpointURL);

			
			val prog = RichProgram(args(0));
			
			val rid = wsnService.flashPrograms(nodeUrns, moteFindex.map(new java.lang.Integer(_)), List(prog))
			log.error("END")
		}

	}

}
