package org.XXXX100.robust.mutant

import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement


abstract class MutationConfiguration {
    override fun toString(): String {
        val className = javaClass.toString().removePrefix("class mutant.")
        return className
    }

    fun removePrefix(s : String): String {
        val delimiters = setOf('#', '/')
        val pos = s.indexOfLast { delimiters.contains(it) }
        //println("$s, $pos")
        return if (pos >= 0)
            s.removeRange(0, pos+1)
        else
            s
    }
}
class SingleStatementConfiguration(private val statement: Statement) : MutationConfiguration() {
    override fun toString(): String {
        val className = super.toString()
        return "$className($statement)"
    }

    fun getStatement() : Statement {
        return statement
    }
}

open class SingleResourceConfiguration(private val resource: Resource) : MutationConfiguration() {

    override fun toString(): String {
        val className = super.toString()
        return "$className(${resource.localName})"
    }

    fun getResource() : Resource {
        return resource
    }
}

class StringAndResourceConfiguration(private val string: String, r: Resource) : SingleResourceConfiguration(r){
    override fun toString(): String {
        val className = super.toString()
        return "$className($string,${getResource().localName})"
    }

    fun getString() : String {
        return string
    }
}

class DoubleResourceConfiguration(private val resource1: Resource,
                                  private  val resource2: Resource) : MutationConfiguration(){

    override fun toString(): String {
        val className = super.toString()
        return "$className(${resource1.localName},${resource2.localName})"
    }
    fun getResource1() : Resource {
       return resource1
    }

    fun getResource2() : Resource {
        return  resource2
    }
}

class DoubleStringAndStatementConfiguration(private val nodeOld: String,
                                            private val nodeNew: String,
                                            private val r: Statement) : MutationConfiguration() {
    override fun toString(): String = "(" +
                "${removePrefix(nodeOld)}," +
                "${removePrefix(nodeNew)}," +
                "$r)"


    fun getOldNode() : String {
        return nodeOld
    }

    fun getNewNode() : String {
        return nodeNew
    }

    fun getStatement() : Statement {
        return r
    }
}
