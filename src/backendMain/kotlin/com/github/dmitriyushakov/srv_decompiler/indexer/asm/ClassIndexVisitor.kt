package com.github.dmitriyushakov.srv_decompiler.indexer.asm

import com.github.dmitriyushakov.srv_decompiler.common.Interner
import com.github.dmitriyushakov.srv_decompiler.indexer.model.*
import com.github.dmitriyushakov.srv_decompiler.reading_context.ReadingContext
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.asmClassNameToPath
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.asmGetObjectTypePathFromDescriptor
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.asmGetObjectTypePathsFromDescriptor
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.asmSplitMethodDescriptors
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.properties.Delegates.notNull

class ClassIndexVisitor: ClassVisitor {
    private val readingContext: ReadingContext

    private class VisitedFieldInformation(
        val access: Int,
        val name: String,
        val descriptor: String,
        val signature: String?,
        val value: Any?)

    private class VisitedMethodInformation(
        val access: Int,
        val name: String,
        val descriptor: String,
        val signature: String?,
        val exceptions: List<String>,
        val methodVisitor: MethodIndexVisitor
    )

    constructor(api: Int, readingContext: ReadingContext): super(api) {
        this.readingContext = readingContext
    }
    constructor(api: Int, classVisitor: ClassVisitor, readingContext: ReadingContext): super(api, classVisitor) {
        this.readingContext = readingContext
    }

    private var visitIsCalled: Boolean = false

    private var version: Int by notNull()
    private var access: Int by notNull()
    private lateinit var name: String
    private var signature: String? = null
    private var superName: String? = null
    private lateinit var interfaces: List<String>
    private var source: String? = null
    private var debug: String? = null

    private val visitedFields: MutableList<VisitedFieldInformation> = mutableListOf()
    private val visitedMethods: MutableList<VisitedMethodInformation> = mutableListOf()

    private fun VisitedMethodInformation.toMethodSubject(owner: ClassSubject): MethodSubject {
        val methodPath: List<String> = owner.path.toMutableList().apply {
            add(name)
        }

        val dependencies: List<Dependency> = methodVisitor.let { mv ->
            sequenceOf(
                mv.visitedFieldInstructions.map { insn ->
                    val fieldPath = asmClassNameToPath(insn.owner) + listOf(insn.name)
                    ASMDependency(methodPath, fieldPath, DependencyType.FieldUsage)
                },
                mv.visitedMethodInstructions.map { insn ->
                    val invokedMethodPath: List<String> = asmClassNameToPath(insn.owner) + listOf(insn.name)
                    ASMDependency(methodPath, invokedMethodPath, DependencyType.MethodCall)
                },
                mv.visitedTypeInstructions.map { insn ->
                    val depType = when(insn.opcode) {
                        Opcodes.NEW -> DependencyType.CreationNewInstance
                        Opcodes.ANEWARRAY -> DependencyType.CreationNewArray
                        Opcodes.CHECKCAST -> DependencyType.CheckingCast
                        Opcodes.INSTANCEOF -> DependencyType.CheckingInstanceOf
                        else -> error("Unknown type instruction - ${insn.opcode}")
                    }

                    val typePath = asmClassNameToPath(insn.type)

                    ASMDependency(methodPath, typePath, depType)
                },
                exceptions.map(::asmClassNameToPath).map { excPath ->
                    ASMDependency(methodPath, excPath, DependencyType.Exception)
                },
                asmSplitMethodDescriptors(signature ?: descriptor).let { (argumentsDescriptor, returnTypeDescriptor) ->
                    val deps = asmGetObjectTypePathsFromDescriptor(argumentsDescriptor).map { path ->
                        ASMDependency(methodPath, path, DependencyType.ArgumentType)
                    }.toMutableList()

                    val returnTypePath = asmGetObjectTypePathFromDescriptor(returnTypeDescriptor)
                    if (returnTypePath != null) {
                        deps.add(
                            ASMDependency(methodPath, returnTypePath, DependencyType.ReturnType)
                        )
                    }

                    deps
                }
            ).flatten().toList()
        }

        val isStatic = access and Opcodes.ACC_STATIC != 0

        val methodSubject = ASMMethodSubject(
            owner,
            isStatic,
            name,
            descriptor,
            methodPath,
            dependencies
        )

        val localVariables: List<LocalVariableSubject> = methodVisitor.visitedLocalVariables.mapNotNull { localVar ->
            if (localVar.name == "this") null
            else {
                val localVarPath = methodPath + listOf(localVar.name)
                val localVarType = asmGetObjectTypePathFromDescriptor(localVar.descriptor) ?: return@mapNotNull null
                val localVarTypeDep = ASMDependency(localVarPath, localVarType, DependencyType.LocalVarType)

                val deps = localVar
                    .signature
                    ?.let(::asmGetObjectTypePathsFromDescriptor)
                    ?.map { ASMDependency(localVarPath, it, DependencyType.LocalVarType) }
                    ?: listOf<Dependency>(localVarTypeDep)

                ASMLocalVariableSubject(methodSubject, localVar.name, localVar.descriptor, localVarPath, deps)
            }
        }

        if (localVariables.isNotEmpty()) methodSubject.localVariableSubject = localVariables

        return methodSubject
    }

