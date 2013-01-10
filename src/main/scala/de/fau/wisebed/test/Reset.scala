package de.fau.wisebed.test
import org.apache.log4j.Level
import org.slf4j.LoggerFactory
import de.uniluebeck.itm.tr.util.Logging
import eu.wisebed.api.common._
import de.fau.wisebed.jobs.MoteAliveState._
import de.fau.wisebed.wrappers._
import de.fau.wisebed.wrappers.WrappedChannelHandlerConfiguration._
import de.fau.wisebed.wrappers.WrappedMessage._

object Reset {
	val log = LoggerFactory.getLogger(this.getClass)

	
		
	def main(args: Array[String]) {
		
		
		TH.init(30)
		
		
		TH.reset
		
		log.debug("Waiting for answer")
		Thread.sleep(20 * 1000)
		
		TH.finish
		
		
		log.debug("DONE")
		sys.exit(0)
	}
}
