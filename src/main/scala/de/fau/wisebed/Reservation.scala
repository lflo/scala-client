package de.fau.wisebed

import java.util.GregorianCalendar
import scala.collection.mutable.Buffer
import eu.wisebed.api._
import javax.xml.datatype.DatatypeFactory
import scala.collection.JavaConversions._
import CalConv.greg2XMLGreg
import javax.xml.datatype.XMLGregorianCalendar

class Reservation(_from:GregorianCalendar, _to:GregorianCalendar,_nodeURNs:List[String], user:String) {

    val from:GregorianCalendar = _from.clone.asInstanceOf[GregorianCalendar]
    val to = _to.clone.asInstanceOf[GregorianCalendar]
    val nodeURNs = _nodeURNs.toList
	
    
	var userData:String = ""
    val secretReservationKeys = Buffer[rs.SecretReservationKey]()
    
   
    
    
    def addKeys(keys:Iterable[rs.SecretReservationKey]):Unit = {
    	secretReservationKeys ++= keys
    }
	
 	def asConfidentialReservationData:rs.ConfidentialReservationData = { 
    	val rv = new rs.ConfidentialReservationData
    	rv.setFrom(greg2XMLGreg(from))
    	rv.setTo(to)
    	rv.setUserData(user)
    	rv.getNodeURNs().addAll(nodeURNs);
    	rv
 	}
    
 	
 	def copy():Reservation = {
 		new Reservation(from, to, nodeURNs, user)
 	}
    
}


object Reservation{
	implicit def reservation2CRD(res:Reservation):rs.ConfidentialReservationData = res.asConfidentialReservationData
	
	implicit def secretReservationKey_Rs2SM(rsKs: Traversable[rs.SecretReservationKey]): Buffer[sm.SecretReservationKey] = {
		val rv = Buffer[sm.SecretReservationKey]()
		for(rsK <- rsKs){
			val rk = new sm.SecretReservationKey;
			rk.setSecretReservationKey(rsK.getSecretReservationKey)
			rk.setUrnPrefix(rsK.getUrnPrefix)
			rv += rk
		}
		rv
	}
	
}


object CalConv{
	implicit def greg2XMLGreg(greg: GregorianCalendar):XMLGregorianCalendar = {
		DatatypeFactory.newInstance().newXMLGregorianCalendar(greg);
	}
	

}