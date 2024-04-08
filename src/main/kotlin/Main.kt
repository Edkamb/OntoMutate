import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import domainSpecific.auv.AddPipeSegmentConfiguration
import domainSpecific.auv.AddPipeSegmentMutation
import domainSpecific.geo.GeoTestCaseGenerator
import domainSpecific.suave.*
import mutant.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import sut.MiniPipeInspection
import java.nio.file.Paths
import kotlin.random.Random

val randomGenerator = Random(2)

class Main : CliktCommand() {
    private val source by argument().file()
    private val contractFile by argument().file()
    private val verbose by option("--verbose","-v", help="Verbose output for debugging. Default = false.").flag()
    private val rounds by option("--rounds","-r", help="Number of mutations applied to input. Default = 1.").int().default(1)

    fun testMutations() {


        if(!source.exists()) throw Exception("Input file $source does not exist")
        val input = RDFDataMgr.loadDataset(source.absolutePath).defaultModel
        if(!contractFile.exists()) throw Exception("Contract file $contractFile does not exist")

        val contract = MutantContract(verbose)
        contract.entailedModel = RDFDataMgr.loadDataset(contractFile.absolutePath).defaultModel

        // test configuration stuff

        //
        var n = 0
        val onlyOneGeneration = true    // we only execute one run
        while(true) {
            println("\n generation ${n++}")
            //val m = Mutator(listOf(AddInstanceMutation::class, RemoveAxiomMutation::class), verbose)
            val ms = MutationSequence(verbose)
            //ms.addRandom(listOf(RemoveSubclassMutation::class))

            val mf = ModelFactory.createDefaultModel()
            val st = mf.createStatement(
                mf.createResource("http://smolang.org#B"),
                mf.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
                mf.createResource("http://smolang.org#A")
            )
            val config = SingleStatementConfiguration(st)

            val r = mf.createResource("http://smolang.org#XYZ")
            val config2 = SingleResourceConfiguration(r)

            val st3 = mf.createStatement(
                mf.createResource("http://smolang.org#B"),
                mf.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
                mf.createResource("http://smolang.org#XYZ")
            )
            val config3 = SingleStatementConfiguration(st3)

            val config4 = StringAndResourceConfiguration("newIndividual", r)

            val segment = mf.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
            val config5 = AddPipeSegmentConfiguration(segment)

            ms.addWithConfig(AddPipeSegmentMutation::class, config5)

            //ms.addWithConfig(RemoveAxiomMutation::class, config)
            ms.addWithConfig(RemoveSubclassMutation::class, config)

            ms.addWithConfig(AddAxiomMutation::class, config3)

            //ms.addWithConfig(AddInstanceMutation::class, config2)
            ms.addRandom(listOf(AddInstanceMutation::class))

            ms.addWithConfig(AddInstanceMutation::class, config4)

            val m = Mutator(ms, verbose)

            //this is copying before mutating, so we must not copy one more time here
            val res = m.mutate(input)

            //XXX: the following ignores blank nodes
            val valid = m.validate(res, contract)
            println("result of validation: $valid")
            if(valid) {
                if(verbose) res.write(System.out, "TTL")
                break
            }

            if (onlyOneGeneration) {
                if (verbose) res.write(System.out, "TTL")
                break
            }
        }



    }


    fun testMiniPipes() {
        if(!source.exists()) throw Exception("Input file $source does not exist")
        val input = RDFDataMgr.loadDataset(source.absolutePath).defaultModel
        val pi = MiniPipeInspection()

        // run without mutations
        pi.readOntology(input)
        pi.doInspection()
        println("everything inspected?: " + pi.allInfrastructureInspected())

        // mutated ontology with "add pipe" at segment1
        println("\nApply mutation to ontology")
        val segment = input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
        val configSegment = AddPipeSegmentConfiguration(segment)
        val msSegment = MutationSequence(verbose)
        msSegment.addWithConfig(AddPipeSegmentMutation::class, configSegment)
        val mSegment = Mutator(msSegment, verbose)
        val resSegment = mSegment.mutate(input)

        pi.readOntology(resSegment)
        pi.doInspection()
        println("everything inspected?: " + pi.allInfrastructureInspected())





        // mutated ontology with deletion of animal and infrastructure are disjoint
        // some implementation work needed: remove axiom mutation + more sophisticated reasoning in algorithm to really
        // use an ontological negation
        val st = input.createStatement(
            input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#Animal"),
            input.createProperty("http://www.w3.org/2002/07/owl#disjointWith"),
            input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#Infrastructure")
        )
        val configAnimal = SingleStatementConfiguration(st)




    }

    fun testSuave() {
        val input = RDFDataMgr.loadDataset("src/main/suave/suave_ontologies/suave_original_with_imports.owl").defaultModel

        val ms = MutationSequence(verbose)


        for (i in 0..10) {
            ms.addRandom(ChangeSolvesFunctionMutation::class)
            ms.addRandom(AddQAEstimationMutation::class)
            ms.addRandom(RemoveQAEstimationMutation::class)
            ms.addRandom(ChangeQualityAttributTypeMutation::class)
            ms.addRandom(ChangeHasValueMutation::class)
            ms.addRandom(ChangeQAComparisonOperatorMutation::class)
            ms.addRandom(AddNewThrusterMutation::class)
        }

        val m = Mutator(ms, verbose)
        val output = m.mutate(input)

        //val output = m.mutate(input)
        //RDFDataMgr.write(File("examples/test2.ttl").outputStream(), output, Lang.TTL)
    }



    fun runSuaveGenerator(contractPath: String) {
        val sg = SuaveTestCaseGenerator(true)
        sg.generateSuaveMutants(10, contractPath)
    }

    fun runGeoGenerator(contractFile: String) {
        val gg = GeoTestCaseGenerator(false)
        gg.generateGeoMutants(contractFile)
    }

    fun evaluateSuaveContract(contractPath : String) {
        // new contract
        val contract = MutantContract(verbose)

        contract.containedModel=RDFDataMgr.loadDataset(contractPath).defaultModel
        contract.checkAgainstOntologies(
            listOf(
                "sut/suave/oracle_mutatedOnt_onlySuave02_2024_03_27_11_52.csv",
                "sut/suave/oracle_mutatedOnt_onlySuave03_2024_03_27_15_11.csv",
                "sut/suave/oracle_mutatedOnt_onlySuave04_2024_03_29_14_15.csv",
                "sut/suave/oracle_mutatedOnt_onlyGeneric03_2024_04_01_11_04.csv",
                "sut/suave/oracle_mutatedOnt_onlySuave05_2024_04_03_17_17.csv"
                ),
            true)
    }

    fun evaluateGeoContract(contractPath : String) {
        // new contract
        val contract = MutantContract(verbose)

        contract.containedModel=RDFDataMgr.loadDataset(contractPath).defaultModel
        contract.useReasonerContainment=true
        contract.checkAgainstOntologies(
            listOf(
                "sut/geo/benchmark_runs/mutations/oracle_mutatedOnt_secondTest_2024_03_26_09_24.csv"
            ),
            true)
    }

    override fun run() {
        //testMutations()
        //testMiniPipes()
        //testSuave()

        runGeoGenerator("sut/geo/contracts/contract1.ttl")
        //runSuaveGenerator("sut/suave/contracts/contract3.owl")

        //evaluateSuaveContract("sut/suave/contracts/contract4.owl")
        //
        //evaluateGeoContract("sut/geo/contracts/contract1.ttl")

    }

}


fun main(args: Array<String>) = Main().main(args)

