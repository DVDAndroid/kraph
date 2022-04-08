package com.dvdandroid.kraph.ksp

import com.dvdandroid.kraph.ksp.AnnotationProcessor.Companion.okBuiltIns
import com.dvdandroid.kraph.ksp.AnnotationProcessor.Companion.pResolver
import com.dvdandroid.kraph.ksp.annotations.GraphQLFieldIgnore
import com.dvdandroid.kraph.ksp.annotations.GraphQLType
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.writeTo

internal class GraphQLTypeVisitor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
) : KSVisitorVoid() {
  private lateinit var ksType: KSType
  private val objects = hashMapOf<String, KSType>()

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    val qualifiedName = classDeclaration.qualifiedName?.asString() ?: run {
      return logger.error("@GraphQLType must target classes with qualified names", classDeclaration)
    }

    if (Modifier.DATA !in classDeclaration.modifiers && Modifier.SEALED !in classDeclaration.modifiers) {
      return logger.error("@GraphQLType cannot target non-data and non-sealed class $qualifiedName", classDeclaration)
    }

    if (classDeclaration.typeParameters.any()) {
      return logger.error("@GraphQLType must data classes with no type parameters", classDeclaration)
    }

    ksType = classDeclaration.asType(emptyList())

    classDeclaration.getAllProperties()
      .forEach {
        it.accept(this, Unit)
      }

    if (Modifier.DATA in classDeclaration.modifiers && objects.isEmpty()) return

    GraphQLTypeClassBuilder(classDeclaration, objects, logger)
      .build()
      .writeTo(codeGenerator = codeGenerator, aggregating = false)
  }

  override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
    if (property.isAnnotationPresent(GraphQLFieldIgnore::class)) {
      logger.info("$property is annotated with @GraphQLFieldIgnore, skipping")
      return
    }

    val ksType = property.type.resolve()
    val graphQLType = ksType.asKSClassDeclaration().isAnnotationPresent(GraphQLType::class)
            || property.isAnnotationPresent(GraphQLType::class)
            || property.findOverridee()?.isAnnotationPresent(GraphQLType::class) ?: false

    val isCollection = ksType.asKSClassDeclaration()
      .getAllSuperTypes()
      .toSet()
      .any { it.starProjection() in setOf(pResolver.builtIns.iterableType, pResolver.builtIns.arrayType) }

    val ksClassListArgType = ksType.arguments.firstOrNull()?.type?.resolve()

    val listAndTypeOfGraphQLType = isCollection
            && ksClassListArgType != null
            && ksType.arguments.size == 1
            && (ksClassListArgType.asKSClassDeclaration().isAnnotationPresent(GraphQLType::class)
            || ksClassListArgType.makeNotNullable() in okBuiltIns)

    if (ksType.makeNotNullable() in okBuiltIns || graphQLType || listAndTypeOfGraphQLType) {
      objects += property.simpleName.asString() to (if (listAndTypeOfGraphQLType) {
        ksType.arguments.first().type?.resolve()!!
      } else {
        ksType
      })
    }
  }

  private fun KSType.asKSClassDeclaration() = declaration as KSClassDeclaration
}