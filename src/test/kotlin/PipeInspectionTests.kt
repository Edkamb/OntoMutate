import io.kotlintest.properties.forAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.smolang.robust.domainSpecific.auv.AddPipeSegmentConfiguration
import org.smolang.robust.domainSpecific.auv.AddPipeSegmentMutation
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.mutant.Mutator
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.sut.MiniPipeInspection
import kotlin.math.absoluteValue


class PipeInspectionTests : StringSpec()  {
    init {
        "add pipe segment should lead to fail" {

            // load ontology
            val verbose = false
            val input = RDFDataMgr.loadDataset("PipeInspection/miniPipes.ttl").defaultModel

            // apply mutation
            val segment = input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
            val configSegment = org.smolang.robust.domainSpecific.auv.AddPipeSegmentConfiguration(segment)
            val msSegment = MutationSequence(verbose)
            msSegment.addWithConfig(org.smolang.robust.domainSpecific.auv.AddPipeSegmentMutation::class, configSegment)
            val mSegment = Mutator(msSegment, verbose)
            val resSegment = mSegment.mutate(input)

            // run program
            val pi = MiniPipeInspection()
            pi.readOntology(resSegment)
            pi.doInspection()

            pi.allInfrastructureInspected() shouldBe false
        }
    }

    init {
        "count number of added segments" {
            forAll(1) { i: Int ->
                // add up to 99 new segments
                val k = (i % 100).absoluteValue
                //println("add: $k new pipe segments")
                // load ontology
                val verbose = false
                val input = RDFDataMgr.loadDataset("PipeInspection/miniPipes.ttl").defaultModel

                // apply mutation
                val msSegment = MutationSequence(verbose)
                for (j in 1..k)
                    msSegment.addRandom(org.smolang.robust.domainSpecific.auv.AddPipeSegmentMutation::class)
                val mSegment = Mutator(msSegment, verbose)
                val resSegment = mSegment.mutate(input)

                // run program
                val pi = MiniPipeInspection()
                pi.readOntology(input)
                val before = pi.allInfrastructure().count()
                pi.readOntology(resSegment)
                val after = pi.allInfrastructure().count()


                for (n in mSegment.affectedSeedNodes)
                    println(n.localName)

                before + k ==  after
            }
        }
    }
}

