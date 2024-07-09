package org.XXXX100.robust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.XXXX100.robust.domainSpecific.geo.GeoScenarioGenerator
import org.XXXX100.robust.domainSpecific.geo.GeoTestCaseGenerator
import org.XXXX100.robust.domainSpecific.suave.*
import org.XXXX100.robust.mutant.*
import org.XXXX100.robust.sut.MiniPipeInspection
import java.io.File
import kotlin.random.Random
import kotlin.system.exitProcess

val randomGenerator = Random(2)

class Main : CliktCommand() {
    private val seed by option("--seedKG" ,"-g", help="KG to mutate, defined by an RDF file").file()
    private val shaclMaskFile by option("--shacl","-s", help="Gives a mask, defined by a set of SHACL shapes").file()
    private val verbose by option("--verbose","-v", help="Verbose output for debugging. Default = false.").flag()
    private val numberMutations by option("--num_mut", "-nm", help="Number of mutation operators to apply. Default = 1").int()
    private val outputFile by option("--out", "-o", help="Give name for mutated KG.").file()
    private val mainMode by option(help="Options to run specialized modes of this program.").switch(
        "--mutate" to "mutate", "-m" to "mutate",
        "--scen_geo" to "geo", "-sg" to "geo",
        "--scen_suave" to "suave", "-sv" to "suave",
        "--scen_test" to "test", "-st" to "test",
        "--issre_graph" to "issre", "-ig" to "issre"
    ).default("free")


    override fun run() {

        when (mainMode){
            "mutate" -> {
                singleMutation()
            }
            "geo" -> {
                runGeoGenerator()
            }
            "suave" -> {
                runSuaveGenerator()
            }
            "issre" -> generateIssreGraph()
            "test" -> {
                // test installation
                testMiniPipes()
            }
            else -> testMiniPipes()
        }


    }


    private fun parseShapes(shapeFile : File?) : Shapes?{
        val shapes: Shapes? =
            if(shapeFile != null && !shapeFile.exists()){
                println("File ${shapeFile.path} does not exist")
                exitProcess(-1)
            } else if(shapeFile != null) {
                val shapesGraph = RDFDataMgr.loadGraph(shapeFile.absolutePath)
                Shapes.parse(shapesGraph)
            } else null
        return shapes
    }


    // apply one mutation to the input KG
    private fun singleMutation() {
        // get seed, mask and output files
        if (seed == null){
            println("You need to provide a seed knowledge graph")
            exitProcess(-1)
        } else if(!seed!!.exists()){
            println("File ${seed!!.path} does not exist")
            exitProcess(-1)
        }
        val seedKG = RDFDataMgr.loadDataset(seed!!.absolutePath).defaultModel

        val shapes: Shapes? = if(shaclMaskFile != null && !shaclMaskFile!!.exists()){
            println("File ${shaclMaskFile!!.path} does not exist")
            exitProcess(-1)
        } else if(shaclMaskFile != null) {
            val shapesGraph = RDFDataMgr.loadGraph(shaclMaskFile!!.absolutePath)
            Shapes.parse(shapesGraph)
        } else null

        val outputPath: File? = if(outputFile != null && outputFile!!.exists()){
            println("Output file ${outputFile!!.path} does already exist. Please choose a different name.")
            exitProcess(-1)
        } else if(outputFile == null) {
            println("Please provide an output file to save the mutated KG to.")
            exitProcess(-1)
        } else outputFile

        val mask = RobustnessMask(verbose, shapes)

        // create selection of mutations that can be applied
        val candidateMutations = listOf(
            AddRelationMutation::class,
            ChangeRelationMutation::class,
            AddInstanceMutation::class,
            RemoveAxiomMutation::class,
            RemoveNodeMutation::class,
        )

        val ms = MutationSequence(verbose)
        // add domain independent mutation operators
        // if no number of mutations is provided --> apply one
        for (j in 1..(numberMutations ?: 1))
            ms.addRandom(candidateMutations.random(randomGenerator))

        // create mutator and apply mutation
        val m = Mutator(ms, verbose)
        val res = m.mutate(seedKG)

        // safe result
        RDFDataMgr.write(outputPath!!.outputStream(), res, Lang.TTL)
    }


    private fun testMiniPipes() {
        //if(!seed.exists()) throw Exception("Input file $seed does not exist")
        //val input = RDFDataMgr.loadDataset(seed.absolutePath).defaultModel

        val input = RDFDataMgr.loadDataset("examples/miniPipes.ttl").defaultModel
        val pi1 = MiniPipeInspection()

        // run without mutations
        pi1.readOntology(input)
        pi1.doInspection()
        println("everything inspected?: " + pi1.allInfrastructureInspected())

        // mutated ontology with "add pipe" at segment1
        println("\nApply mutation to ontology")
        val segment = input.createResource("http://www.anon.anon/miniPipes#segment1")
        val configSegment = org.XXXX100.robust.domainSpecific.auv.AddPipeSegmentConfiguration(segment)
        val msSegment = MutationSequence(verbose)
        msSegment.addWithConfig(org.XXXX100.robust.domainSpecific.auv.AddPipeSegmentMutation::class, configSegment)
        val mSegment = Mutator(msSegment, verbose)
        val resSegment = mSegment.mutate(input)

        val pi2 = MiniPipeInspection()
        pi2.readOntology(resSegment)
        pi2.doInspection()
        println("everything inspected?: " + pi2.allInfrastructureInspected())

        if(!pi1.allInfrastructureInspected() || pi2.allInfrastructureInspected())
            throw Exception("Mutation Generator does not work as expected")
        else
            println("Mutation Generator works as expected.")
    }

