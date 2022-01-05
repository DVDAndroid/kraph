package com.dvdandroid.kraph.ksp

import com.dvdandroid.kraph.ksp.AnnotationProcessor.Companion.asKSClassDeclaration
import com.dvdandroid.kraph.ksp.AnnotationProcessor.Companion.okBuiltIns
import com.dvdandroid.kraph.ksp.annotations.GraphQLFieldIgnore
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.writeTo

internal class GraphQLInputTypeVisitor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
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
    if (property.isAnnotationPresent(GraphQLFieldIgnore::class)) return

    val ksType = property.type.resolve()
    val isEnum = ksType.asKSClassDeclaration().classKind == ClassKind.ENUM_CLASS
    // todo fixme
    val isIterable = "List" in ksType.asKSClassDeclaration().simpleName.asString()
            || "Set" in ksType.asKSClassDeclaration().simpleName.asString()
    if (ksType.makeNotNullable() in okBuiltIns || isEnum || isIterable) {
      objects += property.simpleName.asString() to ksType
    }
  }
}