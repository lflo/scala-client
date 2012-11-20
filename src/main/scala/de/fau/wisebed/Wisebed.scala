package de.fau.wisebed


import org.slf4j.LoggerFactory
import eu.wisebed.wiseml.WiseMLHelper
import eu.wisebed.api._
import de.fau.wiseml.wrappers.RichWiseMLHelper
import java.util.GregorianCalendar
import java.util.Calendar
import java.net.InetAddress
import javax.xml.datatype.DatatypeFactory
import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet
import CalConv.greg2XMLGreg
import eu.wisebed.api.snaa.AuthenticationTriple
import eu.wisebed.api.controller.Controller


class Testbed(
	val smEndpointURL:String = "http://i4dr.informatik.uni-erlangen.de:10011/sessions",
	val snaaEndpointURL:String = "http://i4dr.informatik.uni-erlangen.de:20011/snaa",
	val rsEndpointURL:String = "http://i4dr.informatik.uni-erlangen.de:30011/rs",
	var localControllerEndpointURL:String = "") {
	
	val log = LoggerFactory.getLogger(this.getClass);
	val credentialsList = Buffer[snaa.AuthenticationTriple]()
	
	var defaultUser = ""
		
	
	
	//This is returned by Java and sent there - No need for conversion
	val secretAuthenticationKeys = HashSet[snaa.SecretAuthenticationKey]()
	val reservations = Buffer[Reservation]()
	
	


		
	
	val sessionManagement = WisebedServiceHelper.getSessionManagementService(smEndpointURL)
	lazy val authenticationSystem = WisebedServiceHelper.getSNAAService(snaaEndpointURL);
	lazy val reservationSystem = WisebedServiceHelper.getRSService(rsEndpointURL);

			
	
	
	lazy val wiseML = sessionManagement.getNetwork();
	var currentWSNService:wsn.WSN = null
	
	

	
	
	
	//Implicits

	
	implicit def SectretAuthentificationKey_snaa2rs(sk:Traversable[snaa.SecretAuthenticationKey]):Buffer[rs.SecretAuthenticationKey] = {
		val rv = Buffer[rs.SecretAuthenticationKey]()
		for (snaaKey <- sk) {
			val key = new rs.SecretAuthenticationKey
			key.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey());
			key.setUrnPrefix(snaaKey.getUrnPrefix());
			key.setUsername(snaaKey.getUsername());
			rv += key
		}
		rv
	}
	

	
	implicit def data2Key(dat: Iterable[rs.Data]): Iterable[rs.SecretReservationKey] = {

		dat.map(x => {
			val rv = new rs.SecretReservationKey
			rv.setSecretReservationKey(x.getSecretReservationKey)
			rv.setUrnPrefix(x.getUrnPrefix)
			rv
		})
		
	}
	

	
	// Public funcions
	
	def getnodes(moteType:List[String] = List("telosb")):List[String] = {
		RichWiseMLHelper.getNodeUrns(wiseML, moteType)
	}
	
	def addCrencials(auth:AuthenticationTriple){
		credentialsList += auth
		if(defaultUser == "" ) defaultUser = auth.getUsername
		updateAuthkeys
	}
	
	def addCrencials(prefix:String, user:String, password:String){
		val credentials = new AuthenticationTriple();
		credentials.setUrnPrefix("urn:fau:");
		credentials.setUsername("morty");
		credentials.setPassword("WSN");
		addCrencials(credentials)
		
	}
	
	def updateAuthkeys = {
		val authklist = authenticationSystem.authenticate(seqAsJavaList(credentialsList))
		secretAuthenticationKeys ++=  authklist
	}
	
	private def getReservations(reservation: rs.GetReservations):Buffer[Reservation] = {
		val rv = Buffer[Reservation]()
		val ress = reservationSystem.getConfidentialReservations(seqAsJavaList(secretAuthenticationKeys), reservation)
		for(res <- ress){			
			val tmp = new de.fau.wisebed.Reservation(res.getFrom.toGregorianCalendar, res.getTo.toGregorianCalendar, res.getNodeURNs.toList, res.getUserData())
			tmp.addKeys(asScalaBuffer(res.getData))
			rv += tmp
		}	
		rv
	}
	
	private def getReservations(from:GregorianCalendar, to:GregorianCalendar):Buffer[Reservation] = {
		val res = new rs.GetReservations
		res.setFrom(from)
		res.setTo(to)
		getReservations(res)
	}
	
	
		
	def getReservations(min:Int = 30):Buffer[Reservation] = {
		val from = new GregorianCalendar
		val to = new GregorianCalendar
		to.add(Calendar.MINUTE, min)
		getReservations(from, to)
	}
	
	
	def makeReservation(res:Reservation):Reservation = {
		res addKeys reservationSystem.makeReservation(seqAsJavaList(secretAuthenticationKeys), res.asConfidentialReservationData)
		res
	}
	
	
	def makeReservation(from:GregorianCalendar, to: GregorianCalendar, nodeUrns:List[String], user:String = defaultUser):Reservation = {
		val res = new Reservation(from, to, nodeUrns, user)
		makeReservation(res)
	}
	
	

}