package com.github.dmitriyushakov.srv_decompiler.utils.highlight.parse

import com.github.dmitriyushakov.srv_decompiler.common.SharedMutableReference
import com.github.dmitriyushakov.srv_decompiler.common.getOrNull
import com.github.dmitriyushakov.srv_decompiler.common.ref
import com.github.dmitriyushakov.srv_decompiler.exception.JavaParserProblemsException
import com.github.dmitriyushakov.srv_decompiler.highlight.*
import com.github.dmitriyushakov.srv_decompiler.highlight.TokenType.*
import com.github.dmitriyushakov.srv_decompiler.highlight.scope.ClassScope
import com.github.dmitriyushakov.srv_decompiler.highlight.scope.CompilationUnitScope
import com.github.dmitriyushakov.srv_decompiler.highlight.scope.MethodScope
import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.FieldSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.Path
import com.github.dmitriyushakov.srv_decompiler.registry.globalIndexRegistry
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.addPathSimpleName
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.getPathShortName
import com.github.dmitriyushakov.srv_decompiler.utils.highlight.sourceToHighlight
import com.github.javaparser.JavaParser
import com.github.javaparser.JavaToken
import com.github.javaparser.JavaToken.Kind.*
import com.github.javaparser.Problem
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.VariableDeclarationExpr
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
import org.slf4j.LoggerFactory

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

private fun CompilationUnitScope.Builder.addAsteriskImport(path: Path) {
    val packageChildren = globalIndexRegistry.subjectsIndex.getChildItems(path)

    for ((_, subjects) in packageChildren) {
        for (subject in subjects) {
            val subjPath = subject.path
            val shortName = getPathShortName(subjPath)
            if (shortName != null) {
                addTypeImportOnDemand(shortName, subjPath)
            }
        }
    }
}

private fun CompilationUnitScope.Builder.addSingleImport(subject: Subject) {
    val subjPath = subject.path
    val shortName = getPathShortName(subjPath)
    if (shortName != null) {
        addSingleTypeImport(shortName, subjPath)
    }
    for (child in subject.childrenSubjects) {
        if (child is ClassSubject) {
            addSingleImport(child)
        }
    }
}

private fun CompilationUnitScope.Builder.addSingleImport(path: Path) {
    for (subject in globalIndexRegistry.subjectsIndex.get(path)) {
        addSingleImport(subject)
    }
}

private fun CompilationUnit.collectCompilationUnitScope(): CompilationUnitScope = CompilationUnitScope.build {
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
                    for (child in subject.childrenSubjects) {
                        if (child is MethodSubject && child.static) {
                            addStaticImportOnDemand(child.name, child.path)
                        }
                        if (child is FieldSubject && child.static) {
                            addStaticImportOnDemand(child.name, child.path)
                        }
                    }
                }
            } else {
                val subjects = globalIndexRegistry.subjectsIndex[path]
                for (subject in subjects) {
                    if (subject is MethodSubject && subject.static) {
                        addSingleTypeStaticImport(subject.name, subject.path)
                    }
                    if (subject is FieldSubject && subject.static) {
                        addSingleTypeStaticImport(subject.name, subject.path)
                    }
                }
            }
        } else {
            if (importDeclaration.isAsterisk) {
                addAsteriskImport(path)
            } else {
                addSingleImport(path)
            }
        }
    }
}

private val <T: TypeDeclaration<*>> TypeDeclaration<T>.nestedTypes get(): List<TypeDeclaration<T>> =
    members.mapNotNull { it as? TypeDeclaration<T> }

private fun TypeDeclaration<*>.collectClassScope(compilationUnitScope: CompilationUnitScope): ClassScope = compilationUnitScope.buildClassScope {
    val declaration = this@collectClassScope
    if (declaration.fullyQualifiedName.isEmpty) return@buildClassScope
    val classPath = declaration.fullyQualifiedName.get().split('.')

    for (field in declaration.fields) {
        for (variable in field.variables) {
            val fieldName = variable.name.asString()
            val fieldPath = addPathSimpleName(classPath, fieldName)
            addField(fieldName, fieldPath)
        }
    }

    for (method in declaration.methods) {
        val methodName = method.name.asString()
        val methodPath = addPathSimpleName(classPath, methodName)
        addMethod(methodName, methodPath)
    }

    for (nestedType in declaration.nestedTypes) {
        val nestedTypeName = nestedType.name.asString()
        val nestedTypePath = addPathSimpleName(classPath, nestedTypeName)
        addClass(nestedTypeName, nestedTypePath)
    }
}

private fun MethodScope.fillScopeByVariables(blockPath: Path, variables: Iterable<VariableDeclarator>): MethodScope {
    return buildChild {
        for (variable in variables) {
            val variableName = variable.name.asString()
            val variablePath = addPathSimpleName(blockPath, variableName)

            addLocalVar(variableName, variablePath)
        }
    }
}

private fun SharedMutableReference<MethodScope>.fillScopeByVariables(blockPath: Path, variables: Iterable<VariableDeclarator>) {
    reference = reference.fillScopeByVariables(blockPath, variables)
}

