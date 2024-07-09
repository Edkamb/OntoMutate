package org.XXXX100.robust.sut

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.RDFDataMgr
import java.io.File

/**
 * This is an implementation of the *program* in the running example in the paper
 */
class MiniPipeInspection {

    var ontology : Model? = null
    private val mf = ModelFactory.createDefaultModel()!!

    private val infraClass = mf.createResource("http://www.anon.anon/miniPipes#Infrastructure")!!


    private fun move (thing: Resource, goal : Resource) {
        // "delete all old positions, set new position"
        val isAtProp = mf.createProperty("http://www.anon.anon/miniPipes#isAt")
        val r = MyReasoner(ontology as Model)

        val newOntology = ModelFactory.createDefaultModel()
        r.getInf().listStatements().forEach {
            if (!(it.subject.equals(thing) && it.predicate.equals(isAtProp)))
                newOntology.add(it)
        }

        val s = newOntology.createStatement(
            thing,
            isAtProp,
            goal
        )

        newOntology.add(s)
        ontology = newOntology
    }

    private fun nextTo(thing: Resource) : Set<Resource> {

        // "collect all things that are connected via 'nextTo' "
        var ret = setOf<Resource>()
        val isNextProp = mf.createResource("http://www.anon.anon/miniPipes#nextTo")
        val r = MyReasoner(ontology as Model)
        for (axiom in r.allOutgoingRelations(thing))
            if (axiom.predicate.equals(isNextProp))
                ret = ret + mf.createResource(axiom.`object`.toString())

        return ret
    }

    private fun isAt(thing: Resource) : Resource? {
        // "returns the FIRST thing you find where the thing isAt"
        val isAtProp = mf.createResource("http://www.anon.anon/miniPipes#isAt")
        val r = MyReasoner(ontology as Model)
        for (axiom in r.allOutgoingRelations(thing))
            if (axiom.predicate.equals(isAtProp))
                return mf.createResource(axiom.`object`.toString())
        return null
    }

    private fun visited(thing: Resource) : Boolean {
        val hasStatus = mf.createProperty("http://www.anon.anon/miniPipes#hasStatus")
        val visited = mf.createResource("http://www.anon.anon/miniPipes#visited")
        val r = MyReasoner(ontology as Model)

        for (axiom in r.allOutgoingRelations(thing))
            if (axiom.predicate.equals(hasStatus) && axiom.`object`.equals(visited))
                return true

        return false
    }

    private fun visited(thing: Resource, r : MyReasoner) : Boolean {
        val hasStatus = mf.createProperty("http://www.anon.anon/miniPipes#hasStatus")
        val visited = mf.createResource("http://www.anon.anon/miniPipes#visited")

        for (axiom in r.allOutgoingRelations(thing))
            if (axiom.predicate.equals(hasStatus) && axiom.`object`.equals(visited))
                return true

        return false
    }

    private fun inspect(thing: Resource) {
        val hasStatus = mf.createProperty("http://www.anon.anon/miniPipes#hasStatus")
        val visited = mf.createResource("http://www.anon.anon/miniPipes#visited")
        ontology?.add( mf.createStatement(
            thing,
            hasStatus,
            visited
        ))

        println(thing.localName + " gets inspected")
    }
    private fun notAnimal(thing: Resource) : Boolean {
        val animalClass = mf.createResource("http://www.anon.anon/miniPipes#Animal")
        val r = MyReasoner(ontology as Model)
        return !r.allIndividuals(animalClass).contains(thing)
    }

    fun infrastructure(thing: Resource) : Boolean {
        val r = MyReasoner(ontology as Model)
        return r.allIndividuals(infraClass).contains(thing)
    }

    fun allInfrastructure() : List<Resource> {
        val r = MyReasoner(ontology as Model)
        return r.allIndividuals(infraClass)
    }

    fun allInfrastructureInspected() : Boolean {
        val r = MyReasoner(ontology as Model)
        val I = r.allIndividuals(infraClass)


        for (i in I)
            if (!visited(i, r))
                return false

        return true
    }

    fun readOntology(file : File) {
        ontology = RDFDataMgr.loadDataset(file.absolutePath).defaultModel
    }

    fun readOntology(ont : Model) {
        ontology = ont
    }



    fun doInspection() {
        // "implement inspection agorithm"
        println("\nWohooo! Let's start a new inspection of the infrastructure.")
        val auv = mf.createResource("http://www.anon.anon/miniPipes#auv")
        val position = isAt(auv)
        if (position == null) {
            println("ERROR: position unknown. I can not inspect anything.")
            return
        }


        inspect(position)
        var cand = nextTo(position).toMutableSet()
        while (cand.any())  {
            val newPosition = cand.random()
            cand.remove(newPosition)
            // only move to new position if it is not marked as visited and if it is not an animal
            if (!visited(newPosition) && notAnimal(newPosition)) {
                println("AUV moves from "+ position.localName + " to " + newPosition.localName)
                move(auv, newPosition)
                inspect(newPosition)
                cand = nextTo(newPosition).toMutableSet()
            }
        }

        println("We are done for today: the inspection is completed.")

    }


}
