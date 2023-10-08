package com.github.dmitriyushakov.srv_decompiler.utils.highlight.parse

import com.github.dmitriyushakov.srv_decompiler.common.SharedMutableReference
import com.github.dmitriyushakov.srv_decompiler.common.getOrNull
import com.github.dmitriyushakov.srv_decompiler.common.ref
import com.github.dmitriyushakov.srv_decompiler.exception.JavaParserProblemsException
import com.github.dmitriyushakov.srv_decompiler.highlight.*
import com.github.dmitriyushakov.srv_decompiler.highlight.TokenType.*
import com.github.dmitriyushakov.srv_decompiler.highlight.scope.*
import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.FieldSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.Path
import com.github.dmitriyushakov.srv_decompiler.registry.globalIndexRegistry
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.addPathSimpleName
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.getPathShortName
import com.github.javaparser.JavaParser
import com.github.javaparser.JavaToken
import com.github.javaparser.JavaToken.Kind.*
import com.github.javaparser.Problem
import com.github.javaparser.Range
import com.github.javaparser.TokenRange
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.ArrayInitializerExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.InstanceOfExpr
import com.github.javaparser.ast.expr.LambdaExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.MethodReferenceExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.SuperExpr
import com.github.javaparser.ast.expr.SwitchExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.expr.TypeExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.stmt.AssertStmt
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.SwitchStmt
import com.github.javaparser.ast.stmt.SynchronizedStmt
import com.github.javaparser.ast.stmt.ThrowStmt
import com.github.javaparser.ast.stmt.TryStmt
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.stmt.YieldStmt
import com.github.javaparser.ast.type.Type
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

private class HighlightParseUtils

private val logger = LoggerFactory.getLogger(HighlightParseUtils::class.java)

fun JavaToken.toTokenType(): TokenType = when(valueOf(kind)) {
    SPACE -> Spacing
    SINGLE_LINE_COMMENT,
    ENTER_JAVADOC_COMMENT,
    ENTER_MULTILINE_COMMENT,
    JAVADOC_COMMENT,
    MULTI_LINE_COMMENT,
    COMMENT_CONTENT -> Comment
    ABSTRACT,
    ASSERT,
    BREAK,
    CASE,
    CATCH,
    CLASS,
    CONST,
    CONTINUE,
    _DEFAULT,
    DO,
    ELSE,
    ENUM,
    EXTENDS,
    FINAL,
    FINALLY,
    FOR,
    GOTO,
    IF,
    IMPLEMENTS,
    IMPORT,
    INSTANCEOF,
    INTERFACE,
    NATIVE,
    NEW,
    NULL,
    PACKAGE,
    PRIVATE,
    PROTECTED,
    PUBLIC,
    RECORD,
    RETURN,
    STATIC,
    STRICTFP,
    SUPER,
    SWITCH,
    SYNCHRONIZED,
    THIS,
    THROW,
    THROWS,
    TRANSIENT,
    TRY,
    VOLATILE,
    WHILE,
    YIELD,
    REQUIRES,
    TO,
    WITH,
    OPEN,
    OPENS,
    USES,
    MODULE,
    EXPORTS,
    PROVIDES,
    TRANSITIVE -> Keyword
    BOOLEAN,
    BYTE,
    CHAR,
    DOUBLE,
    FLOAT,
    INT,
    LONG,
    SHORT,
    VOID -> PrimitiveType
    FALSE,
    TRUE -> BooleanLiteral
    LONG_LITERAL,
    INTEGER_LITERAL,
    DECIMAL_LITERAL,
    HEX_LITERAL,
    OCTAL_LITERAL,
    BINARY_LITERAL,
    FLOATING_POINT_LITERAL,
    DECIMAL_FLOATING_POINT_LITERAL,
    DECIMAL_EXPONENT,
    HEXADECIMAL_FLOATING_POINT_LITERAL,
    HEXADECIMAL_EXPONENT,
    HEX_DIGITS -> NumberLiteral
    UNICODE_ESCAPE,
    CHARACTER_LITERAL,
    STRING_LITERAL,
    ENTER_TEXT_BLOCK,
    TEXT_BLOCK_LITERAL,
    TEXT_BLOCK_CONTENT -> StringLiteral
    IDENTIFIER -> Identifier
    ASSIGN,
    LT,
    BANG,
    TILDE,
    HOOK,
    EQ,
    GE,
    LE,
    NE,
    SC_AND,
    SC_OR,
    INCR,
    DECR,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    BIT_AND,
    BIT_OR,
    XOR,
    REM,
    LSHIFT,
    PLUSASSIGN,
    MINUSASSIGN,
    STARASSIGN,
    SLASHASSIGN,
    ANDASSIGN,
    ORASSIGN,
    XORASSIGN,
    REMASSIGN,
    LSHIFTASSIGN,
    RSIGNEDSHIFTASSIGN,
    RUNSIGNEDSHIFTASSIGN,
    RUNSIGNEDSHIFT,
    RSIGNEDSHIFT,
    GT -> Operator
    else -> Default
}

