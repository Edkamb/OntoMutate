package org.XXXX100.robust.domainSpecific.geo

import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit


/**
 * Generates several scenarios using an external tool, then creates a csv files with the test oracle.
 * Note: the external tool does *not* modify the knowledge graph
 */
class GeoScenarioGenerator {

    // list of files + oracle (does maturation happen)
    private val files : MutableList<Pair<File,Boolean>> = mutableListOf()
    fun  generateScenarios(count : Int) {
        // generate scenario main blocks
        "python3 generateScenarios.py scenarios $count".runCommand(File("org/XXXX100/robust/sut/geo"))

        // iterate over scenarios
        val dir = File("org/XXXX100/robust/sut/geo/scenarios")
        val pattern = Regex("sut/geo/scenarios/Output.*.txt")
        val header = "org/XXXX100/robust/sut/geo/scenarios/header.smol"

        var i = 0

        dir.walk().forEach {outputFile ->
            if (pattern.matches(outputFile.toString())) {
                println(outputFile)
                val scenarioName = "${dir.absolutePath}/scenario$i.smol"
                val scenario = File(scenarioName)
                scenario.createNewFile()

                var hasSource = true
                var hasCap = true
                var depth = 0

                println(scenario.absolutePath)
                FileOutputStream(scenarioName).use { fos ->
                    val writer = fos.bufferedWriter()
                    writer.write("id;folder;mutantFile;numMutations;numDel;numAdd;appliedMutations;affectedSeedNodes;addedAxioms;removedAxioms")
                    writer.newLine()
                }
                scenario.createNewFile()
                File(scenarioName).printWriter().use { out ->
                    File(header).forEachLine { out.println(it) }
                    out.println("\n//the following part is the generated scenario")
                    outputFile.forEachLine {
                        out.println(it)
                        if (it.startsWith(" has_source:"))
                            hasSource =
                                !it.endsWith("False")
                        if (it.startsWith("has_cap:"))
                            hasCap =
                                !it.endsWith("False")
                        if (it.startsWith("depth:"))
                            depth = it.removePrefix("depth: ").toInt()
                    }
                }

                // delete file created by Python script
                outputFile.delete()

                // analyze if maturation happens and safe this accordingly
                val maturation = (hasCap && hasSource && depth > 2000)

                files.add(Pair(scenario, maturation))
                i += 1
            }
        }

        //TODO: I understand reading csv for oracles is nice, but I would like to run the geo case without this detour
        writeToCSV("${dir.absolutePath}/scenarios.csv")
    }


    // writes content from "files" list to csv
    private fun writeToCSV(fileName : String) {
        FileOutputStream(fileName).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("id,scenario,maturation")
            writer.newLine()
            var id = 0
            for (f in files) {
                writer.write("$id,scenarios/${f.first.name},${f.second}")
                writer.newLine()
                id += 1
            }
            writer.close()
        }
    }

        // from https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
        private fun String.runCommand(workingDir: File) {
        ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(600, TimeUnit.MINUTES)
    }

}