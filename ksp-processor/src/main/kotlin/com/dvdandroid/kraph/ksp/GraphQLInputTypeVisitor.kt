package com.dvdandroid.kraph.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.writeTo

internal class GraphQLInputTypeVisitor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  private val okBuiltIns: List<KSType>,
) : KSVisitorVoid() {
  private lateinit var ksType: KSType
  private lateinit var packageName: String
  private val objects = hashMapOf<String, KSType>()

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    val qualifiedName = classDeclaration.qualifiedName?.asString() ?: run {
      return logger.error("@GraphQLInputType must target classes with qualified names", classDeclaration)
    }

    if (Modifier.DATA !in classDeclaration.modifiers) {
      return logger.error("@GraphQLInputType cannot target non-data class $qualifiedName", classDeclaration)
    }

    if (classDeclaration.typeParameters.any()) {
      return logger.error("@GraphQLInputType must data classes with no type parameters", classDeclaration)
    }

    ksType = classDeclaration.asType(emptyList())
    packageName = classDeclaration.packageName.asString()

    classDeclaration.getAllProperties()
      .forEach {
        it.accept(this, Unit)
      }

    if (objects.isEmpty()) return

    GraphQLInputTypeFunBuilder(packageName, classDeclaration.simpleName.asString(), objects)
      .build()
      .writeTo(codeGenerator = codeGenerator, aggregating = false)
  }

  override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
    val ksType = property.type.resolve()
    if (ksType in okBuiltIns && !ksType.isMarkedNullable) {
      objects += property.simpleName.asString() to ksType
    }
  }
}