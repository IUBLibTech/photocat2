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
 * Class Thes.
 * 
 * @version $Revision$ $Date$
 */
public class Thes implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _titleList.
     */
    private java.util.Vector _titleList;

    /**
     * Field _creatorList.
     */
    private java.util.Vector _creatorList;

    /**
     * Field _subjectList.
     */
    private java.util.Vector _subjectList;

    /**
     * Field _descriptionList.
     */
    private java.util.Vector _descriptionList;

    /**
     * Field _publisherList.
     */
    private java.util.Vector _publisherList;

    /**
     * Field _contributorList.
     */
    private java.util.Vector _contributorList;

    /**
     * Field _dateList.
     */
    private java.util.Vector _dateList;

    /**
     * Field _typeList.
     */
    private java.util.Vector _typeList;

    /**
     * Field _formatList.
     */
    private java.util.Vector _formatList;

    /**
     * Field _identifierList.
     */
    private java.util.Vector _identifierList;

    /**
     * Field _sourceList.
     */
    private java.util.Vector _sourceList;

    /**
     * Field _languageList.
     */
    private java.util.Vector _languageList;

    /**
     * Field _relationList.
     */
    private java.util.Vector _relationList;

    /**
     * Field _coverageList.
     */
    private java.util.Vector _coverageList;

    /**
     * Field _rightsList.
     */
    private java.util.Vector _rightsList;

    /**
     * Field _thesNoteList.
     */
    private java.util.Vector _thesNoteList;


      //----------------/
     //- Constructors -/
    //----------------/

    public Thes() {
        super();
        this._titleList = new java.util.Vector();
        this._creatorList = new java.util.Vector();
        this._subjectList = new java.util.Vector();
        this._descriptionList = new java.util.Vector();
        this._publisherList = new java.util.Vector();
        this._contributorList = new java.util.Vector();
        this._dateList = new java.util.Vector();
        this._typeList = new java.util.Vector();
        this._formatList = new java.util.Vector();
        this._identifierList = new java.util.Vector();
        this._sourceList = new java.util.Vector();
        this._languageList = new java.util.Vector();
        this._relationList = new java.util.Vector();
        this._coverageList = new java.util.Vector();
        this._rightsList = new java.util.Vector();
        this._thesNoteList = new java.util.Vector();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * 
     * 
     * @param vContributor
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addContributor(
            final java.lang.Object vContributor)
    throws java.lang.IndexOutOfBoundsException {
        this._contributorList.addElement(vContributor);
    }

    /**
     * 
     * 
     * @param index
     * @param vContributor
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addContributor(
            final int index,
            final java.lang.Object vContributor)
    throws java.lang.IndexOutOfBoundsException {
        this._contributorList.add(index, vContributor);
    }

    /**
     * 
     * 
     * @param vCoverage
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addCoverage(
            final java.lang.Object vCoverage)
    throws java.lang.IndexOutOfBoundsException {
        this._coverageList.addElement(vCoverage);
    }

    /**
     * 
     * 
     * @param index
     * @param vCoverage
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addCoverage(
            final int index,
            final java.lang.Object vCoverage)
    throws java.lang.IndexOutOfBoundsException {
        this._coverageList.add(index, vCoverage);
    }

    /**
     * 
     * 
     * @param vCreator
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addCreator(
            final java.lang.Object vCreator)
    throws java.lang.IndexOutOfBoundsException {
        this._creatorList.addElement(vCreator);
    }

    /**
     * 
     * 
     * @param index
     * @param vCreator
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addCreator(
            final int index,
            final java.lang.Object vCreator)
    throws java.lang.IndexOutOfBoundsException {
        this._creatorList.add(index, vCreator);
    }

    /**
     * 
     * 
     * @param vDate
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addDate(
            final java.lang.Object vDate)
    throws java.lang.IndexOutOfBoundsException {
        this._dateList.addElement(vDate);
    }

    /**
     * 
     * 
     * @param index
     * @param vDate
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addDate(
            final int index,
            final java.lang.Object vDate)
    throws java.lang.IndexOutOfBoundsException {
        this._dateList.add(index, vDate);
    }

    /**
     * 
     * 
     * @param vDescription
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addDescription(
            final java.lang.Object vDescription)
    throws java.lang.IndexOutOfBoundsException {
        this._descriptionList.addElement(vDescription);
    }

    /**
     * 
     * 
     * @param index
     * @param vDescription
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addDescription(
            final int index,
            final java.lang.Object vDescription)
    throws java.lang.IndexOutOfBoundsException {
        this._descriptionList.add(index, vDescription);
    }

    /**
     * 
     * 
     * @param vFormat
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addFormat(
            final java.lang.Object vFormat)
    throws java.lang.IndexOutOfBoundsException {
        this._formatList.addElement(vFormat);
    }

    /**
     * 
     * 
     * @param index
     * @param vFormat
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addFormat(
            final int index,
            final java.lang.Object vFormat)
    throws java.lang.IndexOutOfBoundsException {
        this._formatList.add(index, vFormat);
    }

    /**
     * 
     * 
     * @param vIdentifier
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addIdentifier(
            final java.lang.Object vIdentifier)
    throws java.lang.IndexOutOfBoundsException {
        this._identifierList.addElement(vIdentifier);
    }

    /**
     * 
     * 
     * @param index
     * @param vIdentifier
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addIdentifier(
            final int index,
            final java.lang.Object vIdentifier)
    throws java.lang.IndexOutOfBoundsException {
        this._identifierList.add(index, vIdentifier);
    }

    /**
     * 
     * 
     * @param vLanguage
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addLanguage(
            final java.lang.Object vLanguage)
    throws java.lang.IndexOutOfBoundsException {
        this._languageList.addElement(vLanguage);
    }

    /**
     * 
     * 
     * @param index
     * @param vLanguage
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addLanguage(
            final int index,
            final java.lang.Object vLanguage)
    throws java.lang.IndexOutOfBoundsException {
        this._languageList.add(index, vLanguage);
    }

    /**
     * 
     * 
     * @param vPublisher
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addPublisher(
            final java.lang.Object vPublisher)
    throws java.lang.IndexOutOfBoundsException {
        this._publisherList.addElement(vPublisher);
    }

    /**
     * 
     * 
     * @param index
     * @param vPublisher
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addPublisher(
            final int index,
            final java.lang.Object vPublisher)
    throws java.lang.IndexOutOfBoundsException {
        this._publisherList.add(index, vPublisher);
    }

    /**
     * 
     * 
     * @param vRelation
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addRelation(
            final java.lang.Object vRelation)
    throws java.lang.IndexOutOfBoundsException {
        this._relationList.addElement(vRelation);
    }

    /**
     * 
     * 
     * @param index
     * @param vRelation
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addRelation(
            final int index,
            final java.lang.Object vRelation)
    throws java.lang.IndexOutOfBoundsException {
        this._relationList.add(index, vRelation);
    }

    /**
     * 
     * 
     * @param vRights
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addRights(
            final java.lang.Object vRights)
    throws java.lang.IndexOutOfBoundsException {
        this._rightsList.addElement(vRights);
    }

    /**
     * 
     * 
     * @param index
     * @param vRights
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addRights(
            final int index,
            final java.lang.Object vRights)
    throws java.lang.IndexOutOfBoundsException {
        this._rightsList.add(index, vRights);
    }

    /**
     * 
     * 
     * @param vSource
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addSource(
            final java.lang.Object vSource)
    throws java.lang.IndexOutOfBoundsException {
        this._sourceList.addElement(vSource);
    }

    /**
     * 
     * 
     * @param index
     * @param vSource
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addSource(
            final int index,
            final java.lang.Object vSource)
    throws java.lang.IndexOutOfBoundsException {
        this._sourceList.add(index, vSource);
    }

    /**
     * 
     * 
     * @param vSubject
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addSubject(
            final java.lang.Object vSubject)
    throws java.lang.IndexOutOfBoundsException {
        this._subjectList.addElement(vSubject);
    }

    /**
     * 
     * 
     * @param index
     * @param vSubject
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addSubject(
            final int index,
            final java.lang.Object vSubject)
    throws java.lang.IndexOutOfBoundsException {
        this._subjectList.add(index, vSubject);
    }

    /**
     * 
     * 
     * @param vThesNote
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addThesNote(
            final edu.indiana.dlib.xml.zthes.ThesNote vThesNote)
    throws java.lang.IndexOutOfBoundsException {
        this._thesNoteList.addElement(vThesNote);
    }

    /**
     * 
     * 
     * @param index
     * @param vThesNote
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addThesNote(
            final int index,
            final edu.indiana.dlib.xml.zthes.ThesNote vThesNote)
    throws java.lang.IndexOutOfBoundsException {
        this._thesNoteList.add(index, vThesNote);
    }

    /**
     * 
     * 
     * @param vTitle
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addTitle(
            final java.lang.Object vTitle)
    throws java.lang.IndexOutOfBoundsException {
        this._titleList.addElement(vTitle);
    }

    /**
     * 
     * 
     * @param index
     * @param vTitle
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addTitle(
            final int index,
            final java.lang.Object vTitle)
    throws java.lang.IndexOutOfBoundsException {
        this._titleList.add(index, vTitle);
    }

    /**
     * 
     * 
     * @param vType
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addType(
            final java.lang.Object vType)
    throws java.lang.IndexOutOfBoundsException {
        this._typeList.addElement(vType);
    }

    /**
     * 
     * 
     * @param index
     * @param vType
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addType(
            final int index,
            final java.lang.Object vType)
    throws java.lang.IndexOutOfBoundsException {
        this._typeList.add(index, vType);
    }

    /**
     * Method enumerateContributor.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateContributor(
    ) {
        return this._contributorList.elements();
    }

    /**
     * Method enumerateCoverage.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateCoverage(
    ) {
        return this._coverageList.elements();
    }

    /**
     * Method enumerateCreator.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateCreator(
    ) {
        return this._creatorList.elements();
    }

    /**
     * Method enumerateDate.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateDate(
    ) {
        return this._dateList.elements();
    }

    /**
     * Method enumerateDescription.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateDescription(
    ) {
        return this._descriptionList.elements();
    }

    /**
     * Method enumerateFormat.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateFormat(
    ) {
        return this._formatList.elements();
    }

    /**
     * Method enumerateIdentifier.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateIdentifier(
    ) {
        return this._identifierList.elements();
    }

    /**
     * Method enumerateLanguage.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateLanguage(
    ) {
        return this._languageList.elements();
    }

    /**
     * Method enumeratePublisher.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumeratePublisher(
    ) {
        return this._publisherList.elements();
    }

    /**
     * Method enumerateRelation.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateRelation(
    ) {
        return this._relationList.elements();
    }

    /**
     * Method enumerateRights.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateRights(
    ) {
        return this._rightsList.elements();
    }

    /**
     * Method enumerateSource.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateSource(
    ) {
        return this._sourceList.elements();
    }

    /**
     * Method enumerateSubject.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateSubject(
    ) {
        return this._subjectList.elements();
    }

    /**
     * Method enumerateThesNote.
     * 
     * @return an Enumeration over all
     * edu.indiana.dlib.xml.zthes.ThesNote elements
     */
    public java.util.Enumeration enumerateThesNote(
    ) {
        return this._thesNoteList.elements();
    }

    /**
     * Method enumerateTitle.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateTitle(
    ) {
        return this._titleList.elements();
    }

    /**
     * Method enumerateType.
     * 
     * @return an Enumeration over all java.lang.Object elements
     */
    public java.util.Enumeration enumerateType(
    ) {
        return this._typeList.elements();
    }

    /**
     * Method getContributor.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getContributor(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._contributorList.size()) {
            throw new IndexOutOfBoundsException("getContributor: Index value '" + index + "' not in range [0.." + (this._contributorList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _contributorList.get(index);
    }

    /**
     * Method getContributor.Returns the contents of the collection
     * in an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getContributor(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._contributorList.toArray(array);
    }

    /**
     * Method getContributorCount.
     * 
     * @return the size of this collection
     */
    public int getContributorCount(
    ) {
        return this._contributorList.size();
    }

    /**
     * Method getCoverage.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getCoverage(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._coverageList.size()) {
            throw new IndexOutOfBoundsException("getCoverage: Index value '" + index + "' not in range [0.." + (this._coverageList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _coverageList.get(index);
    }

    /**
     * Method getCoverage.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getCoverage(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._coverageList.toArray(array);
    }

    /**
     * Method getCoverageCount.
     * 
     * @return the size of this collection
     */
    public int getCoverageCount(
    ) {
        return this._coverageList.size();
    }

    /**
     * Method getCreator.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getCreator(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._creatorList.size()) {
            throw new IndexOutOfBoundsException("getCreator: Index value '" + index + "' not in range [0.." + (this._creatorList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _creatorList.get(index);
    }

    /**
     * Method getCreator.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getCreator(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._creatorList.toArray(array);
    }

    /**
     * Method getCreatorCount.
     * 
     * @return the size of this collection
     */
    public int getCreatorCount(
    ) {
        return this._creatorList.size();
    }

    /**
     * Method getDate.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getDate(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._dateList.size()) {
            throw new IndexOutOfBoundsException("getDate: Index value '" + index + "' not in range [0.." + (this._dateList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _dateList.get(index);
    }

    /**
     * Method getDate.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getDate(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._dateList.toArray(array);
    }

    /**
     * Method getDateCount.
     * 
     * @return the size of this collection
     */
    public int getDateCount(
    ) {
        return this._dateList.size();
    }

    /**
     * Method getDescription.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getDescription(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._descriptionList.size()) {
            throw new IndexOutOfBoundsException("getDescription: Index value '" + index + "' not in range [0.." + (this._descriptionList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _descriptionList.get(index);
    }

    /**
     * Method getDescription.Returns the contents of the collection
     * in an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getDescription(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._descriptionList.toArray(array);
    }

    /**
     * Method getDescriptionCount.
     * 
     * @return the size of this collection
     */
    public int getDescriptionCount(
    ) {
        return this._descriptionList.size();
    }

    /**
     * Method getFormat.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getFormat(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._formatList.size()) {
            throw new IndexOutOfBoundsException("getFormat: Index value '" + index + "' not in range [0.." + (this._formatList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _formatList.get(index);
    }

    /**
     * Method getFormat.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getFormat(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._formatList.toArray(array);
    }

    /**
     * Method getFormatCount.
     * 
     * @return the size of this collection
     */
    public int getFormatCount(
    ) {
        return this._formatList.size();
    }

    /**
     * Method getIdentifier.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getIdentifier(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._identifierList.size()) {
            throw new IndexOutOfBoundsException("getIdentifier: Index value '" + index + "' not in range [0.." + (this._identifierList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _identifierList.get(index);
    }

    /**
     * Method getIdentifier.Returns the contents of the collection
     * in an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getIdentifier(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._identifierList.toArray(array);
    }

    /**
     * Method getIdentifierCount.
     * 
     * @return the size of this collection
     */
    public int getIdentifierCount(
    ) {
        return this._identifierList.size();
    }

    /**
     * Method getLanguage.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getLanguage(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._languageList.size()) {
            throw new IndexOutOfBoundsException("getLanguage: Index value '" + index + "' not in range [0.." + (this._languageList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _languageList.get(index);
    }

    /**
     * Method getLanguage.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getLanguage(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._languageList.toArray(array);
    }

    /**
     * Method getLanguageCount.
     * 
     * @return the size of this collection
     */
    public int getLanguageCount(
    ) {
        return this._languageList.size();
    }

    /**
     * Method getPublisher.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getPublisher(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._publisherList.size()) {
            throw new IndexOutOfBoundsException("getPublisher: Index value '" + index + "' not in range [0.." + (this._publisherList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _publisherList.get(index);
    }

    /**
     * Method getPublisher.Returns the contents of the collection
     * in an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getPublisher(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._publisherList.toArray(array);
    }

    /**
     * Method getPublisherCount.
     * 
     * @return the size of this collection
     */
    public int getPublisherCount(
    ) {
        return this._publisherList.size();
    }

    /**
     * Method getRelation.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getRelation(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._relationList.size()) {
            throw new IndexOutOfBoundsException("getRelation: Index value '" + index + "' not in range [0.." + (this._relationList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _relationList.get(index);
    }

    /**
     * Method getRelation.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getRelation(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._relationList.toArray(array);
    }

    /**
     * Method getRelationCount.
     * 
     * @return the size of this collection
     */
    public int getRelationCount(
    ) {
        return this._relationList.size();
    }

    /**
     * Method getRights.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getRights(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._rightsList.size()) {
            throw new IndexOutOfBoundsException("getRights: Index value '" + index + "' not in range [0.." + (this._rightsList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _rightsList.get(index);
    }

    /**
     * Method getRights.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getRights(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._rightsList.toArray(array);
    }

    /**
     * Method getRightsCount.
     * 
     * @return the size of this collection
     */
    public int getRightsCount(
    ) {
        return this._rightsList.size();
    }

    /**
     * Method getSource.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getSource(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._sourceList.size()) {
            throw new IndexOutOfBoundsException("getSource: Index value '" + index + "' not in range [0.." + (this._sourceList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _sourceList.get(index);
    }

    /**
     * Method getSource.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getSource(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._sourceList.toArray(array);
    }

    /**
     * Method getSourceCount.
     * 
     * @return the size of this collection
     */
    public int getSourceCount(
    ) {
        return this._sourceList.size();
    }

    /**
     * Method getSubject.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getSubject(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._subjectList.size()) {
            throw new IndexOutOfBoundsException("getSubject: Index value '" + index + "' not in range [0.." + (this._subjectList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _subjectList.get(index);
    }

    /**
     * Method getSubject.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getSubject(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._subjectList.toArray(array);
    }

    /**
     * Method getSubjectCount.
     * 
     * @return the size of this collection
     */
    public int getSubjectCount(
    ) {
        return this._subjectList.size();
    }

    /**
     * Method getThesNote.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the edu.indiana.dlib.xml.zthes.ThesNote
     * at the given index
     */
    public edu.indiana.dlib.xml.zthes.ThesNote getThesNote(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._thesNoteList.size()) {
            throw new IndexOutOfBoundsException("getThesNote: Index value '" + index + "' not in range [0.." + (this._thesNoteList.size() - 1) + "]");
        }
        
        return (edu.indiana.dlib.xml.zthes.ThesNote) _thesNoteList.get(index);
    }

    /**
     * Method getThesNote.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public edu.indiana.dlib.xml.zthes.ThesNote[] getThesNote(
    ) {
        edu.indiana.dlib.xml.zthes.ThesNote[] array = new edu.indiana.dlib.xml.zthes.ThesNote[0];
        return (edu.indiana.dlib.xml.zthes.ThesNote[]) this._thesNoteList.toArray(array);
    }

    /**
     * Method getThesNoteCount.
     * 
     * @return the size of this collection
     */
    public int getThesNoteCount(
    ) {
        return this._thesNoteList.size();
    }

    /**
     * Method getTitle.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getTitle(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._titleList.size()) {
            throw new IndexOutOfBoundsException("getTitle: Index value '" + index + "' not in range [0.." + (this._titleList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _titleList.get(index);
    }

    /**
     * Method getTitle.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getTitle(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._titleList.toArray(array);
    }

    /**
     * Method getTitleCount.
     * 
     * @return the size of this collection
     */
    public int getTitleCount(
    ) {
        return this._titleList.size();
    }

    /**
     * Method getType.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.Object at the given index
     */
    public java.lang.Object getType(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._typeList.size()) {
            throw new IndexOutOfBoundsException("getType: Index value '" + index + "' not in range [0.." + (this._typeList.size() - 1) + "]");
        }
        
        return (java.lang.Object) _typeList.get(index);
    }

    /**
     * Method getType.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.Object[] getType(
    ) {
        java.lang.Object[] array = new java.lang.Object[0];
        return (java.lang.Object[]) this._typeList.toArray(array);
    }

    /**
     * Method getTypeCount.
     * 
     * @return the size of this collection
     */
    public int getTypeCount(
    ) {
        return this._typeList.size();
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
    public void removeAllContributor(
    ) {
        this._contributorList.clear();
    }

    /**
     */
    public void removeAllCoverage(
    ) {
        this._coverageList.clear();
    }

    /**
     */
    public void removeAllCreator(
    ) {
        this._creatorList.clear();
    }

    /**
     */
    public void removeAllDate(
    ) {
        this._dateList.clear();
    }

    /**
     */
    public void removeAllDescription(
    ) {
        this._descriptionList.clear();
    }

    /**
     */
    public void removeAllFormat(
    ) {
        this._formatList.clear();
    }

    /**
     */
    public void removeAllIdentifier(
    ) {
        this._identifierList.clear();
    }

    /**
     */
    public void removeAllLanguage(
    ) {
        this._languageList.clear();
    }

    /**
     */
    public void removeAllPublisher(
    ) {
        this._publisherList.clear();
    }

    /**
     */
    public void removeAllRelation(
    ) {
        this._relationList.clear();
    }

    /**
     */
    public void removeAllRights(
    ) {
        this._rightsList.clear();
    }

    /**
     */
    public void removeAllSource(
    ) {
        this._sourceList.clear();
    }

    /**
     */
    public void removeAllSubject(
    ) {
        this._subjectList.clear();
    }

    /**
     */
    public void removeAllThesNote(
    ) {
        this._thesNoteList.clear();
    }

    /**
     */
    public void removeAllTitle(
    ) {
        this._titleList.clear();
    }

    /**
     */
    public void removeAllType(
    ) {
        this._typeList.clear();
    }

    /**
     * Method removeContributor.
     * 
     * @param vContributor
     * @return true if the object was removed from the collection.
     */
    public boolean removeContributor(
            final java.lang.Object vContributor) {
        boolean removed = _contributorList.remove(vContributor);
        return removed;
    }

    /**
     * Method removeContributorAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeContributorAt(
            final int index) {
        java.lang.Object obj = this._contributorList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeCoverage.
     * 
     * @param vCoverage
     * @return true if the object was removed from the collection.
     */
    public boolean removeCoverage(
            final java.lang.Object vCoverage) {
        boolean removed = _coverageList.remove(vCoverage);
        return removed;
    }

    /**
     * Method removeCoverageAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeCoverageAt(
            final int index) {
        java.lang.Object obj = this._coverageList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeCreator.
     * 
     * @param vCreator
     * @return true if the object was removed from the collection.
     */
    public boolean removeCreator(
            final java.lang.Object vCreator) {
        boolean removed = _creatorList.remove(vCreator);
        return removed;
    }

    /**
     * Method removeCreatorAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeCreatorAt(
            final int index) {
        java.lang.Object obj = this._creatorList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeDate.
     * 
     * @param vDate
     * @return true if the object was removed from the collection.
     */
    public boolean removeDate(
            final java.lang.Object vDate) {
        boolean removed = _dateList.remove(vDate);
        return removed;
    }

    /**
     * Method removeDateAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeDateAt(
            final int index) {
        java.lang.Object obj = this._dateList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeDescription.
     * 
     * @param vDescription
     * @return true if the object was removed from the collection.
     */
    public boolean removeDescription(
            final java.lang.Object vDescription) {
        boolean removed = _descriptionList.remove(vDescription);
        return removed;
    }

    /**
     * Method removeDescriptionAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeDescriptionAt(
            final int index) {
        java.lang.Object obj = this._descriptionList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeFormat.
     * 
     * @param vFormat
     * @return true if the object was removed from the collection.
     */
    public boolean removeFormat(
            final java.lang.Object vFormat) {
        boolean removed = _formatList.remove(vFormat);
        return removed;
    }

    /**
     * Method removeFormatAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeFormatAt(
            final int index) {
        java.lang.Object obj = this._formatList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeIdentifier.
     * 
     * @param vIdentifier
     * @return true if the object was removed from the collection.
     */
    public boolean removeIdentifier(
            final java.lang.Object vIdentifier) {
        boolean removed = _identifierList.remove(vIdentifier);
        return removed;
    }

    /**
     * Method removeIdentifierAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeIdentifierAt(
            final int index) {
        java.lang.Object obj = this._identifierList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeLanguage.
     * 
     * @param vLanguage
     * @return true if the object was removed from the collection.
     */
    public boolean removeLanguage(
            final java.lang.Object vLanguage) {
        boolean removed = _languageList.remove(vLanguage);
        return removed;
    }

    /**
     * Method removeLanguageAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeLanguageAt(
            final int index) {
        java.lang.Object obj = this._languageList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removePublisher.
     * 
     * @param vPublisher
     * @return true if the object was removed from the collection.
     */
    public boolean removePublisher(
            final java.lang.Object vPublisher) {
        boolean removed = _publisherList.remove(vPublisher);
        return removed;
    }

    /**
     * Method removePublisherAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removePublisherAt(
            final int index) {
        java.lang.Object obj = this._publisherList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeRelation.
     * 
     * @param vRelation
     * @return true if the object was removed from the collection.
     */
    public boolean removeRelation(
            final java.lang.Object vRelation) {
        boolean removed = _relationList.remove(vRelation);
        return removed;
    }

    /**
     * Method removeRelationAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeRelationAt(
            final int index) {
        java.lang.Object obj = this._relationList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeRights.
     * 
     * @param vRights
     * @return true if the object was removed from the collection.
     */
    public boolean removeRights(
            final java.lang.Object vRights) {
        boolean removed = _rightsList.remove(vRights);
        return removed;
    }

    /**
     * Method removeRightsAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeRightsAt(
            final int index) {
        java.lang.Object obj = this._rightsList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeSource.
     * 
     * @param vSource
     * @return true if the object was removed from the collection.
     */
    public boolean removeSource(
            final java.lang.Object vSource) {
        boolean removed = _sourceList.remove(vSource);
        return removed;
    }

    /**
     * Method removeSourceAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeSourceAt(
            final int index) {
        java.lang.Object obj = this._sourceList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeSubject.
     * 
     * @param vSubject
     * @return true if the object was removed from the collection.
     */
    public boolean removeSubject(
            final java.lang.Object vSubject) {
        boolean removed = _subjectList.remove(vSubject);
        return removed;
    }

    /**
     * Method removeSubjectAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeSubjectAt(
            final int index) {
        java.lang.Object obj = this._subjectList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeThesNote.
     * 
     * @param vThesNote
     * @return true if the object was removed from the collection.
     */
    public boolean removeThesNote(
            final edu.indiana.dlib.xml.zthes.ThesNote vThesNote) {
        boolean removed = _thesNoteList.remove(vThesNote);
        return removed;
    }

    /**
     * Method removeThesNoteAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public edu.indiana.dlib.xml.zthes.ThesNote removeThesNoteAt(
            final int index) {
        java.lang.Object obj = this._thesNoteList.remove(index);
        return (edu.indiana.dlib.xml.zthes.ThesNote) obj;
    }

    /**
     * Method removeTitle.
     * 
     * @param vTitle
     * @return true if the object was removed from the collection.
     */
    public boolean removeTitle(
            final java.lang.Object vTitle) {
        boolean removed = _titleList.remove(vTitle);
        return removed;
    }

    /**
     * Method removeTitleAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeTitleAt(
            final int index) {
        java.lang.Object obj = this._titleList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * Method removeType.
     * 
     * @param vType
     * @return true if the object was removed from the collection.
     */
    public boolean removeType(
            final java.lang.Object vType) {
        boolean removed = _typeList.remove(vType);
        return removed;
    }

    /**
     * Method removeTypeAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.Object removeTypeAt(
            final int index) {
        java.lang.Object obj = this._typeList.remove(index);
        return (java.lang.Object) obj;
    }

    /**
     * 
     * 
     * @param index
     * @param vContributor
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setContributor(
            final int index,
            final java.lang.Object vContributor)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._contributorList.size()) {
            throw new IndexOutOfBoundsException("setContributor: Index value '" + index + "' not in range [0.." + (this._contributorList.size() - 1) + "]");
        }
        
        this._contributorList.set(index, vContributor);
    }

    /**
     * 
     * 
     * @param vContributorArray
     */
    public void setContributor(
            final java.lang.Object[] vContributorArray) {
        //-- copy array
        _contributorList.clear();
        
        for (int i = 0; i < vContributorArray.length; i++) {
                this._contributorList.add(vContributorArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vCoverage
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setCoverage(
            final int index,
            final java.lang.Object vCoverage)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._coverageList.size()) {
            throw new IndexOutOfBoundsException("setCoverage: Index value '" + index + "' not in range [0.." + (this._coverageList.size() - 1) + "]");
        }
        
        this._coverageList.set(index, vCoverage);
    }

    /**
     * 
     * 
     * @param vCoverageArray
     */
    public void setCoverage(
            final java.lang.Object[] vCoverageArray) {
        //-- copy array
        _coverageList.clear();
        
        for (int i = 0; i < vCoverageArray.length; i++) {
                this._coverageList.add(vCoverageArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vCreator
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setCreator(
            final int index,
            final java.lang.Object vCreator)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._creatorList.size()) {
            throw new IndexOutOfBoundsException("setCreator: Index value '" + index + "' not in range [0.." + (this._creatorList.size() - 1) + "]");
        }
        
        this._creatorList.set(index, vCreator);
    }

    /**
     * 
     * 
     * @param vCreatorArray
     */
    public void setCreator(
            final java.lang.Object[] vCreatorArray) {
        //-- copy array
        _creatorList.clear();
        
        for (int i = 0; i < vCreatorArray.length; i++) {
                this._creatorList.add(vCreatorArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vDate
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setDate(
            final int index,
            final java.lang.Object vDate)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._dateList.size()) {
            throw new IndexOutOfBoundsException("setDate: Index value '" + index + "' not in range [0.." + (this._dateList.size() - 1) + "]");
        }
        
        this._dateList.set(index, vDate);
    }

    /**
     * 
     * 
     * @param vDateArray
     */
    public void setDate(
            final java.lang.Object[] vDateArray) {
        //-- copy array
        _dateList.clear();
        
        for (int i = 0; i < vDateArray.length; i++) {
                this._dateList.add(vDateArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vDescription
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setDescription(
            final int index,
            final java.lang.Object vDescription)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._descriptionList.size()) {
            throw new IndexOutOfBoundsException("setDescription: Index value '" + index + "' not in range [0.." + (this._descriptionList.size() - 1) + "]");
        }
        
        this._descriptionList.set(index, vDescription);
    }

    /**
     * 
     * 
     * @param vDescriptionArray
     */
    public void setDescription(
            final java.lang.Object[] vDescriptionArray) {
        //-- copy array
        _descriptionList.clear();
        
        for (int i = 0; i < vDescriptionArray.length; i++) {
                this._descriptionList.add(vDescriptionArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vFormat
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setFormat(
            final int index,
            final java.lang.Object vFormat)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._formatList.size()) {
            throw new IndexOutOfBoundsException("setFormat: Index value '" + index + "' not in range [0.." + (this._formatList.size() - 1) + "]");
        }
        
        this._formatList.set(index, vFormat);
    }

    /**
     * 
     * 
     * @param vFormatArray
     */
    public void setFormat(
            final java.lang.Object[] vFormatArray) {
        //-- copy array
        _formatList.clear();
        
        for (int i = 0; i < vFormatArray.length; i++) {
                this._formatList.add(vFormatArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vIdentifier
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setIdentifier(
            final int index,
            final java.lang.Object vIdentifier)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._identifierList.size()) {
            throw new IndexOutOfBoundsException("setIdentifier: Index value '" + index + "' not in range [0.." + (this._identifierList.size() - 1) + "]");
        }
        
        this._identifierList.set(index, vIdentifier);
    }

    /**
     * 
     * 
     * @param vIdentifierArray
     */
    public void setIdentifier(
            final java.lang.Object[] vIdentifierArray) {
        //-- copy array
        _identifierList.clear();
        
        for (int i = 0; i < vIdentifierArray.length; i++) {
                this._identifierList.add(vIdentifierArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vLanguage
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setLanguage(
            final int index,
            final java.lang.Object vLanguage)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._languageList.size()) {
            throw new IndexOutOfBoundsException("setLanguage: Index value '" + index + "' not in range [0.." + (this._languageList.size() - 1) + "]");
        }
        
        this._languageList.set(index, vLanguage);
    }

    /**
     * 
     * 
     * @param vLanguageArray
     */
    public void setLanguage(
            final java.lang.Object[] vLanguageArray) {
        //-- copy array
        _languageList.clear();
        
        for (int i = 0; i < vLanguageArray.length; i++) {
                this._languageList.add(vLanguageArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vPublisher
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setPublisher(
            final int index,
            final java.lang.Object vPublisher)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._publisherList.size()) {
            throw new IndexOutOfBoundsException("setPublisher: Index value '" + index + "' not in range [0.." + (this._publisherList.size() - 1) + "]");
        }
        
        this._publisherList.set(index, vPublisher);
    }

    /**
     * 
     * 
     * @param vPublisherArray
     */
    public void setPublisher(
            final java.lang.Object[] vPublisherArray) {
        //-- copy array
        _publisherList.clear();
        
        for (int i = 0; i < vPublisherArray.length; i++) {
                this._publisherList.add(vPublisherArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vRelation
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setRelation(
            final int index,
            final java.lang.Object vRelation)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._relationList.size()) {
            throw new IndexOutOfBoundsException("setRelation: Index value '" + index + "' not in range [0.." + (this._relationList.size() - 1) + "]");
        }
        
        this._relationList.set(index, vRelation);
    }

    /**
     * 
     * 
     * @param vRelationArray
     */
    public void setRelation(
            final java.lang.Object[] vRelationArray) {
        //-- copy array
        _relationList.clear();
        
        for (int i = 0; i < vRelationArray.length; i++) {
                this._relationList.add(vRelationArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vRights
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setRights(
            final int index,
            final java.lang.Object vRights)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._rightsList.size()) {
            throw new IndexOutOfBoundsException("setRights: Index value '" + index + "' not in range [0.." + (this._rightsList.size() - 1) + "]");
        }
        
        this._rightsList.set(index, vRights);
    }

    /**
     * 
     * 
     * @param vRightsArray
     */
    public void setRights(
            final java.lang.Object[] vRightsArray) {
        //-- copy array
        _rightsList.clear();
        
        for (int i = 0; i < vRightsArray.length; i++) {
                this._rightsList.add(vRightsArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vSource
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setSource(
            final int index,
            final java.lang.Object vSource)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._sourceList.size()) {
            throw new IndexOutOfBoundsException("setSource: Index value '" + index + "' not in range [0.." + (this._sourceList.size() - 1) + "]");
        }
        
        this._sourceList.set(index, vSource);
    }

    /**
     * 
     * 
     * @param vSourceArray
     */
    public void setSource(
            final java.lang.Object[] vSourceArray) {
        //-- copy array
        _sourceList.clear();
        
        for (int i = 0; i < vSourceArray.length; i++) {
                this._sourceList.add(vSourceArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vSubject
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setSubject(
            final int index,
            final java.lang.Object vSubject)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._subjectList.size()) {
            throw new IndexOutOfBoundsException("setSubject: Index value '" + index + "' not in range [0.." + (this._subjectList.size() - 1) + "]");
        }
        
        this._subjectList.set(index, vSubject);
    }

    /**
     * 
     * 
     * @param vSubjectArray
     */
    public void setSubject(
            final java.lang.Object[] vSubjectArray) {
        //-- copy array
        _subjectList.clear();
        
        for (int i = 0; i < vSubjectArray.length; i++) {
                this._subjectList.add(vSubjectArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vThesNote
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setThesNote(
            final int index,
            final edu.indiana.dlib.xml.zthes.ThesNote vThesNote)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._thesNoteList.size()) {
            throw new IndexOutOfBoundsException("setThesNote: Index value '" + index + "' not in range [0.." + (this._thesNoteList.size() - 1) + "]");
        }
        
        this._thesNoteList.set(index, vThesNote);
    }

    /**
     * 
     * 
     * @param vThesNoteArray
     */
    public void setThesNote(
            final edu.indiana.dlib.xml.zthes.ThesNote[] vThesNoteArray) {
        //-- copy array
        _thesNoteList.clear();
        
        for (int i = 0; i < vThesNoteArray.length; i++) {
                this._thesNoteList.add(vThesNoteArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vTitle
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setTitle(
            final int index,
            final java.lang.Object vTitle)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._titleList.size()) {
            throw new IndexOutOfBoundsException("setTitle: Index value '" + index + "' not in range [0.." + (this._titleList.size() - 1) + "]");
        }
        
        this._titleList.set(index, vTitle);
    }

    /**
     * 
     * 
     * @param vTitleArray
     */
    public void setTitle(
            final java.lang.Object[] vTitleArray) {
        //-- copy array
        _titleList.clear();
        
        for (int i = 0; i < vTitleArray.length; i++) {
                this._titleList.add(vTitleArray[i]);
        }
    }

    /**
     * 
     * 
     * @param index
     * @param vType
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setType(
            final int index,
            final java.lang.Object vType)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._typeList.size()) {
            throw new IndexOutOfBoundsException("setType: Index value '" + index + "' not in range [0.." + (this._typeList.size() - 1) + "]");
        }
        
        this._typeList.set(index, vType);
    }

    /**
     * 
     * 
     * @param vTypeArray
     */
    public void setType(
            final java.lang.Object[] vTypeArray) {
        //-- copy array
        _typeList.clear();
        
        for (int i = 0; i < vTypeArray.length; i++) {
                this._typeList.add(vTypeArray[i]);
        }
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled edu.indiana.dlib.xml.zthes.Thes
     */
    public static edu.indiana.dlib.xml.zthes.Thes unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (edu.indiana.dlib.xml.zthes.Thes) Unmarshaller.unmarshal(edu.indiana.dlib.xml.zthes.Thes.class, reader);
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
