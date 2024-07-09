package org.XXXX100.robust.domainSpecific.suave

import org.XXXX100.robust.mutant.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.XXXX100.robust.randomGenerator
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class SuaveTestCaseGenerator(val verbose: Boolean) : TestCaseGenerator(verbose) {
    // maximal number of mutations to generate a mutant

    private val mrosRulesPath = "sut/suave/suave_ontologies/mros_rules.owl"
    private val tomasysRulesPath = "sut/suave/suave_ontologies/tomasys_rules.owl"
    //val suaveRulesPath = "sut/suave/suave_ontologies/suave_original_rules.owl"
    // start with loading the rules of the corresponding ontologies
    val mrosRulesModel = RDFDataMgr.loadDataset(mrosRulesPath).defaultModel!!
    //val mrosModel = ModelFactory.createDefaultModel()
    val tomasysRulesModel = RDFDataMgr.loadDataset(tomasysRulesPath).defaultModel!!
    //val suaveRulesModel = RDFDataMgr.loadDataset(suaveRulesPath).defaultModel

    // load ontology without rules, we do not want to mutate them
    private val mutatableSeedPath = "sut/suave/suave_ontologies/suave_mutatable.owl"
    private val unmutatableSeedPath = "sut/suave/suave_ontologies/suave_unmutatable.owl"
    private val mutatableStatements = RDFDataMgr.loadDataset(mutatableSeedPath).defaultModel.listStatements().toList()!!
    private val unmutatableSuaveStatements = RDFDataMgr.loadDataset(unmutatableSeedPath).defaultModel.listStatements().toList()!!


    private val mrosPath = "sut/suave/suave_ontologies/mros_no_import.owl"
    private val tomasysPath = "sut/suave/suave_ontologies/tomasys.owl"
    private val mrosModel = RDFDataMgr.loadDataset(mrosPath).defaultModel!!
    private val tomasysModel = RDFDataMgr.loadDataset(tomasysPath).defaultModel!!


    //TODO: this is not cleaned up, find out what the magic numbers mean first

    // returns the number of tries to generate the mutant
    fun generateSuaveMutants(numberMutants : Int,
                             numberOfMutations : Int,
                             ratioDomainSpecific: Double,
                             useAddQAMutation: Boolean,
                             mask : RobustnessMask,
                             mutantsName : String,
                             saveMutants : Boolean) : Int {
        val unmutatableStatements = unmutatableSuaveStatements.toMutableList()


        // add more statements to unmutatable part
        for (s in tomasysModel.listStatements())
            unmutatableStatements.add(s)
        for (s in mrosModel.listStatements())
            unmutatableStatements.add(s)

        // compute complete seed with all axioms
        val seed = ModelFactory.createDefaultModel()
        for (s in unmutatableStatements)
            seed.add(s)
        for (s in mutatableStatements)
            seed.add(s)

        val mutationNumbers = listOf(numberOfMutations)
        var tries = 0
        for (i in mutationNumbers) {
            val suaveMutator = SuaveMutatorFactory(
                verbose,
                mutatableStatements,
                i,
                ratioDomainSpecific,
                useAddQAMutation
            )
            val triesMutant = super.generateMutants(
                seed,
                mask,
                suaveMutator,
                numberMutants
            )
            tries += triesMutant
        }

        if (saveMutants) {
            saveMutants("sut/suave/mutatedOnt", mutantsName)
            super.writeToCSV("sut/suave/mutatedOnt/" + mutantsName + ".csv")
        }
        return tries
    }




     override fun saveMutants(folderName: String, filePrefix : String) {
        var i = 0

        // create folder, if necessary
        Files.createDirectories(Paths.get(folderName))
        for (mut in mutants) {
            // find all elements in mutant that are not part of the original tomasys or mros ontology
            // these will be added to the rules of the suave ontology and will be saved in the end
            var stats = mut.listStatements().toSet()
            stats = stats.subtract(mrosModel.listStatements().toSet())
            stats = stats.subtract(tomasysModel.listStatements().toSet())

            val suaveOutput = ModelFactory.createDefaultModel()
            for (s in unmutatableSuaveStatements)
                suaveOutput.add(s)
            for (s in stats)
                suaveOutput.add(s)

            // path for the output models
            val suaveOutputPath = "$folderName/$filePrefix.$i.suave.owl"

            RDFDataMgr.write(File(suaveOutputPath).outputStream(), suaveOutput, Lang.RDFXML)

            mutantFiles[i] = suaveOutputPath   // save path of the mutation
            i += 1
        }
    }

    private  fun containsPrefix (stat: Statement, prefix : String) : Boolean {
        if (stat.subject.toString().contains(prefix))
            return true
        if (stat.predicate.toString().contains(prefix))
            return true
        if (stat.`object`.toString().contains(prefix))
            return true

        return false
    }
}





class SuaveMutatorFactory(
    verbose: Boolean,
    private val mutatableStatements: List<Statement>,
    private val maxNumberMutations: Int,
    private val ratioDomainSpecific: Double,
    useAddQAMutation: Boolean) : MutatorFactory(verbose) {

    // boolean determines, if "AddQAEstimationMutation" is used
    private val domainSpecificMutations =
        if (useAddQAMutation)
            listOf(
                ChangeSolvesFunctionMutation::class,
                AddQAEstimationMutation::class,
                RemoveQAEstimationMutation::class,
                ChangeQualityAttributTypeMutation::class,
                ChangeHasValueMutation::class,
                ChangeQAComparisonOperatorMutation::class,
                AddNewThrusterMutation::class
            )
    else
            listOf(
                ChangeSolvesFunctionMutation::class,
                //AddQAEstimationMutation::class,
                RemoveQAEstimationMutation::class,
                ChangeQualityAttributTypeMutation::class,
                ChangeHasValueMutation::class,
                ChangeQAComparisonOperatorMutation::class,
                AddNewThrusterMutation::class
            )


    private val domainIndependentMutations = listOf(
        AddRelationMutation::class,
        ChangeRelationMutation::class,
        AddInstanceMutation::class,
        RemoveAxiomMutation::class,
        RemoveNodeMutation::class,
    )
    override fun randomMutator() : Mutator {

        val ms = MutationSequence(verbose)
        ms.addMutatableAxioms(mutatableStatements)

        // determine the number of the applied mutation operators
        val domSpecMut =
            (maxNumberMutations*ratioDomainSpecific).toInt()

        val domIndMut =
            maxNumberMutations - domSpecMut

        // add domain specific mutations
        for (j in 1..domSpecMut)
            ms.addRandom(domainSpecificMutations.random(randomGenerator))

        // add domain independent mutation operators
        for (j in 1..domIndMut)
            ms.addRandom(domainIndependentMutations.random(randomGenerator))

        // add mutations in random order to mutation sequence
        ms.shuffle()

        return Mutator(ms, verbose)
    }
}