val JavaToken.isEndOfLine: Boolean get() = when(valueOf(kind)) {
    WINDOWS_EOL, UNIX_EOL, OLD_MAC_EOL -> true
    else -> false
}

fun JavaToken.toHighlightToken(): Token = BasicToken(toTokenType(), text)

fun problemsToString(problems: List<Problem>): String {
    val sb = StringBuilder()
    sb.append("This problems are happened during java source parse:\n")

    for ((idx, problem) in problems.withIndex()) {
        if (idx > 0) sb.append("\n")
        sb.append(idx + 1).append(". ").append(problem.message).append("\n")
        sb.append("Description: ").append(problem.verboseMessage).append("\n")
        val location = problem.location.getOrNull()
        if (location != null) {
            val range = location.begin.range.getOrNull()
            if (range != null) {
                val position = range.begin

                sb.append("Position: column - ").append(position.column).append(", line - ").append(position.line)
            }
            sb.append("Source: ").append(location.toString()).append("\n")
        }
    }

    return sb.toString()
}

private class LinksAccumulator {
    val links: MutableList<LinkItem> = mutableListOf()

    fun add(range: Range, link: Link) {
        links.add(LinkItem(range, link))
    }

    data class LinkItem(
        val range: Range,
        val link: Link
    )
}

private fun CompilationUnitScope.Builder.addAsteriskImport(path: Path) {
    val packageChildren = globalIndexRegistry.subjectsIndex.getChildItems(path)

    for ((_, subjects) in packageChildren) {
        var added = false
        for (subject in subjects) {
            val subjPath = subject.path
            val shortName = getPathShortName(subjPath)
            if (shortName != null) {
                addTypeImportOnDemand(shortName, subjPath)
                added = true
            }
            if (added) break
        }
        if (added) break
    }
}

private fun CompilationUnitScope.Builder.addSingleImport(linksAcc: LinksAccumulator, importDeclaration: ImportDeclaration, subject: Subject): Boolean {
    var added = false
    val subjPath = subject.path
    val shortName = getPathShortName(subjPath)
    if (shortName != null) {
        addSingleTypeImport(shortName, subjPath)

        val range = importDeclaration.name.range.getOrNull()
        val link = Link.fromPath(subjPath, LinkType.Class)

        if (range != null && link != null) linksAcc.add(range, link)

        added = true
    }
    for (child in subject.childrenSubjects) {
        if (child is ClassSubject) {
            added = added || addSingleImport(linksAcc, importDeclaration, child)
        }
    }
    return added
}

private fun CompilationUnitScope.Builder.addSingleImport(linksAcc: LinksAccumulator, importDeclaration: ImportDeclaration, path: Path) {
    for (subject in globalIndexRegistry.subjectsIndex.get(path)) {
        val added = addSingleImport(linksAcc, importDeclaration, subject)
        if (added) break
    }
}

