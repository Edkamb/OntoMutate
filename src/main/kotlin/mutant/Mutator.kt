package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.reasoner.ReasonerRegistry



class Mutator(private val mutSeq: MutationSequence, private val verbose: Boolean) {
    var globalMutation : Mutation? = null
    var ran = false

    fun mutate (seed : Model) : Model {
        ran = true
        globalMutation = Mutation(seed, verbose)
        var target = seed
        for (i  in 0 until mutSeq.size()) {
            val mutation = mutSeq[i].concreteMutation(target)
            if(mutation.isApplicable()) {
                target = mutation.applyCopy()
                globalMutation?.mimicMutation(mutation)
            }
        }
        return target
    }

    val affectedNodes : Set<Resource>
        get() {
            val nodes: MutableSet<Resource> = mutableSetOf()
            // collect all nodes that are mentioned in the mutations
            for (add in globalMutation?.addSet ?: mutableSetOf()) {
                nodes.add(add.subject)
                nodes.add(add.predicate.asResource())
                nodes.add(add.`object`.asResource())
            }
            for (add in globalMutation?.deleteSet ?: mutableSetOf()) {
                nodes.add(add.subject)
                nodes.add(add.predicate.asResource())
                nodes.add(add.`object`.asResource())
            }
            // collect all nodes that are mentioned in the mutations
            return nodes.toSet()
        }

    val affectedSeedNodes : Set<Resource>
        get() = affectedNodes.intersect((globalMutation?.allNodes() ?: mutableSetOf()).toSet())

    fun validate(model: Model, contract : Model) : Boolean{
        val reasoner = ReasonerRegistry.getOWLReasoner()
        val inf = ModelFactory.createInfModel(reasoner, model)
        return inf.containsAll(contract)
    }
}
