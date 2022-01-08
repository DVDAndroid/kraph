package com.dvdandroid.kraph.ksp

import com.dvdandroid.kraph.ksp.AnnotationProcessor.Companion.genPackageName
import com.dvdandroid.kraph.ksp.annotations.GraphQLType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import me.lazmaid.kraph.Kraph

@Suppress("PrivatePropertyName")
internal class GraphQLTypeClassBuilder(
  private val classDeclaration: KSClassDeclaration,
  private val objects: Map<String, KSType>,
  private val logger: KSPLogger,
) {
  companion object {
    private val field_block_import = ClassName("me.lazmaid.kraph", "FieldBlock")

    private val fun_field = FunSpec.builder("field")
      .addParameter("name", String::class)
      .addParameter(ParameterSpec.builder("alias", String::class.asTypeName().copy(nullable = true))
        .defaultValue("null")
        .build())
      .addParameter(ParameterSpec.Companion.builder("args", Map::class.parameterizedBy(String::class, Any::class).copy(nullable = true))
        .defaultValue("null")
        .build())
      .addStatement("fieldBuilder.field(name, alias, args, builder = null)")
      .build()


    private val fun_field_object = FunSpec.builder("fieldObject")
      .addParameter("name", String::class)
      .addParameter(ParameterSpec.builder("alias", String::class.asTypeName().copy(nullable = true))
        .defaultValue("null")
        .build())
      .addParameter(ParameterSpec.Companion.builder("args", Map::class.parameterizedBy(String::class, Any::class).copy(nullable = true))
        .defaultValue("null")
        .build())
      .addParameter("builder", field_block_import)
      .addStatement("fieldBuilder.field(name, alias, args, builder)")
      .build()

  }

  fun build(): FileSpec {
    val className = classDeclaration.simpleName.asString()
    val generatedClassName = "${className}GraphQLBuilder"
    val firstLowerCase = className.replaceFirstChar(Char::lowercase)

    val className1 = ClassName(genPackageName, generatedClassName)
    val klass = TypeSpec.classBuilder(className1)
      .addKdoc("Builder for class [${classDeclaration.qualifiedName?.asString()}]")
      .primaryConstructor(FunSpec.constructorBuilder()
        .addParameter("fieldBuilder", Kraph.FieldBuilder::class, KModifier.PRIVATE)
        .addModifiers(KModifier.PRIVATE)
        .build())
      .addProperty(PropertySpec.builder("fieldBuilder", Kraph.FieldBuilder::class)
        .initializer("fieldBuilder")
        .addModifiers(KModifier.PRIVATE)
        .build())
      .addFunction(fun_field)
      .addFunction(fun_field_object)
      .addType(
        TypeSpec.companionObjectBuilder()
          .addFunction(FunSpec.builder(firstLowerCase)
            .receiver(Kraph.FieldBuilder::class)
            .addParameter(ParameterSpec.builder("name", String::class.asTypeName().copy(nullable = true))
              .defaultValue("null")
              .build()
            )
            .addParameter(ParameterSpec.builder("alias", String::class.asTypeName().copy(nullable = true))
              .defaultValue("null")
              .build())
            .addParameter(ParameterSpec.Companion.builder("args", Map::class.parameterizedBy(String::class, Any::class).copy(nullable = true))
              .defaultValue("null")
              .build())
            .addParameter(ParameterSpec.builder("block", LambdaTypeName.get(
              receiver = className1,
              returnType = Unit::class.asTypeName()
            )).build())
            .addCode(
              CodeBlock.builder().add(
                format = """
                  |fieldObject(name ?: %S, alias, args) {
                  |   %L(this).block()
                  |}
                """.trimMargin(),
                args = arrayOf(firstLowerCase, generatedClassName)
              ).build()
            ).build()
          ).build()
      ).apply {
        objects.forEach { (name, type) ->
          val ksClassDeclaration = type.declaration as KSClassDeclaration
          val isGraphQLType = ksClassDeclaration.isAnnotationPresent(GraphQLType::class)

          if (isGraphQLType) {
            val receiverClassName = "${ksClassDeclaration.simpleName.asString()}GraphQLBuilder"
            val companionObjectFunName = ksClassDeclaration.simpleName.asString().replaceFirstChar(Char::lowercase)

            val receiver = ClassName(genPackageName, receiverClassName)
            val extensionMethod = MemberName("$genPackageName.$receiverClassName.Companion", companionObjectFunName, isExtension = true)

            addFunction(FunSpec.builder(name)
              .addParameter(ParameterSpec.builder("alias", String::class.asTypeName().copy(nullable = true))
                .defaultValue("null")
                .build())
              .addParameter(ParameterSpec.Companion.builder("args", Map::class.parameterizedBy(String::class, Any::class).copy(nullable = true))
                .defaultValue("null")
                .build())
              .addParameter(ParameterSpec.builder("block", LambdaTypeName.get(
                receiver = receiver,
                returnType = Unit::class.asTypeName()
              )).build())
              .returns(Unit::class)
              .addKdoc("Builder for [${classDeclaration.qualifiedName?.asString()}.$name]")
              .addStatement(
                "return fieldBuilder.%M(%S, alias, args, block)",
                extensionMethod,
                name,
              ).build()
            )
          } else {
            addProperty(PropertySpec.builder(name, Unit::class)
              .addKdoc("Builder for [${classDeclaration.qualifiedName?.asString()}.$name]")
              .delegate("lazy { field(%S) }", name)
              .build()
            )
            addFunction(FunSpec.builder(name)
              .addKdoc("Builder for [${classDeclaration.qualifiedName?.asString()}.$name]")
              .addParameter(ParameterSpec.builder("alias", String::class.asTypeName().copy(nullable = true))
                .defaultValue("null")
                .build())
              .addParameter(ParameterSpec.Companion.builder("args", Map::class.parameterizedBy(String::class, Any::class).copy(nullable = true))
                .defaultValue("null")
                .build())
              .returns(Unit::class)
              .addStatement(
                "return field(%S, alias, args)",
                name,
              ).build()
            )
          }
        }
      }.build()

    return FileSpec.builder(genPackageName, generatedClassName)
      .addType(klass)
      .build()
  }

}