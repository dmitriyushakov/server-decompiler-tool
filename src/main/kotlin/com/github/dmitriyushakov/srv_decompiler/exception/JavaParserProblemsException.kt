package com.github.dmitriyushakov.srv_decompiler.exception

import com.github.javaparser.Problem

private fun problemsToString(problems: List<Problem>): String {
    val sb = StringBuilder()
    sb.append("This problems are happened during java source parse:\n")

    for ((idx, problem) in problems.withIndex()) {
        if (idx > 0) sb.append("\n")
        sb.append(idx + 1).append(". ").append(problem.message).append("\n")
        sb.append("Description: ").append(problem.verboseMessage).append("\n")
        val locationOpt = problem.location
        if (locationOpt.isPresent) {
            val location = locationOpt.get()
            val rangeOpt = location.begin.range
            if (rangeOpt.isPresent) {
                val position = rangeOpt.get().begin

                sb.append("Position: column - ").append(position.column).append(", line - ").append(position.line)
            }
            sb.append("Source: ").append(location.toString()).append("\n")
        }
    }

    return sb.toString()
}

class JavaParserProblemsException(javaParserProblems: List<Problem>): Exception(problemsToString(javaParserProblems))