//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.11.23 at 12:30:35 PM CET 
//


package playground.gregor.sim2dio.jaxb.sim2denvironment02;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * 
 * 			Openings define line segments of the describing polygon that are open like doors etc. The value of 'vs' attribute
 * 			is the separator.
 * 			
 * 
 * <p>Java class for openingsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="openingsType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="vs" type="{http://www.w3.org/2001/XMLSchema}string" default=" " />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "openingsType", propOrder = {
    "value"
})
public class XMLOpeningsType {

    @XmlValue
    protected String value;
    @XmlAttribute
    protected String vs;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the vs property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVs() {
        if (vs == null) {
            return " ";
        } else {
            return vs;
        }
    }

    /**
     * Sets the value of the vs property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVs(String value) {
        this.vs = value;
    }

}
