/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;

import au.com.langdale.inference.LOG;
import au.com.langdale.inference.RepairFunctors;
import au.com.langdale.inference.RepairFunctors.RepairAction;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.ResourceFactory;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * This is a model that represents a diagnosis graph as a tree.  
 * A diagnosis graph is an RDF document containing diagnostic
 * messages.
 */
public class DiagnosisModel extends JenaTreeModelBase {

	public DiagnosisModel() {
		this.repairs = new RepairMan();
	}

	public DiagnosisModel(RepairMan repairs) {
		this.repairs = repairs;
	}

	public RepairMan getRepairs() {
		return repairs;
	}

	public void setRepairs(RepairMan repairs) {
		this.repairs = repairs;
	}

	public static FrontsNode DIAGNOSIS_ROOT = ResourceFactory.createResource("http://langdale.com.au/2007/Diagnosis#root");
	public static FrontsNode DIAGNOSIS_GENERAL = ResourceFactory.createResource("http://langdale.com.au/2007/Diagnosis#general");
	/**
	 * The root of the diagnosis tree.
	 */
	public class RootNode extends Node {

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return asOntResource(DIAGNOSIS_ROOT);
		}

		@Override
		protected void populate() {
			ResIterator jt = getOntModel().listSubjectsWithProperty(LOG.hasProblems);
			while(jt.hasNext()) {
				OntResource subject = jt.nextResource();
				add( new KeyNode(asOntResource(subject)));
			}
			
			GeneralNode general = new GeneralNode();
			if( general.iterator().hasNext())
				add( general );
		}

	}
	/**
	 * A node to group diagnostics that are not attached to any specific subject.
	 */
	public class GeneralNode extends ModelNode {

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return asOntResource(DIAGNOSIS_GENERAL);
		}

		@Override
		protected void populate() {
			ResIterator it = getOntModel().listSubjectsWithProperty(RDF.type, LOG.Problem);
			while (it.hasNext()) {
				OntResource problem = it.nextResource();
				ResIterator jt = getOntModel().listSubjectsWithProperty(LOG.hasProblems, problem);
				if( ! jt.hasNext())
					add( new DetailNode(getSubject(), asOntResource(problem)));
			}
		}

		@Override
		protected String collation() {
			return "~";
		}

		@Override
		public String toString() {
			return "General Diagnostics";
		}
		
	}
	
	/**
	 * A node that groups diagnostics attached to a specific subject.  
	 */
	public class KeyNode extends ModelNode {
		
		private OntResource subject;
		
		public KeyNode(OntResource subject) {
			this.subject = subject;
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return subject;
		}

		@Override
		protected void populate() {
			ResIterator it = subject.listProperties(LOG.hasProblems);
			while (it.hasNext()) {
				OntResource node = it.nextResource();
				add( new DetailNode(subject, asOntResource(node)));
			}
		}
	}
	
	/**
	 * A node representing a single diagnostic message.
	 */
	public class DetailNode extends ModelNode {
		private OntResource subject, problem;
		
		public DetailNode(OntResource subject, OntResource problem) {
			this.subject = subject;
			this.problem = problem;
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return problem;
		}

		@Override
		public OntResource getBase() {
			return subject;
		}

		@Override
		protected void populate() {
			populate(LOG.hasRepairs);
		}

		private void populate(Property prop) {
			ResIterator it = problem.listProperties(prop);
			boolean prefer = true;
			while (it.hasNext()) {
				OntResource node = it.nextResource();
				add( new RepairNode(subject, asOntResource(node), prefer));
				prefer = false;
			}
		}
		
		@Override
		public String getName() {
			return problem.getComment();
		}

		@Override
		public String toString() {
			return problem.getComment();
		}

		public String getDescription() {
			return problem.getString(LOG.problemDetail);
		}
	}
	
	/**
	 * A node representing a single diagnostic message.
	 */
	public class RepairNode extends ModelNode {
		private OntResource subject,repair;
		private boolean prefer;
		private RepairAction action;
		
		public RepairNode(OntResource subject, OntResource repair, boolean prefer) {
			this.subject = subject;
			this.repair = repair;
			action = RepairFunctors.getAction(repair);
			this.prefer = prefer && action.getPriority() <= 5;
		}
		
		public boolean isPreferred() {
			return prefer;
		}
		
		public boolean isSelected() {
			return repairs.hasAction(action);
		}
		
		public void setSelected(boolean state) {
			if( state ) {
				repairs.addAction(action);
			}
			else {
				repairs.removeAction(action);
			}
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return repair;
		}

		@Override
		public OntResource getBase() {
			return subject;
		}

		@Override
		protected void populate() {
			// no children
		}

		@Override
		public boolean getAllowsChildren() {
			return false;
		}
		
		@Override
		public String getName() {
			return repair.getComment();
		}

		@Override
		public String toString() {
			return repair.getComment();
		}

		public String getDescription() {
			return action.getDescription();
		}
		
		@Override
		protected String collation() {
			return action.getPriority() + repair.getComment();
		}
	}
	
	private RepairMan repairs;
	
	@Override
	protected Node classify(OntResource root) {
		// repairs.clear();
		if( root.equals(DIAGNOSIS_ROOT))
			return new RootNode();
		else
			return null;
	}
}
