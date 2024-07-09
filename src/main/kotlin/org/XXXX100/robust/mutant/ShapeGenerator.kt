package org.XXXX100.robust.mutant

import io.kotlintest.matchers.types.shouldNotBeNull
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


// turns OWL triples into SHACL shapes
class ShapeGenerator() {

    val shapes : Model = ModelFactory.createDefaultModel()

    // maps blank nodes to the shapes that represent them
    val nodesToShapes : MutableMap<Resource, Resource> = mutableMapOf()

    val prefixShacl : String = "http://www.w3.org/ns/shacl#"
    val prefixMask : String = "https://www.ifi.uio.no/tobiajoh/mask#"

    val nodeShape : Resource = shapes.getResource(prefixShacl + "NodeShape")
    val targetNode : Property = shapes.getProperty(prefixShacl + "targetNode")
    val shPropertyProperty : Property = shapes.getProperty(prefixShacl + "property")
    val shPathProperty : Property = shapes.getProperty(prefixShacl + "path")
    val shInProperty : Property = shapes.getProperty(prefixShacl + "in")
    val shNode : Property = shapes.getProperty(prefixShacl + "node")
    val shInversePath : Property = shapes.getProperty(prefixShacl + "inversePath")
    val shQualifiedValueShape : Property = shapes.getProperty(prefixShacl + "qualifiedValueShape")
    val shQualifiedMinCount : Property = shapes.getProperty(prefixShacl + "qualifiedMinCount")



    val shHasValueProperty : Property = shapes.getProperty(prefixShacl + "hasValue")

    val shMinCountProperty : Property = shapes.getProperty(prefixShacl + "minCount")

    val typeProp : Property = shapes.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")

    fun checkOrAddBlankToMap(blankNode : Resource) {
        // add new name for shape, if it does not exist yet
        if (!nodesToShapes.contains(blankNode)) {
            var i = 0
            while (nodesToShapes.containsValue(shapes.getResource(prefixMask+"blankShape$i")))
                i += 1
            nodesToShapes.put(blankNode, shapes.getResource(prefixMask+"blankShape$i"))
        }
    }

    fun turnAxiomsToShapes(maskFile : String) {
        // register prefixes
        shapes.setNsPrefix("sh",prefixShacl);
        shapes.setNsPrefix("schema", "http://schema.org/")
        shapes.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#")
        shapes.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
        shapes.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
        shapes.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")

        shapes.setNsPrefix("mask", prefixMask)

        // specific prefixes for our examples, to make them easier to read
        shapes.setNsPrefix("suave", "http://www.metacontrol.org/suave#")
        shapes.setNsPrefix("tomasys", "http://metacontrol.org/tomasys#")
        shapes.setNsPrefix("obo", "http://purl.obolibrary.org/obo/")
        shapes.setNsPrefix("UFRGS", "http://purl.obolibrary.org/obo/bfo.owl#UFRGS:")
        shapes.setNsPrefix("geo", "http://www.semanticweb.org/quy/ontologies/2023/2/untitled-ontology-38#")


        // add shape axioms for all the axioms
        val triples = RDFDataMgr.loadDataset(maskFile).defaultModel
        var id = 0
        for (axiom in triples.listStatements()) {
            //println(axiom)
            val shape = AxiomToShape(axiom, id)
            shape.forEach {shapes.add(it) }
            id += 1
        }

    }

    fun AxiomToShape(axiom: Statement, id : Int) : Set<Statement> {
        if (axiom.subject.isAnon)
            if (axiom.`object`.isAnon)
                return shapesForBlank(axiom, id)
            else
                return shapesForBlank(axiom, id).union(inverseShapesForBlank(axiom, id))
        else
            return shapesForNonBlank(axiom, id)

    }

    fun shapesForBlank(axiom: Statement, id: Int) : Set<Statement> {
        val node = axiom.subject
        val setShape : MutableSet<Statement> = mutableSetOf()
        checkOrAddBlankToMap(node)

        val shapeNode = nodesToShapes[node]
        shapeNode.shouldNotBeNull()

        setShape.add(shapes.createStatement(
            shapeNode,
            typeProp,
            nodeShape
        ))

        val propertyShapes = shapesForProperty(axiom, shapeNode)

        return  setShape.toSet().union(propertyShapes)
    }

