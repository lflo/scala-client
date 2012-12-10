package de.fau.wisebed

import jobs._
import org.slf4j.LoggerFactory
import eu.wisebed.wiseml.WiseMLHelper
import eu.wisebed.api._
import eu.wisebed.api.snaa.AuthenticationTriple
import eu.wisebed.api.controller.Controller
import java.util.GregorianCalendar
import java.util.Calendar
import java.net.InetAddress
import javax.xml.datatype.DatatypeFactory
import scala.collection.JavaConversions._
import WisebedApiConversions._

class Testbed(val smEndpointURL:String, val snaaEndpointURL:String, val rsEndpointURL:String) {
	val log = LoggerFactory.getLogger(this.getClass)

	var credentials = List[snaa.AuthenticationTriple]()
	var secretAuthenticationKeys = Set[snaa.SecretAuthenticationKey]()
	var reservations = List[Reservation]()
	
	lazy val sessionManagement = WisebedServiceHelper.getSessionManagementService(smEndpointURL)
	lazy val authenticationSystem = WisebedServiceHelper.getSNAAService(snaaEndpointURL)
	lazy val reservationSystem = WisebedServiceHelper.getRSService(rsEndpointURL)

	lazy val wiseML = sessionManagement.getNetwork
	var currentWSNService:wsn.WSN = null
	
	lazy val controller = new ExperimentController
	
	// Public funcions
	def getnodes(moteType:List[String] = List("telosb")):List[String] = {
		WiseMLHelper.getNodeUrns(wiseML, moteType).toList
	}
	
	def addCredencials(auth:AuthenticationTriple) {
		credentials ::= auth
		updateAuthkeys()
	}
	
	def addCredencials(prefix:String, user:String, password:String) {
		val credentials = new AuthenticationTriple()
		credentials.setUrnPrefix("urn:fau:")
		credentials.setUsername("morty")
		credentials.setPassword("WSN")
		addCredencials(credentials)
	}
	
	def updateAuthkeys() = {
		val authklist = authenticationSystem.authenticate(credentials)
		secretAuthenticationKeys ++= authklist
	}
	
	private def getReservations(reservation: rs.GetReservations):List[Reservation] = {
		reservationSystem.getConfidentialReservations(secretAuthenticationKeys.toSeq, reservation).toList.map { res =>
			val tmp = new Reservation(res.getFrom.toGregorianCalendar, res.getTo.toGregorianCalendar, res.getNodeURNs.toList, res.getUserData)
			tmp.addKeys(asScalaBuffer(res.getData))
			tmp
		}
	}
	
	def getReservations(from:GregorianCalendar, to:GregorianCalendar):List[Reservation] = {
		val res = new rs.GetReservations
		res.setFrom(from)
		res.setTo(to)
		getReservations(res)
	}
	
	def getReservations(min:Int = 30):List[Reservation] = {
		val from = new GregorianCalendar
		val to = new GregorianCalendar
		to.add(Calendar.MINUTE, min)
		getReservations(from, to)
	}
	
	def makeReservation(res:Reservation):Reservation = {
		res addKeys reservationSystem.makeReservation(secretAuthenticationKeys.toSeq, res.asConfidentialReservationData)
		res
	}
	
	def makeReservation(from:GregorianCalendar, to: GregorianCalendar, nodeUrns:List[String], user:String = credentials.head.getUsername):Reservation = {
		val res = new Reservation(from, to, nodeUrns, user)
		makeReservation(res)
	}
	
	
	def freeReservation(res:Reservation) {
		import Reservation._
		sessionManagement.free(res.sm_reservationkeys)
	}
	
	def areNodesAlive(nodes:List[String]):NodesAliveJob = {
		val job = new NodesAliveJob(nodes)		
		val url = controller.url
		sessionManagement.areNodesAlive(nodes, url)
		job		
	}
}