private fun CompilationUnit.collectCompilationUnitScope(linksAcc: LinksAccumulator): CompilationUnitScope = CompilationUnitScope.build {
    val cu = this@collectCompilationUnitScope

    val packageDeclaration = cu.packageDeclaration.getOrNull()
    if (packageDeclaration != null) {
        val path = packageDeclaration.name.asString().split('.')
        addAsteriskImport(path)
    }

    for (importDeclaration in imports) {
        val path = importDeclaration.name.asString().split('.')
        if (importDeclaration.isStatic) {
            if (importDeclaration.isAsterisk) {
                val subjects = globalIndexRegistry.subjectsIndex[path]
                for (subject in subjects) {
                    var added = false
                    for (child in subject.childrenSubjects) {
                        if (child is MethodSubject && child.static) {
                            addStaticImportOnDemand(child.name, child.path)
                            added = true
                        }
                        if (child is FieldSubject && child.static) {
                            addStaticImportOnDemand(child.name, child.path)
                            added = true
                        }
                        if (added) break
                    }
                    if (added) break
                }
            } else {
                val subjects = globalIndexRegistry.subjectsIndex[path]
                for (subject in subjects) {
                    var added = false
                    if (subject is MethodSubject && subject.static) {
                        addSingleTypeStaticImport(subject.name, subject.path)
                        added = true
                    }
                    if (subject is FieldSubject && subject.static) {
                        addSingleTypeStaticImport(subject.name, subject.path)
                        added = true
                    }
                    if (added) {
                        val link = Link.fromPath(subject.path, LinkType.Class)
                        val range = importDeclaration.name.range.getOrNull()

                        if (link != null && range != null) linksAcc.add(range, link)
                        break
                    }
                }
            }
        } else {
            if (importDeclaration.isAsterisk) {
                addAsteriskImport(path)
            } else {
                addSingleImport(linksAcc, importDeclaration, path)
            }
        }
    }
}

private val Node.lineNumber: Int? get() = range.getOrNull()?.begin?.line

private val <T: TypeDeclaration<*>> TypeDeclaration<T>.nestedTypes get(): List<TypeDeclaration<T>> =
    members.mapNotNull { it as? TypeDeclaration<T> }

private fun TypeDeclaration<*>.collectClassScope(linksAcc: LinksAccumulator, parentScope: Scope): ClassScope = ClassScope.buildFrom(parentScope) {
    val declaration = this@collectClassScope
    if (declaration.fullyQualifiedName.isEmpty) return@buildFrom
    val classPath = declaration.fullyQualifiedName.get().split('.')

    for (field in declaration.fields) {
        for (variable in field.variables) {
            val fieldName = variable.name.asString()
            val fieldPath = addPathSimpleName(classPath, fieldName)
            val lineNumber = variable.lineNumber

            addField(fieldName, fieldPath, lineNumber)
        }
    }

    for (method in declaration.methods) {
        val methodName = method.name.asString()
        val methodPath = addPathSimpleName(classPath, methodName)
        val lineNumber = method.lineNumber

        addMethod(methodName, methodPath, lineNumber)
    }

    for (nestedType in declaration.nestedTypes) {
        val nestedTypeName = nestedType.name.asString()
        val nestedTypePath = addPathSimpleName(classPath, nestedTypeName)
        val lineNumber = nestedType.lineNumber

        addClass(nestedTypeName, nestedTypePath, lineNumber)
    }
}

private fun MethodScope.fillScopeByVariables(blockPath: Path, variables: Iterable<VariableDeclarator>): MethodScope {
    return buildChild {
        for (variable in variables) {
            val variableName = variable.name.asString()
            val variablePath = addPathSimpleName(blockPath, variableName)
            val variableLineNumber = variable.lineNumber

            addLocalVar(variableName, variablePath, variableLineNumber)
        }
    }
}

private fun SharedMutableReference<MethodScope>.fillScopeByVariables(blockPath: Path, variables: Iterable<VariableDeclarator>) {
    reference = reference.fillScopeByVariables(blockPath, variables)
}

