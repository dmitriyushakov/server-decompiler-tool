package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor

class MethodIndexVisitor: MethodVisitor {
    class VisitedMethodInsn(
        val opcode: Int,
        val owner: String,
        val name: String,
        val descriptor: String,
        val isInterface: Boolean)

    class VisitedLocalVariable(
        val name: String,
        val descriptor: String,
        val signature: String?,
        val index: Int)

    class VisitedFieldInsn(
        val opcode: Int,
        val owner: String,
        val name: String,
        val descriptor: String
    )

    class VisitedTypeInsn(
        val opcode: Int,
        val type: String
    )

    constructor(api: Int): super(api)
    constructor(api: Int, methodVisitor: MethodVisitor): super(api, methodVisitor)

    val visitedMethodInstructions: MutableList<VisitedMethodInsn> = mutableListOf()
    val visitedLocalVariables: MutableList<VisitedLocalVariable> = mutableListOf()
    val visitedFieldInstructions: MutableList<VisitedFieldInsn> = mutableListOf()
    val visitedTypeInstructions: MutableList<VisitedTypeInsn> = mutableListOf()

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        visitedMethodInstructions.add(
            VisitedMethodInsn(opcode, owner, name, descriptor, isInterface)
        )

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitLocalVariable(
        name: String,
        descriptor: String,
        signature: String?,
        start: Label,
        end: Label,
        index: Int
    ) {
        visitedLocalVariables.add(
            VisitedLocalVariable(name, descriptor, signature, index)
        )

        super.visitLocalVariable(name, descriptor, signature, start, end, index)
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        visitedFieldInstructions.add(
            VisitedFieldInsn(opcode, owner, name, descriptor)
        )

        super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        visitedTypeInstructions.add(
            VisitedTypeInsn(opcode, type)
        )

        super.visitTypeInsn(opcode, type)
    }
}