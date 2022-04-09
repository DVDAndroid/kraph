package com.dvdandroid.kraph.ksp

import com.dvdandroid.kraph.ksp.AnnotationProcessor.Companion.genPackageName
import com.dvdandroid.kraph.ksp.AnnotationProcessor.Companion.pResolver
import com.dvdandroid.kraph.ksp.annotations.GraphQLTypeWrapper
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getKotlinClassByName
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName

@Suppress("PrivatePropertyName")
internal class GraphQLWrapperClassBuilder(
  private val classDeclaration: KSClassDeclaration,
  private val logger: KSPLogger,
) {

  fun build(): FileSpec {
    val wrapperAnnotation = classDeclaration.getAnnotationsByType(GraphQLTypeWrapper::class).first()
    val className = wrapperAnnotation.outClassName
    val fieldName = wrapperAnnotation.outFieldName

    val klass = TypeSpec.classBuilder(ClassName(genPackageName, className))
      .addAnnotation(
        AnnotationSpec.builder(Suppress::class)
          .addMember(""""ObjectPropertyName", "RedundantVisibilityModifier", "unused", "RedundantUnitReturnType"""")
          .build()
      )
      .addModifiers(KModifier.DATA)
      .addKdoc("Builder for class [${classDeclaration.qualifiedName?.asString()}]")
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter(fieldName, List::class.asTypeName().parameterizedBy(classDeclaration.toClassName()))
          .build()
      )
      .addProperty(
        PropertySpec.builder(
          fieldName,
          List::class.asTypeName().parameterizedBy(classDeclaration.toClassName())
        ).initializer(fieldName)
          .build()
      )
      .apply {
        pResolver.getKotlinClassByName("kotlinx.serialization.Serializable")?.toClassName()?.let {
          addAnnotation(it)
        }
      }
      .build()

    return FileSpec.builder(genPackageName, className)
      .addType(klass)
      .build()
  }

}