private fun highlightVisitMethodExpression(linksAcc: LinksAccumulator, expression: Expression, blockPath: Path, scope: SharedMutableReference<MethodScope>) {
    if (expression is VariableDeclarationExpr) {
        for (variable in expression.variables) {
            addTypeLink(variable.type, linksAcc, scope.reference)
            variable.initializer.getOrNull()?.let { highlightVisitExpression(linksAcc, it, blockPath, scope.reference) }
        }
        scope.fillScopeByVariables(blockPath, expression.variables)
    } else {
        highlightVisitExpression(linksAcc, expression, blockPath, scope.reference)
    }
}

private fun Scope.toMethodScope(): MethodScope = (this as? MethodScope) ?: MethodScope.Builder(this).build()

private fun highlightVisitExpression(linksAcc: LinksAccumulator, expression: Expression, blockPath: Path, scope: Scope) {
    when (expression) {
        is ArrayAccessExpr -> {
            highlightVisitExpression(linksAcc, expression.name, blockPath, scope)
            highlightVisitExpression(linksAcc, expression.index, blockPath, scope)
        }
        is ArrayCreationExpr -> {
            addTypeLink(expression.elementType, linksAcc, scope)
            expression.initializer.getOrNull()?.let { highlightVisitExpression(linksAcc, it, blockPath, scope) }
        }
        is ArrayInitializerExpr -> {
            for (valueExpr in expression.values) {
                highlightVisitExpression(linksAcc, valueExpr, blockPath, scope)
            }
        }
        is AssignExpr -> {
            highlightVisitExpression(linksAcc, expression.target, blockPath, scope)
            highlightVisitExpression(linksAcc, expression.value, blockPath, scope)
        }
        is BinaryExpr -> {
            highlightVisitExpression(linksAcc, expression.left, blockPath, scope)
            highlightVisitExpression(linksAcc, expression.right, blockPath, scope)
        }
        is CastExpr -> {
            addTypeLink(expression.type, linksAcc, scope)
            highlightVisitExpression(linksAcc, expression.expression, blockPath, scope)
        }
        is ClassExpr -> {
            addTypeLink(expression.type, linksAcc, scope)
        }
        is ConditionalExpr -> {
            highlightVisitExpression(linksAcc, expression.condition, blockPath, scope)
            highlightVisitExpression(linksAcc, expression.thenExpr, blockPath, scope)
            highlightVisitExpression(linksAcc, expression.elseExpr, blockPath, scope)
        }
        is EnclosedExpr -> {
            highlightVisitExpression(linksAcc, expression.inner, blockPath, scope)
        }
        is FieldAccessExpr -> {
            highlightVisitExpression(linksAcc, expression.scope, blockPath, scope)
            //TODO: scope expression type resolution and field link search
        }
        is InstanceOfExpr -> {
            highlightVisitExpression(linksAcc, expression.expression, blockPath, scope)
            addTypeLink(expression.type, linksAcc, scope)
        }
        is LambdaExpr -> {
            val methodScope = scope.toMethodScope().let(::ref)
            highlightVisitStatement(linksAcc, expression.body, blockPath, methodScope)
        }
        is MethodCallExpr -> {
            expression.scope.getOrNull()?.let { highlightVisitExpression(linksAcc, it, blockPath, scope) }
            //TODO: scope expression type resolution and method link search
            for (argument in expression.arguments) {
                highlightVisitExpression(linksAcc, argument, blockPath, scope)
            }
        }
        is MethodReferenceExpr -> {
            highlightVisitExpression(linksAcc, expression.scope, blockPath, scope)
            //TODO: scope expression type resolution and method link search
            expression.typeArguments.getOrNull()?.forEach { type ->
                addTypeLink(type, linksAcc, scope)
            }
        }
        is NameExpr -> {
            val name = expression.name.asString()
            val link = scope.resolveLocalVariable(name) ?: scope.resolveField(name)
            val range = expression.range.getOrNull()

            if (range != null && link != null) linksAcc.add(range, link)
        }
        is ObjectCreationExpr -> {
            expression.scope.getOrNull()?.let { highlightVisitExpression(linksAcc, expression, blockPath, scope) }
            addTypeLink(expression.type, linksAcc, scope)
            expression.typeArguments.getOrNull()?.forEach { type ->
                addTypeLink(type, linksAcc, scope)
            }
            for (argument in expression.arguments) {
                highlightVisitExpression(linksAcc, argument, blockPath, scope)
            }
        }
        is SuperExpr -> {
            val typeName = expression.typeName.getOrNull()
            if (typeName != null) {
                val typeLink = typeName.asString().let { scope.resolveClass(it) }
                val typeRange = typeName.range.getOrNull()

                if (typeLink != null && typeRange != null) linksAcc.add(typeRange, typeLink)
            }
        }
        is SwitchExpr -> {
            highlightVisitExpression(linksAcc, expression.selector, blockPath, scope)
            val methodScope = scope.toMethodScope().let(::ref)
            for (entry in expression.entries) {
                for (label in entry.labels) {
                    highlightVisitMethodExpression(linksAcc, label, blockPath, methodScope)
                }

                for (entryStatement in entry.statements) {
                    highlightVisitStatement(linksAcc, entryStatement, blockPath, methodScope)
                }
            }
        }
        is ThisExpr -> {
            val typeName = expression.typeName.getOrNull()
            if (typeName != null) {
                val typeLink = typeName.asString().let { scope.resolveClass(it) }
                val typeRange = typeName.range.getOrNull()

                if (typeLink != null && typeRange != null) linksAcc.add(typeRange, typeLink)
            }
        }
        is TypeExpr -> {
            addTypeLink(expression.type, linksAcc, scope)
        }
        is UnaryExpr -> {
            highlightVisitExpression(linksAcc, expression.expression, blockPath, scope)
        }
    }
}
private fun highlightVisitStatement(linksAcc: LinksAccumulator, statement: Statement, blockPath: Path, scope: SharedMutableReference<MethodScope>) {
    when (statement) {
        is ExpressionStmt -> {
            val expression = statement.expression
            highlightVisitMethodExpression(linksAcc, expression, blockPath, scope)
        }

        is ForStmt -> {
            val initialization = statement.initialization
            val forScope = ref(scope.reference)

            for (expr in initialization) {
                highlightVisitMethodExpression(linksAcc, expr, blockPath, forScope)
            }

            highlightVisitStatement(linksAcc, statement.body, blockPath, forScope)
        }

        is BlockStmt -> {
            statement.highlightVisitBlock(linksAcc, blockPath, scope.reference)
        }

        is DoStmt -> {
            highlightVisitStatement(linksAcc, statement.body, blockPath, scope)
            highlightVisitMethodExpression(linksAcc, statement.condition, blockPath, scope)
        }

        is ForEachStmt -> {
            val forEachScope = scope.reference.fillScopeByVariables(blockPath, statement.variable.variables)
            val forEachBody = statement.body

            highlightVisitStatement(linksAcc, forEachBody, blockPath, ref(forEachScope))
        }

        is IfStmt -> {
            highlightVisitMethodExpression(linksAcc, statement.condition, blockPath, scope)
            highlightVisitStatement(linksAcc, statement.thenStmt, blockPath, scope)
            statement.elseStmt.getOrNull()?.let { highlightVisitStatement(linksAcc, it, blockPath, scope) }
        }

        is ReturnStmt -> {
            statement.expression.getOrNull()?.let { highlightVisitMethodExpression(linksAcc, it, blockPath, scope) }
        }

        is SwitchStmt -> {
            highlightVisitMethodExpression(linksAcc, statement.selector, blockPath, scope)

            for (entry in statement.entries) {
                for (label in entry.labels) {
                    highlightVisitMethodExpression(linksAcc, label, blockPath, scope)
                }

                for (entryStatement in entry.statements) {
                    highlightVisitStatement(linksAcc, entryStatement, blockPath, scope)
                }
            }
        }

        is SynchronizedStmt -> {
            highlightVisitMethodExpression(linksAcc, statement.expression, blockPath, scope)
            statement.body.highlightVisitBlock(linksAcc, blockPath, scope.reference)
        }

        is ThrowStmt -> {
            highlightVisitMethodExpression(linksAcc, statement.expression, blockPath, scope)
        }

        is TryStmt -> {
            val tryScope = ref(scope.reference)
            for (res in statement.resources) {
                highlightVisitMethodExpression(linksAcc, res, blockPath, tryScope)
            }

            statement.tryBlock.highlightVisitBlock(linksAcc, blockPath, tryScope.reference)

            for (catchClause in statement.catchClauses) {
                val parameterName = catchClause.parameter.name.asString()
                val parameterPath = addPathSimpleName(blockPath, parameterName)
                val parameterLineNumber = catchClause.parameter.lineNumber

                val catchScope = tryScope.reference.buildChild {
                    addLocalVar(parameterName, parameterPath, parameterLineNumber)
                }

                catchClause.body.highlightVisitBlock(linksAcc, blockPath, catchScope)
            }

            statement.finallyBlock.getOrNull()?.highlightVisitBlock(linksAcc, blockPath, tryScope.reference)
        }

        is WhileStmt -> {
            highlightVisitMethodExpression(linksAcc, statement.condition, blockPath, scope)
            highlightVisitStatement(linksAcc, statement.body, blockPath, scope)
        }

        is YieldStmt -> {
            highlightVisitMethodExpression(linksAcc, statement.expression, blockPath, scope)
        }

        is AssertStmt -> {
            highlightVisitMethodExpression(linksAcc, statement.check, blockPath, scope)
            statement.message.getOrNull()?.let { highlightVisitMethodExpression(linksAcc, it, blockPath, scope) }
        }

        is ExplicitConstructorInvocationStmt -> {
            statement.expression.getOrNull()?.let { highlightVisitMethodExpression(linksAcc, it, blockPath, scope) }
            for (arg in statement.arguments) {
                highlightVisitMethodExpression(linksAcc, arg, blockPath, scope)
            }
        }
    }
}

