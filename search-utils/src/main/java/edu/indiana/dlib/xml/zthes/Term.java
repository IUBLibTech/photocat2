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
 * Class Term.
 * 
 * @version $Revision$ $Date$
 */
public class Term implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _termId.
     */
    private java.lang.String _termId;

    /**
     * Field _termUpdate.
     */
    private edu.indiana.dlib.xml.zthes.types.TermUpdateType _termUpdate;

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

    /**
     * Field _termVocabulary.
     */
    private java.lang.String _termVocabulary;

    /**
     * Field _termCategoryList.
     */
    private java.util.Vector _termCategoryList;

    /**
     * Field _termStatus.
     */
    private edu.indiana.dlib.xml.zthes.types.TermStatusType _termStatus;

    /**
     * Field _termApproval.
     */
    private edu.indiana.dlib.xml.zthes.types.TermApprovalType _termApproval;

    /**
     * Field _termSortkey.
     */
    private java.lang.String _termSortkey;

    /**
     * Field _termNoteList.
     */
    private java.util.Vector _termNoteList;

    /**
     * Field _termCreatedDate.
     */
    private java.lang.String _termCreatedDate;

    /**
     * Field _termCreatedBy.
     */
    private java.lang.String _termCreatedBy;

    /**
     * Field _termModifiedDate.
     */
    private java.lang.String _termModifiedDate;

    /**
     * Field _termModifiedBy.
     */
    private java.lang.String _termModifiedBy;

    /**
     * Field _postingsList.
     */
    private java.util.Vector _postingsList;

    /**
     * Field _relationList.
     */
    private java.util.Vector _relationList;


      //----------------/
     //- Constructors -/
    //----------------/

    public Term() {
        super();
        this._termCategoryList = new java.util.Vector();
        this._termNoteList = new java.util.Vector();
        this._postingsList = new java.util.Vector();
        this._relationList = new java.util.Vector();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * 
     * 
     * @param vPostings
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addPostings(
            final edu.indiana.dlib.xml.zthes.Postings vPostings)
    throws java.lang.IndexOutOfBoundsException {
        this._postingsList.addElement(vPostings);
    }

    /**
     * 
     * 
     * @param index
     * @param vPostings
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addPostings(
            final int index,
            final edu.indiana.dlib.xml.zthes.Postings vPostings)
    throws java.lang.IndexOutOfBoundsException {
        this._postingsList.add(index, vPostings);
    }

    /**
     * 
     * 
     * @param vRelation
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addRelation(
            final edu.indiana.dlib.xml.zthes.Relation vRelation)
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
            final edu.indiana.dlib.xml.zthes.Relation vRelation)
    throws java.lang.IndexOutOfBoundsException {
        this._relationList.add(index, vRelation);
    }

    /**
     * 
     * 
     * @param vTermCategory
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addTermCategory(
            final java.lang.String vTermCategory)
    throws java.lang.IndexOutOfBoundsException {
        this._termCategoryList.addElement(vTermCategory);
    }

    /**
     * 
     * 
     * @param index
     * @param vTermCategory
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addTermCategory(
            final int index,
            final java.lang.String vTermCategory)
    throws java.lang.IndexOutOfBoundsException {
        this._termCategoryList.add(index, vTermCategory);
    }

    /**
     * 
     * 
     * @param vTermNote
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addTermNote(
            final edu.indiana.dlib.xml.zthes.TermNote vTermNote)
    throws java.lang.IndexOutOfBoundsException {
        this._termNoteList.addElement(vTermNote);
    }

    /**
     * 
     * 
     * @param index
     * @param vTermNote
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addTermNote(
            final int index,
            final edu.indiana.dlib.xml.zthes.TermNote vTermNote)
    throws java.lang.IndexOutOfBoundsException {
        this._termNoteList.add(index, vTermNote);
    }

    /**
     * Method enumeratePostings.
     * 
     * @return an Enumeration over all
     * edu.indiana.dlib.xml.zthes.Postings elements
     */
    public java.util.Enumeration enumeratePostings(
    ) {
        return this._postingsList.elements();
    }

    /**
     * Method enumerateRelation.
     * 
     * @return an Enumeration over all
     * edu.indiana.dlib.xml.zthes.Relation elements
     */
    public java.util.Enumeration enumerateRelation(
    ) {
        return this._relationList.elements();
    }

    /**
     * Method enumerateTermCategory.
     * 
     * @return an Enumeration over all java.lang.String elements
     */
    public java.util.Enumeration enumerateTermCategory(
    ) {
        return this._termCategoryList.elements();
    }

    /**
     * Method enumerateTermNote.
     * 
     * @return an Enumeration over all
     * edu.indiana.dlib.xml.zthes.TermNote elements
     */
    public java.util.Enumeration enumerateTermNote(
    ) {
        return this._termNoteList.elements();
    }

    /**
     * Method getPostings.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the edu.indiana.dlib.xml.zthes.Postings
     * at the given index
     */
    public edu.indiana.dlib.xml.zthes.Postings getPostings(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._postingsList.size()) {
            throw new IndexOutOfBoundsException("getPostings: Index value '" + index + "' not in range [0.." + (this._postingsList.size() - 1) + "]");
        }
        
        return (edu.indiana.dlib.xml.zthes.Postings) _postingsList.get(index);
    }

    /**
     * Method getPostings.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public edu.indiana.dlib.xml.zthes.Postings[] getPostings(
    ) {
        edu.indiana.dlib.xml.zthes.Postings[] array = new edu.indiana.dlib.xml.zthes.Postings[0];
        return (edu.indiana.dlib.xml.zthes.Postings[]) this._postingsList.toArray(array);
    }

    /**
     * Method getPostingsCount.
     * 
     * @return the size of this collection
     */
    public int getPostingsCount(
    ) {
        return this._postingsList.size();
    }

    /**
     * Method getRelation.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the edu.indiana.dlib.xml.zthes.Relation
     * at the given index
     */
    public edu.indiana.dlib.xml.zthes.Relation getRelation(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._relationList.size()) {
            throw new IndexOutOfBoundsException("getRelation: Index value '" + index + "' not in range [0.." + (this._relationList.size() - 1) + "]");
        }
        
        return (edu.indiana.dlib.xml.zthes.Relation) _relationList.get(index);
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
    public edu.indiana.dlib.xml.zthes.Relation[] getRelation(
    ) {
        edu.indiana.dlib.xml.zthes.Relation[] array = new edu.indiana.dlib.xml.zthes.Relation[0];
        return (edu.indiana.dlib.xml.zthes.Relation[]) this._relationList.toArray(array);
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
     * Returns the value of field 'termApproval'.
     * 
     * @return the value of field 'TermApproval'.
     */
    public edu.indiana.dlib.xml.zthes.types.TermApprovalType getTermApproval(
    ) {
        return this._termApproval;
    }

    /**
     * Method getTermCategory.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.String at the given index
     */
    public java.lang.String getTermCategory(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._termCategoryList.size()) {
            throw new IndexOutOfBoundsException("getTermCategory: Index value '" + index + "' not in range [0.." + (this._termCategoryList.size() - 1) + "]");
        }
        
        return (java.lang.String) _termCategoryList.get(index);
    }

    /**
     * Method getTermCategory.Returns the contents of the
     * collection in an Array.  <p>Note:  Just in case the
     * collection contents are changing in another thread, we pass
     * a 0-length Array of the correct type into the API call. 
     * This way we <i>know</i> that the Array returned is of
     * exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public java.lang.String[] getTermCategory(
    ) {
        java.lang.String[] array = new java.lang.String[0];
        return (java.lang.String[]) this._termCategoryList.toArray(array);
    }

    /**
     * Method getTermCategoryCount.
     * 
     * @return the size of this collection
     */
    public int getTermCategoryCount(
    ) {
        return this._termCategoryList.size();
    }

    /**
     * Returns the value of field 'termCreatedBy'.
     * 
     * @return the value of field 'TermCreatedBy'.
     */
    public java.lang.String getTermCreatedBy(
    ) {
        return this._termCreatedBy;
    }

    /**
     * Returns the value of field 'termCreatedDate'.
     * 
     * @return the value of field 'TermCreatedDate'.
     */
    public java.lang.String getTermCreatedDate(
    ) {
        return this._termCreatedDate;
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
     * Returns the value of field 'termModifiedBy'.
     * 
     * @return the value of field 'TermModifiedBy'.
     */
    public java.lang.String getTermModifiedBy(
    ) {
        return this._termModifiedBy;
    }

    /**
     * Returns the value of field 'termModifiedDate'.
     * 
     * @return the value of field 'TermModifiedDate'.
     */
    public java.lang.String getTermModifiedDate(
    ) {
        return this._termModifiedDate;
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
     * Method getTermNote.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the edu.indiana.dlib.xml.zthes.TermNote
     * at the given index
     */
    public edu.indiana.dlib.xml.zthes.TermNote getTermNote(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._termNoteList.size()) {
            throw new IndexOutOfBoundsException("getTermNote: Index value '" + index + "' not in range [0.." + (this._termNoteList.size() - 1) + "]");
        }
        
        return (edu.indiana.dlib.xml.zthes.TermNote) _termNoteList.get(index);
    }

    /**
     * Method getTermNote.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public edu.indiana.dlib.xml.zthes.TermNote[] getTermNote(
    ) {
        edu.indiana.dlib.xml.zthes.TermNote[] array = new edu.indiana.dlib.xml.zthes.TermNote[0];
        return (edu.indiana.dlib.xml.zthes.TermNote[]) this._termNoteList.toArray(array);
    }

    /**
     * Method getTermNoteCount.
     * 
     * @return the size of this collection
     */
    public int getTermNoteCount(
    ) {
        return this._termNoteList.size();
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
     * Returns the value of field 'termSortkey'.
     * 
     * @return the value of field 'TermSortkey'.
     */
    public java.lang.String getTermSortkey(
    ) {
        return this._termSortkey;
    }

    /**
     * Returns the value of field 'termStatus'.
     * 
     * @return the value of field 'TermStatus'.
     */
    public edu.indiana.dlib.xml.zthes.types.TermStatusType getTermStatus(
    ) {
        return this._termStatus;
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
     * Returns the value of field 'termUpdate'.
     * 
     * @return the value of field 'TermUpdate'.
     */
    public edu.indiana.dlib.xml.zthes.types.TermUpdateType getTermUpdate(
    ) {
        return this._termUpdate;
    }

    /**
     * Returns the value of field 'termVocabulary'.
     * 
     * @return the value of field 'TermVocabulary'.
     */
    public java.lang.String getTermVocabulary(
    ) {
        return this._termVocabulary;
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
    public void removeAllPostings(
    ) {
        this._postingsList.clear();
    }

    /**
     */
    public void removeAllRelation(
    ) {
        this._relationList.clear();
    }

    /**
     */
    public void removeAllTermCategory(
    ) {
        this._termCategoryList.clear();
    }

    /**
     */
    public void removeAllTermNote(
    ) {
        this._termNoteList.clear();
    }

    /**
     * Method removePostings.
     * 
     * @param vPostings
     * @return true if the object was removed from the collection.
     */
    public boolean removePostings(
            final edu.indiana.dlib.xml.zthes.Postings vPostings) {
        boolean removed = _postingsList.remove(vPostings);
        return removed;
    }

    /**
     * Method removePostingsAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public edu.indiana.dlib.xml.zthes.Postings removePostingsAt(
            final int index) {
        java.lang.Object obj = this._postingsList.remove(index);
        return (edu.indiana.dlib.xml.zthes.Postings) obj;
    }

    /**
     * Method removeRelation.
     * 
     * @param vRelation
     * @return true if the object was removed from the collection.
     */
    public boolean removeRelation(
            final edu.indiana.dlib.xml.zthes.Relation vRelation) {
        boolean removed = _relationList.remove(vRelation);
        return removed;
    }

    /**
     * Method removeRelationAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public edu.indiana.dlib.xml.zthes.Relation removeRelationAt(
            final int index) {
        java.lang.Object obj = this._relationList.remove(index);
        return (edu.indiana.dlib.xml.zthes.Relation) obj;
    }

    /**
     * Method removeTermCategory.
     * 
     * @param vTermCategory
     * @return true if the object was removed from the collection.
     */
    public boolean removeTermCategory(
            final java.lang.String vTermCategory) {
        boolean removed = _termCategoryList.remove(vTermCategory);
        return removed;
    }

    /**
     * Method removeTermCategoryAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public java.lang.String removeTermCategoryAt(
            final int index) {
        java.lang.Object obj = this._termCategoryList.remove(index);
        return (java.lang.String) obj;
    }

    /**
     * Method removeTermNote.
     * 
     * @param vTermNote
     * @return true if the object was removed from the collection.
     */
    public boolean removeTermNote(
            final edu.indiana.dlib.xml.zthes.TermNote vTermNote) {
        boolean removed = _termNoteList.remove(vTermNote);
        return removed;
    }

    /**
     * Method removeTermNoteAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public edu.indiana.dlib.xml.zthes.TermNote removeTermNoteAt(
            final int index) {
        java.lang.Object obj = this._termNoteList.remove(index);
        return (edu.indiana.dlib.xml.zthes.TermNote) obj;
    }

    /**
     * 
     * 
     * @param index
     * @param vPostings
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setPostings(
            final int index,
            final edu.indiana.dlib.xml.zthes.Postings vPostings)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._postingsList.size()) {
            throw new IndexOutOfBoundsException("setPostings: Index value '" + index + "' not in range [0.." + (this._postingsList.size() - 1) + "]");
        }
        
        this._postingsList.set(index, vPostings);
    }

    /**
     * 
     * 
     * @param vPostingsArray
     */
    public void setPostings(
            final edu.indiana.dlib.xml.zthes.Postings[] vPostingsArray) {
        //-- copy array
        _postingsList.clear();
        
        for (int i = 0; i < vPostingsArray.length; i++) {
                this._postingsList.add(vPostingsArray[i]);
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
            final edu.indiana.dlib.xml.zthes.Relation vRelation)
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
            final edu.indiana.dlib.xml.zthes.Relation[] vRelationArray) {
        //-- copy array
        _relationList.clear();
        
        for (int i = 0; i < vRelationArray.length; i++) {
                this._relationList.add(vRelationArray[i]);
        }
    }

    /**
     * Sets the value of field 'termApproval'.
     * 
     * @param termApproval the value of field 'termApproval'.
     */
    public void setTermApproval(
            final edu.indiana.dlib.xml.zthes.types.TermApprovalType termApproval) {
        this._termApproval = termApproval;
    }

    /**
     * 
     * 
     * @param index
     * @param vTermCategory
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setTermCategory(
            final int index,
            final java.lang.String vTermCategory)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._termCategoryList.size()) {
            throw new IndexOutOfBoundsException("setTermCategory: Index value '" + index + "' not in range [0.." + (this._termCategoryList.size() - 1) + "]");
        }
        
        this._termCategoryList.set(index, vTermCategory);
    }

    /**
     * 
     * 
     * @param vTermCategoryArray
     */
    public void setTermCategory(
            final java.lang.String[] vTermCategoryArray) {
        //-- copy array
        _termCategoryList.clear();
        
        for (int i = 0; i < vTermCategoryArray.length; i++) {
                this._termCategoryList.add(vTermCategoryArray[i]);
        }
    }

    /**
     * Sets the value of field 'termCreatedBy'.
     * 
     * @param termCreatedBy the value of field 'termCreatedBy'.
     */
    public void setTermCreatedBy(
            final java.lang.String termCreatedBy) {
        this._termCreatedBy = termCreatedBy;
    }

    /**
     * Sets the value of field 'termCreatedDate'.
     * 
     * @param termCreatedDate the value of field 'termCreatedDate'.
     */
    public void setTermCreatedDate(
            final java.lang.String termCreatedDate) {
        this._termCreatedDate = termCreatedDate;
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
     * Sets the value of field 'termModifiedBy'.
     * 
     * @param termModifiedBy the value of field 'termModifiedBy'.
     */
    public void setTermModifiedBy(
            final java.lang.String termModifiedBy) {
        this._termModifiedBy = termModifiedBy;
    }

    /**
     * Sets the value of field 'termModifiedDate'.
     * 
     * @param termModifiedDate the value of field 'termModifiedDate'
     */
    public void setTermModifiedDate(
            final java.lang.String termModifiedDate) {
        this._termModifiedDate = termModifiedDate;
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
     * 
     * 
     * @param index
     * @param vTermNote
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setTermNote(
            final int index,
            final edu.indiana.dlib.xml.zthes.TermNote vTermNote)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._termNoteList.size()) {
            throw new IndexOutOfBoundsException("setTermNote: Index value '" + index + "' not in range [0.." + (this._termNoteList.size() - 1) + "]");
        }
        
        this._termNoteList.set(index, vTermNote);
    }

    /**
     * 
     * 
     * @param vTermNoteArray
     */
    public void setTermNote(
            final edu.indiana.dlib.xml.zthes.TermNote[] vTermNoteArray) {
        //-- copy array
        _termNoteList.clear();
        
        for (int i = 0; i < vTermNoteArray.length; i++) {
                this._termNoteList.add(vTermNoteArray[i]);
        }
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
     * Sets the value of field 'termSortkey'.
     * 
     * @param termSortkey the value of field 'termSortkey'.
     */
    public void setTermSortkey(
            final java.lang.String termSortkey) {
        this._termSortkey = termSortkey;
    }

    /**
     * Sets the value of field 'termStatus'.
     * 
     * @param termStatus the value of field 'termStatus'.
     */
    public void setTermStatus(
            final edu.indiana.dlib.xml.zthes.types.TermStatusType termStatus) {
        this._termStatus = termStatus;
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
     * Sets the value of field 'termUpdate'.
     * 
     * @param termUpdate the value of field 'termUpdate'.
     */
    public void setTermUpdate(
            final edu.indiana.dlib.xml.zthes.types.TermUpdateType termUpdate) {
        this._termUpdate = termUpdate;
    }

    /**
     * Sets the value of field 'termVocabulary'.
     * 
     * @param termVocabulary the value of field 'termVocabulary'.
     */
    public void setTermVocabulary(
            final java.lang.String termVocabulary) {
        this._termVocabulary = termVocabulary;
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled edu.indiana.dlib.xml.zthes.Term
     */
    public static edu.indiana.dlib.xml.zthes.Term unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (edu.indiana.dlib.xml.zthes.Term) Unmarshaller.unmarshal(edu.indiana.dlib.xml.zthes.Term.class, reader);
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
