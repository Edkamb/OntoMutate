package org.XXXX100.robust.domainSpecific.geo

import org.XXXX100.robust.mutant.*
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.XXXX100.robust.randomGenerator

class GeoTestCaseGenerator(val verbose: Boolean) : TestCaseGenerator(verbose) {

    private val geoOntoPath = "sut/geo/total_mini.ttl"
    fun generateGeoMutants(numberMutants : Int,
                           numberOfMutations : Int,
                           mask: RobustnessMask,
                           mutantsName: String,
                           saveMutants : Boolean) {
        val seed = RDFDataMgr.loadDataset(geoOntoPath).defaultModel

        val mutationNumbers = listOf(numberOfMutations)
        for (i in mutationNumbers) {
            val geoMutator = GeoMutatorFactory(verbose, i)
            super.generateMutants(
                seed,
                mask,
                geoMutator,
                numberMutants
            )
        }
        if (saveMutants) {
            saveMutants("sut/geo/mutatedOnt", mutantsName)
            super.writeToCSV("sut/geo/mutatedOnt/" + mutantsName + ".csv")
        }
    }
}

class GeoMutatorFactory(verbose: Boolean, private val NumberMutations: Int): MutatorFactory(verbose) {

    private val domainIndependentMutations = listOf(
        CEUAMutation::class,
        CEUOMutation::class,
        ChangeDataPropertyMutation::class,  // test also, if we could replace with other datatype
        ChangeDoubleMutation::class,        // also targets T-Box
        ACATOMutation::class,
        ACOTAMutation::class,
        ToSiblingClassMutation::class,
    )

    override fun randomMutator(): Mutator {
        val ms = MutationSequence(verbose)

        for (i in 1..NumberMutations)
            ms.addRandom(domainIndependentMutations.random(randomGenerator))

        return Mutator(ms, verbose)
    }
}

