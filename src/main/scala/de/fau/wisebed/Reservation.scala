package de.fau.wisebed

import java.util.GregorianCalendar
import scala.collection.mutable.Buffer
import eu.wisebed.api._
import javax.xml.datatype.DatatypeFactory
import scala.collection.JavaConversions._
import WisebedApiConversions._
import javax.xml.datatype.XMLGregorianCalendar
import java.text.SimpleDateFormat
import org.slf4j.LoggerFactory

class Reservation(_from:GregorianCalendar, _to:GregorianCalendar,_nodeURNs:List[String], user:String) {
	val log = LoggerFactory.getLogger(this.getClass)

	val lfrom:GregorianCalendar = _from.clone.asInstanceOf[GregorianCalendar]
	val lto = _to.clone.asInstanceOf[GregorianCalendar]
    
	def from:GregorianCalendar = lfrom.clone.asInstanceOf[GregorianCalendar]
	def to:GregorianCalendar = lto.clone.asInstanceOf[GregorianCalendar]
    
	val nodeURNs = _nodeURNs.toList
	
	var userData:String = ""
	val secretReservationKeys = Buffer[rs.SecretReservationKey]()
    
	def inThePast = to.before(new GregorianCalendar)    
	def now = {
		val t = new GregorianCalendar
		from.before(t) && to.after(t)
	}

	def addKeys(keys:Iterable[rs.SecretReservationKey]):Unit = {
		secretReservationKeys ++= keys
	}
	
	def asConfidentialReservationData:rs.ConfidentialReservationData = { 
		val rv = new rs.ConfidentialReservationData
		rv.setFrom(lfrom)
		rv.setTo(lto)
		rv.setUserData(user)
		rv.getNodeURNs.addAll(nodeURNs)
		rv
	}

	def copy():Reservation = {
 		val rv = new Reservation(from, to, nodeURNs, user)
 		rv.addKeys(secretReservationKeys)
 		rv
 	}
 	
 	def dateString(format:String = "HH:mm:ss", split:String = " - "):String = {
		val f = new SimpleDateFormat(format)
		f.format(from.getTime) + split + f.format(to.getTime)
 	}
 	
 	def sm_reservationkeys:List[sm.SecretReservationKey] = {
 		import Reservation._
 		secretReservationKey_Rs2SM(secretReservationKeys).toList
 	}
}

object Reservation {
	implicit def reservation2CRD(res:Reservation):rs.ConfidentialReservationData = res.asConfidentialReservationData
	
	implicit def secretReservationKey_Rs2SM(rsKs: Traversable[rs.SecretReservationKey]): Buffer[sm.SecretReservationKey] = {
		val rv = Buffer[sm.SecretReservationKey]()
		for(rsK <- rsKs) {
			val rk = new sm.SecretReservationKey
			rk.setSecretReservationKey(rsK.getSecretReservationKey)
			rk.setUrnPrefix(rsK.getUrnPrefix)
			rv += rk
		}
		rv
	}
}
