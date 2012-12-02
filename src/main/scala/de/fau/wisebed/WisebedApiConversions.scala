package de.fau.wisebed

import eu.wisebed.api._
import scala.collection.JavaConversions._
import java.util.GregorianCalendar
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.datatype.DatatypeFactory

object WisebedApiConversions {
	implicit def kvp2map(kvp:List[common.KeyValuePair]):Map[String, String] = kvp.map(x => (x.getKey -> x.getValue)).toMap
	
	implicit def map2kvp(m:Map[String,String]):List[common.KeyValuePair] = m.map {
		case (k, v) => {
			val rv = new common.KeyValuePair
			rv.setKey(k)
			rv.setValue(v)
			rv
		}
	}.toList
	
	implicit def secretAuthentificationKey_snaa2rs(snaaKey:snaa.SecretAuthenticationKey):rs.SecretAuthenticationKey = {
		val key = new rs.SecretAuthenticationKey
		key.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey)
		key.setUrnPrefix(snaaKey.getUrnPrefix)
		key.setUsername(snaaKey.getUsername)
		key
	}
	
	implicit def secretAuthentificationKeys_snaa2rs(snaaKeys:Iterable[snaa.SecretAuthenticationKey]):java.util.List[rs.SecretAuthenticationKey] = {
		snaaKeys.map(secretAuthentificationKey_snaa2rs).toSeq
	}

	implicit def data2Key(dat: Iterable[rs.Data]): Iterable[rs.SecretReservationKey] = dat.map(x => {
		val rv = new rs.SecretReservationKey
		rv.setSecretReservationKey(x.getSecretReservationKey)
		rv.setUrnPrefix(x.getUrnPrefix)
		rv
	})
	
	implicit def greg2XMLGreg(greg: GregorianCalendar):XMLGregorianCalendar = {
		DatatypeFactory.newInstance().newXMLGregorianCalendar(greg)
	}	
}
