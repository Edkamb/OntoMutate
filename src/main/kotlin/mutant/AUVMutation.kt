package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import kotlin.random.Random

// all the domain-dependent mutation operators that are specific for the auv domain
abstract class AUVMutation(model: Model, verbose: Boolean) : Mutation(model, verbose) {
    val auvURI = "http://www.ifi.uio.no/tobiajoh/miniPipes"
    val delimiter = "#"
    val pipeSegmentClass: Resource = model.createResource(auvURI + delimiter + "PipeSegment")

}

// the corresponding configurations for the mutations
interface AUVConfiguration

// a single resource (the start segment) can be contained in the configuration
class AddPipeSegmentConfiguration(start: Resource) : SingleResourceConfiguration(start), AUVConfiguration


class AddPipeSegmentMutation(model: Model, verbose: Boolean) : AUVMutation(model, verbose) {
    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is AddPipeSegmentConfiguration)
        super.setConfiguration(_config)
    }

    private fun getCandidates() : List<Resource> {
        var ret = listOf<Resource>()
        for (axiom in model.listStatements())
            if (axiom.`object`.equals(pipeSegmentClass) && axiom.predicate.equals(typeProp))
                ret = ret + axiom.subject
        return ret

    }
    override fun isApplicable(): Boolean {
        return hasConfig || getCandidates().any()
    }

    override fun createMutation() {
        //val m = ModelFactory.createDefaultModel()

        // select the start segment
        val start =
            if (hasConfig){
                assert(config is SingleResourceConfiguration)
                val c = config as SingleResourceConfiguration
                c.getResource()
            }
            else
                getCandidates().random()

        // create new individual of class "PipeSement" by usig the "AddInstance" mutation
        val nameNewSegment = auvURI + delimiter + "newPipeSegment"+Random.nextInt(0,Int.MAX_VALUE)

        val configAIM = StringAndResourceConfiguration(nameNewSegment, pipeSegmentClass)

        val aim = AddInstanceMutation(model, verbose)
        aim.setConfiguration(configAIM)
        val tempModel = aim.applyCopy()

        // mimic performed mutation
        mimicMutation(aim)

        // create "nexto" relation between start and the new individual

        val s = model.createStatement(
            start,
            model.createProperty(auvURI + delimiter + "nextTo"),
            model.createResource(nameNewSegment))
        val configAAM = SingleStatementConfiguration(s)
        val aam = AddAxiomMutation(tempModel, verbose)
        aam.setConfiguration(configAAM)
        aam.applyCopy()

        // mimic the mutations that happend
        mimicMutation(aim)
        mimicMutation(aam)

        super.createMutation()

    }

}