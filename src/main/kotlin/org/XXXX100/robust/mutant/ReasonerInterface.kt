package org.XXXX100.robust.mutant

import openllet.atom.OpenError
import openllet.owlapi.OpenlletReasoner
import openllet.owlapi.OpenlletReasonerFactory
import org.apache.jena.rdf.model.InfModel
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Statement
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.semanticweb.HermiT.ReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.OWLReasoner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

enum class ReasoningBackend {
    HERMIT, OPENLLET, JENA
}


class CustomReasonerFactory(private val verbose: Boolean, private val reasoningBackend: ReasoningBackend) {
    fun getReasoner(model: Model) : CustomReasoner =
        when(reasoningBackend) {
            ReasoningBackend.OPENLLET -> CustomOpenlletReasoner(model, verbose)
            ReasoningBackend.HERMIT -> CustomHermitReasoner(model, verbose)
            ReasoningBackend.JENA -> CustomJenaApiReasoner(model, verbose)
        }
}

class CustomJenaApiReasoner(val model: Model, verbose : Boolean) : CustomReasoner(model, verbose) {
    private val reasoner: Reasoner = ReasonerRegistry.getOWLReasoner()
    private val inf : InfModel = ModelFactory.createInfModel(reasoner, model)

    override fun isConsistent(): Boolean {
        var consistent = true
        try {
            val validityReport = inf.validate()
            if (!validityReport.isValid) {
                for (reason in validityReport.reports) {
                    // ignore errors from range check, they do not work correctly for data properties with explicit range
                    // TODO: dive deeper into this problem and figure out how to solve it
                    if (reason.type.toString() != "\"range check\"") {
                        consistent = false
                    }
                }
            }
        } catch (e : Exception) {
            if (verbose)
                println("Exception in validation: $e --> consider as inconsistent")
            // validation failed --> play safe and assume ontology is inconsistent
            consistent = false
        }
        return consistent
    }

    override fun entailsAll(jenaModel: Model): Boolean {
        return inf.containsAll(jenaModel)
    }

    override fun containsAll(jenaModel: Model): Boolean {
        return model.containsAll(jenaModel.listStatements())
    }


}

class CustomHermitReasoner(jenaModel: Model,
                           verbose :Boolean) : OwlApiReasoner(jenaModel, verbose) {
    override fun initReasoner(): OWLReasoner? {
        return ontology?.let { ReasonerFactory().createReasoner(it) }
    }
}

class CustomOpenlletReasoner(jenaModel: Model,
                             verbose : Boolean) : OwlApiReasoner(jenaModel, verbose) {
    override fun initReasoner(): OpenlletReasoner? {
        return ontology?.let { OpenlletReasonerFactory.getInstance().createReasoner(it) }
    }
}

abstract class CustomReasoner(private val jenaModel : Model,
                              val verbose : Boolean) {

    abstract fun isConsistent() : Boolean

    abstract fun entailsAll(jenaModel: Model) : Boolean

    abstract fun containsAll(jenaModel: Model): Boolean

    fun entails(s : Statement) : Boolean {
        // treat statement as a model and check that for entailment
        val m : Model = ModelFactory.createDefaultModel()
        m.add(s)
        return entailsAll(m)
    }
}

abstract class OwlApiReasoner(jenaModel : Model,
                              verbose: Boolean) : CustomReasoner(jenaModel, verbose) {

    private val manager: OWLOntologyManager = OWLManager.createOWLOntologyManager()
    val ontology = owlApiOnt(jenaModel)
    private val reasoner : OWLReasoner? by lazy { initReasoner()}

    abstract fun initReasoner() : OWLReasoner?

    override fun isConsistent(): Boolean {
        return try {
            reasoner?.isConsistent ?: false
        }
        catch (e : Exception) {
            // treat ontologies that result in exception of reasoner as inconsistent
            when ( e) {
                is OpenError, is ClassCastException -> {
                    //known exeptions
                    false
                }
                else -> throw e
            }
        }
    }

    override fun entailsAll(jenaModel: Model): Boolean {
        // inconsistent ontology entails everything
        if (!isConsistent()) {
            return true
        }

        val entailedOntology = owlApiOnt(jenaModel)
        if (entailedOntology != null) {
            return reasoner?.isEntailed(entailedOntology.axioms) ?: false
        }
        return false
    }

    override  fun containsAll(jenaModel: Model): Boolean {
        val containedOntology = owlApiOnt(jenaModel)
        if (containedOntology != null) {
            for (a in containedOntology.axioms)
                if (ontology?.containsAxiom(a) != true)
                    return false
        }
        return true
    }

    // converts Jena model into an OWL-API ontology
    private fun owlApiOnt(jenaModel: Model) : OWLOntology? {

        // TODO: get this direct conversion running, seems to be some problem with Maven dependencies...
        // use ONT-API to pass Jena model to OWL API
        //var manager: OntologyManager = OntManagers.createManager()
        //var ontology: Ontology = manager.addOntology(jenaModel.graph)

        // temporary solution: write ontology to stream using Jena API
        // and read from stream with OWL API
        // problem: probably rather slow...
        val ontology: OWLOntology?

        ByteArrayOutputStream().use { outStream ->
            RDFDataMgr.write(outStream, jenaModel, Lang.RDFXML)
            ByteArrayInputStream(outStream.toByteArray()).use { inStream ->
                ontology =
                    try {
                        manager.loadOntologyFromOntologyDocument(inStream)
                    }
                    catch (e : Exception){
                        if (verbose)
                            println("reasoner could not read ontolgy because of exception $e")
                        null
                    }
            }
        }
        return ontology
    }

}