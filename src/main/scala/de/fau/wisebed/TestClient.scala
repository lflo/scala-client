package de.fau.wisebed

import java.util.Calendar
import java.util.GregorianCalendar

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.log4j.Level
import org.slf4j.LoggerFactory

import de.fau.wisebed.Reservation.reservation2CRD
import de.fau.wisebed.jobs.MoteAliveState._
import de.fau.wisebed.wrappers.WrappedChannelHandlerConfiguration
import de.fau.wisebed.wrappers.WrappedChannelHandlerConfiguration._
import de.uniluebeck.itm.tr.util.Logging

object MyExperiment {
	val log = LoggerFactory.getLogger("MyExperiment");
	
	val ffile = "sky-shell.ihex"
	val smEndpointURL = "http://i4dr.informatik.uni-erlangen.de:10011/sessions"
	val snaaEndpointURL = "http://i4dr.informatik.uni-erlangen.de:20011/snaa"
	val rsEndpointURL = "http://i4dr.informatik.uni-erlangen.de:30011/rs"
	
	val flash = false
		
	def main(args: Array[String]) {
		Logging.setLoggingDefaults(Level.ALL) // new PatternLayout("%-11d{HH:mm:ss,SSS} %-5p - %m%n"))

		//Get Motes
		log.debug("Starting Testbed")
		val tb = new Testbed(smEndpointURL, snaaEndpointURL, rsEndpointURL)
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
		tb.addCredencials("urn:fau:", "morty", "WSN")
		
		
		log.debug("Requesting reservations")
		var res = tb.getReservations(240)
		
		for(r <- res) {
			log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.getNodeURNs.mkString(", ")) 
		}
		
		if(!res.exists(_.now)) {
			log.debug("No Reservations or in the Past- Requesting")
			val from = new GregorianCalendar
			val to = new GregorianCalendar
			to.add(Calendar.MINUTE, 120)
			val r = tb.makeReservation(from, to, motes, "morty")
			log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.getNodeURNs.mkString(", ")) 
			res ::= r
		}
		
		val exp = new Experiment(res.toList, tb)
		
		log.debug("Requesting Motestate")
		val statusj = exp.areNodesAlive(motes)
		val status = statusj.status
		for((m, s) <- status) log.info(m +": " + s)
		
		val activemotes = (for((m, s) <- status; if(s == Alive)) yield m).toList
		
		log.debug("Requesting Supported Channel Handlers")
		val handls = exp.supportedChannelHandlers
		for(h <- handls){
			println(h.format)
		}
		
		val setHand = "contiki"
		
		if(handls.find(_.name == setHand) == None){
			log.error("Can not set handler: {}", setHand)
		} else {
			log.debug("Setting Handler: {}", setHand)
			val chd = exp.setChannelHandler(activemotes, new WrappedChannelHandlerConfiguration("contiki") )
			if(!chd()){
				log.error("Failed setting Handler")
			}
		}
		
		
		if(flash){
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
