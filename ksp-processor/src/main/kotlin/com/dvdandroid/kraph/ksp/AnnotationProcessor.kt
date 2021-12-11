package com.dvdandroid.kraph.ksp

import com.dvdandroid.kraph.ksp.annotations.GraphQLInputType
import com.dvdandroid.kraph.ksp.annotations.GraphQLType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

class AnnotationProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
) : SymbolProcessor {

  private lateinit var builtIns: KSBuiltIns
  private val okBuiltIns by lazy {
    listOf(
      builtIns.stringType,
      builtIns.intType,
      builtIns.booleanType,
      builtIns.doubleType,
      builtIns.floatType,
    )
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    builtIns = resolver.builtIns
    val graphQLTypes = resolver.getSymbolsWithAnnotation(GraphQLType::class.qualifiedName!!)
    val graphQLInputTypes = resolver.getSymbolsWithAnnotation(GraphQLInputType::class.qualifiedName!!)
    val unableToProcess1 = graphQLTypes.filterNot { it.validate() }
    val unableToProcess2 = graphQLInputTypes.filterNot { it.validate() }

    graphQLTypes.filter { it is KSClassDeclaration && it.validate() }
      .forEach { it.accept(GraphQLTypeVisitor(codeGenerator, logger, okBuiltIns), Unit) }

    graphQLInputTypes.filter { it is KSClassDeclaration && it.validate() }
      .forEach { it.accept(GraphQLInputTypeVisitor(codeGenerator, logger, okBuiltIns), Unit) }

    return (unableToProcess1 + unableToProcess2).distinct().toList()
  }

}