package org.XXXX100.robust.mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.*
import org.XXXX100.robust.randomGenerator


class AddInstanceMutation(model: Model, verbose : Boolean) : Mutation(model, verbose) {
    private fun getCandidates(): List<String> {

        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n SELECT * WHERE { ?x a owl:Class. }"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        var ret = listOf<String>()
        for(r in res){
            val candidate = r.get("?x").toString()
            //first condition removes rdf: owl: and other standard prefixes, second condition matches on blank nodes
            if(candidate.startsWith("http://www.w3.org") || candidate.matches("(\\d|\\w){8}-(\\d|\\w){4}-(\\d|\\w){4}-(\\d|\\w){4}-(\\d|\\w){12}".toRegex()))
                continue
            ret = ret + candidate
        }
        return ret.sorted()
        // we do not filter, when we add stuff, only when we remove
        //return filterRelevantPrefixesString(ret.toList()).sorted()
    }

    override fun isApplicable(): Boolean {
        return hasConfig || getCandidates().isNotEmpty()
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleResourceConfiguration)
        super.setConfiguration(_config)
    }

    override fun createMutation() {
        val m = ModelFactory.createDefaultModel()
        val owlClass =
            if (hasConfig){
                assert(config is SingleResourceConfiguration)
                val c = config as SingleResourceConfiguration
                c.getResource().toString()
            }
            else
                getCandidates().random(randomGenerator)
        val instanceName =
            if (hasConfig && config is StringAndResourceConfiguration) {
                val c = config as StringAndResourceConfiguration
                c.getString()
            }
            else
                "inner:asd"+ randomGenerator.nextInt(0,Int.MAX_VALUE)
        // create new "type" relation for the individual and the selected class
        val s = m.createStatement(
            m.createResource(instanceName),
            m.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#","type"),
            m.createResource(owlClass))

        addSet.add(s)
        super.createMutation()
    }
}

open class RemoveObjectPropertyMutation(model: Model, verbose : Boolean) : RemoveAxiomMutation(model, verbose) {
    override fun getCandidates(): List<Statement> {
        val allProps = allOfType(objectProp)
        val candidates : MutableSet<Statement> = hashSetOf()
        allProps.forEach {
            candidates.addAll(model.listStatements(
                null as Resource?, model.getProperty(it.toString()), null as RDFNode?
            ).toSet())
        }
        return filterMutatableAxioms(candidates.toList()).sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleResourceConfiguration)
        when (_config) {
            is SingleResourceConfiguration -> {
                // select a random element to delete
                val p = model.getProperty(_config.getResource().toString())
                val cand = model.listStatements(null as Resource?, p, null as RDFNode?).toSet()

                val axiom =
                    if (cand.any())
                        cand.random(randomGenerator)
                    else {
                        if (verbose)
                            println("no relation with this property exists / can be deleted")
                        emptyAxiom
                    }
                super.setConfiguration(
                    SingleStatementConfiguration(axiom)
                )
            }
            is SingleStatementConfiguration -> {
                super.setConfiguration(_config)
            }
        }
    }
}

open class AddObjectPropertyMutation(model: Model, verbose: Boolean) : AddRelationMutation(model, verbose) {
    override fun getCandidates() : List<Resource> {
        val cand = ArrayList<Resource>()
        val l = model.listStatements().toList()
        for (s in l) {
            if (s.predicate == typeProp && s.`object` == objectProp)
                cand.add(s.subject)
        }
        return cand.sortedBy { it.toString() }
        // we do not filter, when we add stuff, only when we remove
        //return filterRelevantPrefixesResource(cand.toList()).sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        val p =
            when (_config) {
                is SingleResourceConfiguration -> {
                    _config.getResource()
                }

                is SingleStatementConfiguration -> {
                    _config.getStatement().predicate
                }

                else -> model.createResource()
            }

        // check that p is really an ObjectProperty, if it exists in the model at all
        if (model.listResourcesWithProperty(null ).toSet().contains(p.asResource()))
            if (!isOfType(p.asResource(), objectProp)) {
                println("WARNING: resource $p is not an object property but is used as such in configuration.")
            }

        super.setConfiguration(_config)
    }
}

open class ChangeObjectPropertyMutation(model: Model, verbose: Boolean) : AddObjectPropertyMutation(model, verbose) {
    override fun getCandidates() : List<Resource> {
        val cand =  super.getCandidates()
        return cand
        return filterMutatableAxiomsResource(cand.toList()).sortedBy { it.toString() }
    }

    override fun createMutation() {
        // create the mutation as usual (i.e. adding a new triple)
        super.createMutation()

        // find existing relations and remove them
        turnAdditionsToChanges()
    }
}

class RemoveIndividualMutation(model: Model, verbose : Boolean) : RemoveNodeMutation(model, verbose) {
    override fun getCandidates(): List<Resource> {
        val l = model.listStatements().toList().toMutableList()
        val candidates = ArrayList<Resource>()
        for (s in l) {
            // select statements that are not subClass relations
            if (s.predicate == typeProp && s.`object` == namedInd) {
                candidates.add(s.subject)
            }
        }
        return filterMutatableAxiomsResource(candidates).sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleResourceConfiguration)
        // assert that the resource is really an individual
        val ind = (_config as SingleResourceConfiguration).getResource()
        assert(isOfType(ind, namedInd))
        super.setConfiguration(_config)
    }
}


class ChangeDataPropertyMutation(model: Model, verbose: Boolean) : ChangeRelationMutation(model, verbose) {
    override fun getCandidates() : List<Resource> {
        val cand =  super.getCandidates()
        // only select data properties
        val newCand =  cand.filter { isOfInferredType(it, dataProp)}
        return newCand
    }
}
