package com.dvdandroid.kraph.ksp

import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

@Suppress("PrivatePropertyName")
internal class GraphQLInputTypeFunBuilder(
  private val builtIns: KSBuiltIns,
  private val packageName: String,
  private val className: String,
  private val objects: Map<String, KSType>,
) {
  fun build(): FileSpec {
    val generatedClassName = "${className}GraphQLInputExtensions"

    val args = objects.map { (k, v) ->
      val isString = v.makeNotNullable() == builtIns.stringType
      val isNullable = v.isMarkedNullable

      buildString {
        if (isNullable) {
          append("if ($k == null) null else ")
        }
        append(""""$k" to $k""")
        if (!isString) {
          append(".toString()")
        }
      }
    }
    val function = FunSpec.builder("asHashMap")
      .returns(Map::class.parameterizedBy(String::class, String::class))
      .receiver(ClassName(packageName, className))
      .addStatement("val pairs = listOfNotNull(%L).toTypedArray()", args.joinToString(",\n"))
      .addStatement("return hashMapOf(*pairs)")
      .build()

    return FileSpec.builder(packageName, generatedClassName)
      .addFunction(function)
      .build()
  }

}