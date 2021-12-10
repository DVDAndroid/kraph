package com.dvdandroid.kraph.ksp

import com.dvdandroid.kraph.ksp.annotations.GraphQLType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import me.lazmaid.kraph.Kraph

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
    val symbols = resolver.getSymbolsWithAnnotation(GraphQLType::class.qualifiedName!!)
    val unableToProcess = symbols.filterNot { it.validate() }

    symbols.filter { it is KSClassDeclaration && it.validate() }
      .forEach { it.accept(Visitor(), Unit) }

    return unableToProcess.toList()
  }

  private inner class Visitor : KSVisitorVoid() {
    private lateinit var ksType: KSType
    private lateinit var packageName: String
    private val objects = hashMapOf<String, KSType>()

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      val qualifiedName = classDeclaration.qualifiedName?.asString() ?: run {
        return logger.error("@GraphQLType must target classes with qualified names", classDeclaration)
      }

      if (Modifier.DATA !in classDeclaration.modifiers) {
        return logger.error("@GraphQLType cannot target non-data class $qualifiedName", classDeclaration)
      }

      if (classDeclaration.typeParameters.any()) {
        return logger.error("@GraphQLType must data classes with no type parameters", classDeclaration)
      }

      ksType = classDeclaration.asType(emptyList())
      packageName = classDeclaration.packageName.asString()

      classDeclaration.getAllProperties()
        .forEach {
          it.accept(this, Unit)
        }

      if (objects.isEmpty()) return

      ClassBuilder(packageName, classDeclaration.simpleName.asString(), objects)
        .build()
        .writeTo(codeGenerator = codeGenerator, aggregating = false)
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
      val ksType = property.type.resolve()
      val graphQLType = (ksType.declaration as KSClassDeclaration).isAnnotationPresent(GraphQLType::class)
      if (ksType in okBuiltIns || graphQLType) {
        objects += property.simpleName.asString() to ksType
      }
    }
  }
}


@Suppress("PrivatePropertyName")
class ClassBuilder(
  private val packageName: String,
  private val className: String,
  private val objects: Map<String, KSType>,
) {
  companion object {
    private val field_block_import = ClassName("me.lazmaid.kraph", "FieldBlock")

    private val fun_field = FunSpec.builder("field")
      .addParameter("name", String::class)
      .addParameter(ParameterSpec.builder("alias", String::class.asTypeName().copy(nullable = true))
        .defaultValue("null")
        .build())
      .addParameter(ParameterSpec.builder("args", Map::class.parameterizedBy(String::class, Any::class).copy(nullable = true))
        .defaultValue("null")
        .build())
      .addStatement("fieldBuilder.field(name, alias, args, builder = null)")
      .build()


    private val fun_field_object = FunSpec.builder("fieldObject")
      .addParameter("name", String::class)
      .addParameter(ParameterSpec.builder("alias", String::class.asTypeName().copy(nullable = true))
        .defaultValue("null")
        .build())
      .addParameter(ParameterSpec.builder("args", Map::class.parameterizedBy(String::class, Any::class).copy(nullable = true))
        .defaultValue("null")
        .build())
      .addParameter("builder", field_block_import)
      .addStatement("fieldBuilder.field(name, alias, args, builder)")
      .build()

  }

  fun build(): FileSpec {
    val generatedClassName = "${className}GraphQLBuilder"
    val firstLowerCase = className.replaceFirstChar(Char::lowercase)

    val className1 = ClassName(packageName, generatedClassName)
    val klass = TypeSpec.classBuilder(className1)
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
            .addParameter(ParameterSpec.builder("block", LambdaTypeName.get(
              receiver = className1,
              returnType = Unit::class.asTypeName()
            )).build())
            .addCode(
              CodeBlock.builder().add(
                format = """
                  |fieldObject(name ?: %S) {
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
            val receiver = ClassName(packageName, receiverClassName)
            val extensionMethod = MemberName("$packageName.$receiverClassName.Companion", name, isExtension = true)

            addFunction(FunSpec.builder(name)
              .addParameter(ParameterSpec.builder("block", LambdaTypeName.get(
                receiver = receiver,
                returnType = Unit::class.asTypeName()
              )).build())
              .returns(Unit::class)
              .addStatement(
                "return fieldBuilder.%M(%S, block)",
                extensionMethod,
                name,
              ).build()
            )
          } else {
            addProperty(PropertySpec.builder(name, Unit::class)
              .delegate("lazy { field(%S) }", name)
              .build()
            )
            addFunction(FunSpec.builder(name)
              .addParameter(ParameterSpec.builder("alias", String::class.asTypeName().copy(nullable = true))
                .defaultValue("null")
                .build())
              .addParameter(ParameterSpec.builder("args", Map::class.parameterizedBy(String::class, Any::class).copy(nullable = true))
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

    return FileSpec.builder(packageName, generatedClassName)
      .addType(klass)
      .build()
  }

}