private fun BlockStmt.highlightVisitBlock(linksAcc: LinksAccumulator, blockPath: Path, initialScope: MethodScope) {
    val scope = ref(initialScope)

    for (statement in statements) {
        highlightVisitStatement(linksAcc, statement, blockPath, scope)
    }
}

private fun addTypeLink(type: Type, linksAcc: LinksAccumulator, scope: Scope) {
    val typeRange = type.range.getOrNull()
    val typeName = (type as? NodeWithSimpleName<*>)?.name?.asString()

    if (typeRange != null && typeName != null) {
        val variableTypeLink = scope.resolveClass(typeName)
        if (variableTypeLink != null) linksAcc.add(typeRange, variableTypeLink)
    }
}

private fun highlightVisitType(linksAcc: LinksAccumulator, scope: Scope, typeDeclaration: TypeDeclaration<*>) {
    val classScope = typeDeclaration.collectClassScope(linksAcc, scope)
    val classPath = typeDeclaration.fullyQualifiedName.get().split('.')

    for (field in typeDeclaration.fields) {
        for (variable in field.variables) {
            addTypeLink(variable.type, linksAcc, scope)

            val variableInitializer = variable.initializer.getOrNull()
            if (variableInitializer != null) {
                val variableName = variable.name.asString()
                val variablePath = addPathSimpleName(classPath, variableName)
                highlightVisitExpression(linksAcc, variableInitializer, variablePath, classScope)
            }
        }
    }

    for (method in typeDeclaration.methods) {
        val body = method.body.getOrNull()
        if (body != null) {
            val methodName = method.name.asString()
            val methodPath = addPathSimpleName(classPath, methodName)

            val methodScope = classScope.buildMethodScope {
                for (parameter in method.parameters) {
                    val parameterName = parameter.name.asString()
                    val parameterPath = addPathSimpleName(methodPath, parameterName)

                    addLocalVar(parameterName, parameterPath, parameter.lineNumber)
                }
            }

            val returnType = method.type
            val returnTypeName = returnType.asString()
            val returnTypeRange = returnType.range.getOrNull()
            val returnTypeLink = classScope.resolveClass(returnTypeName)

            if (returnTypeRange != null && returnTypeLink != null) linksAcc.add(returnTypeRange, returnTypeLink)

            body.highlightVisitBlock(linksAcc, methodPath, methodScope)
        }
    }

    for (constructor in typeDeclaration.constructors) {
        val constructorName = if (constructor.isStatic) "<clinit>" else "<init>"
        val constructorPath = addPathSimpleName(classPath, constructorName)
        val body = constructor.body

        val constructorScope = classScope.buildMethodScope {
            for (parameter in constructor.parameters) {
                val parameterName = parameter.name.asString()
                val parameterPath = addPathSimpleName(constructorPath, parameterName)

                addLocalVar(parameterName, parameterPath, parameter.lineNumber)
            }
        }

        body.highlightVisitBlock(linksAcc, constructorPath, constructorScope)
    }

    for(nestedType in typeDeclaration.nestedTypes) {
        highlightVisitType(linksAcc, classScope, nestedType)
    }
}

private fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null

private fun StringBuilder.flushLinkContents(link: Link?, lineTokens: MutableList<Token>) {
    if (!isEmpty()) {
        if (link == null) throw IllegalStateException("Null next link not expected on string builder flushing!")
        lineTokens.add(LinkedToken(toString(), link))
        clear()
    }
}

private fun List<CodeLine>.splitMultilineTokens(): List<CodeLine> {
    val newLines: MutableList<CodeLine> = mutableListOf()

    for (line in this) {
        val newTokens: MutableList<Token> = mutableListOf()
        var lineNumber = line.lineNumber
        var isMultiline = false

        for (token in line.tokens) {
            val splitToken = token.splitMultilineOrNull()

            if (splitToken == null) {
                newTokens.add(token)
            } else {
                isMultiline = true
                for (subToken in splitToken) {
                    newTokens.add(subToken)
                    val newLine = CodeLine(lineNumber, newTokens.toList())
                    newTokens.clear()
                    lineNumber = null
                    newLines.add(newLine)
                }
            }
        }

        if (!isMultiline) {
            val newLine = CodeLine(lineNumber, newTokens)
            newLines.add(newLine)
        }
    }

    return newLines
}

private fun processJavaTokens(javaTokens: TokenRange, linksAcc: LinksAccumulator): List<CodeLine> {
    val currentLineTokens = mutableListOf<Token>()
    val highlightLines = mutableListOf<CodeLine>()

    val links = linksAcc.links.sortedBy { it.range.begin }.iterator()
    var nextLink = links.nextOrNull()
    val currentLinkContents = StringBuilder()
    val flushLinkContents: () -> Unit = { currentLinkContents.flushLinkContents(nextLink?.link, currentLineTokens) }

    for (javaToken in javaTokens) {
        if (javaToken.isEndOfLine) {
            flushLinkContents()
            val lineNumber = javaToken.range.getOrNull()?.begin?.line
            highlightLines.add(CodeLine(lineNumber, currentLineTokens.toList()))
            currentLineTokens.clear()
        } else {
            val tokenRange = javaToken.range.getOrNull()
            val tokenContent = javaToken.text
            if (nextLink != null && tokenRange != null) {
                if (nextLink.range.contains(tokenRange)) {
                    currentLinkContents.append(tokenContent)
                } else {
                    flushLinkContents()
                    val isLink = if (tokenRange.begin.isAfterOrEqual(nextLink.range.end)){
                        nextLink = links.nextOrNull()

                        if (nextLink != null && nextLink.range.contains(tokenRange)) {
                            currentLinkContents.append(tokenContent)
                            true
                        } else false
                    } else false

                    if (!isLink) {
                        currentLineTokens.add(javaToken.toHighlightToken())
                    }
                }
            } else {
                flushLinkContents()
                currentLineTokens.add(javaToken.toHighlightToken())
            }
        }
    }
    flushLinkContents()

    val lastLineNumber = highlightLines.mapNotNull { it.lineNumber }.maxOrNull()?.let { it + 1 }
    if (currentLineTokens.size > 0) highlightLines.add(CodeLine(lastLineNumber, currentLineTokens.toList()))

    return highlightLines.splitMultilineTokens()
}

