import com.dvdandroid.kraph.ksp.AnnotationProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test

class ProcessorTest {

  @Rule
  @JvmField
  var temporaryFolder: TemporaryFolder = TemporaryFolder()

  private val classes = SourceFile.kotlin(
    "classes.kt", """import com.dvdandroid.kraph.ksp.annotations.GraphQLFieldIgnore
import com.dvdandroid.kraph.ksp.annotations.GraphQLInputType
import com.dvdandroid.kraph.ksp.annotations.GraphQLType

@GraphQLType
data class User(
  val id: String,
  val name: String,
  val email: MutableList<MyType>? = mutableListOf(),
  @GraphQLFieldIgnore
  val ignored: String,
  override val x: MyType,
) : Address

interface Address {
  @GraphQLType
  val x: MyType

  @GraphQLFieldIgnore
  val y: MyType
    get() = MyType("y")
}

@GraphQLInputType
data class MyType(val z: String)
    """
  )

  @Test
  fun test() {
    compile(classes) {
      assert(exitCode == KotlinCompilation.ExitCode.OK)
    }
  }

  private fun compile(vararg files: SourceFile, assertions: KotlinCompilation.Result.() -> Unit) = KotlinCompilation().apply {
    sources = files.toList()
    symbolProcessorProviders = listOf(AnnotationProcessorProvider())
    inheritClassPath = true
    workingDir = temporaryFolder.root
    kspArgs = hashMapOf("kraph.packageName" to "generated")
  }.compile().assertions()

  private fun KotlinCompilation.Result.sourceFor(fileName: String): String {
    return kspGeneratedSources().find { it.name == fileName }
      ?.readText()
      ?: throw IllegalArgumentException("Could not find file $fileName in ${kspGeneratedSources()}")
  }

  private fun KotlinCompilation.Result.kspGeneratedSources(): List<File> {
    val kspWorkingDir = outputDirectory.parentFile.resolve("ksp")
    val kspGeneratedDir = kspWorkingDir.resolve("sources")
    val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
    val javaGeneratedDir = kspGeneratedDir.resolve("java")
    return kotlinGeneratedDir.walk().toList() + javaGeneratedDir.walk().toList()
  }

}