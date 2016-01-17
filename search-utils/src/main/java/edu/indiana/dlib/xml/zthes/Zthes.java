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
 * Class Zthes.
 * 
 * @version $Revision$ $Date$
 */
public class Zthes implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _thes.
     */
    private edu.indiana.dlib.xml.zthes.Thes _thes;

    /**
     * Field _termList.
     */
    private java.util.Vector _termList;


      //----------------/
     //- Constructors -/
    //----------------/

    public Zthes() {
        super();
        this._termList = new java.util.Vector();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * 
     * 
     * @param vTerm
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addTerm(
            final edu.indiana.dlib.xml.zthes.Term vTerm)
    throws java.lang.IndexOutOfBoundsException {
        this._termList.addElement(vTerm);
    }

    /**
     * 
     * 
     * @param index
     * @param vTerm
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addTerm(
            final int index,
            final edu.indiana.dlib.xml.zthes.Term vTerm)
    throws java.lang.IndexOutOfBoundsException {
        this._termList.add(index, vTerm);
    }

    /**
     * Method enumerateTerm.
     * 
     * @return an Enumeration over all
     * edu.indiana.dlib.xml.zthes.Term elements
     */
    public java.util.Enumeration enumerateTerm(
    ) {
        return this._termList.elements();
    }

    /**
     * Method getTerm.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the edu.indiana.dlib.xml.zthes.Term at
     * the given index
     */
    public edu.indiana.dlib.xml.zthes.Term getTerm(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._termList.size()) {
            throw new IndexOutOfBoundsException("getTerm: Index value '" + index + "' not in range [0.." + (this._termList.size() - 1) + "]");
        }
        
        return (edu.indiana.dlib.xml.zthes.Term) _termList.get(index);
    }

    /**
     * Method getTerm.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public edu.indiana.dlib.xml.zthes.Term[] getTerm(
    ) {
        edu.indiana.dlib.xml.zthes.Term[] array = new edu.indiana.dlib.xml.zthes.Term[0];
        return (edu.indiana.dlib.xml.zthes.Term[]) this._termList.toArray(array);
    }

    /**
     * Method getTermCount.
     * 
     * @return the size of this collection
     */
    public int getTermCount(
    ) {
        return this._termList.size();
    }

    /**
     * Returns the value of field 'thes'.
     * 
     * @return the value of field 'Thes'.
     */
    public edu.indiana.dlib.xml.zthes.Thes getThes(
    ) {
        return this._thes;
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
     */
    public void removeAllTerm(
    ) {
        this._termList.clear();
    }

    /**
     * Method removeTerm.
     * 
     * @param vTerm
     * @return true if the object was removed from the collection.
     */
    public boolean removeTerm(
            final edu.indiana.dlib.xml.zthes.Term vTerm) {
        boolean removed = _termList.remove(vTerm);
        return removed;
    }

    /**
     * Method removeTermAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public edu.indiana.dlib.xml.zthes.Term removeTermAt(
            final int index) {
        java.lang.Object obj = this._termList.remove(index);
        return (edu.indiana.dlib.xml.zthes.Term) obj;
    }

    /**
     * 
     * 
     * @param index
     * @param vTerm
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setTerm(
            final int index,
            final edu.indiana.dlib.xml.zthes.Term vTerm)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._termList.size()) {
            throw new IndexOutOfBoundsException("setTerm: Index value '" + index + "' not in range [0.." + (this._termList.size() - 1) + "]");
        }
        
        this._termList.set(index, vTerm);
    }

    /**
     * 
     * 
     * @param vTermArray
     */
    public void setTerm(
            final edu.indiana.dlib.xml.zthes.Term[] vTermArray) {
        //-- copy array
        _termList.clear();
        
        for (int i = 0; i < vTermArray.length; i++) {
                this._termList.add(vTermArray[i]);
        }
    }

    /**
     * Sets the value of field 'thes'.
     * 
     * @param thes the value of field 'thes'.
     */
    public void setThes(
            final edu.indiana.dlib.xml.zthes.Thes thes) {
        this._thes = thes;
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled edu.indiana.dlib.xml.zthes.Zthes
     */
    public static edu.indiana.dlib.xml.zthes.Zthes unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (edu.indiana.dlib.xml.zthes.Zthes) Unmarshaller.unmarshal(edu.indiana.dlib.xml.zthes.Zthes.class, reader);
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