private fun <T: Node> List<T>.collectCodeDeclarations(path: Path, declarations: MutableList<CodeDeclaration>) {
    for (node in this) {
        when (node) {
            is ClassOrInterfaceDeclaration -> {
                val classPath = path + listOf(node.name.asString())
                val lineNumber = node.lineNumber ?: continue
                declarations.add(CodeDeclaration(classPath, lineNumber))
                node.childNodes.collectCodeDeclarations(classPath, declarations)
            }
            is FieldDeclaration -> {
                for (variable in node.variables) {
                    val variablePath = path + listOf(variable.name.asString())
                    val lineNumber = variable.lineNumber ?: continue
                    declarations.add(CodeDeclaration(variablePath, lineNumber))
                }
            }
            is ConstructorDeclaration -> {
                val constructorPath = path + listOf("<init>")
                val lineNumber = node.lineNumber ?: continue
                declarations.add(CodeDeclaration(constructorPath, lineNumber))
                node.body.statements.collectCodeDeclarations(constructorPath, declarations)
                for (parameter in node.parameters) {
                    val parLineNumber = parameter.lineNumber ?: continue
                    val parPath = constructorPath + listOf(parameter.name.asString())
                    declarations.add(CodeDeclaration(parPath, parLineNumber))
                }
            }
            is MethodDeclaration -> {
                val methodPath = path + listOf(node.name.asString())
                val lineNumber = node.lineNumber ?: continue
                declarations.add(CodeDeclaration(methodPath, lineNumber))
                node.body.getOrNull()?.statements?.collectCodeDeclarations(methodPath, declarations)
                for (parameter in node.parameters) {
                    val parLineNumber = parameter.lineNumber ?: continue
                    val parPath = methodPath + listOf(parameter.name.asString())
                    declarations.add(CodeDeclaration(parPath, parLineNumber))
                }
            }
            is ExpressionStmt -> {
                val expression = node.expression
                if (expression is VariableDeclarationExpr) {
                    for (variable in expression.variables) {
                        val variablePath = path + listOf(variable.name.asString())
                        val lineNumber = variable.lineNumber ?: continue
                        declarations.add(CodeDeclaration(variablePath, lineNumber))
                    }
                }
            }
        }
    }
}