private fun highlightVisitExpression(expression: Expression, blockPath: Path, scope: SharedMutableReference<MethodScope>) {
    if (expression is VariableDeclarationExpr) {
        scope.fillScopeByVariables(blockPath, expression.variables)
    }
}
private fun highlightVisitStatement(statement: Statement, blockPath: Path, scope: SharedMutableReference<MethodScope>) {
    when (statement) {
        is ExpressionStmt -> {
            val expression = statement.expression
            highlightVisitExpression(expression, blockPath, scope)
        }

        is ForStmt -> {
            val initialization = statement.initialization
            val forScope = ref(scope.reference)

            for (expr in initialization) {
                highlightVisitExpression(expr, blockPath, forScope)
            }

            highlightVisitStatement(statement.body, blockPath, forScope)
        }

        is BlockStmt -> {
            statement.highlightVisitBlock(blockPath, scope.reference)
        }

        is DoStmt -> {
            highlightVisitStatement(statement.body, blockPath, scope)
            highlightVisitExpression(statement.condition, blockPath, scope)
        }

        is ForEachStmt -> {
            val forEachScope = scope.reference.fillScopeByVariables(blockPath, statement.variable.variables)
            val forEachBody = statement.body

            highlightVisitStatement(forEachBody, blockPath, ref(forEachScope))
        }

        is IfStmt -> {
            highlightVisitExpression(statement.condition, blockPath, scope)
            highlightVisitStatement(statement.thenStmt, blockPath, scope)
            statement.elseStmt.getOrNull()?.let { highlightVisitStatement(it, blockPath, scope) }
        }

        is ReturnStmt -> {
            statement.expression.getOrNull()?.let { highlightVisitExpression(it, blockPath, scope) }
        }

        is SwitchStmt -> {
            highlightVisitExpression(statement.selector, blockPath, scope)

            for (entry in statement.entries) {
                for (label in entry.labels) {
                    highlightVisitExpression(label, blockPath, scope)
                }

                for (entryStatement in entry.statements) {
                    highlightVisitStatement(entryStatement, blockPath, scope)
                }
            }
        }

        is SynchronizedStmt -> {
            highlightVisitExpression(statement.expression, blockPath, scope)
            statement.body.highlightVisitBlock(blockPath, scope.reference)
        }

        is ThrowStmt -> {
            highlightVisitExpression(statement.expression, blockPath, scope)
        }

        is TryStmt -> {
            val tryScope = ref(scope.reference)
            for (res in statement.resources) {
                highlightVisitExpression(res, blockPath, tryScope)
            }

            statement.tryBlock.highlightVisitBlock(blockPath, tryScope.reference)

            for (catchClause in statement.catchClauses) {
                val parameterName = catchClause.parameter.name.asString()
                val parameterPath = addPathSimpleName(blockPath, parameterName)
                val catchScope = tryScope.reference.buildChild {
                    addLocalVar(parameterName, parameterPath)
                }

                catchClause.body.highlightVisitBlock(blockPath, catchScope)
            }

            statement.finallyBlock.getOrNull()?.highlightVisitBlock(blockPath, tryScope.reference)
        }

        is WhileStmt -> {
            highlightVisitExpression(statement.condition, blockPath, scope)
            highlightVisitStatement(statement.body, blockPath, scope)
        }

        is YieldStmt -> {
            highlightVisitExpression(statement.expression, blockPath, scope)
        }

        is AssertStmt -> {
            highlightVisitExpression(statement.check, blockPath, scope)
            statement.message.getOrNull()?.let { highlightVisitExpression(it, blockPath, scope) }
        }

        is ExplicitConstructorInvocationStmt -> {
            statement.expression.getOrNull()?.let { highlightVisitExpression(it, blockPath, scope) }
            for (arg in statement.arguments) {
                highlightVisitExpression(arg, blockPath, scope)
            }
        }
    }
}

private fun BlockStmt.highlightVisitBlock(blockPath: Path, initialScope: MethodScope) {
    val scope = ref(initialScope)

    for (statement in statements) {
        highlightVisitStatement(statement, blockPath, scope)
    }
}

private fun highlightVisitType(cuScope: CompilationUnitScope, typeDeclaration: TypeDeclaration<*>) {
    val classScope = typeDeclaration.collectClassScope(cuScope)
    val classPath = typeDeclaration.fullyQualifiedName.get().split('.')

    for (method in typeDeclaration.methods) {
        val body = method.body.getOrNull()
        if (body != null) {
            val methodName = method.name.asString()
            val methodPath = addPathSimpleName(classPath, methodName)

            val methodScope = classScope.buildMethodScope {
                for (parameter in method.parameters) {
                    val parameterName = parameter.name.asString()
                    val parameterPath = addPathSimpleName(methodPath, parameterName)

                    addLocalVar(parameterName, parameterPath)
                }
            }

            body.highlightVisitBlock(methodPath, methodScope)
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

                addLocalVar(parameterName, parameterPath)
            }
        }

        body.highlightVisitBlock(constructorPath, constructorScope)
    }

    for(nestedType in typeDeclaration.nestedTypes) {
        highlightVisitType(cuScope, typeDeclaration)
    }
}

fun javaSourceToHighlight(text: String): CodeHighlight {
    val javaParser = JavaParser()
    val parseResult = javaParser.parse(text)
    val cu = parseResult.result.getOrNull()

    if (cu != null) {
        if (!parseResult.isSuccessful) {
            logger.error("Some problems was found during parsing source:\n${problemsToString(parseResult.problems)}")
        }

        val cuScope = cu.collectCompilationUnitScope()

        for (typeDeclaration in cu.types) {
            highlightVisitType(cuScope, typeDeclaration)
        }

        val currentLineTokens = mutableListOf<Token>()
        val highlightLines = mutableListOf<CodeLine>()

        for (javaToken in cu.tokenRange.get()) {
            if (javaToken.isEndOfLine) {
                highlightLines.add(CodeLine(currentLineTokens.toList()))
                currentLineTokens.clear()
            } else {
                currentLineTokens.add(javaToken.toHighlightToken())
            }
        }

        if (currentLineTokens.size > 0) highlightLines.add(CodeLine(currentLineTokens.toList()))

        return CodeHighlight(highlightLines)
    } else {
        throw JavaParserProblemsException(parseResult.problems)
    }
}