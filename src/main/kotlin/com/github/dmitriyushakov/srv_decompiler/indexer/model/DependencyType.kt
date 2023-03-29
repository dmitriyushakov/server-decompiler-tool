package com.github.dmitriyushakov.srv_decompiler.indexer.model

enum class DependencyType {
    ClassExtension,
    InterfaceImplementation,
    FieldUsage,
    FieldType,
    MethodCall,
    ReturnType,
    LocalVarType,
    ArgumentType,
    Exception,
    CreationNewInstance,
    CreationNewArray,
    CheckingCast,
    CheckingInstanceOf
}