    fun toClassSubject(stringInterner: Interner<String>): ClassSubject {
        if (!visitIsCalled) error("Class header can't be readed!")
        val classPath = asmClassNameToPath(name).map(stringInterner)
        val classDeps = mutableListOf<Dependency>()

        superName?.let { superName ->
            if (superName == "java/lang/Object") null
            else asmClassNameToPath(superName).map(stringInterner)
        }?.let { superPath ->
            val extensionDep = ASMDependency(classPath, superPath, DependencyType.ClassExtension)
            classDeps.add(extensionDep)
        }

        classDeps.addAll(
            interfaces.map(::asmClassNameToPath).map { ifPath ->
                ASMDependency(classPath, ifPath.map(stringInterner), DependencyType.InterfaceImplementation)
            }
        )

        val classSubject = ASMClassSubject(classPath, stringInterner(name), access, classDeps, readingContext)

        val classSubjectFields = visitedFields.map { field ->
            val fieldName = stringInterner(field.name)
            val fieldPath = classPath + listOf(fieldName)

            val dependencies = (field.signature ?: field.descriptor)
                .let(::asmGetObjectTypePathsFromDescriptor)
                .map { depPath ->
                    ASMDependency(fieldPath, depPath.map(stringInterner), DependencyType.FieldType)
                }

            val isStatic = field.access and Opcodes.ACC_STATIC != 0

            ASMFieldSubject(classSubject, isStatic, fieldName, stringInterner(field.descriptor), fieldPath, dependencies)
        }

        if (classSubjectFields.isNotEmpty()) classSubject.fields = classSubjectFields

        val classSubjectMethods = visitedMethods.map { it.toMethodSubject(classSubject) }

        if (classSubjectMethods.isNotEmpty()) classSubject.methods = classSubjectMethods

        return classSubject
    }

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?) {
        super.visit(version, access, name, signature, superName, interfaces)

        visitIsCalled = true

        this.version = version
        this.access = access
        this.name = name
        this.signature = signature
        this.superName = superName
        this.interfaces = interfaces?.asList() ?: emptyList()
    }

    override fun visitSource(source: String?, debug: String?) {
        super.visitSource(source, debug)

        this.source = source
        this.debug = debug
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        visitedFields.add(
            VisitedFieldInformation(access, name, descriptor, signature, value)
        )

        return super.visitField(access, name, descriptor, signature, value)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor {
        val methodVisitor = MethodIndexVisitor(api)

        visitedMethods.add(
            VisitedMethodInformation(access, name, descriptor, signature, exceptions?.asList() ?: emptyList(), methodVisitor)
        )

        return methodVisitor
    }
}