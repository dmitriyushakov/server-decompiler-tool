package com.github.dmitriyushakov.srv_decompiler.exception

import com.github.dmitriyushakov.srv_decompiler.utils.highlight.parse.problemsToString
import com.github.javaparser.Problem

class JavaParserProblemsException(javaParserProblems: List<Problem>): Exception(problemsToString(javaParserProblems))