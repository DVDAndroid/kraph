package com.dvdandroid.kraph.ksp

import com.dvdandroid.kraph.ksp.AnnotationProcessor.Companion.asKSClassDeclaration
import com.dvdandroid.kraph.ksp.AnnotationProcessor.Companion.pResolver
import com.google.devtools.ksp.symbol.ClassKind
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

    val args = objects.map { (k, v) ->
      val isString = v.makeNotNullable() == pResolver.builtIns.stringType
      val isEnum = v.asKSClassDeclaration().classKind == ClassKind.ENUM_CLASS
      val isIterable = "List" in v.asKSClassDeclaration().simpleName.asString()
              || "Set" in v.asKSClassDeclaration().simpleName.asString()
      val isNullable = v.isMarkedNullable

      buildString {
        if (isNullable) {
          append("if ($k == null")
          if (isIterable) {
            append(" || $k.isEmpty()")
          }
          append(") null else ")
        }
        append(""""$k" to $k""")
        append(when {
          isString || isEnum -> ""
          isIterable -> ".map { it.toString() }"
          else -> ".toString()"
        })
      }
    }
    val function = FunSpec.builder("asHashMap")
      .returns(Map::class.parameterizedBy(String::class, Any::class))
      .receiver(ClassName(packageName, className))
      .addStatement("val pairs = listOfNotNull(%L).toTypedArray()", args.joinToString(",\n"))
      .addStatement("return hashMapOf(*pairs)")
      .build()

    return FileSpec.builder(packageName, generatedClassName)
      .addFunction(function)
      .build()
  }

}