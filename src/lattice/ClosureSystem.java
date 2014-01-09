package lattice;

/*
 * ClosureSystem.java
 *
 * last update on December 2013
 *
 */
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import dgraph.DAGraph;
import dgraph.DGraph;
import dgraph.Node;
/**
 * This class is an abstract class defining the common behavior of closure systems,
 * and specialy its closed set lattice generation.
 *
 * Both a context and an implicational system have properties of a closure system,
 * and therefore extend this class.
 *
 * A closure system is formaly defined by a set of indexed elements and a closure operator
 * (abstract methods `getSet()` and `closure`). 
 * Abstract method  `toFile (String file)` also describe 
 * the common behavior of a closure system.
 *
 * However, this abstract class provides both abstract and non abstract methods. 
 * Although abstract methods depends on data, and so have to be implemented by each extended class, 
 * non abstract methods only used property of a closure system. It is the case for methods 
 * `nextClosure` (that computes the next closure of the specified one according to the lectic order
 * implemented the well-known Wille algorithm)
 * invoked by method `allClosure`
 * and the main method `closetSetLattice(boolean diagram)` (where lattice can be transitively closed or reduced).
 *
 * Copyright: 2013 University of La Rochelle, France
 *
 * License: http://www.cecill.info/licences/Licence_CeCILL-B_V1-en.html CeCILL-B license
 *
 * This file is part of lattice, free package. You can redistribute it and/or modify
 * it under the terms of CeCILL-B license.
 *
 * @author Karell Bertet
 * @version 2013
 */
public abstract class ClosureSystem {

	/* ------------- ABSTRACT METHODS ------------------ */

    /** Returns the set of elements of the closure system */
    public abstract TreeSet<Comparable> getSet () ;

    /** Returns the closure of the specified set */
    public abstract TreeSet<Comparable> closure (TreeSet<Comparable> X) ;

    /** Saves this component in a file which name is specified **/
    public abstract void toFile (String file) ;

	/* ------------- IMPLEMENTED METHODS ------------------ */

    /** Returns the closed set lattice of this component.
    *
    * A true value of the boolean `diagram` indicates that the
     * Hasse diagramm of the lattice is computed (i.e. it is transitively reduced),
     * whereas a false value indicates that the lattice is transitively closed
     *
     * A transitively reduced lattice is generated by the static method
     * `ConceptLattice diagramLattice (ClosureSystem init)`
     * that implements an adaptation of Bordat's algorithm.
     * This adaptation computes the dependance graph while the lattice is generated,
     * with the same complexity.
     *
     * A transitively closed lattice is generated bye well-known Next Closure algorithm.
     * In this case, the dependance graph of the lattice isn't computed.
     *
     * @param diagram a boolean indicating if the Hasse diagramm of the lattice is computed or not.
	 **/
	public ConceptLattice closedSetLattice (boolean  diagram) {
        if (diagram)
            return ConceptLattice.diagramLattice(this);
        else
            return ConceptLattice.completeLattice(this);
	}


   /** Returns all the closed sets of the specified closure system
    * (that can be an IS or a context).
    *
	* Closed sets are generated in lecticaly order, with the emptyset's closure
	* as first closed set, using the Ganter's Next Closure algorithm.
	*
    *  Therefore,
     * closed sets have to be comparable using `ComparableSet` class.
     * This treatment is performed in O(cCl|S|^3) where S is the initial set of elements,
     * c is the number of closed sets that could be exponential in the worst case,
     * and Cl is the closure computation complexity.
    *
	* @return all the closeds set in the lectically order.
	*/
	public Vector<Concept> allClosures (){
		Vector<Concept> allclosure = new Vector<Concept>();
		// first closure: closure of the empty set
		allclosure.add(new Concept(this.closure(new ComparableSet()),false));
		Concept cl = allclosure.firstElement();
		// next closures in lectcally order
		boolean continu = true;
        do {
            cl = this.nextClosure(cl);
            if (allclosure.contains(cl)) continu=false;
            else allclosure.add(cl);
            } while (continu);

        return allclosure;
	}

