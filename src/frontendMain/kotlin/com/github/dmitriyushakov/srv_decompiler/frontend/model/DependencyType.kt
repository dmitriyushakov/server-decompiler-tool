package com.github.dmitriyushakov.srv_decompiler.frontend.model

enum class DependencyType(val displayName: String) {
    ClassExtension("Class extension"),
    InterfaceImplementation("Interface implementation"),
    FieldUsage("Field usage"),
    FieldType("Field type"),
    MethodCall("Method call"),
    ReturnType("Return type"),
    LocalVarType("Local var type"),
    ArgumentType("Argument type"),
    Exception("Exception"),
    CreationNewInstance("Creation new instance"),
    CreationNewArray("Creation new array"),
    CheckingCast("Checking case"),
    CheckingInstanceOf("Checking instance of")
}