    fun shapesForProperty(axiom: Statement, shapeNode : Resource) : Set<Statement> {
        val setShape : MutableSet<Statement> = mutableSetOf()

        val propertyNode = shapes.createResource()

        setShape.add(shapes.createStatement(
            shapeNode,
            shPropertyProperty,
            propertyNode
        ))

        setShape.add(shapes.createStatement(
            propertyNode,
            shPathProperty,
            axiom.predicate
        ))

        setShape.add(shapes.createStatement(
            propertyNode,
            shMinCountProperty,
            shapes.createTypedLiteral("1", "http://www.w3.org/2001/XMLSchema#integer")
        ))

        if (axiom.`object`.isAnon){
            val blank = axiom.`object`
            checkOrAddBlankToMap(blank.asResource())
            val blankShape =nodesToShapes[blank.asResource()]
            blankShape.shouldNotBeNull()

            setShape.add(shapes.createStatement(
                propertyNode,
                shNode,
                blankShape
            ))
        }
        else
            setShape.add(shapes.createStatement(
                propertyNode,
                shHasValueProperty,
                axiom.`object`
            ))

        return setShape.toSet()
    }

    fun shapesForNonBlank(axiom: Statement, id : Int) : Set<Statement> {
        assert(!axiom.subject.isAnon)

        val shapeNode = shapes.getResource(prefixMask+"shape$id")
        val setShape : MutableSet<Statement> = mutableSetOf()
        setShape.add(shapes.createStatement(
            shapeNode,
            typeProp,
            nodeShape
        ))

        setShape.add(
            shapes.createStatement(
                shapeNode,
                targetNode,
                axiom.subject
            )
        )

        val propertyShapes = shapesForProperty(axiom, shapeNode)

        return  setShape.toSet().union(propertyShapes)
    }

    // switches source and target and uses the inverse of the relation of the axiom
    // goal: connect blank node to the specific IRI that is given
    fun inverseShapesForBlank(axiom: Statement, id : Int) : Set<Statement> {
        assert(!axiom.`object`.isAnon)

        val shapeNode = shapes.getResource(prefixMask+"inverseShape$id")
        val setShape : MutableSet<Statement> = mutableSetOf()
        setShape.add(shapes.createStatement(
            shapeNode,
            typeProp,
            nodeShape
        ))

        setShape.add(
            shapes.createStatement(
                shapeNode,
                targetNode,
                axiom.`object`
            )
        )

        val propertyShapes = inverseShapesForProperty(axiom, shapeNode)

        return  setShape.toSet().union(propertyShapes)
    }

    fun inverseShapesForProperty(axiom: Statement, shapeNode : Resource) : Set<Statement> {
        val setShape : MutableSet<Statement> = mutableSetOf()

        //val propertyNode = shapes.createResource("inversePropNode$id")

        val propertyNode = shapes.createResource()


        val inversePathNode = shapes.createResource()

        // declare shape for property
        setShape.add(shapes.createStatement(
            shapeNode,
            shPropertyProperty,
            propertyNode
        ))

        // declare property as inverse of existing property
        setShape.add(shapes.createStatement(
            propertyNode,
            shPathProperty,
            inversePathNode
        ))
        setShape.add(shapes.createStatement(
            inversePathNode,
            shInversePath,
            axiom.predicate
        ))


        // add shapes to ensure that there is one relation with given shape
        val qualifiedShapeNode = shapes.createResource()

        setShape.add(shapes.createStatement(
            propertyNode,
            shQualifiedMinCount,
            shapes.createTypedLiteral("1", "http://www.w3.org/2001/XMLSchema#integer")
        ))

        setShape.add(shapes.createStatement(
            propertyNode,
            shQualifiedValueShape,
            qualifiedShapeNode
        ))

        if (axiom.subject.isAnon){
            val blank = axiom.subject
            checkOrAddBlankToMap(blank.asResource())
            val blankShape =nodesToShapes[blank.asResource()]
            blankShape.shouldNotBeNull()

            setShape.add(shapes.createStatement(
                qualifiedShapeNode,
                shNode,
                blankShape
            ))
        }
        else
            setShape.add(shapes.createStatement(
                qualifiedShapeNode,
                shHasValueProperty,
                axiom.subject
            ))

        return setShape.toSet()
    }

    // writes shapes to file
    fun saveShapes(folderName: String, targetFile : String) {
        Files.createDirectories(Paths.get(folderName))
        val path = "$folderName/$targetFile.ttl"
        RDFDataMgr.write(File(path).outputStream(), shapes, Lang.TURTLE)
    }
}