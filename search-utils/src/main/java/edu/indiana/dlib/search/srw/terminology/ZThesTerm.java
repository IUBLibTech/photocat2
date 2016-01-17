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
package edu.indiana.dlib.search.srw.terminology;

import java.util.List;

public class ZThesTerm {

	private String termId;

	private String termName;

	private String termType;

	private List<String> termNotes;

	private List<TermRelation> termRelations;

	private List<String> usedForTermNames;

	private List<String> broaderTermNames;

	private List<String> narrowerTermNames;

	private List<String> useTermNames;

	private List<String> relatedTermNames;
	
	public class TermRelation {
		private String relationType;

		private String relatedTermName;

		public TermRelation(String relationType, String relatedTermName) {
			this.relationType = relationType;
			this.relatedTermName = relatedTermName;
		}

		/**
		 * @return the relationType
		 */
		public String getRelationType() {
			return relationType;
		}

		/**
		 * @param relationType
		 *            the relationType to set
		 */
		public void setRelationType(String relationType) {
			this.relationType = relationType;
		}

		/**
		 * @return the relatedTerm
		 */
		public String getRelatedTermName() {
			return relatedTermName;
		}

		/**
		 * @param relatedTerm
		 *            the relatedTerm to set
		 */
		public void setRelatedTermName(String relatedTerm) {
			this.relatedTermName = relatedTerm;
		}

		@Override
		public String toString() {
			return "Term: [" + termName + "] Relation type: [" + relationType + "] to ["
					+ relatedTermName + "]";
		}
	}

	/**
	 * @return the termId
	 */
	public String getTermId() {
		return termId;
	}

	/**
	 * @param termId
	 *            the termId to set
	 */
	public void setTermId(String termId) {
		this.termId = termId;
	}

	/**
	 * @return the termName
	 */
	public String getTermName() {
		return termName;
	}

	/**
	 * @param termName
	 *            the termName to set
	 */
	public void setTermName(String termName) {
		this.termName = termName;
	}

	/**
	 * @return the termType
	 */
	public String getTermType() {
		return termType;
	}

	/**
	 * @param termType
	 *            the termType to set
	 */
	public void setTermType(String termType) {
		this.termType = termType;
	}

	/**
	 * @return the termNotes
	 */
	public List<String> getTermNotes() {
		return termNotes;
	}

	/**
	 * @param termNotes
	 *            the termNotes to set
	 */
	public void setTermNotes(List<String> termNotes) {
		this.termNotes = termNotes;
	}

	/**
	 * @return the termRelations
	 */
	public List<TermRelation> getTermRelations() {
		return termRelations;
	}

	/**
	 * @param termRelations
	 *            the termRelations to set
	 */
	public void setTermRelations(List<TermRelation> termRelations) {
		this.termRelations = termRelations;
	}

	/**
	 * @return the usedForTermNames
	 */
	public List<String> getUsedForTermNames() {
		return usedForTermNames;
	}

	/**
	 * @param usedForTermNames
	 *            the usedForTermNames to set
	 */
	public void setUsedForTermNames(List<String> usedForTermNames) {
		this.usedForTermNames = usedForTermNames;
	}

	/**
	 * @return the broaderTermNames
	 */
	public List<String> getBroaderTermNames() {
		return broaderTermNames;
	}

	/**
	 * @param broaderTermNames
	 *            the broaderTermNames to set
	 */
	public void setBroaderTermNames(List<String> broaderTermNames) {
		this.broaderTermNames = broaderTermNames;
	}

	/**
	 * @return the narrowerTermNames
	 */
	public List<String> getNarrowerTermNames() {
		return narrowerTermNames;
	}

	/**
	 * @param narrowerTermNames
	 *            the narrowerTermNames to set
	 */
	public void setNarrowerTermNames(List<String> narrowerTermNames) {
		this.narrowerTermNames = narrowerTermNames;
	}

	/**
	 * @return the useTermNames
	 */
	public List<String> getUseTermNames() {
		return useTermNames;
	}

	/**
	 * @param useTermNames
	 *            the useTermNames to set
	 */
	public void setUseTermNames(List<String> useTermNames) {
		this.useTermNames = useTermNames;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("TERM: " + this.termName + "\n");
		buf.append("\tTerm ID: " + this.termId + "\n");
		buf.append("\tTerm type: " + this.termType + "\n");

		if (this.termNotes != null) {
			for (String termNode : this.termNotes) {
				buf.append("\tNote: " + termNode + "\n");
			}
		}

		buf.append("Related terms:\n");
		if (relatedTermNames != null)
			buf.append(relatedTermNames.toString() + "\n");

		buf.append("Used for terms:\n");
		if (usedForTermNames != null)
			buf.append(usedForTermNames.toString() + "\n");

		buf.append("Broader terms:\n");
		if (broaderTermNames != null)
			buf.append(broaderTermNames.toString() + "\n");

		buf.append("Narrower terms:\n");
		if (narrowerTermNames != null)
			buf.append(narrowerTermNames.toString() + "\n");

		buf.append("Use terms:\n");
		if (useTermNames != null)
			buf.append(useTermNames.toString() + "\n");

		return buf.toString();
	}

	public void setRelatedTermNames(List<String> relatedTermNames) {
		this.relatedTermNames = relatedTermNames;
	}

	public List<String> getRelatedTermNames() {
		return relatedTermNames;
	}
}
