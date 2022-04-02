package com.dvdandroid.kraph.ksp

import com.dvdandroid.kraph.ksp.annotations.GraphQLTypeWrapper
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.writeTo

internal class GraphQLWrapperVisitor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger
) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    val qualifiedName = classDeclaration.qualifiedName?.asString() ?: run {
      return logger.error("@GraphQLTypeWrapper must target classes with qualified names", classDeclaration)
    }

    if (Modifier.DATA !in classDeclaration.modifiers) {
      return logger.error("@GraphQLTypeWrapper cannot target non-data class $qualifiedName", classDeclaration)
    }

    if (classDeclaration.typeParameters.any()) {
      return logger.error("@GraphQLTypeWrapper must data classes with no type parameters", classDeclaration)
    }

    if (!classDeclaration.isAnnotationPresent(GraphQLTypeWrapper::class)) return

    GraphQLWrapperClassBuilder(classDeclaration, logger)
      .build()
      .writeTo(codeGenerator = codeGenerator, aggregating = false)
  }

}