	/** Returns the lecticaly next closed set of the specified one.
	*
	* This treatment is an implementation of the best knowm algorithm of Wille
	* whose complexity is in O(Cl|S|^2), where S is the initial set of elements,
     * and Cl is the closure computation complexity.
	* @return the lecticaly next closed set
	*/
	public Concept nextClosure (Concept cl) {
		TreeSet<Comparable> S = new TreeSet(this.getSet());
		boolean success = false;
		TreeSet A = new TreeSet(cl.getSetA());
		Comparable ni = S.last();
		do {
			ni = (Comparable)S.last();
			S.remove(ni);
			if (!A.contains(ni)) {
				A.add(ni);
				TreeSet B = this.closure(A);
				B.removeAll(A);
				if (B.isEmpty() || ((Comparable)B.first()).compareTo(ni)>=1) {
					A = this.closure(A);
					success = true;
				}
                else A.remove(ni);
			}
            else A.remove(ni);
		} while (!success && ni.compareTo(this.getSet().first())>=1);
		return new Concept(A,false);
	}


   /** Returns the precedence graph of this component.
   *
	* Nodes of the graph are elements of this component.
	* There is an edge from element a to element b when
    * b belongs to the closure of a.
    * When precedenc graph is acyclcic, then this component is a reduced one.    
	*/
	public DGraph precedenceGraph () {
        // compute a TreeMap of closures for each element of the component
        TreeMap<Comparable,TreeSet<Comparable>> Closures = new TreeMap<Comparable,TreeSet<Comparable>>();
	for (Comparable x : this.getSet()) {
            ComparableSet setX = new ComparableSet();
            setX.add(x);        
            Closures.put(x, this.closure(setX));
        }
        // nodes of the graph are elements
	DGraph prec = new DGraph();
        TreeMap<Comparable,Node> nodeCreated = new TreeMap<Comparable,Node>();
	for (Comparable x : this.getSet()) {
            Node n = new Node(x);
            prec.addNode (n);
            nodeCreated.put(x, n);
        }        
        // edges of the graph are closures containments
        for (Comparable from : this.getSet())
            for (Comparable to : this.getSet())
               if (!from.equals(to)) {
                // check if from belongs to the closure of to                
                if (Closures.get(to).contains(from))
                    prec.addEdge(nodeCreated.get(from), nodeCreated.get(to));
               }
        return prec;
	}
        
/*** 
* This function returns all reducible elements. 
* A reducible elements is equivalent by closure to one or more other attributes.
* Reducible elements are computed using the precedence graph of the closure system.
* Complexity is in O()
* @return The map of reduced attributes with their equivalent attributes
*/ 
public TreeMap<Object, TreeSet> getReducibleElements() {
        // Initialize a map Red of reducible attributes 
        TreeMap<Object, TreeSet> Red = new TreeMap();	
	// Initialize the precedence graph G of the closure system
	DGraph G = this.precedenceGraph();                
	// First, compute each group of equivalent attributes          
	// This group will be a strongly connected component on the graph. 
        // Then, only one element of each group	is skipped, others will be deleted. 
	DAGraph CFC = G.stronglyConnectedComponent();                
	for (Node C : CFC.getNodes()) {             
            // Get list of node of this component             
            TreeSet<Node> sCC = (TreeSet) C.content; 
            if (sCC.size() > 1) {    
		Node y = sCC.first(); 
		TreeSet yClass = new TreeSet ();
                yClass.add(y.content);
		for (Node x : sCC) 
			if (!x.content.equals(y.content)) {                             			                         
                            G.removeNode(x);
                            Red.put(x.content, yClass);                                    
			}                 
            }
        }     
        // Next, check if an attribute is equivalent to emptyset
        // i.e. its closure is equal to emptyset closure
        TreeSet <Node> sinks = G.sinks();
        if (sinks.size()==1) {
            Node s = sinks.first();
            Red.put (s.content, new TreeSet());
            G.removeNode(s);
        }        
	// Finaly, checking a remaining attribute equivalent to its predecessors or not may reduce more attributes. 
	// Check all remaining nodes of graph G 
	for (Node x : G.getNodes()) { 	            
            TreeSet<Node> P = G.getNodesPred(x);	                
            if (P.size() > 1) { 
                // Create the closure of x 
		TreeSet X = new TreeSet();
                X.add(x.content);
                TreeSet closureX = this.closure(X);                                                                        			
		// Create the closure of P 						                        
                TreeSet<Comparable> Pred = new TreeSet <Comparable> ();
                for (Node n : P)
                    Pred.add((Comparable)n.content);    
                    TreeSet <Comparable> closureP = this.closure(Pred);                    
                    // Check the equality of two closures 
                    if (closureX.containsAll(closureP) && closureP.containsAll(closureX))
                        Red.put(x.content,Pred);                    
		} 
	} 
	// Finally, return the list of reducible elements with their equivalent attributes. 
	return Red; 
    } 
}// end of ClosureSystem
