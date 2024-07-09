package org.XXXX100.robust.mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Statement


//removes one (random) subclass axiom       // val m = Mutator
// only overrides the method to select the candidate axioms from super class
class RemoveSubclassMutation(model: Model, verbose : Boolean) : RemoveAxiomMutation(model, verbose) {
    override fun getCandidates(): List<Statement> {
        val l = model.listStatements().toList()
        val candidates = l.toMutableSet()
        for (s in l) {
            // select statements that are not subClass relations
            if (!s.predicate.toString().matches(".*#subClassOf$".toRegex())) {
                candidates.remove(s)
            }
        }
        return filterMutatableAxioms(candidates.toList()).sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleStatementConfiguration)
        val c = _config as SingleStatementConfiguration
        assert(c.getStatement().predicate.toString().matches(".*#subClassOf$".toRegex()))
        super.setConfiguration(_config)
    }
}

// removes one part of an "AND" in a logical axiom
class CEUAMutation(model: Model, verbose: Boolean): ReplaceNodeInAxiomMutation(model, verbose)   {
    // selects names of nodes that should be removed / replaced by owl:Thing
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT * WHERE { " +
                "?x owl:intersectionOf ?b. " +
                "?b (rdf:rest)* ?a." +
                "?a rdf:first ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for(r in res){
            val y = r.get("?y")
            val a = r.get("?a")
            val axiom = model.createStatement(a.asResource(),rdfFirst, y)
            //println("$a $y $axiom")
            ret += DoubleStringAndStatementConfiguration(
                y.toString(),
                owlThing.toString(),
                axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        if (_config is DoubleStringAndStatementConfiguration)
            super.setConfiguration(_config)
        else {
            assert(_config is SingleStatementConfiguration)
            val con = _config as SingleStatementConfiguration
            val c = DoubleStringAndStatementConfiguration(
                con.getStatement().`object`.toString(),
                owlThing.toString(),
                con.getStatement()
            )

            super.setConfiguration(_config)
        }
    }
}

// removes one part of an "OR" in a logical axiom
class CEUOMutation(model: Model, verbose: Boolean): ReplaceNodeInAxiomMutation(model, verbose)   {

    // selects names of nodes that should be removed / replaced by owl:Nothing
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT * WHERE { " +
                "?x owl:unionOf ?b. " +
                "?b (rdf:rest)* ?a." +
                "?a rdf:first ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for(r in res){
            val y = r.get("?y")
            val a = r.get("?a")
            val axiom = model.createStatement(a.asResource(),rdfFirst, y)
            //println("$a $y $axiom")
            ret += DoubleStringAndStatementConfiguration(
                y.toString(),
                owlNothing.toString(),
                axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        if (_config is DoubleStringAndStatementConfiguration)
            super.setConfiguration(_config)
        else {
            assert(_config is SingleStatementConfiguration)
            val con = _config as SingleStatementConfiguration
            val c = DoubleStringAndStatementConfiguration(
                con.getStatement().`object`.toString(),
                owlNothing.toString(),
                con.getStatement()
            )

            super.setConfiguration(_config)
        }
    }

}

// replace "AND" by "OR"
class ACATOMutation(model: Model, verbose: Boolean): ReplaceNodeInAxiomMutation(model, verbose) {

    // selects names of nodes that should be removed / replaced by owl:Thing
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "SELECT * WHERE { " +
                "?x owl:intersectionOf ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for (r in res) {
            val x = r.get("?x")
            val y = r.get("?y")
            val axiom = model.createStatement(x.asResource(), intersectionProp, y)
            //println("$a $y $axiom")
            ret += DoubleStringAndStatementConfiguration(
                intersectionProp.toString(),
                unionProp.toString(),
                axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        if (_config is DoubleStringAndStatementConfiguration)
            super.setConfiguration(_config)
        else {
            assert(_config is SingleStatementConfiguration)
            val con = _config as SingleStatementConfiguration
            val c = DoubleStringAndStatementConfiguration(
                intersectionProp.toString(),
                unionProp.toString(),
                con.getStatement()
            )

            super.setConfiguration(_config)
        }
    }
}


// replace "Or" by "And"
class ACOTAMutation(model: Model, verbose: Boolean): ReplaceNodeInAxiomMutation(model, verbose) {
    // selects names of nodes that should be removed / replaced by owl:Thing
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "SELECT * WHERE { " +
                "?x owl:unionOf ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for (r in res) {
            val x = r.get("?x")
            val y = r.get("?y")
            val axiom = model.createStatement(x.asResource(), unionProp, y)
            //println("$a $y $axiom")
            ret += DoubleStringAndStatementConfiguration(
                unionProp.toString(),
                intersectionProp.toString(),
                axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        if (_config is DoubleStringAndStatementConfiguration)
            super.setConfiguration(_config)
        else {
            assert(_config is SingleStatementConfiguration)
            val con = _config as SingleStatementConfiguration
            val c = DoubleStringAndStatementConfiguration(
                unionProp.toString(),
                intersectionProp.toString(),
                con.getStatement()
            )
            super.setConfiguration(_config)
        }
    }

}


// replaces arguments in
class ToSiblingClassMutation(model: Model, verbose: Boolean): ReplaceNodeInAxiomMutation(model, verbose) {

    // selects names of classes
    // TODO: also in other parts of logical axioms, e.g. after restrictions
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        // withing union or intersection
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT DISTINCT ?a ?c ?cSibling WHERE { " +
                "?x (owl:unionOf | owl:intersectionOf) ?y. " +
                "?b (rdf:rest)* ?a." +
                "?a rdf:first ?c. " + // find class in union / intersection
                "?c rdf:type owl:Class." +
                "?c rdfs:subClassOf ?cSuper." +
                "?cSibling rdfs:subClassOf ?cSuper." +
                "}"

        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for (r in res) {
            val a = r.get("?a")
            val c = r.get("?c")
            val cSibling = r.get("?cSibling")
            if (c.toString() != cSibling.toString()) { // check, if entities different
                val axiom = model.createStatement(a.asResource(), rdfFirst, c)
                //println("$c $cSibling $axiom")
                ret += DoubleStringAndStatementConfiguration(
                    c.toString(),
                    cSibling.toString(),
                    axiom)
            }
        }

        // after existential quantification
        val queryString2 = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT DISTINCT ?r ?c ?cSibling WHERE { " +
                "?r owl:someValuesFrom ?c. " + // find class after existential quantification
                "?r rdf:type owl:Restriction. " +
                "?c rdf:type owl:Class." +
                "?c rdfs:subClassOf ?cSuper. " +
                "?cSibling rdfs:subClassOf ?cSuper." +
                "}"
        val query2 = QueryFactory.create(queryString2)
        val res2 = QueryExecutionFactory.create(query2, model).execSelect()
        for (r in res2) {
            val restriction = r.get("?r")
            val c = r.get("?c")
            val cSibling = r.get("?cSibling")
            //println("$c $cSibling $restriction")
            if (c.toString() != cSibling.toString()) { // check, if entities different
                val axiom = model.createStatement(restriction.asResource(),someValuesFromProp, c)
                //println("$c $cSibling $axiom")
                ret += DoubleStringAndStatementConfiguration(
                    c.toString(),
                    cSibling.toString(),
                    axiom)
            }
        }

        return ret.sortedBy { it.toString() }
    }

}
