/**
 * Copyright 2015 Trustees of Indiana University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE TRUSTEES OF INDIANA UNIVERSITY ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE TRUSTEES OF INDIANA UNIVERSITY OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of the Trustees of Indiana University.
 */
/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.2</a>, using an XML
 * Schema.
 * $Id$
 */

package edu.indiana.dlib.xml.zthes;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

/**
 * Class Relation.
 * 
 * @version $Revision$ $Date$
 */
public class Relation implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _weight.
     */
    private java.lang.Object _weight;

    /**
     * Field _relationType.
     */
    private java.lang.String _relationType;

    /**
     * Field _sourceDb.
     */
    private java.lang.String _sourceDb;

    /**
     * Field _termId.
     */
    private java.lang.String _termId;

    /**
     * Field _termName.
     */
    private java.lang.String _termName;

    /**
     * Field _termQualifier.
     */
    private java.lang.String _termQualifier;

    /**
     * Field _termType.
     */
    private java.lang.String _termType;

    /**
     * Field _termLanguage.
     */
    private java.lang.String _termLanguage;


      //----------------/
     //- Constructors -/
    //----------------/

    public Relation() {
        super();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'relationType'.
     * 
     * @return the value of field 'RelationType'.
     */
    public java.lang.String getRelationType(
    ) {
        return this._relationType;
    }

    /**
     * Returns the value of field 'sourceDb'.
     * 
     * @return the value of field 'SourceDb'.
     */
    public java.lang.String getSourceDb(
    ) {
        return this._sourceDb;
    }

    /**
     * Returns the value of field 'termId'.
     * 
     * @return the value of field 'TermId'.
     */
    public java.lang.String getTermId(
    ) {
        return this._termId;
    }

    /**
     * Returns the value of field 'termLanguage'.
     * 
     * @return the value of field 'TermLanguage'.
     */
    public java.lang.String getTermLanguage(
    ) {
        return this._termLanguage;
    }

    /**
     * Returns the value of field 'termName'.
     * 
     * @return the value of field 'TermName'.
     */
    public java.lang.String getTermName(
    ) {
        return this._termName;
    }

    /**
     * Returns the value of field 'termQualifier'.
     * 
     * @return the value of field 'TermQualifier'.
     */
    public java.lang.String getTermQualifier(
    ) {
        return this._termQualifier;
    }

    /**
     * Returns the value of field 'termType'.
     * 
     * @return the value of field 'TermType'.
     */
    public java.lang.String getTermType(
    ) {
        return this._termType;
    }

    /**
     * Returns the value of field 'weight'.
     * 
     * @return the value of field 'Weight'.
     */
    public java.lang.Object getWeight(
    ) {
        return this._weight;
    }

    /**
     * Method isValid.
     * 
     * @return true if this object is valid according to the schema
     */
    public boolean isValid(
    ) {
        try {
            validate();
        } catch (org.exolab.castor.xml.ValidationException vex) {
            return false;
        }
        return true;
    }

    /**
     * 
     * 
     * @param out
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void marshal(
            final java.io.Writer out)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, out);
    }

    /**
     * 
     * 
     * @param handler
     * @throws java.io.IOException if an IOException occurs during
     * marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     */
    public void marshal(
            final org.xml.sax.ContentHandler handler)
    throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, handler);
    }

    /**
     * Sets the value of field 'relationType'.
     * 
     * @param relationType the value of field 'relationType'.
     */
    public void setRelationType(
            final java.lang.String relationType) {
        this._relationType = relationType;
    }

    /**
     * Sets the value of field 'sourceDb'.
     * 
     * @param sourceDb the value of field 'sourceDb'.
     */
    public void setSourceDb(
            final java.lang.String sourceDb) {
        this._sourceDb = sourceDb;
    }

    /**
     * Sets the value of field 'termId'.
     * 
     * @param termId the value of field 'termId'.
     */
    public void setTermId(
            final java.lang.String termId) {
        this._termId = termId;
    }

    /**
     * Sets the value of field 'termLanguage'.
     * 
     * @param termLanguage the value of field 'termLanguage'.
     */
    public void setTermLanguage(
            final java.lang.String termLanguage) {
        this._termLanguage = termLanguage;
    }

    /**
     * Sets the value of field 'termName'.
     * 
     * @param termName the value of field 'termName'.
     */
    public void setTermName(
            final java.lang.String termName) {
        this._termName = termName;
    }

    /**
     * Sets the value of field 'termQualifier'.
     * 
     * @param termQualifier the value of field 'termQualifier'.
     */
    public void setTermQualifier(
            final java.lang.String termQualifier) {
        this._termQualifier = termQualifier;
    }

    /**
     * Sets the value of field 'termType'.
     * 
     * @param termType the value of field 'termType'.
     */
    public void setTermType(
            final java.lang.String termType) {
        this._termType = termType;
    }

    /**
     * Sets the value of field 'weight'.
     * 
     * @param weight the value of field 'weight'.
     */
    public void setWeight(
            final java.lang.Object weight) {
        this._weight = weight;
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled edu.indiana.dlib.xml.zthes.Relation
     */
    public static edu.indiana.dlib.xml.zthes.Relation unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (edu.indiana.dlib.xml.zthes.Relation) Unmarshaller.unmarshal(edu.indiana.dlib.xml.zthes.Relation.class, reader);
    }

    /**
     * 
     * 
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void validate(
    )
    throws org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
        validator.validate(this);
    }

}
