package com.dvdandroid.kraph.ksp

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

@Suppress("PrivatePropertyName")
internal class GraphQLInputTypeFunBuilder(
  private val packageName: String,
  private val className: String,
  private val objects: Map<String, KSType>,
) {
  fun build(): FileSpec {
    val generatedClassName = "${className}GraphQLInputExtensions"

    val args = objects.map { (k, _) -> """"$k" to ${k}.toString()""" }
    val function = FunSpec.builder("asHashMap")
      .returns(Map::class.parameterizedBy(String::class, String::class))
      .receiver(ClassName(packageName, className))
      .addStatement("return hashMapOf(%L)", args.joinToString(", "))
      .build()

    return FileSpec.builder(packageName, generatedClassName)
      .addFunction(function)
      .build()
  }

}