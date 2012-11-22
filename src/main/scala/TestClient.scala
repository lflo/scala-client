

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
import de.fau.wisebed.Testbed
import java.text.SimpleDateFormat
import de.fau.wisebed.Experiment
import de.fau.wisebed.jobs.MoteAliveState._


object MyExperiment {
	val ffile = "sky-shell.ihex"

	def main(args: Array[String]) {
		Logging.setLoggingDefaults(Level.ALL) // new PatternLayout("%-11d{HH:mm:ss,SSS} %-5p - %m%n"))

		val log = LoggerFactory.getLogger("MyExperiment");

		//Get Motes

		log.debug("Starting Testbed")
		val tb = new Testbed()
		log.debug("Requesting Motes")
		val motes = tb.getnodes()
		log.debug("Motes: " + motes.mkString(", "))
		
		
		
		/* FIXME: This does not work!
		 * log.debug("Requesting Motesate")
		val statusj = tb.areNodesAlive(motes)
		val status = statusj.status
		status.foreach(m => log.info(m._1 + ": " +  m._2.toString))
		* 
		*/
		
	
		log.debug("Logging in")
		tb.addCrencials("urn:fau:", "morty", "WSN")
		
		
		log.debug("Requesting reservations")
		val res = tb.getReservations(240)
		
		if(res.size != 0){
			for(r <- res){
				log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.getNodeURNs().mkString(", ")) 
			}
		}
		
		if(res.size == 0 || res.forall(_.inThePast)){
			log.debug("No Reservations or in the Past- Requesting")
			val from = new GregorianCalendar
			val to = new GregorianCalendar
			to.add(Calendar.MINUTE, 120)
			val r = tb.makeReservation(from, to, motes, "morty")
			log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.getNodeURNs().mkString(", ")) 
			res += r;
		}
		
		val exp = new Experiment(res.toList, tb)
		
		log.debug("Requesting Motestate")
		val statusj = exp.areNodesAlive(motes)
		val status = statusj.status
		status.foreach(m => log.info(m._1 + ": " +  m._2.toString))
		
		
		val activemotes = status.filter(_._2 == Alive).map(_._1).toList
		
		if(false){
			log.debug("Flashing")
			val flashj = exp.flash(ffile, activemotes)
			flashj()
		}
		
		log.debug("Resetting")
		val resj = exp.resetNodes(activemotes)
		resj()
		
		log.debug("Waiting for bootup")
		Thread.sleep(10 * 1000)
		
		log.debug("Sending \\n")
		val snd = exp.send(activemotes, "help\n")
		snd()
		log.debug("Waiting for answer")
		Thread.sleep(20 * 1000)
		
		log.debug("Removing Reservation")
		res.foreach(tb.freeReservation(_))
		
		
		log.debug("DONE")
		//sys.exit(0)
		

	}

}
