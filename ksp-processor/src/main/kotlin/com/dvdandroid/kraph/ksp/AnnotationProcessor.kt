package com.dvdandroid.kraph.ksp

import com.dvdandroid.kraph.ksp.annotations.GraphQLInputType
import com.dvdandroid.kraph.ksp.annotations.GraphQLType
import com.dvdandroid.kraph.ksp.annotations.GraphQLTypeWrapper
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate

class AnnotationProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  private val options: Map<String, String>,
) : SymbolProcessor {

  companion object {
    internal lateinit var pResolver: Resolver
    internal lateinit var genPackageName: String
    internal val okBuiltIns by lazy {
      setOf(
        pResolver.builtIns.stringType,
        pResolver.builtIns.intType,
        pResolver.builtIns.booleanType,
        pResolver.builtIns.doubleType,
        pResolver.builtIns.floatType,
        pResolver.builtIns.shortType,
        pResolver.builtIns.longType,
        pResolver.builtIns.charType,
      )
    }

    internal fun KSType.asKSClassDeclaration() = declaration as KSClassDeclaration
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    options["kraph.packageName"].let {
      requireNotNull(it) { "kraph.packageName is not set" }
      genPackageName = it
    }
    pResolver = resolver

    val graphQLTypes = resolver.getSymbolsWithAnnotation(GraphQLType::class.qualifiedName!!)
    val graphQLInputTypes = resolver.getSymbolsWithAnnotation(GraphQLInputType::class.qualifiedName!!)
    val graphQLTypesWrappers = resolver.getSymbolsWithAnnotation(GraphQLTypeWrapper::class.qualifiedName!!)
    val unableToProcess1 = graphQLTypes.filterNot { it.validate() }
    val unableToProcess2 = graphQLInputTypes.filterNot { it.validate() }
    val unableToProcess3 = graphQLTypesWrappers.filterNot { it.validate() }

    graphQLTypes.filter { it is KSClassDeclaration && it.validate() }
      .forEach { it.accept(GraphQLTypeVisitor(codeGenerator, logger), Unit) }

    graphQLInputTypes.filter { it is KSClassDeclaration && it.validate() }
      .forEach { it.accept(GraphQLInputTypeVisitor(codeGenerator, logger), Unit) }

    graphQLTypesWrappers.filter { it is KSClassDeclaration && it.validate() }
      .forEach { it.accept(GraphQLWrapperVisitor(codeGenerator, logger), Unit) }

    return (unableToProcess1 + unableToProcess2 + unableToProcess3).distinct().toList()
  }

}