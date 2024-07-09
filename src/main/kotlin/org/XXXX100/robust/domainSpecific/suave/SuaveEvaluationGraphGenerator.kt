package org.XXXX100.robust.domainSpecific.suave

import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.XXXX100.robust.mutant.RobustnessMask
import java.io.File
import java.io.FileOutputStream

class SuaveEvaluationGraphGenerator(val verbose: Boolean) {

    // generates data for the graph presented in ISSRE paper
    fun generateGraph(numberOfMutants : Int, outputFile : File) {
        val numberOfMutations = 2
        val nameOfMutants = "temp"
        val saveMutants = false
        val ratioDomainDependent1 = 1.0
        val ratioDomainDependent2 = 0.0
        val useAddQAMutation = false


        // lists to collect number of attempts for domain-independent and -specific operators
        val listAttemptsDI : MutableList<Int> = mutableListOf()
        val listAttemptsDS : MutableList<Int> = mutableListOf()
        val ids = listOf(0,1,2,3,4,5,6,7)
        for (id in ids) {
            println("create mutants for mask with id $id")
            val maskFile = File("sut/suave/masks/mask$id.ttl")
            val shapesGraph = RDFDataMgr.loadGraph(maskFile!!.absolutePath)

            val mask = RobustnessMask(verbose, Shapes.parse(shapesGraph))


            // generate domain-independent mutants
            val sg = SuaveTestCaseGenerator(false)
            val attemptsDI = sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations,
                ratioDomainDependent1,
                useAddQAMutation,
                mask,
                nameOfMutants,
                saveMutants
            )
            listAttemptsDI.add(attemptsDI)

            // generate domain-specific mutants
            val attemptsDS = sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations,
                ratioDomainDependent2,
                useAddQAMutation,
                mask,
                nameOfMutants,
                saveMutants
            )
            listAttemptsDS.add(attemptsDS)

        }

        // output results to csv file
        FileOutputStream(outputFile).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("contract,number mutants,attemptsDI,ratioDI,attemptsDS,ratioDS")
            writer.newLine()
            for (id in ids) {
                val attemptsDI = listAttemptsDI[id]
                val ratioDI = attemptsDI.toFloat() / numberOfMutants
                val attemptsDS = listAttemptsDS[id]
                val ratioDS = attemptsDS.toFloat() / numberOfMutants
                writer.write("$id,$numberOfMutants,$attemptsDS,$ratioDS,$attemptsDI,$ratioDI")
                writer.newLine()
            }
            writer.close()
            println("writetoFile $outputFile")
        }
    }
}