    private fun runSuaveGenerator() {
        val maskFiles = listOf(
            "sut/suave/masks/mask0.ttl",
            "sut/suave/masks/mask1.ttl",
            "sut/suave/masks/mask2.ttl",
            "sut/suave/masks/mask3.ttl",
            "sut/suave/masks/mask4.ttl",
            "sut/suave/masks/mask5.ttl",
            "sut/suave/masks/mask6.ttl",
            "sut/suave/masks/mask7.ttl",
        )



        assert(maskFiles.size == 8)

        for(i in 0..7){
            val shapes = parseShapes(File(maskFiles[i]))
            val sg = SuaveTestCaseGenerator(verbose)

            // number of mutants differs for each generation
            val numberOfMutants =
                when (i) {
                    0, 1, 3 -> 10
                    2 -> 20
                    4 -> 40
                    5, 6, 7 -> 30
                    else -> 0
                }
            val numberOfMutations = 2

            // select domain specific / independent based on generation
            val ratioDomainSpecific =
                if (i == 3)
                    0.0
                else
                    1.0

            // use this specific mutation only for first four generations
            val useAddQAMutation = (i <= 4)

            val mask = RobustnessMask(verbose, shapes)

            // select name based on which mutations are used
            val nameOfMutants =
                when (i) {
                    3 -> "generation${i}_domain_independent"
                    else -> "generation${i}_domain_specific"
                }

            val saveMutants = true

            sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations,
                ratioDomainSpecific,
                useAddQAMutation,
                mask,
                nameOfMutants,
                saveMutants)
        }

    }

    private fun runGeoGenerator() {

        val maskFiles = listOf(
            "sut/geo/masks/mask0.ttl",
            "sut/geo/masks/mask1.ttl"
        )

        var id = 0
        for (maskFile in maskFiles) {
            val gg = GeoTestCaseGenerator(verbose)
            val numberOfMutants = 100
            val numberOfMutations = 2

            val shapes = parseShapes(File(maskFile))
            val mask = RobustnessMask(verbose, shapes)

            val nameOfMutants = "geoMutant$id"
            val saveMutants = true
            gg.generateGeoMutants(
                numberOfMutants,
                numberOfMutations,
                mask,
                nameOfMutants,
                saveMutants
            )

            id += 1
        }
    }

    // evaluates provided shapes against the suave test runs
    fun evaluateSuaveMask(shapes: Shapes?) {
        // new mask
        val mask = RobustnessMask(verbose, shapes)

        val relevantSutRuns =  listOf(
            "sut/suave/oracle_mutatedOnt_onlySuave02_2024_03_27_11_52.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave03_2024_03_27_15_11.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave04_2024_03_29_14_15.csv",
            "sut/suave/oracle_mutatedOnt_onlyGeneric03_2024_04_01_11_04.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave05_2024_04_03_17_17.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave06_2024_04_08_09_58.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave07_2024_04_11_08_40.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave08_2024_04_15_08_39.csv"
        )
        val detailedEvaluationOutput = true

        mask.checkAgainstOntologies(
           relevantSutRuns,
            detailedEvaluationOutput)
    }

    // evaluates provided shapes against the geo test runs
    fun evaluateGeoMask(shapes: Shapes?) {
        val mask = RobustnessMask(verbose, shapes)

        val relevantSutRuns = listOf(
            "sut/geo/benchmark_runs/mutations/oracle_mutatedOnt_secondTest_2024_03_26_09_24.csv"
        )
        val detailedEvaluationOutput = true

        mask.checkAgainstOntologies(
            relevantSutRuns,
            detailedEvaluationOutput)
    }

    // creates scenarios, i.e. different layers, for the geo simulator
    fun generateGeoScenarios() {
        val geoGenerator = GeoScenarioGenerator()
        geoGenerator.generateScenarios(10)
    }

    // takes ontologies, i.e. files with axioms and turns them into SHACL shapes
    fun turnContractIntoSHACLShape() {
        val ids = listOf(0,1,2,3,4,5,6,7)
        for (i in ids ) {
            val sg = ShapeGenerator()
            sg.turnAxiomsToShapes("sut/suave/contracts/contract$i.owl")
            sg.saveShapes("sut/suave/masks", "mask$i")
        }

        val sg = ShapeGenerator()

        sg.turnAxiomsToShapes("sut/geo/contracts/contract1.ttl")
        sg.saveShapes("sut/geo/masks", "mask1")

        val sgExample = ShapeGenerator()

        sgExample.turnAxiomsToShapes("examples/contract.ttl")
        sgExample.saveShapes("examples", "mask")
    }

    // generates graph for ISSRE paper
    fun generateIssreGraph() {
        val numberOfMutants = 100
        val outputFile = File("sut/suave/evaluation/attemptsPerMask.csv")
        SuaveEvaluationGraphGenerator(false).generateGraph(numberOfMutants, outputFile)
    }
}


fun main(args: Array<String>) = Main().main(args)