fun CompilationUnit.collectCodeDeclarations(): List<CodeDeclaration> {
    val declarations: MutableList<CodeDeclaration> = mutableListOf()

    val path: Path = packageDeclaration.get().name.asString().split('.')
    types.collectCodeDeclarations(path, declarations)
    declarations.sortBy { it.lineNumber }

    return declarations.distinctBy { it.path }
}

fun javaSourceToHighlight(text: String): CodeHighlight {
    val javaParser = JavaParser()
    val parseResult = javaParser.parse(text)
    val cu = parseResult.result.getOrNull() ?: throw JavaParserProblemsException(parseResult.problems)

    if (!parseResult.isSuccessful) {
        logger.error("Some problems was found during parsing source:\n${problemsToString(parseResult.problems)}")
    }

    val linksAcc = LinksAccumulator()
    val cuScope = cu.collectCompilationUnitScope(linksAcc)

    for (typeDeclaration in cu.types) {
        highlightVisitType(linksAcc, cuScope, typeDeclaration)
    }

    val javaTokens = cu.tokenRange.getOrNull()
        ?: throw IllegalStateException("Not null token range expected from compilation unit")

    val highlightLines = processJavaTokens(javaTokens, linksAcc)
    val codeDeclarations = cu.collectCodeDeclarations()

    return CodeHighlight(highlightLines, codeDeclarations)
}