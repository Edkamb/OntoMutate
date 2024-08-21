package org.smolang.robust.planner.pddl

class PddlAction(
    val name: String,
    val parameters: List<String>,
    val preconditions: List<PddlAssertion>,
    val effects: List<PddlAssertion>
) {

    val usedObjects : Set<String> get() {
        return preconditions.flatMap { it.arguments }
            .plus(effects.flatMap { it.arguments })
            .filter { !it.startsWith("?") }     // exclude variables
            .toSet()
    }

    val usedPredicates : Set<String> get() = run {
        val relations = preconditions.map { it.relation }.plus(effects.map { it.relation })
        relations.map { relationToPred(it, 2) }.toSet()
    }

    private fun relationToPred(relation: String, arity : Int) : String {
        var arguments = ""
        for (i in 1..arity)
            arguments += "?x$i "
        return "($relation $arguments)"
    }

    override fun toString() : String {
        val sb = StringBuilder()

        sb.appendLine("  (:action ${name}")
        sb.appendLine("    :parameters (${parameters.joinToString(" ")})")
        sb.appendLine("    :precondition (and ${preconditions.joinToString(
            "\n        ",
            "\n        ",
            "\n    ")})")
        sb.appendLine("    :effect (and ${effects.joinToString(
            "\n        ",
            "\n        ",
            "\n    ")})")
        sb.appendLine("  )")

        return sb.toString()
    }
}