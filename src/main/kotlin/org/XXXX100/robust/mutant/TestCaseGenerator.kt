package org.XXXX100.robust.mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths


open class TestCaseGenerator(private val verbose: Boolean) {
    private val mutators : MutableList<Mutator> = mutableListOf()
    val mutants : MutableList<Model> = mutableListOf()
    val mutantFiles: MutableList<String> = mutableListOf()

    // returns the number of tries to generate the desired number of mutants
    fun generateMutants(seed : Model,
                        mask: RobustnessMask,
                        mutFactory : MutatorFactory,
                        countDesired : Int) : Int {
        var countGenerated = 0
        var countTries = 0
        while (countGenerated < countDesired) {
            val m = mutFactory.randomMutator()
            val mutant = m.mutate(seed)
            countTries += 1
            if (verbose)
                println("generated mutants: $countGenerated")
            if (mask.validate(mutant)){
                countGenerated += 1
                mutators.add(m)
                mutants.add(mutant)
                mutantFiles.add("?")
            }
        }
        if (verbose) {
            println("generated: $countGenerated tries: $countTries")
            println("ratio: " + countTries.toFloat() / countGenerated)
        }
        return countTries
    }



    open fun saveMutants(folderName: String, filePrefix : String) {
        var i = 0
        // create folder, if necessary
        Files.createDirectories(Paths.get(folderName))
        for (mut in mutants) {
            val path = "$folderName/$filePrefix.$i.nt"
            RDFDataMgr.write(File(path).outputStream(), mut, Lang.NTRIPLES)
            mutantFiles[i] = path   // save path of the mutation
            i += 1
        }
    }

    fun writeToCSV(fileName : String) {
        FileOutputStream(fileName).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("id;folder;mutantFile;numMutations;numDel;numAdd;appliedMutations;affectedSeedNodes;addedAxioms;removedAxioms")
            writer.newLine()
            var id = 0
            for (m in mutators) {
                val path = Paths.get("").toAbsolutePath().toString()
                val folder = path
                val mutantFile = mutantFiles[id]
                val numMut = m.numMutations
                val numDel = m.globalMutation?.removeSet?.size ?: -1
                val numAdd = m.globalMutation?.addSet?.size ?: -1
                val appliedMutations = m.appliedMutations
                val affectedSeedNodes = m.affectedSeedNodes.joinToString(",", "[", "]") {
                    if (it.localName != null)
                        it.localName
                    else
                        it.toString()
                }

                val addedAxioms = m.addSet.joinToString( ",", "[", "]")
                val removedAxioms = m.removeSet.joinToString( ",", "[", "]").replace("\n", ",")

                writer.write("$id;$path;$mutantFile;$numMut;$numDel;$numAdd;$appliedMutations;$affectedSeedNodes;$addedAxioms;$removedAxioms")
                writer.newLine()
                id += 1
            }
            writer.close()
        }
    }
}