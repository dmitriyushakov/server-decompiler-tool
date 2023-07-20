package com.github.dmitriyushakov.srv_decompiler.utils.bytecode

import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.Path
import com.github.dmitriyushakov.srv_decompiler.registry.PathIndex

data class ArgumentsAndReturnTypeDescriptor (
    val argumentsDescriptor: String,
    val returnTypeDescriptor: String)

fun asmClassNameToPath(name: String): Path = name.split("/").filter { it != "." }

fun asmSplitMethodDescriptors(desc: String): ArgumentsAndReturnTypeDescriptor {
    val firstIdx = desc.indexOf('(')
    val lastIdx = desc.lastIndexOf(')')
    if (firstIdx == -1 || lastIdx == -1) error("Method descriptor should have brackets!")

    val argumentsDescriptor = desc.substring(firstIdx + 1,lastIdx)
    val returnTypeDescriptor = desc.substring(lastIdx + 1)

    return ArgumentsAndReturnTypeDescriptor(argumentsDescriptor, returnTypeDescriptor)
}

private fun asmGetObjectTypePathsFromDescriptor(descIter: Iterator<IndexedValue<Char>>, result: MutableList<Path>, desc: String) {
    for ((firstIdx, firstCh) in descIter) {
        if (firstCh == 'L') {
            var pathComplete = false
            for ((secondIdx, secondCh) in descIter) {
                if (secondCh == ';') {
                    if (!pathComplete) {
                        val path = asmClassNameToPath(desc.substring(firstIdx + 1, secondIdx))
                        result.add(path)
                    }
                    break
                } else if(secondCh == '<') {
                    pathComplete = true
                    val path = asmClassNameToPath(desc.substring(firstIdx + 1, secondIdx))
                    result.add(path)
                    asmGetObjectTypePathsFromDescriptor(descIter, result, desc)
                }
            }
        } else if(firstCh == '>') {
            break
        }
    }
}

fun asmGetObjectTypePathsFromDescriptor(desc: String): List<Path> {
    val descIter = desc.withIndex().iterator()
    val result: MutableList<Path> = mutableListOf()

    asmGetObjectTypePathsFromDescriptor(descIter, result, desc)

    return result
}

fun asmGetObjectTypePathFromDescriptor(desc: String): Path? = asmGetObjectTypePathsFromDescriptor(desc).firstOrNull()

fun pathToHumanReadableName(path: Path): String = path.joinToString(".")
fun pathToString(path: Path): String = path.joinToString("/")

fun addPathSimpleName(path: Path, name: String): Path = path.toMutableList().apply { add(name) }
fun getPathShortName(path: Path): String? {
    val lastName = path.last()
    if (lastName.indexOf('$') == -1) return lastName
    else {
        val parts = lastName.split('$')
        val sb = StringBuilder()
        var first = true
        for (part in parts) {
            if (first) {
                first = false
            } else {
                sb.append('.')
            }
            if (part.length == 0 || part.first().isDigit()) return null
            sb.append(part)
        }

        return sb.toString()
    }
}

private fun listAsmPathsForHumanReadableName(name: String): List<Path> {
    val parts = name.split('.')
    val result = mutableListOf<Path>()

    result.add(parts)

    // In case if we look for nested class
    var currentPath = parts.toMutableList()
    while (currentPath.size > 1) {
        val head = currentPath.removeLast()
        val preHead = currentPath.removeLast()
        currentPath.add("$preHead\$$head")

        result.add(currentPath.toList())
    }

    // In case if we look for method in nested class
    currentPath = parts.toMutableList()
    while (currentPath.size > 2) {
        val methodName = currentPath.removeLast()
        val head = currentPath.removeLast()
        val preHead = currentPath.removeLast()
        currentPath.add("$preHead\$$head")
        currentPath.add(methodName)

        result.add(currentPath.toList())
    }

    return result
}

fun <T> PathIndex<T>.searchForHumanReadableName(name: String): List<T> {
    val paths = listAsmPathsForHumanReadableName(name)
    val results = mutableListOf<T>()

    for (path in paths) {
        results.addAll(searchForPath(path, onlyRoot = true))
    }

    if (results.isEmpty()) {
        for (path in paths) {
            results.addAll(searchForPath(path, onlyRoot = false))
        }
    }

    return results
}

fun PathIndex<Subject>.findTopLevelClassPath(path: Path): Path? = findTopElement(path) { _, item -> item is ClassSubject }?.first