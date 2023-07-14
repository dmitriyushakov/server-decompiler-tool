package com.github.dmitriyushakov.srv_decompiler.utils.highlight.parse

import com.github.dmitriyushakov.srv_decompiler.exception.JavaParserProblemsException
import com.github.dmitriyushakov.srv_decompiler.highlight.*
import com.github.dmitriyushakov.srv_decompiler.highlight.TokenType.*
import com.github.dmitriyushakov.srv_decompiler.highlight.scope.CompilationUnitScope
import com.github.dmitriyushakov.srv_decompiler.indexer.model.ClassSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.FieldSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.MethodSubject
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject
import com.github.dmitriyushakov.srv_decompiler.registry.Path
import com.github.dmitriyushakov.srv_decompiler.registry.globalIndexRegistry
import com.github.dmitriyushakov.srv_decompiler.utils.bytecode.getPathShortName
import com.github.javaparser.JavaParser
import com.github.javaparser.JavaToken
import com.github.javaparser.JavaToken.Kind.*
import com.github.javaparser.ast.CompilationUnit

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

private fun CompilationUnit.collectScope(): CompilationUnitScope = CompilationUnitScope.build {
    val reg = globalIndexRegistry
    val cu = this@collectScope

    if (packageDeclaration.isPresent) {
        val packageDeclaration = cu.packageDeclaration.get()
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

fun javaSourceToHighlight(text: String): CodeHighlight {
    val javaParser = JavaParser()
    val parseResult = javaParser.parse(text)

    if (parseResult.isSuccessful) {
        val cu = parseResult.result.get()
        val cuScope = cu.collectScope()
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