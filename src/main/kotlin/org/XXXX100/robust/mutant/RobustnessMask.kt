package org.XXXX100.robust.mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.Shapes
import java.io.File


class RobustnessMask(val verbose: Boolean,
                     private val shacl: Shapes?
    ) {

    // checks, if the provided model is valid w.r.t. the shacl shapes
    fun validate(model: Model) : Boolean {

        // create reasoner with the selected backend
        val reasonerFactory = CustomReasonerFactory(verbose, ReasoningBackend.OPENLLET)
        val reasoner = reasonerFactory.getReasoner(model)

        val consistent = reasoner.isConsistent()
        if(!consistent) return false

        return if (shacl != null) ShaclValidator.get().validate(shacl, model.graph).conforms() else true
    }

    // makes a prediction based on the mask
    private fun maskOracle(ontologyPath: String): OracleOutcome {
        // build model for ontology
        val model = RDFDataMgr.loadDataset(ontologyPath).defaultModel

        return if (this.validate(model))
            OracleOutcome.PASS
        else
            OracleOutcome.FAIL
    }

    // checks, if the predictions of the mask are in line with the predictions from the real oracle
    fun checkAgainstOntologies(oracleFiles: List<String>, detailedEvaluation: Boolean) {

        // sets to collect the ontologies for which the oracle is correct or wrong
        val correctOracle : MutableSet<String> = HashSet()
        val falsePass : MutableSet<String> = HashSet()
        val falseFail : MutableSet<String> = HashSet()

        for (oracleFilePath in oracleFiles) {
            // iterate through oracle file
            val file = File(oracleFilePath)
            file.forEachLine { line ->
                val tokens = line.split(",")
                assert(tokens.size == 4)
                val id = tokens[0]
                val folder = tokens[1]
                val ontologyPath = tokens[2]
                val oracle = tokens[3]
                if (id == "id") {
                    // are in first row
                    assert(folder == "folder") { "Header of CSV file malformed. Should be \"id,folder,ontology,oracle\"" }
                    assert(ontologyPath == "ontology") { "Header of CSV file malformed. Should be \"id,folder,ontology,oracle\"" }
                    assert(oracle == "oracle") { "Header of CSV file malformed. Should be \"id,folder,ontology,oracle\"" }
                } else {
                    // data row

                    // call mask oracle
                    println("test mask for $ontologyPath")
                    val maskOracle = this.maskOracle(ontologyPath)

                    // check against real oracle
                    val realOracle = parseOutcome(oracle)

                    // collect deviations (wrong mask fail / pass)
                    if (realOracle != OracleOutcome.UNDECIDED) {// only consider cases where we have an oracle
                        if (maskOracle == realOracle)
                            correctOracle += ontologyPath
                        else
                            when (maskOracle) {
                                OracleOutcome.FAIL -> falseFail += ontologyPath
                                OracleOutcome.PASS -> falsePass += ontologyPath
                                OracleOutcome.UNDECIDED -> Unit
                            }
                    }
                }

            }
        }

        // print details of evaluation, if desired
        if (detailedEvaluation) {
            if (falseFail.any()) {
                println()
                println("false \"fail\" oracle from mask")
                for (ontology in falseFail)
                    println(ontology)
            }

            if (falsePass.any()) {
                println()
                println("false \"pass\" oracle from mask")
                for (ontology in falsePass.sorted())
                    println(ontology)
            }
        }

        // output result: is mask too permissive or too strict?
        println()
        println("———————————————————————————————————————")
        println("evaluation of mask:")
        println("total number of cases: ${correctOracle.size + falsePass.size + falseFail.size}")
        println("correct: ${correctOracle.size}")
        println("falsePass: ${falsePass.size}")
        println("falseFail: ${falseFail.size}")
        if ( falsePass.size == 0 && falseFail.size >0 )
            println("The mask is too strict.")
        if ( falsePass.size > 0 && falseFail.size == 0 )
            println("The mask is too permissive.")
        if ( falsePass.size == 0 && falseFail.size == 0 )
            println("The mask correct w.r.t. the test cases.")


    }

    private fun parseOutcome(outcome: String) :OracleOutcome {
        return when (outcome) {
            "passed" -> OracleOutcome.PASS
            "pass"   -> OracleOutcome.PASS
            "failed" -> OracleOutcome.FAIL
            "fail"   -> OracleOutcome.FAIL
            "undecided" -> OracleOutcome.UNDECIDED
            else -> {
                assert(false) {"Argument $outcome can not be parsed to OracleOutcome."}
                OracleOutcome.PASS
            }
        }
    }
}

enum class OracleOutcome {
    PASS, FAIL, UNDECIDED;
}