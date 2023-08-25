import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import mutant.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFLanguages
import java.sql.Statement


class Main : CliktCommand() {
    private val source by argument().file()
    private val contract by argument().file()
    private val verbose by option("--verbose","-v", help="Verbose output for debugging. Default = false.").flag()
    private val rounds by option("--rounds","-r", help="Number of mutations applied to input. Default = 1.").int().default(1)

    override fun run() {

        if(!source.exists()) throw Exception("Input file $source does not exist")
        val input = RDFDataMgr.loadDataset(source.absolutePath).defaultModel
        if(!contract.exists()) throw Exception("Contract file $contract does not exist")
        val contractModel = RDFDataMgr.loadDataset(contract.absolutePath).defaultModel


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

            ms.addWithConfig(RemoveAxiomMutation::class, config)

            val m = Mutator(ms, verbose)

            //this is copying before mutating, so we must not copy one more time here
            val res = m.mutate(input)

            //XXX: the following ignores blank nodesn
            val valid = m.validate(res, contractModel)
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
}


fun main(args: Array<String>) = Main().main(args)

